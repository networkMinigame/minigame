package networkgame.server;

import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.Direction;
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

    @Test
    @DisplayName("네 참가자 모두 흙길 안에서 입장한다")
    void placesEveryPlayerOnDirt() {
        // given
        PlayerManager manager = new PlayerManager();
        LobbyWalkArea area = LobbyWalkArea.standard();

        // when
        manager.register(100, "첫째");
        manager.register(200, "둘째");
        manager.register(300, "셋째");
        manager.register(400, "넷째");

        // then
        assertThat(manager.players()).hasSize(4)
                .allMatch(player -> area.contains(player.x(), player.y()));
    }

    @Test
    @DisplayName("흙길 사이에서 풀섬을 가로지르는 요청은 마지막 승인 위치를 바꾸지 않는다")
    void keepsLastPositionWhenMoveCrossesGrass() {
        // given
        PlayerManager manager = new PlayerManager();
        manager.register(100, "모카");
        manager.move(100, 430, 590, Direction.DOWN);
        manager.clearMoved();

        // when
        manager.move(100, 310, 590, Direction.LEFT);

        // then
        assertThat(manager.players()).containsExactly(new PlayerInfo(0, "모카", 0, 430, 590, Direction.DOWN));
        assertThat(manager.movedPlayerInfos()).isEmpty();
    }
}
