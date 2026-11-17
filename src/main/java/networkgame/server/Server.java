package networkgame.server;

import networkgame.server.session.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static networkgame.util.Log.log;

/** 클라이언트 TCP 연결을 받고 서버의 요청 처리 스레드를 실행한다. */
public class Server {

    private final int port;

    /**
     * 서버가 사용할 수신 포트를 지정한다.
     *
     * @param port 수신 포트 번호. 0이면 운영체제가 빈 포트를 배정한다.
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * 서버 소켓을 열고 접속이 들어올 때마다 세션을 시작한다.
     * 호출한 스레드는 접속을 기다리며, 수신 루프가 끝나면 DispatcherLoop에 인터럽트를 보낸다.
     *
     * @throws IOException 서버 소켓을 열거나 접속을 받는 중 복구할 수 없는 입출력 오류가 발생한 경우
     */
    public void start() throws IOException {
        log("서버 소켓 연결 대기");
        SessionManager sessions = new SessionManager();
        DispatcherLoop dispatcherLoop = new DispatcherLoop(sessions);
        long nextConnectionId = 0;
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
                    long connectionId = nextConnectionId++;
                    Session session = new Session(socket, connectionId, dispatcherLoop);
                    sessions.register(connectionId, session::send);
                    session.start();
                }
            } finally {
                loopThread.interrupt();
            }
        }
    }
}
