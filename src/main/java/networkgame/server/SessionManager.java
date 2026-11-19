package networkgame.server;

import networkgame.protocol.ServerMsg;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static networkgame.util.Log.log;

/** JOIN 전 연결까지 추적하며 응답 전달과 전체 종료를 담당한다. */
final class SessionManager {
    private final ConcurrentHashMap<Long, Connection> connections = new ConcurrentHashMap<>();
    private final List<Connection> retiring = new ArrayList<>();
    private List<Connection> closingConnections = List.of();

    /** Acceptor가 Reader 시작 전에 연결을 등록한다. */
    synchronized void register(long connectionId, Connection connection) {
        retiring.removeIf(Connection::isTerminated);
        connections.put(connectionId, connection);
    }

    /** 퇴장한 연결을 전송 대상에서 빼고 스레드 종료 대상을 보존한다. */
    synchronized void remove(long connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection != null && !connection.isTerminated()) {
            retiring.add(connection);
        }
    }

    /** 등록된 연결의 전송 큐에 응답을 전달한다. */
    void send(long connectionId, ServerMsg message) {
        Connection connection = connections.get(connectionId);
        if (connection != null) {
            connection.send(message);
        }
    }

    /** 등록된 연결과 종료 중인 연결의 복사본을 반환한다. */
    private synchronized List<Connection> snapshot() {
        LinkedHashSet<Connection> snapshot = new LinkedHashSet<>(connections.values());
        snapshot.addAll(retiring);
        return List.copyOf(snapshot);
    }

    /**
     * 모든 연결에 종료를 요청하고 각 연결의 실패를 기록한다.
     * Server가 새 연결 등록을 차단한 뒤 종료 담당 스레드에서 한 번 호출한다.
     * 종료 대기는 awaitTermination에서 수행한다.
     *
     * @return 모든 연결에 종료 요청이 성공했으면 true
     */
    boolean closeAll() {
        closingConnections = snapshot();
        boolean success = true;
        for (Connection connection : closingConnections) {
            try {
                connection.close();
            } catch (RuntimeException e) {
                success = false;
                log("연결 종료 실패 " + connection + " " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * closeAll에서 종료를 요청한 연결의 작업 스레드를 공통 마감까지 기다린다.
     *
     * @param deadline System.nanoTime() 기준의 서버 전체 종료 마감 시각
     * @return 모든 연결의 작업 스레드가 마감 안에 끝났으면 true
     * @throws InterruptedException 대기 중 호출 스레드가 인터럽트된 경우
     */
    boolean awaitTermination(long deadline) throws InterruptedException {
        boolean success = true;
        for (Connection connection : closingConnections) {
            try {
                if (!connection.awaitTermination(deadline)) {
                    success = false;
                    log("연결 스레드 종료 시간 초과 " + connection);
                }
            } catch (RuntimeException e) {
                success = false;
                log("연결 스레드 종료 확인 실패 " + connection + " " + e.getMessage());
            }
        }
        return success;
    }}
