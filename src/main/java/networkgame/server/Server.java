package networkgame.server;

import networkgame.server.session.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static networkgame.util.Log.log;

public class Server {

    private final int port;
    private ServerSocket listener;
    private final SessionManager sessionManager = new SessionManager();

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        log("서버 소켓 연결 대기");
        listener = new ServerSocket(port);
        log("서버 소켓 요청 대기");

        Socket socket = listener.accept();
        Session session = new Session(socket, sessionManager, "guest");

        // 콘솔은 여기서만 읽는다. 세션마다 Scanner를 만들면 여러 Scanner가 콘솔 줄을 나눠 가져간다.
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            session.send(scanner.nextLine());
        }
    }
}
