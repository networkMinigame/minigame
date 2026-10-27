package networkgame.client;

import java.io.IOException;
import java.net.Socket;

import static networkgame.util.Log.log;

public abstract class ClientHandler implements Runnable {

    protected final Client client;
    protected final Socket socket;

    protected ClientHandler(Client client, Socket socket) {
        this.client = client;
        this.socket = socket;
    }

    @Override
    public final void run() {
        try {
            handle();
        } catch (IOException e) {
            log("연결 끊김 " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            client.close();
        }
    }

    protected abstract void handle() throws IOException, InterruptedException;
}
