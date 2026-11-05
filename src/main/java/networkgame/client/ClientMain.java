package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.EnumSet;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import networkgame.client.LobbyState.Direction;
import networkgame.client.LobbyState.Player;

public class ClientMain {
    private static final String DEFAULT_SERVER = "54.79.179.4:12345";
    private static final int LOCAL_PLAYER = 1;
    private static final double MOVE_STEP = 5;

    public static void main(String[] args) {
        String server = args.length == 0 ? DEFAULT_SERVER : args[0];
        try {
            LobbyAssets assets = new LobbyAssets();
            SwingUtilities.invokeLater(() -> showConnect(server, assets));
        } catch (IOException error) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, error.getMessage(),
                    "로비 에셋 오류", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static void showConnect(String server, LobbyAssets assets) {
        FlatLightLaf.setup();
        JFrame window = new JFrame("미니게임 · 접속");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(LobbyStyle.PAPER);
        form.setBorder(BorderFactory.createEmptyBorder(24, 36, 28, 36));

        JLabel title = new JLabel("고양이 마을");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(LobbyStyle.INK);
        form.add(title);
        form.add(new JLabel("캐릭터를 골라 로비를 둘러보세요."));
        form.add(Box.createVerticalStrut(18));

        JTextField address = new JTextField(server);
        JTextField name = new JTextField();
        JComboBox<String> characters = new JComboBox<>();
        for (int i = 1; i <= LobbyAssets.CHARACTER_COUNT; i++) {
            characters.addItem("고양이 " + i);
        }
        addField(form, "서버 주소", address);
        addField(form, "플레이어 이름", name);
        addField(form, "캐릭터", characters);

        JLabel preview = new JLabel();
        Runnable updatePreview = () -> {
            Image sprite = assets.sprite(characters.getSelectedIndex(), Direction.DOWN, false, 0);
            preview.setIcon(new ImageIcon(sprite.getScaledInstance(92, 92, Image.SCALE_SMOOTH)));
        };
        characters.addActionListener(event -> updatePreview.run());
        updatePreview.run();
        form.add(preview);

        JButton enter = new JButton("로비 입장");
        enter.setBackground(LobbyStyle.LEAF);
        enter.setForeground(java.awt.Color.WHITE);
        enter.setAlignmentX(Component.LEFT_ALIGNMENT);
        enter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        enter.addActionListener(event -> {
            if (address.getText().isBlank() || name.getText().isBlank()) {
                JOptionPane.showMessageDialog(window, "서버 주소와 이름을 입력해 주세요.");
                return;
            }
            showLobby(window, assets, name.getText().strip(), characters.getSelectedIndex());
        });
        form.add(enter);
        window.setContentPane(form);
        window.getRootPane().setDefaultButton(enter);
        window.setResizable(false);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        name.requestFocusInWindow();
    }

    private static void addField(JPanel form, String caption, JComponent field) {
        JLabel label = new JLabel(caption);
        label.setLabelFor(field);
        form.add(label);
        form.add(Box.createVerticalStrut(4));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(320, 34));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(field);
        form.add(Box.createVerticalStrut(12));
    }

    private static void showLobby(JFrame window, LobbyAssets assets, String name, int character) {
        LobbyState state = new LobbyState();
        double centerX = assets.map.getWidth() / 2.0;
        double centerY = assets.map.getHeight() / 2.0;
        state.put(new Player(LOCAL_PLAYER, name, character, centerX, centerY, Direction.DOWN, false));
        state.put(new Player(2, "모카", (character + 1) % LobbyAssets.CHARACTER_COUNT,
                centerX - 165, centerY + 75, Direction.RIGHT, false));
        state.put(new Player(3, "구름", (character + 2) % LobbyAssets.CHARACTER_COUNT,
                centerX + 165, centerY + 45, Direction.LEFT, false));

        LobbyPanel lobby = new LobbyPanel(state, assets);
        EnumSet<Direction> pressed = EnumSet.noneOf(Direction.class);
        bind(lobby, pressed, Direction.UP, KeyEvent.VK_UP);
        bind(lobby, pressed, Direction.DOWN, KeyEvent.VK_DOWN);
        bind(lobby, pressed, Direction.LEFT, KeyEvent.VK_LEFT);
        bind(lobby, pressed, Direction.RIGHT, KeyEvent.VK_RIGHT);

        Timer timer = new Timer(16, event -> {
            moveLocalPlayer(state, pressed, assets);
            lobby.renderFrame();
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent event) {
                pressed.clear();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                timer.stop();
            }
        });

        JLabel hint = new JLabel("오프라인 로비   ·   방향키로 이동");
        hint.setOpaque(true);
        hint.setBackground(LobbyStyle.PAPER);
        hint.setForeground(LobbyStyle.INK);
        hint.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JPanel content = new JPanel(new BorderLayout());
        content.add(lobby, BorderLayout.CENTER);
        content.add(hint, BorderLayout.SOUTH);
        window.getRootPane().setDefaultButton(null);
        window.setTitle("미니게임 · 로비");
        window.setContentPane(content);
        window.setResizable(true);
        window.setMinimumSize(new Dimension(620, 380));
        window.pack();
        window.setLocationRelativeTo(null);
        timer.start();
    }

    private static void bind(JComponent component, EnumSet<Direction> pressed,
                             Direction direction, int keyCode) {
        for (boolean release : new boolean[]{false, true}) {
            KeyStroke key = KeyStroke.getKeyStroke(keyCode, 0, release);
            component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, key);
            component.getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (release) {
                        pressed.remove(direction);
                    } else {
                        pressed.add(direction);
                    }
                }
            });
        }
    }

    private static void moveLocalPlayer(LobbyState state, EnumSet<Direction> pressed, LobbyAssets assets) {
        Player player = state.player(LOCAL_PLAYER);
        int dx = (pressed.contains(Direction.RIGHT) ? 1 : 0) - (pressed.contains(Direction.LEFT) ? 1 : 0);
        int dy = (pressed.contains(Direction.DOWN) ? 1 : 0) - (pressed.contains(Direction.UP) ? 1 : 0);
        if (dx == 0 && dy == 0) {
            if (player.walking()) {
                state.move(LOCAL_PLAYER, player.x(), player.y(), player.direction(), false);
            }
            return;
        }
        Direction facing = dx == 0 ? (dy < 0 ? Direction.UP : Direction.DOWN)
                : (dx < 0 ? Direction.LEFT : Direction.RIGHT);
        double step = MOVE_STEP / Math.hypot(dx, dy);
        int size = LobbyStyle.SPRITE_SIZE;
        double x = Math.clamp(player.x() + dx * step, size / 2, assets.map.getWidth() - size / 2);
        double y = Math.clamp(player.y() + dy * step, size + 32, assets.map.getHeight());
        state.move(LOCAL_PLAYER, x, y, facing, x != player.x() || y != player.y());
    }
}
