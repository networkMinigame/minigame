package networkgame.client.contentpane.lobby.assets;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.protocol.Direction;

class LobbyAssetLoaderTest {
    @Test
    @DisplayName("리소스에서 지도와 고양이 4마리의 모든 방향 그림을 읽는다")
    void loadsEveryCatInEveryDirection() throws IOException {
        // given
        int catCount = 4;

        // when
        LobbyAssets assets = LobbyAssetLoader.load();

        // then
        assertThat(assets.map()).isNotNull();
        for (int character = 0; character < catCount; character++) {
            for (Direction direction : Direction.values()) {
                assertThat(assets.sprite(character, direction, false, 0)).isNotNull();
                assertThat(assets.sprite(character, direction, true, 15)).isNotNull();
            }
        }
    }
}
