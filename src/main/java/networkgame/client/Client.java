package networkgame.client;

import networkgame.client.handler.ReadHandler;
import networkgame.client.handler.WriteHandler;
import networkgame.protocol.ClientMsg;
import networkgame.protocol.ServerMsg;

import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static networkgame.util.Log.log;

public class Client {

    private final Socket socket;
    private final String host;
    private final int port;
    private Runnable onDisconnected = () -> {};
    private final BlockingQueue<ClientMsg> sendQueue = new LinkedBlockingQueue<>();
    private final Thread writeHandler;
    private final AtomicBoolean closed = new AtomicBoolean();

    public Client(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5_000);
            socket.setTcpNoDelay(true);
        } catch (IOException | RuntimeException error) {
            try {
                socket.close();
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
        this.writeHandler = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler");

        log("서버 연결 " + host + ":" + port);
    }

    /**
     * Reader·Writer 스레드를 시작하고 기다리지 않고 바로 돌아온다. EDT에서 부르기 때문이다.
     *
     * @param serverMsgConsumer 받은 메시지를 넘길 곳. Reader 스레드에서 불리므로 화면을 고치려면 EDT로 넘겨야 한다
     */
    public void start(Consumer<ServerMsg> serverMsgConsumer) {
        start(serverMsgConsumer, () -> {});
    }

    /** 종료 콜백은 네트워크 스레드에서 호출하므로 화면 변경은 EDT로 전달해야 한다. */
    public void start(Consumer<ServerMsg> serverMsgConsumer, Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
        Thread readHandler = new Thread(new ReadHandler(this, socket, serverMsgConsumer));
        readHandler.start();
        writeHandler.start();
    }

    /** 같은 서버에 새 연결을 만든다. 소켓 연결을 기다리므로 EDT에서 호출하지 않는다. */
    public Client reconnect() throws IOException {
        return new Client(host, port);
    }

    /** sendQueue가 private 이라 보낼 메시지를 큐에 넣고 바로 돌아옵니다. 실제로 보내는건 WriteHandler 이다 */
    public void send(ClientMsg clientMsg) {
        sendQueue.add(clientMsg); // 큐가 가득 찼을 때 IllegalStateException
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
        writeHandler.interrupt();

        log("서버 연결 종료");
        onDisconnected.run();
    }
}
