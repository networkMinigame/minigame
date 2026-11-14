package networkgame.server.session;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.ServerMsg;
import networkgame.server.DispatcherLoop;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

public class Session {

    private final Socket socket;
    private final DispatcherLoop dispatcherLoop;
    private final BlockingQueue<ServerMsg> sendQueue = new LinkedBlockingQueue<>();
    private final Thread readThread;
    private final Thread writeThread;
    private final AtomicBoolean closed = new AtomicBoolean();

    public Session(Socket socket, DispatcherLoop dispatcherLoop) {
        this.socket = socket;
        this.dispatcherLoop = dispatcherLoop;
        this.readThread = new Thread(new ReadHandler(this, socket), "readHandler-" + socket.getPort());
        this.writeThread = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler-" + socket.getPort());

        readThread.start();
        writeThread.start();
    }

    void receive(ClientMsg message) {
        dispatcherLoop.receive(sendQueue, message);
    }

    void readerFinished() {
        dispatcherLoop.leave(sendQueue);
    }

    // Reader와 Writer가 동시에 닫을 수 있어 소켓 정리는 한 번만 한다.
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
