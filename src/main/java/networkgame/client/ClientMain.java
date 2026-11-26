package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import static networkgame.util.Log.log;
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
    // 이름을 실행 인자로 지정하지 않으면 기본값을 사용한다. 고양이는 아직 고정값이다.
    private static final String PLAYER_NAME = "담비";
    private static final int CHARACTER_INDEX = 0;

    public static void main(String[] args) {
        String playerName = playerName(args);
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
            log("서버 연결 실패 " + HOST + ":" + PORT + " " + e);
            showError("서버 연결", "연결 실패");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new LobbySession(assets, client, playerName, CHARACTER_INDEX).show();
        });
    }

    /** 첫 실행 인자를 이름으로 사용한다. 인자가 없거나 공백이면 기존 기본 이름을 사용한다. */
    static String playerName(String[] args) {
        if (args.length == 0 || args[0].isBlank()) {
            return PLAYER_NAME;
        }
        return args[0].strip();
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
