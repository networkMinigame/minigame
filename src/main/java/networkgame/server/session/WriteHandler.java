package networkgame.server.session;

import networkgame.protocol.MessageCodec;
import networkgame.protocol.ServerMsg;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.util.Log.log;

/** 세션의 응답 큐에서 메시지를 꺼내 소켓으로 보내는 Writer 작업이다. */
public class WriteHandler extends SessionHandler {

    private final BlockingQueue<ServerMsg> sendQueue;

    /**
     * 세션에 대한 Writer를 구성한다.
     *
     * @param session 전송이 끝났을 때 정리할 세션
     * @param socket 해당 세션의 연결된 소켓
     * @param sendQueue 전송 순서대로 응답을 담는 해당 세션의 큐
     */
    public WriteHandler(Session session, Socket socket, BlockingQueue<ServerMsg> sendQueue) {
        super(session, socket);
        this.sendQueue = sendQueue;
    }

    /**
     * 큐에 응답이 들어올 때까지 기다렸다가 UTF-8 JSON 한 줄씩 소켓에 쓴다.
     *
     * @throws IOException 소켓 출력을 열거나 쓰는 중 오류가 발생한 경우
     * @throws InterruptedException 응답을 기다리는 동안 Writer가 인터럽트된 경우
     */
    @Override
    protected void handle() throws IOException, InterruptedException {
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        while (true) {
            String outputMessage = MessageCodec.encode(sendQueue.take());

            out.write(outputMessage.getBytes(UTF_8));
            out.write('\n');
            out.flush();
            log(outputMessage);
        }
    }
}
