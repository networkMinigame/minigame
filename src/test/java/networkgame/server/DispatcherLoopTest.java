package networkgame.server;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.Direction;
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
    private final SessionManager sessionManager = new SessionManager();
    private final DispatcherLoop dispatcherLoop = new DispatcherLoop(sessionManager);

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

    @Test
    @DisplayName("MOVE를 받은 tick에 다른 입장자에게만 MOVED를 보낸다")
    void broadcastsMovedToOthersOnTick() {
        // given
        List<ServerMsg> mover = connect(0);
        join(0, "모카", 0);
        List<ServerMsg> other = connect(1);
        join(1, "구름", 1);
        mover.clear();
        other.clear();

        // when
        move(0, 300, 400, Direction.LEFT);

        // then
        assertThat(other).containsExactly(new ServerMsg.Moved(0, 300, 400, Direction.LEFT));
        assertThat(mover).isEmpty();
    }

    @Test
    @DisplayName("한 tick에 MOVE가 여러 번 오면 마지막 위치만 MOVED로 보낸다")
    void sendsOnlyLatestMovePerTick() {
        // given
        connect(0);
        join(0, "모카", 0);
        List<ServerMsg> other = connect(1);
        join(1, "구름", 1);
        other.clear();
        sessionManager.receive(0, new ClientMsg.Move(300, 400, Direction.LEFT));
        sessionManager.receive(0, new ClientMsg.Move(310, 400, Direction.RIGHT));

        // when
        dispatcherLoop.tick();

        // then
        assertThat(other).containsExactly(new ServerMsg.Moved(0, 310, 400, Direction.RIGHT));
    }

    @Test
    @DisplayName("로비 밖 좌표는 가장 가까운 경계로 잘라 MOVED로 보낸다")
    void clampsMoveOutsideLobby() {
        // given
        connect(0);
        join(0, "모카", 0);
        List<ServerMsg> other = connect(1);
        join(1, "구름", 1);
        other.clear();

        // when
        move(0, -100, 99_999, Direction.DOWN);

        // then
        assertThat(other).containsExactly(new ServerMsg.Moved(0, 52, 941, Direction.DOWN));
    }

    @Test
    @DisplayName("이미 경계에 있는데 다시 로비 밖 좌표가 오면 MOVED를 보내지 않는다")
    void skipsRepeatedMoveOutsideLobby() {
        // given
        connect(0);
        join(0, "모카", 0);
        List<ServerMsg> other = connect(1);
        join(1, "구름", 1);
        move(0, -100, 99_999, Direction.DOWN);
        other.clear();

        // when
        move(0, -500, 50_000, Direction.DOWN);

        // then
        assertThat(other).isEmpty();
    }

    @Test
    @DisplayName("세션 ID가 커도 움직인 본인에게는 MOVED를 보내지 않는다")
    void skipsMoverWithLargeSessionId() {
        // given
        List<ServerMsg> mover = connect(200);
        join(200, "모카", 0);
        connect(300);
        join(300, "구름", 1);
        mover.clear();

        // when
        move(200, 300, 400, Direction.LEFT);

        // then
        assertThat(mover).isEmpty();
    }

    @Test
    @DisplayName("남은 참가자 모두에게 퇴장한 플레이어 ID를 알린다")
    void notifiesRemainingPlayersOfLeave() {
        // given
        connect(100);
        join(100, "모카", 0);
        List<ServerMsg> second = connect(200);
        join(200, "구름", 1);
        List<ServerMsg> third = connect(300);
        join(300, "별", 2);
        second.clear();
        third.clear();

        // when
        leave(100);

        // then
        assertThat(second).containsExactly(new ServerMsg.Left(0));
        assertThat(third).containsExactly(new ServerMsg.Left(0));
    }

    @Test
    @DisplayName("이미 퇴장한 연결의 종료에는 알림을 다시 보내지 않는다")
    void ignoresRepeatedLeave() {
        // given
        connect(100);
        join(100, "모카", 0);
        List<ServerMsg> remaining = connect(200);
        join(200, "구름", 1);
        leave(100);
        remaining.clear();

        // when
        leave(100);

        // then
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("입장하지 않은 연결이 끊겨도 퇴장 알림을 보내지 않는다")
    void ignoresLeaveBeforeJoin() {
        // given
        List<ServerMsg> remaining = connect(100);
        join(100, "모카", 0);
        connect(200);
        remaining.clear();

        // when
        leave(200);

        // then
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("입장 전 연결에는 다른 참가자의 퇴장 알림을 보내지 않는다")
    void skipsUnjoinedConnectionWhenNotifyingLeave() {
        // given
        connect(100);
        join(100, "모카", 0);
        List<ServerMsg> pending = connect(200);

        // when
        leave(100);

        // then
        assertThat(pending).isEmpty();
    }

    @Test
    @DisplayName("퇴장한 연결에는 자신의 퇴장 알림을 보내지 않는다")
    void skipsDepartedConnectionWhenNotifyingLeave() {
        // given
        List<ServerMsg> departed = connect(100);
        join(100, "모카", 0);
        departed.clear();

        // when
        leave(100);

        // then
        assertThat(departed).isEmpty();
    }

    @Test
    @DisplayName("3초가 되기 전에는 PING을 보내지 않는다")
    void doesNotPingBeforeInterval() {
        // given
        List<ServerMsg> responses = connect(100);
        join(100, "모카", 0);
        responses.clear();

        // when
        dispatcherLoop.tick(2_999);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("3초가 되면 모든 입장자에게 같은 시각의 PING을 보낸다")
    void pingsJoinedPlayers() {
        // given
        List<ServerMsg> first = connect(100);
        join(100, "모카", 0);
        List<ServerMsg> second = connect(200);
        join(200, "구름", 1);
        first.clear();
        second.clear();

        // when
        dispatcherLoop.tick(3_000);

        // then
        assertThat(first).containsExactly(new ServerMsg.Ping(3_000));
        assertThat(second).containsExactly(new ServerMsg.Ping(3_000));
    }

    @Test
    @DisplayName("PING을 보낸 뒤 다시 3초가 되기 전에는 보내지 않는다")
    void waitsBeforeNextPing() {
        // given
        List<ServerMsg> responses = connect(100);
        join(100, "모카", 0);
        dispatcherLoop.tick(3_000);
        responses.clear();

        // when
        dispatcherLoop.tick(5_999);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("다음 3초 주기에는 새 시각의 PING을 보낸다")
    void sendsNextPing() {
        // given
        List<ServerMsg> responses = connect(100);
        join(100, "모카", 0);
        dispatcherLoop.tick(3_000);
        responses.clear();

        // when
        dispatcherLoop.tick(6_000);

        // then
        assertThat(responses).containsExactly(new ServerMsg.Ping(6_000));
    }

    @Test
    @DisplayName("입장하지 않은 연결에는 PING을 보내지 않는다")
    void doesNotPingBeforeJoin() {
        // given
        List<ServerMsg> responses = connect(100);
        dispatcherLoop.tick(0);

        // when
        dispatcherLoop.tick(3_000);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("퇴장한 연결에는 PING을 보내지 않는다")
    void doesNotPingDepartedPlayer() {
        // given
        List<ServerMsg> responses = connect(100);
        join(100, "모카", 0);
        leave(100);
        responses.clear();

        // when
        dispatcherLoop.tick(3_000);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("입장한 시각부터 첫 응답을 기다린다")
    void startsResponseWaitOnJoin() {
        // given
        connect(100);
        sessionManager.receive(100, new ClientMsg.Join("모카", 0));

        // when
        dispatcherLoop.tick(1_000);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(100)).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("PONG의 전송 시각 대신 서버의 현재 처리 시각을 응답 시각으로 기록한다")
    void recordsPongProcessingTime() {
        // given
        connect(100);
        join(100, "모카", 0);
        dispatcherLoop.tick(3_000);
        sessionManager.receive(100, new ClientMsg.Pong(3_000));

        // when
        dispatcherLoop.tick(3_100);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(100)).isEqualTo(3_100L);
    }

    @Test
    @DisplayName("PONG을 보낸 참가자의 응답 시각만 갱신한다")
    void keepsOtherSessionResponseTime() {
        // given
        connect(100);
        join(100, "모카", 0);
        connect(200);
        join(200, "구름", 1);
        dispatcherLoop.tick(3_000);
        sessionManager.receive(100, new ClientMsg.Pong(3_000));

        // when
        dispatcherLoop.tick(3_100);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(200)).isZero();
    }

    @Test
    @DisplayName("입장 전 PONG은 응답 기록을 만들지 않는다")
    void ignoresPongBeforeJoin() {
        // given
        connect(100);
        sessionManager.receive(100, new ClientMsg.Pong(0));

        // when
        dispatcherLoop.tick(1_000);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(100)).isNull();
    }

    @Test
    @DisplayName("퇴장하면 응답 시각 기록도 제거한다")
    void removesResponseTimeOnLeave() {
        // given
        connect(100);
        join(100, "모카", 0);
        sessionManager.leave(100);

        // when
        dispatcherLoop.tick(1_000);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(100)).isNull();
    }

    @Test
    @DisplayName("퇴장 뒤 늦게 들어온 PONG은 응답 기록을 되살리지 않는다")
    void ignoresPongAfterLeave() {
        // given
        connect(100);
        join(100, "모카", 0);
        dispatcherLoop.tick(3_000);
        sessionManager.leave(100);
        dispatcherLoop.tick(3_100);
        sessionManager.receive(100, new ClientMsg.Pong(3_000));

        // when
        dispatcherLoop.tick(3_200);

        // then
        assertThat(dispatcherLoop.lastResponseMillis(100)).isNull();
    }

    @Test
    @DisplayName("10초 직전에는 연결을 유지한다")
    void keepsBeforeTimeout() {
        // given
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        sessionManager.register(100, new Session(new Socket(), 100, sessionManager) {
            @Override public void send(ServerMsg message) {}
            @Override public void close() { closed.set(true); }
        });
        join(100, "모카", 0);


        // when
        dispatcherLoop.tick(9999);

        // then
        assertThat(closed.get()).isFalse();
    }

    @Test
    @DisplayName("응답 없이 10초가 지나면 연결을 닫는다")
    void closesAtTimeout() {
        // given
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        sessionManager.register(100, new Session(new Socket(), 100, sessionManager) {
            @Override public void send(ServerMsg message) {}
            @Override public void close() { closed.set(true); }
        });
        join(100, "모카", 0);


        // when
        dispatcherLoop.tick(10000);

        // then
        assertThat(closed.get()).isTrue();
    }

    @Test
    @DisplayName("PONG을 받으면 응답 대기 시간을 다시 센다")
    void pongExtendsTimeout() {
        // given
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        sessionManager.register(100, new Session(new Socket(), 100, sessionManager) {
            @Override public void send(ServerMsg message) {}
            @Override public void close() { closed.set(true); }
        });
        join(100, "모카", 0);
        sessionManager.receive(100, new ClientMsg.Pong(3000));
        dispatcherLoop.tick(9000);

        // when
        dispatcherLoop.tick(10000);

        // then
        assertThat(closed.get()).isFalse();
    }

    @Test
    @DisplayName("타임아웃 퇴장은 LEFT를 한 번만 알린다")
    void notifiesTimeoutOnce() {
        // given
        connect(100);
        join(100, "모카", 0);
        List<ServerMsg> remaining = connect(200);
        join(200, "구름", 1);
        sessionManager.receive(200, new ClientMsg.Pong(0));
        dispatcherLoop.tick(9_000);
        remaining.clear();

        // when
        dispatcherLoop.tick(10_000);
        sessionManager.leave(100);
        dispatcherLoop.tick(10_001);

        // then
        assertThat(remaining).containsExactly(new ServerMsg.Left(0));
    }

    @Test
    @DisplayName("한 연결의 종료가 실패해도 다른 시간 초과 연결을 정리한다")
    void continuesAfterCloseFailure() {
        // given
        sessionManager.register(100, new Session(new Socket(), 100, sessionManager) {
            @Override public void send(ServerMsg message) {}
            @Override public void close() { throw new IllegalStateException("종료 실패"); }
        });
        join(100, "모카", 0);
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        sessionManager.register(200, new Session(new Socket(), 200, sessionManager) {
            @Override public void send(ServerMsg message) {}
            @Override public void close() { closed.set(true); }
        });
        join(200, "구름", 1);

        // when
        dispatcherLoop.tick(10_000);

        // then
        assertThat(closed.get()).isTrue();
        assertThat(dispatcherLoop.lastResponseMillis(100)).isNull();
        assertThat(dispatcherLoop.lastResponseMillis(200)).isNull();
    }

    private List<ServerMsg> connect(long sessionId) {
        List<ServerMsg> responses = new ArrayList<>();
        sessionManager.register(sessionId, new Session(new Socket(), 0L, sessionManager) {
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
        sessionManager.receive(sessionId, new ClientMsg.Join(name, character));
        dispatcherLoop.tick(0);
    }

    private void leave(long sessionId) {
        sessionManager.leave(sessionId);
        dispatcherLoop.tick(0);
    }

    private void move(long sessionId, double x, double y, Direction direction) {
        sessionManager.receive(sessionId, new ClientMsg.Move(x, y, direction));
        dispatcherLoop.tick();
    }
}
