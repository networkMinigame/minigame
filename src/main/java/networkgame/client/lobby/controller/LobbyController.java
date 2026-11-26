package networkgame.client.lobby.controller;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

import networkgame.client.network.Client;
import networkgame.client.lobby.model.LobbyMovement;
import networkgame.client.lobby.model.LobbyState;
import networkgame.client.lobby.model.Player;
import networkgame.client.lobby.view.LobbyView;
import networkgame.client.lobby.view.assets.LobbyAssets;
import networkgame.client.input.ArrowKeys;
import networkgame.protocol.ClientMsg;
import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.RejectReason;
import networkgame.protocol.ServerMsg;

import static networkgame.util.Log.log;

/**
 * 로비 창 하나를 띄우고, 정해진 주기마다 내 고양이를 옮기고 다시 그린다.
 * <p>생성자를 포함한 모든 메서드는 EDT에서 부른다. 생성할 때 {@link JFrame}을 만들고, 타이머와 키 입력도 EDT에서 돌고,
 * 서버 메시지도 {@code invokeLater}로 EDT에 넘겨 받아서 {@link LobbyState}를 락 없이 바꾼다.
 */
public final class LobbyController {
    // 약 60fps.
    private static final int FRAME_MILLIS = 16;

    // 서버 DispatcherLoop의 tick과 같은 주기. 이보다 자주 보내도 서버는 tick마다 마지막 위치만 전달한다.
    private static final int MOVED_SEND_MILLIS = 33;

    private final JFrame window = new JFrame("미니게임 · 로비");
    private final LobbyAssets assets;
    private final LobbyState state = new LobbyState();
    private final LobbyMovement movement;
    private final Client client;
    private final ClientMsg.Join join;
    private final MoveSender moveSender;

    // 창을 닫았거나 입장 거절·연결 끊김을 처리하기 시작하면 true. 이후 도착한 서버 메시지는 버린다.
    private boolean closing;
    private Timer[] timers;

    /**
     * 창은 {@link #show()}를 불러야 뜬다. 내 고양이는 서버가 {@code WELCOME}을 보낸 뒤에 나타난다.
     *
     * @param client    연결은 됐지만 아직 {@link Client#start}를 부르지 않은 클라이언트. 창을 닫으면 이 컨트롤러가 닫는다
     * @param character 고양이 번호. 0 이상, 고양이 수 미만
     */
    public LobbyController(LobbyAssets assets, Client client, String name, int character) {
        this.assets = assets;
        this.client = client;
        this.movement = new LobbyMovement(assets.map().getWidth(), assets.map().getHeight());
        this.join = new ClientMsg.Join(name, character);
        this.moveSender = new MoveSender(client::send);
    }

    /**
     * 창을 띄우고 서버에 {@code JOIN}을 보낸다. {@value #FRAME_MILLIS}ms마다 내 고양이를 옮기고 다시 그린다.
     * 창을 닫으면 멈추고 연결을 닫는다.
     */
    public void show() {
        LobbyView lobby = new LobbyView(state::findAll, assets);
        configureWindow(lobby);
        ArrowKeys keys = new ArrowKeys(window);

        Timer frameTimer = new Timer(FRAME_MILLIS, event -> updateFrame(keys.pressed(), lobby));
        Timer moveSendTimer = new Timer(MOVED_SEND_MILLIS, event -> sendLocalMove());
        timers = new Timer[] {frameTimer, moveSendTimer};
        stopOnClose();

        window.setVisible(true);
        frameTimer.start();
        moveSendTimer.start();

        connectToServer();
    }

    // pack()은 내용이 들어간 뒤에, 가운데 놓기는 pack()으로 크기가 정해진 뒤에 해야 해서 이 순서를 지킨다.
    private void configureWindow(JPanel content) {
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setContentPane(content);
        window.setResizable(true);
        window.setMinimumSize(new Dimension(620, 380));
        window.pack();
        window.setLocationRelativeTo(null);
    }

