package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import networkgame.client.contentpane.lobby.LobbyAssets;
import networkgame.client.contentpane.lobby.LobbySession;

public class ClientMain {
    private static final String PLAYER_NAME = "플레이어";
    private static final int CHARACTER_INDEX = 0;

    public static void main(String[] args) {
        try {
            LobbyAssets assets = new LobbyAssets();
            SwingUtilities.invokeLater(() -> {
                FlatLightLaf.setup();
                new LobbySession(assets, PLAYER_NAME, CHARACTER_INDEX).show();
            });
        } catch (IOException error) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, error.getMessage(),
                    "로비 에셋 오류", JOptionPane.ERROR_MESSAGE));
        }
    }
}
