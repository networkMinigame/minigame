package networkgame.server;

import networkgame.protocol.ServerMsg;
import networkgame.server.session.Session;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static networkgame.util.Log.log;

/** JOIN 전 연결까지 추적하며 응답 전달과 전체 종료를 담당한다. */
final class SessionManager {
    private final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<>();

    /** Acceptor가 Reader 시작 전에 세션을 등록한다. */
    void register(long connectionId, Session session) {
        sessions.put(connectionId, session);
    }

    /** 종료된 연결을 전송 대상에서 제거한다. */
    void remove(long connectionId) {
        sessions.remove(connectionId);
    }

    /** 등록된 세션의 전송 큐에 응답을 넣는다. */
    void send(long connectionId, ServerMsg message) {
        Session session = sessions.get(connectionId);
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
