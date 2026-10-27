package networkgame.server;

import java.io.IOException;

public class ServerMain {

    private static final int PORT = 12345;

    static void main() throws IOException {
        Server server = new Server(PORT);
        server.start();
    }
}
