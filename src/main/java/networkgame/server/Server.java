package networkgame.server;

import networkgame.server.session.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static networkgame.util.Log.log;

public class Server {

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        log("서버 소켓 연결 대기");
        DispatcherLoop dispatcherLoop = new DispatcherLoop();
        try (ServerSocket listener = new ServerSocket(port)) {
            log("서버 소켓 요청 대기");
            Thread loopThread = Thread.ofPlatform().name("dispatcher-loop").start(dispatcherLoop);
            try {
                while (true) {
                    Socket socket = listener.accept();
                    try {
                        socket.setTcpNoDelay(true);
                    } catch (IOException e) {
                        socket.close();
                        log("접속 설정 실패 " + e.getMessage());
                        continue;
                    }
                    log("접속 " + socket.getRemoteSocketAddress());
                    new Session(socket, dispatcherLoop);
                }
            } finally {
                loopThread.interrupt();
            }
        }
    }
}
