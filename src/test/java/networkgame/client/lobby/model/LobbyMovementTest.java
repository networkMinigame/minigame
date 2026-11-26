package networkgame.client.lobby.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.Direction;

class LobbyMovementTest {
    @Test
    @DisplayName("오른쪽 키를 누르면 오른쪽으로 한 걸음 가고 오른쪽을 보며 걷는다")
    void movesRight() {
        // given
        LobbyMovement movement = new LobbyMovement(LobbyWalkArea.standard());
        Player player = new Player(1, "모카", 0, 500, 500, Direction.DOWN, false);

        // when
        Player next = movement.next(player, EnumSet.of(Direction.RIGHT));

        // then
        assertThat(next).isEqualTo(new Player(1, "모카", 0, 500 + LobbyMovement.STEP, 500, Direction.RIGHT, true));
    }

    @Test
    @DisplayName("대각선으로 가도 상하좌우와 같은 거리만 가고 좌우 방향을 본다")
    void movesDiagonallyAtSameSpeed() {
        // given
        LobbyMovement movement = new LobbyMovement(LobbyWalkArea.standard());
        Player player = new Player(1, "모카", 0, 500, 500, Direction.DOWN, false);

        // when
        Player next = movement.next(player, EnumSet.of(Direction.RIGHT, Direction.DOWN));

        // then
        double distance = Math.hypot(next.x() - player.x(), next.y() - player.y());
        assertThat(distance).isCloseTo(LobbyMovement.STEP, within(1e-9));
        assertThat(next.x()).isGreaterThan(player.x());
        assertThat(next.y()).isGreaterThan(player.y());
        assertThat(next.direction()).isEqualTo(Direction.RIGHT);
    }

    @Test
    @DisplayName("반대 방향 키를 같이 누르면 그 자리에서 바라보던 방향 그대로 멈춘다")
    void stopsWhenOppositeKeysCancel() {
        // given
        LobbyMovement movement = new LobbyMovement(LobbyWalkArea.standard());
        Player player = new Player(1, "모카", 0, 500, 500, Direction.UP, true);

        // when
        Player next = movement.next(player, EnumSet.of(Direction.LEFT, Direction.RIGHT));

        // then
        assertThat(next).isEqualTo(new Player(1, "모카", 0, 500, 500, Direction.UP, false));
    }

    @Test
    @DisplayName("키를 모두 떼면 그 자리에서 바라보던 방향 그대로 멈춘다")
    void stopsWhenNoKeyPressed() {
        // given
        LobbyMovement movement = new LobbyMovement(LobbyWalkArea.standard());
        Player player = new Player(1, "모카", 0, 500, 500, Direction.LEFT, true);

        // when
        Player next = movement.next(player, EnumSet.noneOf(Direction.class));

        // then
        assertThat(next).isEqualTo(new Player(1, "모카", 0, 500, 500, Direction.LEFT, false));
    }

    @Test
    @DisplayName("대각선 이동이 흙길 밖으로 나가면 옆으로도 움직이지 않는다")
    void stopsAtDirtBoundary() {
        // given
        LobbyMovement movement = new LobbyMovement(LobbyWalkArea.standard());
        Player player = new Player(1, "모카", 0, 836, 294, Direction.DOWN, true);

        // when
        Player next = movement.next(player, EnumSet.of(Direction.UP, Direction.RIGHT));

        // then
        assertThat(next).isEqualTo(new Player(1, "모카", 0, 836, 294, Direction.RIGHT, false));
    }
}
