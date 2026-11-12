package networkgame.client.contentpane.lobby;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import networkgame.client.contentpane.lobby.assets.LobbyAssets;
import networkgame.client.input.ArrowKeys;
import networkgame.protocol.Direction;

/**
 * 로비 창 하나를 띄우고, 정해진 주기마다 내 고양이를 옮기고 다시 그린다.
 * <p>생성자를 포함한 모든 메서드는 EDT에서 부른다. 생성할 때 {@link JFrame}을 만들고, 타이머와 키 입력도 EDT에서 돌아서
 * {@link LobbyState}를 락 없이 바꾼다.
 */
public final class LobbySession {
    // 오프라인이라 내 playerId를 고정해 둔다. 서버에 연결하면 WELCOME으로 받은 id로 바뀐다.
    private static final int LOCAL_PLAYER = 1;
    // 약 60fps.
    private static final int FRAME_MILLIS = 16;

    private final JFrame window = new JFrame("미니게임 · 로비");
    private final LobbyAssets assets;
    private final LobbyState state = new LobbyState();
    private final LobbyMovement movement;

    /**
     * 내 고양이를 지도 가운데에 아래를 보고 서 있는 상태로 넣는다. 창은 {@link #show()}를 불러야 뜬다.
     *
     * @param character 고양이 번호. 0 이상, 고양이 수 미만
     */
    public LobbySession(LobbyAssets assets, String name, int character) {
        this.assets = assets;
        int mapWidth = assets.map().getWidth();
        int mapHeight = assets.map().getHeight();
        this.movement = new LobbyMovement(mapWidth, mapHeight);
        state.save(new Player(LOCAL_PLAYER, name, character, mapWidth / 2.0, mapHeight / 2.0, Direction.DOWN, false));
    }

    /** 창을 띄우고 {@value #FRAME_MILLIS}ms마다 내 고양이를 옮기고 다시 그린다. 창을 닫으면 멈춘다. */
    public void show() {
        LobbyPanel lobby = new LobbyPanel(state, assets);
        configureWindow(lobby);
        ArrowKeys keys = new ArrowKeys(window);

        // 16ms마다 EDT에서 눌린 방향키에 따라 고양이 위치를 갱신하고 화면을 다시 그리도록 요청합니다.
        Timer timer = new Timer(FRAME_MILLIS, event -> {
            moveLocalPlayer(keys.pressed());
            lobby.renderFrame();
        });
        stopOnClose(timer);
        window.setVisible(true);
        timer.start();
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
            }
        });
    }

    private void moveLocalPlayer(Set<Direction> pressed) {
        Player player = state.findByPlayerId(LOCAL_PLAYER);
        state.save(movement.next(player, pressed));
    }
}