    private void stopOnClose() {
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                closing = true;
                stopTimers();
                client.close();
            }
        });
    }

    // 메시지와 연결 끊김 알림은 Reader 스레드에서 오므로 EDT로 넘긴다. JOIN은 Reader·Writer를 띄운 뒤 보낸다.
    private void connectToServer() {
        client.start(
                message -> SwingUtilities.invokeLater(() -> handleIfOpen(message)),
                () -> SwingUtilities.invokeLater(this::connectionLost));
        client.send(join);
    }

    // 내 고양이를 옮기고, MOVED가 끊긴 다른 고양이를 멈춘 뒤 다시 그려 달라고 요청한다.
    private void updateFrame(Set<Direction> pressed, LobbyView lobby) {
        moveLocalPlayer(pressed);
        state.stopIdleRemotePlayers(nowMillis());
        lobby.renderFrame();
    }

    private void moveLocalPlayer(Set<Direction> pressed) {
        Player player = state.findLocalPlayer();
        // WELCOME을 받기 전에는 내 플레이어가 없다.
        if (player == null) {
            return;
        }
        state.save(movement.next(player, pressed));
    }

    private void sendLocalMove() {
        Player localPlayer = state.findLocalPlayer();
        if (localPlayer != null) {
            moveSender.sendIfChanged(localPlayer);
        }
    }

    private void handleIfOpen(ServerMsg message) {
        if (!closing) {
            handle(message);
        }
    }

    /** 서버 메시지를 EDT에서 로비 상태에 반영한다. 화면은 다음 타이머 프레임에 갱신된다. */
    private void handle(ServerMsg message) {
        switch (message) {
            case ServerMsg.Welcome(int id, List<PlayerInfo> playerInfos) -> state.welcome(id, playerInfos);
            case ServerMsg.Joined(PlayerInfo playerInfo) -> state.save(Player.from(playerInfo));
            case ServerMsg.Left(int id) -> state.removeByPlayerId(id);
            case ServerMsg.Moved(int id, double x, double y, Direction direction) -> state.moveRemote(id, x, y, direction, nowMillis());
            case ServerMsg.Reject(RejectReason reason) -> closeOnReject(reason);
            case ServerMsg.Ping ignored -> {} // Reader에서 PONG 응답을 처리한다.
            // 아래 메시지는 아직 로비에서 처리하지 않는다.
            case ServerMsg.Chat ignored -> {}
            case ServerMsg.ReadyState ignored -> {}
            case ServerMsg.Start ignored -> {}
        }
    }

    private void closeOnReject(RejectReason reason) {
        closing = true;
        stopTimers();
        JOptionPane.showMessageDialog(window, rejectMessage(reason), "입장 거절", JOptionPane.WARNING_MESSAGE);
        window.dispose();
    }

    private static String rejectMessage(RejectReason reason) {
        return switch (reason) {
            case FULL -> "로비 정원이 가득 찼습니다.";
            case NAME_TAKEN -> "이미 사용 중인 이름입니다. 이름을 변경한 뒤 다시 접속해 주세요.";
        };
    }

    /** 로컬 종료와 입장 거절은 안내를 생략하고, 예기치 않은 종료에만 선택지를 표시한다. */
    private void connectionLost() {
        if (closing || !window.isDisplayable()) {
            return;
        }
        closing = true;
        stopTimers();
        showReconnectOptions("서버 연결이 끊겼습니다.");
    }

    /** EDT에서 확인 또는 재연결을 선택한다. 실패 상세 내용은 로그에만 남긴다. */
    private void showReconnectOptions(String message) {
        Object[] options = {"확인", "재연결 시도"};
        int selected = JOptionPane.showOptionDialog(window, message, "서버 연결",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (selected == 1 && window.isDisplayable()) {
            reconnect();
        } else {
            window.dispose();
        }
    }

    /** 소켓 연결은 작업 스레드에서 실행하고 새 로비 창 생성은 EDT에서 수행한다. */
    private void reconnect() {
        new SwingWorker<Client, Void>() {
            @Override
            protected Client doInBackground() throws Exception {
                return client.reconnect();
            }

            @Override
            protected void done() {
                try {
                    Client reconnected = get();
                    if (!window.isDisplayable()) {
                        reconnected.close();
                        return;
                    }
                    new LobbyController(assets, reconnected, join.name(), join.character()).show();
                    window.dispose();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    log("재연결 실패 " + error);
                    showReconnectOptions("연결 실패");
                } catch (ExecutionException error) {
                    log("재연결 실패 " + error.getCause());
                    if (window.isDisplayable()) {
                        showReconnectOptions("연결 실패");
                    }
                }
            }
        }.execute();
    }

    /** 연결 종료 후 이동 전송과 화면 갱신을 중단한다. */
    private void stopTimers() {
        if (timers != null) {
            for (Timer timer : timers) {
                timer.stop();
            }
        }
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000;
    }
}
