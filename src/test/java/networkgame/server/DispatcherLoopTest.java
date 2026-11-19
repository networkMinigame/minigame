package networkgame.server;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;
import networkgame.server.session.Session;
import java.net.Socket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherLoopTest {
    private final SessionManager sessions = new SessionManager();
    private final DispatcherLoop dispatcherLoop = new DispatcherLoop(sessions);

    @Test
    @DisplayName("첫 참가자는 자기 정보만 담긴 WELCOME을 받는다")
    void sendsWelcomeToFirstPlayer() {
        // given
        List<ServerMsg> first = connect(100);

        // when
        join(100, "모카", 2);

        // then
        assertThat(first).singleElement().isInstanceOfSatisfying(ServerMsg.Welcome.class, welcome -> {
            assertThat(welcome.id()).isZero();
            assertThat(welcome.players()).singleElement().satisfies(player -> {
                assertThat(player.id()).isEqualTo(welcome.id());
                assertThat(player.name()).isEqualTo("모카");
                assertThat(player.character()).isEqualTo(2);
            });
        });
    }

    @Test
    @DisplayName("두 번째 참가자는 두 명의 입장자 목록을 WELCOME으로 받는다")
    void sendsWelcomeToSecondPlayer() {
        // given
        connect(0);
        join(0, "모카", 0);
        List<ServerMsg> newcomer = connect(1);

        // when
        join(1, "구름", 1);

        // then
        assertThat(newcomer).singleElement().isInstanceOfSatisfying(ServerMsg.Welcome.class, welcome -> {
            assertThat(welcome.id()).isEqualTo(1);
            assertThat(welcome.players()).extracting(PlayerInfo::name).containsExactly("모카", "구름");
        });
    }

    @Test
    @DisplayName("기존 참가자는 새 참가자의 정보를 JOINED로 받는다")
    void notifiesExistingPlayerOfJoin() {
        // given
        List<ServerMsg> existing = connect(0);
        join(0, "모카", 0);
        existing.clear();
        connect(1);

        // when
        join(1, "구름", 1);

        // then
        assertThat(existing).singleElement().isInstanceOfSatisfying(ServerMsg.Joined.class, joined -> {
            assertThat(joined.player().id()).isEqualTo(1);
            assertThat(joined.player().name()).isEqualTo("구름");
            assertThat(joined.player().character()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("퇴장한 참가자는 다음 WELCOME의 입장자 목록에서 빠진다")
    void removesDepartedPlayerFromRoster() {
        // given
        connect(0);
        join(0, "모카", 0);
        connect(1);
        join(1, "구름", 1);
        leave(0);
        List<ServerMsg> newcomer = connect(2);

        // when
        join(2, "별", 2);

        // then
        assertThat(newcomer).singleElement().isInstanceOfSatisfying(ServerMsg.Welcome.class,
                welcome -> assertThat(welcome.players()).extracting(PlayerInfo::name)
                        .containsExactly("구름", "별"));
    }

    @Test
    @DisplayName("퇴장한 연결에는 이후 입장 알림을 보내지 않는다")
    void skipsDepartedConnectionWhenNotifyingJoin() {
        // given
        List<ServerMsg> departed = connect(0);
        join(0, "모카", 0);
        departed.clear();
        leave(0);
        connect(1);

        // when
        join(1, "구름", 1);

        // then
        assertThat(departed).isEmpty();
    }

    @Test
    @DisplayName("퇴장으로 비워진 ID를 새 참가자에게 배정한다")
    void reusesDepartedPlayerId() {
        // given
        connect(0);
        join(0, "모카", 0);
        connect(1);
        join(1, "구름", 1);
        leave(0);
        List<ServerMsg> newcomer = connect(2);

        // when
        join(2, "별", 2);

        // then
        assertThat(newcomer).singleElement().isInstanceOfSatisfying(ServerMsg.Welcome.class,
                welcome -> assertThat(welcome.id()).isZero());
    }

    private List<ServerMsg> connect(long sessionId) {
        List<ServerMsg> responses = new ArrayList<>();
        sessions.register(sessionId, new Session(new Socket(), 0L, sessions, new DispatcherLoop(sessions)) {
            @Override
            public void send(ServerMsg message) {
                responses.add(message);
            }

            @Override
            public void close() {
            }
        });
        return responses;
    }

    private void join(long sessionId, String name, int character) {
        dispatcherLoop.receive(sessionId, new ClientMsg.Join(name, character));
        dispatcherLoop.tick();
    }

    private void leave(long sessionId) {
        dispatcherLoop.leave(sessionId);
        dispatcherLoop.tick();
    }
}
