package networkgame.server;

import networkgame.protocol.MessageCodec;
import networkgame.protocol.RejectReason;
import networkgame.protocol.ServerMsg;
import networkgame.server.session.Session;
import networkgame.util.Utf8LineReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;

class RejectDeliveryTest {
    @Test
    @DisplayName("거절 응답을 받은 뒤 연결이 종료된다")
    void deliversRejectBeforeClosing() throws Exception {
        // given
        SessionManager manager = new SessionManager();
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket client = new Socket(listener.getInetAddress(), listener.getLocalPort());
             Socket accepted = listener.accept()) {
            client.setSoTimeout(5_000);
            Session session = new Session(accepted, 100, manager);
            session.start();
            try {
                Utf8LineReader input = new Utf8LineReader(client.getInputStream());

                // when
                manager.reject(100, RejectReason.FULL);
                ServerMsg response = MessageCodec.decodeServer(input.readLine());
                String nextLine = input.readLine();

                // then
                assertThat(response).isEqualTo(new ServerMsg.Reject(RejectReason.FULL));
                assertThat(nextLine).isNull();
            } finally {
                session.close();
            }
        }
    }
}
