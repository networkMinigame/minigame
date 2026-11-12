package networkgame.client.contentpane.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.client.contentpane.lobby.LobbyState.Player;
import networkgame.protocol.Direction;

class LobbyStateTest {
    @Test
    @DisplayName("같은 캐릭터를 고른 플레이어도 서로 다른 ID로 로비에 표시된다")
    void addsPlayersById() {
        // given
        LobbyState state = new LobbyState();
        Player player = new Player(7, "모카", 0, 100, 200, Direction.DOWN, false);
        Player guest = new Player(8, "구름", 0, 200, 300, Direction.LEFT, false);
        state.save(player);

        // when
        state.save(guest);

        // then
        assertThat(state.findAll()).containsExactly(player, guest);
    }

    @Test
    @DisplayName("플레이어가 나가면 로비 목록에서 사라진다")
    void removesPlayer() {
        // given
        LobbyState state = new LobbyState();
        Player player = new Player(7, "모카", 0, 100, 200, Direction.DOWN, false);
        state.save(player);

        // when
        state.removeByPlayerId(player.playerId());

        // then
        assertThat(state.findAll()).isEmpty();
    }

    @Test
    @DisplayName("플레이어가 움직이면 위치와 방향이 바뀌고 걷는 그림이 된다")
    void movesPlayer() {
        // given
        LobbyState state = new LobbyState();
        Player player = new Player(7, "모카", 0, 100, 200, Direction.DOWN, false);
        state.save(player);

        // when
        state.moveTo(player.playerId(), 110, 200, Direction.RIGHT);

        // then
        assertThat(state.findAll()).containsExactly(new Player(7, "모카", 0, 110, 200, Direction.RIGHT, true));
    }

    @Test
    @DisplayName("같은 좌표로 옮기면 방향은 바뀌고 멈춘 그림이 된다")
    void stopsWhenPositionUnchanged() {
        // given
        Player walking = new Player(7, "모카", 0, 100, 200, Direction.RIGHT, true);

        // when
        Player stopped = walking.moveTo(100, 200, Direction.UP);

        // then
        assertThat(stopped).isEqualTo(new Player(7, "모카", 0, 100, 200, Direction.UP, false));
    }

    @Test
    @DisplayName("목록에 없는 플레이어를 옮기면 아무도 생기지 않는다")
    void ignoresUnknownPlayer() {
        // given
        LobbyState state = new LobbyState();

        // when
        state.moveTo(7, 110, 200, Direction.RIGHT);

        // then
        assertThat(state.findAll()).isEmpty();
    }
}
