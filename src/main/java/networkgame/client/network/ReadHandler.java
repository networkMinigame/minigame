package networkgame.client.network;

import networkgame.protocol.ServerMsg;
import networkgame.protocol.ClientMsg;
import networkgame.util.Utf8LineReader;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

import static networkgame.protocol.MessageCodec.decodeServer;
import static networkgame.util.Log.log;

class ReadHandler extends ClientHandler {

    // 외부로부터 작업을 전달받아 단순히 값을 받고 읽습니다.
    private final Consumer<ServerMsg> onMessage;

    ReadHandler(Client client, Socket socket, Consumer<ServerMsg> onMessage) {
        super(client, socket);
        this.onMessage = onMessage;
    }

    /** PING은 Reader에서 전송 큐로 응답하고, 다른 메시지는 화면 쪽 콜백으로 전달한다. */
    @Override
    protected void handle() throws IOException {
        Utf8LineReader in = new Utf8LineReader(socket.getInputStream());

        String inputMessage;
        while ((inputMessage = in.readLine()) != null) { // IOException
            if (inputMessage.isBlank()) {
                continue;
            }
            log(inputMessage);

            try {
                ServerMsg serverMsg = decodeServer(inputMessage);
                if (serverMsg instanceof ServerMsg.Ping(long t)) {
                    client.send(new ClientMsg.Pong(t));
                } else {
                    onMessage.accept(serverMsg);
                }
            } catch (JacksonException e) { // RuntimeException
                log("잘못된 서버 메시지 " + e.getOriginalMessage()); // 이 줄만 버리고 계속 읽음
            }
        }

        log("서버가 연결을 닫음");
    }
}
