package networkgame.server;

import networkgame.protocol.ServerMsg;

/** 서버가 응답 전송과 종료를 요청할 수 있는 연결이다. */
public interface Connection {
    /** 응답을 연결의 전송 큐에 넣는다. */
    void send(ServerMsg message);

    /** 연결 종료를 요청하고 즉시 반환한다. */
    void close();

    /** System.nanoTime() 기준의 공통 마감 시각까지 스레드 종료를 기다린다. */
    default boolean awaitTermination(long deadline) throws InterruptedException {
        return true;
    }

    /** 연결의 입출력 스레드가 모두 종료됐는지 반환한다. */
    default boolean isTerminated() {
        return true;
    }
}
