package networkgame.server.session;

import networkgame.protocol.MessageCodec;
import tools.jackson.core.JacksonException;

import networkgame.util.Utf8LineReader;
import java.io.IOException;
import java.net.Socket;

import static networkgame.util.Log.log;

/** 세션의 소켓에서 클라이언트 메시지를 읽는 Reader 작업이다. */
public class ReadHandler extends SessionHandler {

    /**
     * 세션에 대한 Reader를 구성한다.
     *
     * @param session 받은 메시지를 전달할 세션
     * @param socket 해당 세션의 연결된 소켓
     */
    public ReadHandler(Session session, Socket socket) {
        super(session, socket);
    }

    /**
     * UTF-8 메시지를 한 줄씩 읽어 전달한다. 변환에 실패한 줄은 로그를 남기고 건너뛴다.
     * 읽기가 끝나면 마지막 메시지 뒤에 퇴장 이벤트를 등록한다.
     *
     * @throws IOException 소켓 입력을 열거나 읽는 중 오류가 발생한 경우
     */
    @Override
    protected void handle() throws IOException {
        try {
            Utf8LineReader in = new Utf8LineReader(socket.getInputStream());
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
