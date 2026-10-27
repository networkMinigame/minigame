package networkgame.server;

import networkgame.server.session.Session;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private final List<Session> sessions = new ArrayList<>();

    // crud

    // create
    public synchronized void add(Session session) {
        sessions.add(session);
    }

    // retrieve 조회
    // 단건 조회
    public synchronized Session findByName(String name) {
        for (Session session : sessions) {
            if(session.getName().equals(name)) {
                return session;
            }
        }

        return null; // 없을 경우
    }

    public synchronized void remove(Session session) {
        sessions.remove(session);
    }

    // 복사본을 돈다. session.close()가 remove()로 자기를 빼기 때문에 원본을 돌면 ConcurrentModificationException이 난다.
    public void closeAll() {
        List<Session> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(sessions);
        }

        for (Session session : snapshot) {
            session.close();
        }
    }

    // 전체 조회
}
