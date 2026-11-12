package networkgame.client.contentpane.lobby;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import networkgame.client.KeyCommandInvoker;
import networkgame.client.contentpane.lobby.assets.LobbyAssets;
import networkgame.protocol.Direction;

/** Runs the offline lobby and owns its input and frame timer on the EDT. */
public final class LobbySession {
    private static final int LOCAL_PLAYER = 1;

    private final JFrame window = new JFrame("미니게임 · 로비");
    private final LobbyAssets assets;
    private final LobbyState state = new LobbyState();
    private final LobbyMovement movement;
    private final EnumSet<Direction> pressed = EnumSet.noneOf(Direction.class);

    public LobbySession(LobbyAssets assets, String name, int character) {
        this.assets = assets;
        int mapWidth = assets.map().getWidth();
        int mapHeight = assets.map().getHeight();
        this.movement = new LobbyMovement(mapWidth, mapHeight);
        state.save(new Player(LOCAL_PLAYER, name, character, mapWidth / 2.0, mapHeight / 2.0, Direction.DOWN, false));
    }

    public void show() {
        LobbyPanel lobby = new LobbyPanel(state, assets);
        configureWindow(createContentPane(lobby));

        Timer timer = new Timer(16, event -> {
            moveLocalPlayer();
            lobby.renderFrame();
        });
        installControls(timer);
        window.setVisible(true);
        timer.start();
    }

    private JPanel createContentPane(LobbyPanel lobby) {
        JPanel content = new JPanel(new BorderLayout());
        content.add(lobby, BorderLayout.CENTER);
        content.add(createHint(), BorderLayout.SOUTH);
        return content;
    }

    private JLabel createHint() {
        JLabel hint = new JLabel("오프라인 로비   ·   방향키로 이동");
        hint.setOpaque(true);
        hint.setBackground(LobbyStyle.PAPER);
        hint.setForeground(LobbyStyle.INK);
        hint.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return hint;
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

    private void installControls(Timer timer) {
        KeyCommandInvoker invoker = new KeyCommandInvoker();
        bindDirection(invoker, KeyEvent.VK_UP, Direction.UP);
        bindDirection(invoker, KeyEvent.VK_DOWN, Direction.DOWN);
        bindDirection(invoker, KeyEvent.VK_LEFT, Direction.LEFT);
        bindDirection(invoker, KeyEvent.VK_RIGHT, Direction.RIGHT);

        KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyEventDispatcher dispatcher = event -> window.isFocused() && invoker.dispatchKeyEvent(event);
        focus.addKeyEventDispatcher(dispatcher);
        WindowAdapter lifecycle = new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                pressed.clear();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                focus.removeKeyEventDispatcher(dispatcher);
                timer.stop();
            }
        };
        window.addWindowFocusListener(lifecycle);
        window.addWindowListener(lifecycle);
    }

    private void bindDirection(KeyCommandInvoker invoker, int keyCode, Direction direction) {
        invoker.bind(keyCode, () -> pressed.add(direction), () -> pressed.remove(direction));
    }

    private void moveLocalPlayer() {
        Player player = state.findByPlayerId(LOCAL_PLAYER);
        state.save(movement.next(player, pressed));
    }
}
