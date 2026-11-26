package networkgame.server;

import networkgame.protocol.RejectReason;

import networkgame.protocol.ServerMsg;
import networkgame.protocol.ClientMsg;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import networkgame.server.session.Session;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static networkgame.util.Log.log;

/** JOIN 전 연결까지 추적하며 응답 전달과 전체 종료를 담당한다. */
public final class SessionManager {
    /** 수신 메시지와 연결 종료를 동일한 큐에 담는 서버 내부 이벤트다. */
    sealed interface Event {
        record Received(long sessionId, ClientMsg message) implements Event {}
        record Leave(long sessionId) implements Event {}
    }

    private final BlockingQueue<Event> receiveQueue = new LinkedBlockingQueue<>();

    /** Reader가 읽은 메시지를 세션 식별자와 함께 공용 수신 큐에 넣는다. */
    public void receive(long sessionId, ClientMsg message) {
        receiveQueue.add(new Event.Received(sessionId, message));
    }

    /** Reader가 마지막 수신 메시지를 넣은 뒤 퇴장 이벤트를 등록한다. */
    public void leave(long sessionId) {
        receiveQueue.add(new Event.Leave(sessionId));
    }

    /** DispatcherLoop가 가장 먼저 등록된 이벤트를 꺼낸다. 큐가 비어 있으면 null이다. */
    Event pollEvent() {
        return receiveQueue.poll();
    }

    private final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<>();

    /** Acceptor가 Reader 시작 전에 세션을 등록한다. */
    public void register(long sessionId, Session session) {
        sessions.put(sessionId, session);
    }

    /** 종료된 연결을 전송 대상에서 제거한다. */
    void remove(long sessionId) {
        sessions.remove(sessionId);
    }

    /** 지정한 연결을 닫는다. 이미 제거된 연결이면 아무 작업도 하지 않는다. */
    void closeSession(long sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.close();
        }
    }

    /** 거절 응답을 Writer에 맡긴다. 전체 종료를 위해 연결 등록은 유지한다. Writer는 전송 후 연결을 닫는다. */
    void reject(long sessionId, RejectReason reason) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.send(new ServerMsg.Reject(reason));
        }
    }

    /** 등록 여부를 확인한다. 거절 뒤 큐에 남은 요청은 처리하지 않는다. */
    boolean contains(long sessionId) {
        return sessions.containsKey(sessionId);
    }

    /** 등록된 세션의 전송 큐에 응답을 넣는다. */
    void send(long sessionId, ServerMsg message) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.send(message);
        }
    }

    /**
     * 모든 세션의 소켓을 닫고 Writer에 인터럽트를 요청한다.
     * Server가 새 연결 등록을 차단한 뒤 호출한다. 각 세션의 실패를 기록하고 다음 세션을 정리한다.
     *
     * @return 모든 세션의 종료 요청에 성공했으면 true
     */
    boolean closeAll() {
        boolean success = true;
        for (Session session : List.copyOf(sessions.values())) {
            try {
                session.close();
            } catch (RuntimeException e) {
                success = false;
                log("세션 종료 실패 " + session + " " + e.getMessage());
            }
        }
        return success;
    }
}
