package networkgame.client;

import networkgame.util.Utf8LineReader;
import java.io.IOException;
import java.net.Socket;

import static networkgame.util.Log.log;

public class ReadHandler extends ClientHandler {

    public ReadHandler(Client client, Socket socket) {
        super(client, socket);
    }

    @Override
    protected void handle() throws IOException {
        Utf8LineReader in = new Utf8LineReader(socket.getInputStream());

        String inputMessage;
        while ((inputMessage = in.readLine()) != null) {
            log(inputMessage);
        }

        log("서버가 연결을 닫음");
    }
}
