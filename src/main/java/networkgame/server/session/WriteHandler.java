package networkgame.server.session;

import networkgame.protocol.MessageCodec;
import networkgame.protocol.ServerMsg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.util.Log.log;

public class WriteHandler extends SessionHandler {

    private final BlockingQueue<ServerMsg> sendQueue;

    public WriteHandler(Session session, Socket socket, BlockingQueue<ServerMsg> sendQueue) {
        super(session, socket);
        this.sendQueue = sendQueue;
    }

    @Override
    protected void handle() throws IOException, InterruptedException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));

        while (true) {
            String outputMessage = MessageCodec.encode(sendQueue.take());

            out.write(outputMessage);
            out.write('\n');
            out.flush();
            log(outputMessage);
        }
    }
}
