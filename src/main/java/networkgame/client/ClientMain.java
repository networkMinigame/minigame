package networkgame.client;

import java.io.IOException;

public class ClientMain {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    static void main() throws IOException, InterruptedException {
        Client client = new Client(HOST, PORT);
        client.start();
    }
}
