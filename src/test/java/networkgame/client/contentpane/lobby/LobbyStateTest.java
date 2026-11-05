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
        state.put(player);

        // when
        state.put(guest);

        // then
        assertThat(state.players()).containsExactly(player, guest);
    }

    @Test
    @DisplayName("플레이어가 나가면 로비 목록에서 사라진다")
    void removesPlayer() {
        // given
        LobbyState state = new LobbyState();
        Player player = new Player(7, "모카", 0, 100, 200, Direction.DOWN, false);
        state.put(player);

        // when
        state.remove(player.playerId());

        // then
        assertThat(state.players()).isEmpty();
    }

    @Test
    @DisplayName("플레이어가 움직이면 위치와 방향이 바뀐다")
    void movesPlayer() {
        // given
        LobbyState state = new LobbyState();
        Player player = new Player(7, "모카", 0, 100, 200, Direction.DOWN, false);
        state.put(player);

        // when
        state.move(player.playerId(), 110, 200, Direction.RIGHT, true);

        // then
        assertThat(state.players()).containsExactly(new Player(7, "모카", 0, 110, 200, Direction.RIGHT, true));
    }
}
