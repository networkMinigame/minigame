package networkgame.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.*;
import static networkgame.util.Log.log;

public class ReadHandler extends ClientHandler {

    public ReadHandler(Client client, Socket socket) {
        super(client, socket);
    }

    @Override
    protected void handle() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));

        String inputMessage;
        while ((inputMessage = in.readLine()) != null) {
            log(inputMessage);
        }

        log("서버가 연결을 닫음");
    }
}
