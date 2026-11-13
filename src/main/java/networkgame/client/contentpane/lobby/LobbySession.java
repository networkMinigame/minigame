package networkgame.client.contentpane.lobby;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Set;
import javax.swing.*;

import networkgame.client.Client;
import networkgame.client.contentpane.lobby.assets.LobbyAssets;
import networkgame.client.input.ArrowKeys;
import networkgame.protocol.ClientMsg;
import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;

/**
 * 로비 창 하나를 띄우고, 정해진 주기마다 내 고양이를 옮기고 다시 그린다.
 * <p>생성자를 포함한 모든 메서드는 EDT에서 부른다. 생성할 때 {@link JFrame}을 만들고, 타이머와 키 입력도 EDT에서 돌고,
 * 서버 메시지도 {@code invokeLater}로 EDT에 넘겨 받아서 {@link LobbyState}를 락 없이 바꾼다.
 */
public final class LobbySession {
    // 약 60fps.
    private static final int FRAME_MILLIS = 16;

    private final JFrame window = new JFrame("미니게임 · 로비");
    private final LobbyAssets assets;
    private final LobbyState state = new LobbyState();
    private final LobbyMovement movement;
    private final Client client;
    private final ClientMsg.Join join;

    /**
     * 창은 {@link #show()}를 불러야 뜬다. 내 고양이는 서버가 {@code WELCOME}을 보낸 뒤에 나타난다.
     *
     * @param client    연결은 됐지만 아직 {@link Client#start}를 부르지 않은 클라이언트. 창을 닫으면 이 세션이 닫는다
     * @param character 고양이 번호. 0 이상, 고양이 수 미만
     */
    public LobbySession(LobbyAssets assets, Client client, String name, int character) {
        this.assets = assets;
        this.client = client;

        int mapWidth = assets.map().getWidth();
        int mapHeight = assets.map().getHeight();
        this.movement = new LobbyMovement(mapWidth, mapHeight);

        join = new ClientMsg.Join(name, character);
    }

    /**
     * 창을 띄우고 서버에 {@code JOIN}을 보낸다. {@value #FRAME_MILLIS}ms마다 내 고양이를 옮기고 다시 그린다.
     * 창을 닫으면 멈추고 연결을 닫는다.
     */
    public void show() {
        LobbyPanel lobby = new LobbyPanel(state, assets);
        configureWindow(lobby);
        ArrowKeys keys = new ArrowKeys(window);

        // 16ms마다 EDT에서 눌린 방향키에 따라 고양이 위치를 갱신하고 화면을 다시 그리도록 요청합니다.
        Timer timer = new Timer(FRAME_MILLIS, _ -> {
            moveLocalPlayer(keys.pressed());
            lobby.renderFrame();
        });
        stopOnClose(timer);
        window.setVisible(true);
        timer.start();

        client.start(message ->
                SwingUtilities.invokeLater(
                        () -> handle(message)));
        client.send(join);
    }

    private void handle(ServerMsg message) {
        switch (message) {
            case ServerMsg.Welcome(int id, List<PlayerInfo> playerInfos) -> {
                state.welcome(id, playerInfos);
            }
            case ServerMsg.Joined(PlayerInfo playerInfo) -> {
                state.save(Player.from(playerInfo));
            }
            // 아래 메시지는 #35 ~ 38에서 처리합니다.
            case ServerMsg.Left ignored -> {}
            case ServerMsg.Moved ignored -> {}
            case ServerMsg.Reject ignored -> {}
            case ServerMsg.Ping ignored -> {}
            case ServerMsg.Chat ignored -> {}
            case ServerMsg.ReadyState ignored -> {}
            case ServerMsg.Start ignored -> {}
        }
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

    private void stopOnClose(Timer timer) {
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                timer.stop();
                client.close();
            }
        });
    }

    private void moveLocalPlayer(Set<Direction> pressed) {
        Player player = state.findLocalPlayer();
        // 1. JOIN > WELCOME > JOINED 까지 수신하고도
        // 고양이가 화면에 나오지 않는 에려가 있었습니다. moveLocalPlayer에 null 검사를 도입하엿습니다.
        if (player == null) {
            return;
        }
        state.save(movement.next(player, pressed));
    }
}
