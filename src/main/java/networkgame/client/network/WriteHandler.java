package networkgame.client.network;

import networkgame.protocol.ClientMsg;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.protocol.MessageCodec.encode;
import static networkgame.util.Log.log;

class WriteHandler implements Runnable {

    private final Client client;
    private final Socket socket;
    private final BlockingQueue<ClientMsg> sendQueue;

    WriteHandler(Client client, Socket socket, BlockingQueue<ClientMsg> sendQueue) {
        this.client = client;
        this.socket = socket;
        this.sendQueue = sendQueue;
    }

    // 쓰다가 끊기거나 close()가 interrupt로 깨우면 끝나고, 끝나면 연결을 닫는다.
    @Override
    public void run() {
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

    private void handle() throws IOException, InterruptedException {
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        while (true) {
            // take(): 큐가 비어 있을 때 들어올 때까지 스레드가 멈춰서 가디려줍니다.
            ClientMsg clientMsg = sendQueue.take(); // InterruptedException
            String encodedOutputMessage = encode(clientMsg);

            out.write(encodedOutputMessage.getBytes(UTF_8)); // IOException
            out.write('\n');
            out.flush();

            log(encodedOutputMessage + "을 보냈습니다.");
        }
    }
}
