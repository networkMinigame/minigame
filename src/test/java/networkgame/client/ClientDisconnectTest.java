package networkgame.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import java.io.IOException;

class ClientDisconnectTest {
    @Test
    @DisplayName("서버가 종료된 뒤 재연결하면 실패를 호출자에게 전달한다")
    void reportsReconnectFailure() throws Exception {
        // given
        ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Client original = new Client(listener.getInetAddress().getHostAddress(), listener.getLocalPort());
        try (Socket accepted = listener.accept()) {
            original.close();
            listener.close();

            // when
            Throwable failure = catchThrowable(() -> {
                Client unexpected = original.reconnect();
                unexpected.close();
            });

            // then
            assertThat(failure).isInstanceOf(IOException.class);
        } finally {
            original.close();
            listener.close();
        }
    }

    @Test
    @DisplayName("서버가 연결을 닫으면 종료 콜백을 한 번 전달한다")
    void reportsServerDisconnectOnce() throws Exception {
        // given
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Client client = new Client(listener.getInetAddress().getHostAddress(), listener.getLocalPort());
            try (Socket server = listener.accept()) {
                CountDownLatch disconnected = new CountDownLatch(1);
                AtomicInteger calls = new AtomicInteger();
                client.start(message -> {}, () -> {
                    calls.incrementAndGet();
                    disconnected.countDown();
                });

                // when
                server.close();
                boolean notified = disconnected.await(5, TimeUnit.SECONDS);
                client.close();

                // then
                assertThat(notified).isTrue();
                assertThat(calls.get()).isEqualTo(1);
            } finally {
                client.close();
            }
        }
    }

    @Test
    @DisplayName("재연결하면 같은 서버에 새 소켓으로 연결한다")
    void reconnectsToSameServer() throws Exception {
        // given
        try (ServerSocket listener = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
            listener.setSoTimeout(5_000);
            Client original = new Client(listener.getInetAddress().getHostAddress(), listener.getLocalPort());
            try (Socket first = listener.accept()) {
                original.close();

                // when
                Client reconnected = original.reconnect();
                try (Socket second = listener.accept()) {
                    try {
                        // then
                        assertThat(second.isConnected()).isTrue();
                        assertThat(second.getPort()).isNotEqualTo(first.getPort());
                    } finally {
                        reconnected.close();
                    }
                }
            } finally {
                original.close();
            }
        }
    }
}
