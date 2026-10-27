package networkgame.server.session;

import networkgame.server.SessionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

public class Session {

    private final Socket socket;
    private final String name;
    private final SessionManager sessionManager;
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private final Thread readThread;
    private final Thread writeThread;
    private final AtomicBoolean closed = new AtomicBoolean();

    public Session(Socket socket, SessionManager sessionManager, String name) {
        this.socket = socket;
        this.name = name;
        this.sessionManager = sessionManager;
        this.readThread = new Thread(new ReadHandler(this, socket), "readHandler-" + socket.getPort());
        this.writeThread = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler-" + socket.getPort());

        // 스레드보다 먼저 넣는다. 스레드가 바로 끊기면 remove()가 add()보다 먼저 불려 닫힌 세션이 목록에 남는다.
        sessionManager.add(this);
        readThread.start();
        writeThread.start();
    }

    public void send(String message) {
        sendQueue.add(message);
    }

    // 받은 줄에 보낸 사람 이름을 붙여 모두에게 보낸다. 보낸 사람도 돌려받아야 모두가 같은 순서로 본다.
    void relay(String message) {
        sessionManager.broadcast(name + ": " + message);
    }

    // Reader, Writer, closeAll()이 동시에 부를 수 있다. if (closed) 검사 후 대입하면 둘이 함께 통과하므로 CAS로 한 번만 통과시킨다.
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        sessionManager.remove(this);

        // 소켓을 닫으면 그 소켓의 입출력 스트림도 닫히고, readLine()·write()에 막힌 스레드가 예외로 깨어난다.
        try {
            socket.close();
        } catch (IOException e) {
            log("소켓 닫기 실패 " + e.getMessage());
        }

        // sendQueue.take()에서 기다리는 Writer는 소켓을 닫아도 안 깨어나서 interrupt로 깨운다.
        writeThread.interrupt();

        log("세션 종료 " + name);
    }

    public String getName() {
        return name;
    }
}
