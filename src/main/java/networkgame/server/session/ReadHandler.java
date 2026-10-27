package networkgame.server.session;

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
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));

        String inputMessage;
        while ((inputMessage = in.readLine()) != null) {
            if (inputMessage.isBlank()) {
                continue;
            }
            log(inputMessage);
        }

        log("클라이언트가 연결을 닫음");
    }
}
