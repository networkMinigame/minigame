package networkgame.client;

import java.io.IOException;

public class ClientMain {

    private static final String HOST = "100.125.68.41";
    private static final int PORT = 12345;

    static void main() throws IOException, InterruptedException {
        Client client = new Client(HOST, PORT);
        client.start();
    }
}
