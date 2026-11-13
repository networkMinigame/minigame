package networkgame.client.handler;

import networkgame.client.Client;
import networkgame.protocol.ServerMsg;
import tools.jackson.core.JacksonException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.protocol.MessageCodec.decodeServer;
import static networkgame.util.Log.log;

public class ReadHandler extends ClientHandler {

    // 외부로부터 작업을 전달받아 단순히 값을 받고 읽습니다.
    private final Consumer<ServerMsg> onMessage;

    public ReadHandler(Client client, Socket socket, Consumer<ServerMsg> onMessage) {
        super(client, socket);
        this.onMessage = onMessage;
    }

    @Override
    protected void handle() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));

        String inputMessage;
        while ((inputMessage = in.readLine()) != null) { // IOException
            if (inputMessage.isBlank()) {
                continue;
            }
            log(inputMessage);

            try {
                ServerMsg serverMsg = decodeServer(inputMessage);
                onMessage.accept(serverMsg); // 이미 검증되어서 날아오기 때문
            } catch (JacksonException e) { // RuntimeException
                log("잘못된 서버 메시지 " + e.getOriginalMessage()); // 이 줄만 버리고 계속 읽음
            }
        }

        log("서버가 연결을 닫음");
    }
}
