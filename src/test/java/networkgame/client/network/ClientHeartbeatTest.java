package networkgame.client.network;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.MessageCodec;
import networkgame.protocol.ServerMsg;
import networkgame.util.Utf8LineReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/** 로컬 연결로 실제 Reader·전송 큐·Writer를 통한 PONG 응답을 확인한다. */
class ClientHeartbeatTest {
    @Test
    @DisplayName("PING을 받으면 화면 콜백을 거치지 않고 같은 값을 PONG으로 돌려준다")
    void repliesToPingThroughWriter() throws Exception {
        // given
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Client client = new Client(listener.getInetAddress().getHostAddress(), listener.getLocalPort());
            try (Socket serverSide = listener.accept()) {
                serverSide.setSoTimeout(5_000);
                BlockingQueue<ServerMsg> delivered = new LinkedBlockingQueue<>();
                client.start(delivered::add);
                OutputStream output = serverSide.getOutputStream();
                Utf8LineReader input = new Utf8LineReader(serverSide.getInputStream());
                long timestamp = 123_456L;
                ServerMsg.Left followingMessage = new ServerMsg.Left(2);

                // when
                output.write((MessageCodec.encode(new ServerMsg.Ping(timestamp)) + "\n"
                        + MessageCodec.encode(followingMessage) + "\n").getBytes(UTF_8));
                output.flush();
                ClientMsg response = MessageCodec.decodeClient(input.readLine());
                ServerMsg forwarded = delivered.poll(5, TimeUnit.SECONDS);

                // then
                assertThat(response).isEqualTo(new ClientMsg.Pong(timestamp));
                assertThat(forwarded).isEqualTo(followingMessage);
                assertThat(delivered).isEmpty();
            } finally {
                client.close();
            }
        }
    }
}
