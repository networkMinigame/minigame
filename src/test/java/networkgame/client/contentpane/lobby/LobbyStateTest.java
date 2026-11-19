package networkgame.client.contentpane.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.protocol.Direction;
import networkgame.protocol.MessageCodec;
import networkgame.protocol.ServerMsg;

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

    @Test
    @DisplayName("WELCOME을 받기 전에는 내 플레이어가 없다")
    void hasNoLocalPlayerBeforeWelcome() {
        // given
        LobbyState state = new LobbyState();

        // when
        Player localPlayer = state.findLocalPlayer();

        // then
        assertThat(localPlayer).isNull();
    }

    @Test
    @DisplayName("JOINED를 받으면 새 접속자가 로비에 생긴다")
    void addsJoinedPlayer() {
        // given
        LobbyState state = new LobbyState();
        ServerMsg.Joined joined = (ServerMsg.Joined) MessageCodec.decodeServer(
                "{\"type\":\"JOINED\",\"player\":"
                        + "{\"id\":2,\"name\":\"보리\",\"character\":1,\"x\":1076.0,\"y\":470.0,\"direction\":\"DOWN\"}}");

        // when
        state.save(Player.from(joined.player()));

        // then
        assertThat(state.findAll()).containsExactly(new Player(2, "보리", 1, 1076, 470, Direction.DOWN, false));
    }

    @Test
    @DisplayName("WELCOME을 받으면 접속자 전원이 로비에 생기고 받은 id가 내 플레이어가 된다")
    void welcomesPlayers() {
        // given
        LobbyState state = new LobbyState();
        ServerMsg.Welcome welcome = (ServerMsg.Welcome) MessageCodec.decodeServer(
                "{\"type\":\"WELCOME\",\"id\":1,\"players\":["
                        + "{\"id\":0,\"name\":\"모카\",\"character\":2,\"x\":836.0,\"y\":470.0,\"direction\":\"DOWN\"},"
                        + "{\"id\":1,\"name\":\"구름\",\"character\":0,\"x\":956.0,\"y\":470.0,\"direction\":\"DOWN\"}]}");

        // when
        state.welcome(welcome.id(), welcome.players());

        // then
        assertThat(state.findAll()).containsExactly(
                new Player(0, "모카", 2, 836, 470, Direction.DOWN, false),
                new Player(1, "구름", 0, 956, 470, Direction.DOWN, false));
        assertThat(state.findLocalPlayer()).isEqualTo(new Player(1, "구름", 0, 956, 470, Direction.DOWN, false));
    }
}
