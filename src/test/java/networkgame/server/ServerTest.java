package networkgame.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import networkgame.server.session.Session;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class ServerTest {
    @Test
    @DisplayName("JOIN 전 연결도 서버 종료 시 닫히고 수신 작업이 끝난다")
    void closesConnectionBeforeJoin() throws Exception {
        // given
        CountDownLatch registered = new CountDownLatch(1);
        Server server = new Server(0) {
            @Override
            void registered(Session connection) {
                registered.countDown();
            }
        };
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            Future<?> running = executor.submit(() -> { server.start(); return null; });
            try (Socket client = new Socket("127.0.0.1", server.awaitListening())) {
                client.setSoTimeout(3000);
                registered.await();

                // when
                server.close();

                // then
                assertThat(client.getInputStream().read()).isEqualTo(-1);
                running.get(3, TimeUnit.SECONDS);
            } finally {
                server.close();
            }
        }
    }

    @Test
    @DisplayName("여러 호출자가 동시에 종료해도 정상적으로 반환한다")
    void closesConcurrently() throws Exception {
        // given
        Server server = new Server(0);
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            Future<?> running = executor.submit(() -> { server.start(); return null; });
            server.awaitListening();
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            List<Future<?>> callers = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                callers.add(executor.submit(() -> {
                    ready.countDown();
                    release.await();
                    server.close();
                    return null;
                }));
            }
            try {
                ready.await();

                // when
                release.countDown();

                // then
                for (Future<?> caller : callers) {
                    caller.get(3, TimeUnit.SECONDS);
                }
                running.get(3, TimeUnit.SECONDS);
            } finally {
                release.countDown();
                server.close();
            }
        }
    }

    @Test
    @DisplayName("연결 등록 직전 종료하면 새 연결도 정리된다")
    void closesConnectionAcceptedDuringShutdown() throws Exception {
        // given
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Server server = new Server(0) {
            @Override
            void accepted() {
                accepted.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            Future<?> running = executor.submit(() -> { server.start(); return null; });
            try (Socket client = new Socket("127.0.0.1", server.awaitListening())) {
                client.setSoTimeout(3000);
                accepted.await();

                // when
                server.close();
                release.countDown();

                // then
                assertThat(client.getInputStream().read()).isEqualTo(-1);
                running.get(3, TimeUnit.SECONDS);
            } finally {
                release.countDown();
                server.close();
            }
        }
    }

    @Test
    @DisplayName("시작 전에 종료한 서버는 이후 수신을 시작하지 않는다")
    void closesBeforeStart() throws Exception {
        // given
        Server server = new Server(0);

        // when
        server.close();
        server.start();

        // then
        assertThat(server.awaitListening()).isZero();
    }
}
