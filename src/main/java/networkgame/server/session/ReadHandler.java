package networkgame.server.session;

import networkgame.protocol.MessageCodec;
import tools.jackson.core.JacksonException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.util.Log.log;

public class ReadHandler extends SessionHandler {

    public ReadHandler(Session session, Socket socket) {
        super(session, socket);
    }

    @Override
    protected void handle() throws IOException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    session.receive(MessageCodec.decodeClient(line));
                } catch (JacksonException e) {
                    log("잘못된 클라이언트 메시지 " + e.getOriginalMessage());
                }
            }
            log("클라이언트가 연결을 닫음");
        } finally {
            // 같은 Reader가 마지막 메시지 뒤에 LEAVE를 넣어 순서를 보장한다.
            session.readerFinished();
        }
    }
}
