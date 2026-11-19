package networkgame.server.session;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.ServerMsg;
import networkgame.server.DispatcherLoop;
import networkgame.server.Connection;
import java.time.Duration;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

/** 클라이언트 연결 하나의 소켓, Reader·Writer 스레드, 응답 큐를 관리한다. */
public class Session implements Connection {

    private final Socket socket;
    private final long connectionId;
    private final DispatcherLoop dispatcherLoop;
    private final BlockingQueue<ServerMsg> sendQueue = new LinkedBlockingQueue<>();
    private final Thread readThread;
    private final Thread writeThread;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 연결을 맡을 Reader와 Writer를 구성한다. 연결을 등록한 뒤 start()를 호출한다.
     *
     * @param socket 서버가 수락한 클라이언트 소켓
     * @param connectionId 서버가 이 TCP 연결에 부여한 식별자
     * @param dispatcherLoop 메시지와 퇴장 이벤트를 처리할 서버 루프
     */
    public Session(Socket socket, long connectionId, DispatcherLoop dispatcherLoop) {
        this.socket = socket;
        this.connectionId = connectionId;
        this.dispatcherLoop = dispatcherLoop;
        this.readThread = new Thread(new ReadHandler(this, socket), "readHandler-" + socket.getPort());
        this.writeThread = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler-" + socket.getPort());

    }

    /** 연결 등록이 끝난 뒤 Reader와 Writer를 한 번 시작한다. */
    public void start() {
        writeThread.start();
        readThread.start();
    }

    /** Writer가 전송할 응답을 세션의 큐에 넣는다. */
    public void send(ServerMsg message) {
        sendQueue.add(message);
    }

    /** 서버가 공통 마감까지 두 작업 스레드를 기다린다. 현재 스레드는 제외한다. */
    @Override
    public boolean awaitTermination(long deadline) throws InterruptedException {
        for (Thread thread : new Thread[]{readThread, writeThread}) {
            if (thread == Thread.currentThread()) {
                continue;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining > 0) {
                thread.join(Duration.ofNanos(remaining));
            }
        }
        return isTerminated();
    }

    /** 두 작업 스레드의 종료 여부를 반환한다. */
    @Override
    public boolean isTerminated() {
        return !readThread.isAlive() && !writeThread.isAlive();
    }

    /** 이 연결의 Reader가 읽은 메시지를 DispatcherLoop에 전달한다. */
    void receive(ClientMsg message) {
        dispatcherLoop.receive(connectionId, message);
    }

    /** Reader의 마지막 수신 메시지 뒤에 퇴장 이벤트를 등록한다. */
    void readerFinished() {
        dispatcherLoop.leave(connectionId);
    }

    /**
     * 소켓을 닫고 응답 큐에서 대기 중인 Writer를 깨운다.
     * Reader와 Writer가 동시에 호출해도 소켓 정리는 한 번만 실행한다.
     */
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        // 소켓을 닫으면 그 소켓의 입출력 스트림도 닫히고, readLine()·write()에 막힌 스레드가 예외로 깨어난다.
        try {
            socket.close();
        } catch (IOException e) {
            log("소켓 닫기 실패 " + e.getMessage());
        }

        // sendQueue.take()에서 기다리는 Writer는 소켓을 닫아도 안 깨어나서 interrupt로 깨운다.
        writeThread.interrupt();

        log("세션 종료 " + socket.getRemoteSocketAddress());
    }
}
