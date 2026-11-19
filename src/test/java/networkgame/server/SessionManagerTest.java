package networkgame.server;

import networkgame.protocol.ServerMsg;
import networkgame.server.session.Session;
import java.net.Socket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {
    @Test
    @DisplayName("한 연결의 종료가 실패해도 다른 연결에 종료를 요청한다")
    void closesRemainingConnectionsAfterFailure() {
        // given
        SessionManager sessions = new SessionManager();
        List<Long> attempted = new ArrayList<>();
        sessions.register(0, new Session(new Socket(), 0L, sessions) {
            public void send(ServerMsg message) {}
            public void close() {
                attempted.add(0L);
                throw new IllegalStateException("종료 실패 테스트");
            }
        });
        sessions.register(1, new Session(new Socket(), 0L, sessions) {
            public void send(ServerMsg message) {}
            public void close() {
                attempted.add(1L);
            }
        });

        // when
        boolean success = sessions.closeAll();

        // then
        assertThat(success).isFalse();
        assertThat(attempted).containsExactlyInAnyOrder(0L, 1L);
    }

}
