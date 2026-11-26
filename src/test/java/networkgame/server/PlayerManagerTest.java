package networkgame.server;

import networkgame.protocol.PlayerInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerManagerTest {
    @Test
    @DisplayName("입장 순서대로 서로 다른 고양이를 배정한다")
    void assignsCharactersInOrder() {
        // given
        PlayerManager manager = new PlayerManager();

        // when
        manager.register(100, "첫째");
        manager.register(200, "둘째");
        manager.register(300, "셋째");
        manager.register(400, "넷째");

        // then
        assertThat(manager.players()).extracting(PlayerInfo::character).containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("퇴장으로 비워진 첫 고양이 번호를 재사용한다")
    void reusesAvailableCharacter() {
        // given
        PlayerManager manager = new PlayerManager();
        manager.register(100, "첫째");
        manager.register(200, "둘째");
        manager.register(300, "셋째");
        manager.remove(200);

        // when
        PlayerInfo newcomer = manager.register(400, "넷째");

        // then
        assertThat(newcomer.character()).isEqualTo(1);
    }

    @Test
    @DisplayName("새 참가자가 들어와도 기존 참가자의 고양이는 바뀌지 않는다")
    void preservesExistingCharacters() {
        // given
        PlayerManager manager = new PlayerManager();
        manager.register(100, "첫째");
        PlayerInfo existing = manager.register(200, "둘째");
        manager.remove(100);

        // when
        manager.register(300, "셋째");

        // then
        assertThat(manager.players()).contains(existing);
    }
}
