package networkgame.client.handler;

import networkgame.client.Client;
import networkgame.protocol.ClientMsg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.protocol.MessageCodec.encode;
import static networkgame.util.Log.log;

public class WriteHandler extends ClientHandler {

    private final BlockingQueue<ClientMsg> sendQueue;

    public WriteHandler(Client client, Socket socket, BlockingQueue<ClientMsg> sendQueue) {
        super(client, socket);
        this.sendQueue = sendQueue;
    }

    @Override
    protected void handle() throws IOException, InterruptedException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));

        while (true) {
            // take(): 큐가 비어 있을 때 들어올 때까지 스레드가 멈춰서 가디려줍니다.
            ClientMsg clientMsg = sendQueue.take(); // InterruptedException
            String encodedOutputMessage = encode(clientMsg);

            out.write(encodedOutputMessage); // IOException
            out.newLine(); // \n
            out.flush();

            log(encodedOutputMessage + "을 보냈습니다.");
        }
    }
}
