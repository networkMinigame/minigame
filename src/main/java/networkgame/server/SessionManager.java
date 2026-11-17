package networkgame.server;

import networkgame.protocol.ServerMsg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** JOIN 여부와 관계없이 TCP 연결의 응답 전달 경로를 관리한다. */
final class SessionManager {
    private final Map<Long, Consumer<ServerMsg>> connections = new ConcurrentHashMap<>();

    /** 수락한 연결의 응답 전달 경로를 Reader 시작 전에 등록한다. */
    void register(long connectionId, Consumer<ServerMsg> send) {
        connections.put(connectionId, send);
    }

    /** 종료된 연결의 응답 전달 경로를 제거한다. */
    void remove(long connectionId) {
        connections.remove(connectionId);
    }

    /** 연결이 등록돼 있으면 응답을 해당 세션의 큐로 전달한다. */
    void send(long connectionId, ServerMsg message) {
        var send = connections.get(connectionId);
        if (send != null) {
            send.accept(message);
        }
    }
}
