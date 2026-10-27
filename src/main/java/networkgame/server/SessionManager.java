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

    public void broadcast(String message) {
        for (Session session : snapshot()) {
            session.send(message);
        }
    }

    public void closeAll() {
        for (Session session : snapshot()) {
            session.close();
        }
    }

    // 목록을 돌 때는 복사본을 쓴다. 도는 중에 다른 스레드가 add·remove하거나 session.close()가 remove()를 부르면 ConcurrentModificationException이 난다.
    private synchronized List<Session> snapshot() {
        return new ArrayList<>(sessions);
    }

    // 전체 조회
}
