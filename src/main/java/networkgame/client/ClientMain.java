package networkgame.client;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import networkgame.client.contentpane.lobby.LobbySession;
import networkgame.client.contentpane.lobby.assets.LobbyAssetLoader;
import networkgame.client.contentpane.lobby.assets.LobbyAssets;

/**
 * 클라이언트 실행 진입점. 로비 그림을 읽은 뒤 로비 창을 띄운다.
 */
public class ClientMain {
    // 접속 화면이 없어서 이름과 고양이는 아직 고정값이다.
    private static final String PLAYER_NAME = "플레이어";
    private static final int CHARACTER_INDEX = 0;

    public static void main(String[] args) {
        try {
            // 그림 수백 장을 읽는 데 시간이 걸려서 EDT로 넘기기 전에 main 스레드에서 읽는다.
            LobbyAssets assets = LobbyAssetLoader.load();
            SwingUtilities.invokeLater(() -> {
                // Look and Feel은 화면 부품을 만들기 전에 정해야 해서 로비 창보다 먼저 설정한다.
                FlatLightLaf.setup();
                new LobbySession(assets, PLAYER_NAME, CHARACTER_INDEX).show();
            });
        } catch (IOException error) {
            // 대화상자도 Swing 화면이라 EDT에서 띄운다.
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, error.getMessage(),
                    "로비 에셋 오류", JOptionPane.ERROR_MESSAGE));
        }
    }
}
