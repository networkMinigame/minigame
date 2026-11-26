package networkgame.server;

import networkgame.protocol.RejectReason;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;

import networkgame.server.SessionManager.Event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static networkgame.util.Log.log;

/** 한 스레드에서 로비 요청을 처리하고 세션별 응답을 보낸다. */
public final class DispatcherLoop implements Runnable {
    private static final long TICK_MILLIS = 33;
    private static final long PING_INTERVAL_MILLIS = 3_000;
    private static final long RESPONSE_TIMEOUT_MILLIS = 10_000;

    private final PlayerManager playerManager = new PlayerManager();
    private final SessionManager sessionManager;
    private Long lastPingMillis;
    private final Set<Long> rejectedSessions = new HashSet<>();
    // DispatcherLoop 스레드만 접근한다. JOIN 시각을 첫 응답 대기의 기준으로 사용한다.
    private final Map<Long, Long> lastResponseMillisBySession = new HashMap<>();

    /** 서버와 연결 목록을 공유하는 요청 처리 루프를 구성한다. */
    DispatcherLoop(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 요청을 처리한 뒤 33ms를 기다리는 작업을 인터럽트될 때까지 반복한다.
     * DispatcherLoop 인스턴스당 하나의 스레드에서 실행한다.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                tick();
                Thread.sleep(TICK_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 서버 요청 처리의 한 주기를 실행한다. DispatcherLoop를 담당하는 스레드만 호출한다. */
    void tick() {
        tick(System.nanoTime() / 1_000_000);
    }

    /**
     * 전달받은 시각으로 이벤트 처리와 PING 전송 주기를 확인한다.
     *
     * @param nowMillis 단조 증가 시계의 밀리초 값. 테스트에서는 대기 없이 시각을 전달한다
     */
    void tick(long nowMillis) {
        processPendingEvents(nowMillis);
        disconnectTimedOut(nowMillis);
        sendPingIfDue(nowMillis);
    }

    /** 응답 이후 10초가 지난 연결을 닫고 퇴장시킨다. 뒤늦은 LEAVE는 기존 퇴장 처리에서 무시한다. */
    private void disconnectTimedOut(long nowMillis) {
        for (long sessionId : List.copyOf(lastResponseMillisBySession.keySet())) {
            long lastResponse = lastResponseMillisBySession.get(sessionId);
            if (nowMillis - lastResponse >= RESPONSE_TIMEOUT_MILLIS) {
                try {
                    sessionManager.closeSession(sessionId);
                } catch (RuntimeException error) {
                    log("타임아웃" + " 세션 종료 실패 " + sessionId + " " + error.getMessage());
                } finally {
                    leave(sessionId);
                }
            }
        }
    }

    /** 첫 주기를 기준으로 3초마다 입장자에게 PING을 보낸다. 지연된 주기를 몰아서 보내지 않는다. */
    private void sendPingIfDue(long nowMillis) {
        if (lastPingMillis == null) {
            lastPingMillis = nowMillis;
            return;
        }
        if (nowMillis - lastPingMillis < PING_INTERVAL_MILLIS) {
            return;
        }

        lastPingMillis = nowMillis;
        ServerMsg.Ping ping = new ServerMsg.Ping(nowMillis);
        for (long sessionId : playerManager.sessionIds()) {
            sessionManager.send(sessionId, ping);
        }
    }

    /** 수신 큐가 빌 때까지 메시지와 연결 종료 이벤트를 등록된 순서대로 처리한다. */
    private void processPendingEvents(long nowMillis) {
        Event event;
        while ((event = sessionManager.pollEvent()) != null) {
            switch (event) {
                case Event.Received(long sessionId, ClientMsg message) -> handle(sessionId, message, nowMillis);
                case Event.Leave(long sessionId) -> leave(sessionId);
            }
        }
        broadcastMoves();
    }

    /**
     * 퇴장한 연결과 플레이어를 제거하고 남은 입장자에게 LEFT를 보낸다.
     * 이미 처리했거나 JOIN하지 않은 연결의 종료에는 알림을 보내지 않는다.
     */
    private void leave(long sessionId) {
        rejectedSessions.remove(sessionId);
        lastResponseMillisBySession.remove(sessionId);
        PlayerInfo departed = playerManager.remove(sessionId);
        sessionManager.remove(sessionId);
        if (departed == null) {
            return;
        }

        ServerMsg.Left left = new ServerMsg.Left(departed.id());
        for (long recipient : playerManager.sessionIds()) {
            sessionManager.send(recipient, left);
        }
    }

    /** 입장과 PONG을 처리한다. 다른 메시지는 후속 기능에서 처리한다. */
    private void handle(long sessionId, ClientMsg message, long nowMillis) {
        if (rejectedSessions.contains(sessionId) || !sessionManager.contains(sessionId)) {
            return;
        }
        switch (message) {
            case ClientMsg.Join join -> join(sessionId, join, nowMillis);
            case ClientMsg.Move move -> playerManager.move(
                    sessionId,
                    move.x(),
                    move.y(),
                    move.direction()
            );
            case ClientMsg.Ready ignored -> {}
            case ClientMsg.Chat ignored -> {}
            case ClientMsg.Pong ignored -> recordPong(sessionId, nowMillis);
        }
    }

    /**
     * 입장한 세션의 마지막 응답 시각을 서버의 현재 처리 시각으로 갱신한다.
     * PONG의 t는 PING 전송 시각이므로 응답 시각으로 사용하지 않는다.
     * 입장 전이거나 이미 퇴장한 세션의 PONG은 무시한다.
     */
    private void recordPong(long sessionId, long nowMillis) {
        lastResponseMillisBySession.replace(sessionId, nowMillis);
    }

    /**
     * 마지막 응답 시각을 반환한다. 아직 PONG이 없으면 JOIN 처리 시각이다.
     * DispatcherLoop 스레드에서 조회하며, 입장 기록이 없으면 null을 반환한다.
     */
    Long lastResponseMillis(long sessionId) {
        return lastResponseMillisBySession.get(sessionId);
    }

    /** 입장에 성공하면 본인에게 전체 목록을 보내고 기존 입장자에게 새 플레이어를 알린다. */
    private void join(long sessionId, ClientMsg.Join message, long nowMillis) {
        if (playerManager.sessionIds().contains(sessionId)) {
            return;
        }
        RejectReason reason = playerManager.rejectionReason(message.name());
        if (reason != null) {
            rejectedSessions.add(sessionId);
            sessionManager.reject(sessionId, reason);
            return;
        }
        PlayerInfo player = playerManager.register(sessionId, message.name());
        if (player == null) {
            return;
        }

        lastResponseMillisBySession.put(sessionId, nowMillis);
        sessionManager.send(sessionId, new ServerMsg.Welcome(player.id(), playerManager.players()));
        var joined = new ServerMsg.Joined(player);
        for (long recipient : playerManager.sessionIds()) {
            if (recipient != sessionId) {
                sessionManager.send(recipient, joined);
            }
        }
    }

    private void broadcastMoves() {
        Map<Long, PlayerInfo> movedPlayerInfos = playerManager.movedPlayerInfos();

        for (long sessionId : movedPlayerInfos.keySet()) {
            PlayerInfo playerInfo = movedPlayerInfos.get(sessionId);
            ServerMsg.Moved moved = new ServerMsg.Moved(
                    playerInfo.id(),
                    playerInfo.x(),
                    playerInfo.y(),
                    playerInfo.direction()
            );
            for (Long receiveSessionId : playerManager.sessionIds()) {
                if (receiveSessionId != sessionId) {
                    sessionManager.send(receiveSessionId, moved);
                }
            }
        }
        playerManager.clearMoved();
    }


}
