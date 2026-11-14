package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import networkgame.client.contentpane.lobby.LobbySession;
import networkgame.client.contentpane.lobby.assets.LobbyAssetLoader;
import networkgame.client.contentpane.lobby.assets.LobbyAssets;

/**
 * 클라이언트 실행 진입점. 로비 그림을 읽고 서버에 연결한 뒤 로비 창을 띄운다.
 */
public class ClientMain {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    // 접속 화면이 없어서 이름과 고양이는 아직 고정값이다.
    private static final String PLAYER_NAME = "담비";
    private static final int CHARACTER_INDEX = 0;

    public static void main(String[] args) {
        LobbyAssets assets;
        try {
            assets = LobbyAssetLoader.load();
        } catch (IOException e) {
            showError("로비 에셋 오류", e.getMessage());
            return;
        }

        Client client;
        try {
            client = new Client(HOST, PORT);
        } catch (IOException e) {
            showError("서버 연결 실패", HOST + ":" + PORT + "\n" + e.getMessage());
            return;
        }

        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new LobbySession(assets, client, PLAYER_NAME, CHARACTER_INDEX).show();
        });
    }

    // 대화상자도 Swing 화면이라 EDT에서 띄운다.
    private static void showError(String title, String message) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(
                        null,
                        message,
                        title,
                        JOptionPane.ERROR_MESSAGE
                )
        );
    }
}
