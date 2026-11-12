package networkgame.server;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.ServerMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherLoopTest {
    @Test
    @DisplayName("먼저 입장한 플레이어는 다음 입장을 알림으로 받고 새 플레이어는 전체 목록을 받는다")
    void handlesJoinsInQueueOrder() {
        // given
        DispatcherLoop dispatcherLoop = new DispatcherLoop();
        var first = new LinkedBlockingQueue<ServerMsg>();
        var second = new LinkedBlockingQueue<ServerMsg>();
        dispatcherLoop.receive(first, new ClientMsg.Join("모카", 0));
        dispatcherLoop.receive(second, new ClientMsg.Join("구름", 1));

        // when
        dispatcherLoop.tick();

        // then
        assertThat(first).hasSize(2);
        assertThat(second).hasSize(1);
        var firstWelcome = (ServerMsg.Welcome) first.remove();
        var firstJoined = (ServerMsg.Joined) first.remove();
        var secondWelcome = (ServerMsg.Welcome) second.remove();
        assertThat(firstWelcome.id()).isZero();
        assertThat(secondWelcome.id()).isEqualTo(1);
        assertThat(firstWelcome.players()).singleElement().satisfies(player -> {
            assertThat(player.id()).isZero();
            assertThat(player.name()).isEqualTo("모카");
            assertThat(player.character()).isZero();
        });
        assertThat(firstJoined.player().id()).isEqualTo(1);
        assertThat(firstJoined.player().name()).isEqualTo("구름");
        assertThat(firstJoined.player().character()).isEqualTo(1);
        assertThat(secondWelcome.players()).containsExactly(firstWelcome.players().getFirst(), firstJoined.player());
    }

    @Test
    @DisplayName("퇴장한 플레이어의 ID를 다음 입장자에게 다시 배정한다")
    void reusesDepartedPlayerId() {
        // given
        DispatcherLoop dispatcherLoop = new DispatcherLoop();
        var first = new LinkedBlockingQueue<ServerMsg>();
        var second = new LinkedBlockingQueue<ServerMsg>();
        var next = new LinkedBlockingQueue<ServerMsg>();
        dispatcherLoop.receive(first, new ClientMsg.Join("모카", 0));
        dispatcherLoop.receive(second, new ClientMsg.Join("구름", 1));
        dispatcherLoop.tick();
        first.clear();
        second.clear();

        // when
        dispatcherLoop.leave(first);
        dispatcherLoop.receive(next, new ClientMsg.Join("별", 2));
        dispatcherLoop.tick();

        // then
        assertThat(first).isEmpty();
        var welcome = (ServerMsg.Welcome) next.remove();
        assertThat(welcome.id()).isZero();
        assertThat(welcome.players()).extracting(player -> player.name()).containsExactly("구름", "별");
        assertThat(second).containsExactly(new ServerMsg.Joined(welcome.players().getLast()));
    }
}
