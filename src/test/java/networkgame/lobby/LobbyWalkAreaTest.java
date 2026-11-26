package networkgame.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LobbyWalkAreaTest {
    @Test
    @DisplayName("중앙 흙길 안의 두 지점 사이를 대각선으로 통과할 수 있다")
    void allowsDiagonalOnDirt() {
        // given
        LobbyWalkArea area = LobbyWalkArea.standard();

        // when
        boolean allowed = area.canMove(836, 470, 826.5, 460.5);

        // then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("목적지가 흙길이어도 중간에 풀섬이 있으면 통과할 수 없다")
    void rejectsCrossingGrassIsland() {
        // given
        LobbyWalkArea area = LobbyWalkArea.standard();
        double y = 590;

        // when
        boolean allowed = area.canMove(310, y, 430, y);

        // then
        assertThat(area.contains(310, y)).isTrue();
        assertThat(area.contains(430, y)).isTrue();
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("원본 지도 밖의 좌표에는 이동할 수 없다")
    void rejectsOutsideMap() {
        // given
        LobbyWalkArea area = LobbyWalkArea.standard();

        // when
        boolean allowed = area.canMove(836, 470, -0.1, 470);

        // then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("유효한 지도 좌표가 아니면 이동할 수 없다")
    void rejectsNaN() {
        // given
        LobbyWalkArea area = LobbyWalkArea.standard();

        // when
        boolean allowed = area.canMove(836, 470, Double.NaN, 470);

        // then
        assertThat(allowed).isFalse();
    }
}
