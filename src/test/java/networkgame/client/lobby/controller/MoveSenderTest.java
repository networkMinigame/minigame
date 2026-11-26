package networkgame.client.lobby.controller;

import networkgame.client.lobby.model.Player;
import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.ClientMsg;
import networkgame.protocol.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoveSenderTest {

    private final List<ClientMsg> sent = new ArrayList<>();
    private final MoveSender moveSender = new MoveSender(sent::add, LobbyWalkArea.standard());

    @Test
    @DisplayName("처음 넌긴 위치는 MOVE로 보낸다")
    void sendsFirstPosition() {
        // given
        Player player = new Player(
                0,
                "모카",
                0,
                300,
                400,
                Direction.DOWN,
                false
        );

        // when
        moveSender.sendIfChanged(player);

        // then
        assertThat(sent).containsExactly(new ClientMsg.Move(300, 400, Direction.DOWN));
    }

    @Test
    @DisplayName("위치와 방향이 그대로면 다시 보내지 않는다")
    void skipsUnchangedPosition() {
        // given
        Player player = new Player(0, "모카", 0, 300, 400, Direction.DOWN, false);
        moveSender.sendIfChanged(player);
        sent.clear();

        // when
        moveSender.sendIfChanged(new Player(0, "모카", 0, 300, 400, Direction.DOWN, false));

        // then
        assertThat(sent).isEmpty();
    }

    @Test
    @DisplayName("벽 끝에서 방향만 바뀌어도 MOVE로 보낸다")
    void sendsDirectionOnlyChange() {
        // given
        Player player = new Player(0, "모카", 0, 300, 400, Direction.DOWN, false);
        moveSender.sendIfChanged(player);
        sent.clear();

        // when
        moveSender.sendIfChanged(new Player(0, "모카", 0, 300, 400, Direction.LEFT, false));

        // then
        assertThat(sent).containsExactly(new ClientMsg.Move(300, 400, Direction.LEFT));
    }

    @Test
    @DisplayName("흙길 모서리에서 방향을 바꾸면 서버에 경유 위치를 먼저 보낸다")
    void sendsWaypointAtCorner() {
        // given
        List<ClientMsg> messages = new ArrayList<>();
        MoveSender sender = new MoveSender(messages::add, LobbyWalkArea.standard());
        Player start = new Player(0, "모카", 0, 407.25, 571.5, Direction.RIGHT, false);
        Player corner = new Player(0, "모카", 0, 412.25, 571.5, Direction.RIGHT, true);
        Player next = new Player(0, "모카", 0, 412.25, 576.5, Direction.DOWN, true);
        sender.sendIfChanged(start);
        messages.clear();

        // when
        sender.sendWaypointIfNeeded(corner, next);

        // then
        assertThat(messages).containsExactly(new ClientMsg.Move(412.25, 571.5, Direction.RIGHT));
    }
}
