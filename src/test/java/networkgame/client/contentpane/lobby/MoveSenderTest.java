package networkgame.client.contentpane.lobby;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoveSenderTest {

    private final List<ClientMsg> sent = new ArrayList<>();
    private final MoveSender moveSender = new MoveSender(sent::add);

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
        moveSender.sendIfChanged(player.stop());

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
        moveSender.sendIfChanged(player.moveTo(300, 400, Direction.LEFT));

        // then
        assertThat(sent).containsExactly(new ClientMsg.Move(300, 400, Direction.LEFT));
    }
}