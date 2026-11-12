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
import networkgame.client.contentpane.lobby.LobbyState.Player;
import networkgame.protocol.Direction;

/** Runs the offline lobby and owns its input and frame timer on the EDT. */
public final class LobbySession {
    private static final int LOCAL_PLAYER = 1;
    private static final double MOVE_STEP = 5;

    private final JFrame window = new JFrame("미니게임 · 로비");
    private final LobbyAssets assets;
    private final LobbyState state = new LobbyState();
    private final EnumSet<Direction> pressed = EnumSet.noneOf(Direction.class);

    public LobbySession(LobbyAssets assets, String name, int character) {
        this.assets = assets;
        double centerX = assets.map.getWidth() / 2.0;
        double centerY = assets.map.getHeight() / 2.0;
        state.save(new Player(LOCAL_PLAYER, name, character, centerX, centerY, Direction.DOWN, false));
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
        int dx = (pressed.contains(Direction.RIGHT) ? 1 : 0) - (pressed.contains(Direction.LEFT) ? 1 : 0);
        int dy = (pressed.contains(Direction.DOWN) ? 1 : 0) - (pressed.contains(Direction.UP) ? 1 : 0);
        if (dx == 0 && dy == 0) {
            if (player.walking()) {
                state.moveTo(LOCAL_PLAYER, player.x(), player.y(), player.direction());
            }
            return;
        }
        Direction facing = dx == 0 ? (dy < 0 ? Direction.UP : Direction.DOWN)
                : (dx < 0 ? Direction.LEFT : Direction.RIGHT);
        double step = MOVE_STEP / Math.hypot(dx, dy);
        int size = LobbyStyle.SPRITE_SIZE;
        double x = Math.clamp(player.x() + dx * step, size / 2, assets.map.getWidth() - size / 2);
        double y = Math.clamp(player.y() + dy * step, size + 32, assets.map.getHeight());
        state.moveTo(LOCAL_PLAYER, x, y, facing);
    }
}
