package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
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
import javax.swing.SwingUtilities;

import networkgame.client.LobbyState.Direction;

final class ClientWindow {
    private static final String DEFAULT_SERVER = "54.79.179.4:12345";

    static void launch(String[] args) {
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
            new LobbySession(window, assets, name.getText().strip(), characters.getSelectedIndex()).show();
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
}
