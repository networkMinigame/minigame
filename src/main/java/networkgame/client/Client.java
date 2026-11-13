package networkgame.client;

import networkgame.client.handler.ReadHandler;
import networkgame.client.handler.WriteHandler;
import networkgame.protocol.ClientMsg;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

public class Client {

    private final Socket socket;
    private final BlockingQueue<ClientMsg> sendQueue = new LinkedBlockingQueue<>();
    private final Thread readThread;
    private final Thread writeThread;
    private final AtomicBoolean closed = new AtomicBoolean();

    public Client(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.readThread = new Thread(new ReadHandler(this, socket), "readHandler");
        this.writeThread = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler");

        log("서버 연결 " + host + ":" + port);
    }

    public void start() throws InterruptedException {
        readThread.start();
        writeThread.start();

        readThread.join();
        writeThread.join();
    }

    // Reader와 Writer가 동시에 부를 수 있어서 CAS로 한 번만 통과시킨다.
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            log("소켓 닫기 실패 " + e.getMessage());
        }

        // sendQueue.take()에서 기다리는 Writer는 소켓을 닫아도 안 깨어나서 interrupt로 깨운다.
        writeThread.interrupt();

        log("서버 연결 종료");
    }
}
