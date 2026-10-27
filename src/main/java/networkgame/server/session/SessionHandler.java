package networkgame.server.session;

import java.io.IOException;
import java.net.Socket;

import static networkgame.util.Log.log;

public abstract class SessionHandler implements Runnable {

    protected final Session session;
    protected final Socket socket;

    protected SessionHandler(Session session, Socket socket) {
        this.session = session;
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
            session.close();
        }
    }

    protected abstract void handle() throws IOException, InterruptedException;
}
