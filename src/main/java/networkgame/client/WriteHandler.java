package networkgame.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.util.Log.log;

public class WriteHandler extends ClientHandler {

    private final BlockingQueue<String> sendQueue;

    public WriteHandler(Client client, Socket socket, BlockingQueue<String> sendQueue) {
        super(client, socket);
        this.sendQueue = sendQueue;
    }

    @Override
    protected void handle() throws IOException, InterruptedException {
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        while (true) {
            String outputMessage = sendQueue.take();

            out.write(outputMessage.getBytes(UTF_8));
            out.write('\n');
            out.flush();
            log(outputMessage);
        }
    }
}
