package networkgame.server;

import networkgame.server.session.Session;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

/** TCP 연결을 받고 서버 전체의 시작과 종료를 관리한다. */
public class Server implements AutoCloseable {
    private static final long SHUTDOWN_NANOS = Duration.ofSeconds(3).toNanos();
    private final int port;
    private final Object lifecycle = new Object();
    private final SessionManager sessions = new SessionManager();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final CountDownLatch listening = new CountDownLatch(1);
    private final CountDownLatch closed = new CountDownLatch(1);
    private ServerSocket listener;
    private Thread loopThread;
    private long shutdownDeadline;
    private long nextConnectionId;
    private boolean started;

    /** 수신 포트를 지정한다. 0이면 운영체제가 빈 포트를 배정한다. */
    public Server(int port) {
        this.port = port;
    }

    /** 요청 루프를 시작하고 접속을 받는다. 반환 또는 실패 시 전체 종료를 수행한다.
     * @throws IOException 수신 소켓을 열거나 접속을 받는 중 오류가 발생한 경우
     */
    public void start() throws IOException {
        try {
            DispatcherLoop dispatcher = new DispatcherLoop(sessions);
            synchronized (lifecycle) {
                if (closing.get()) {
                    listening.countDown();
                    return;
                }
                if (started) {
                    throw new IllegalStateException("서버는 한 번만 시작할 수 있습니다.");
                }
                started = true;
                listener = new ServerSocket(port);
                loopThread = Thread.ofPlatform().name("dispatcher-loop").start(dispatcher);
                listening.countDown();
            }
            while (!closing.get()) {
                Socket socket;
                try {
                    socket = listener.accept();
                } catch (IOException e) {
                    if (closing.get()) {
                        break;
                    }
                    throw e;
                }
                accepted();
                synchronized (lifecycle) {
                    if (closing.get()) {
                        socket.close();
                        continue;
                    }
                    try {
                        socket.setTcpNoDelay(true);
                        Session session = new Session(socket, nextConnectionId, dispatcher);
                        sessions.register(nextConnectionId++, session);
                        session.start();
                        registered(session);
                    } catch (IOException | RuntimeException e) {
                        try {
                            socket.close();
                        } catch (IOException closeError) {
                            e.addSuppressed(closeError);
                        }
                        throw e;
                    }
                }
            }
        } finally {
            close();
        }
    }

    /** 서버 수신 준비를 최대 3초 기다려 포트를 반환한다. 시작 전 종료되면 0이다. */
    int awaitListening() throws InterruptedException {
        if (!listening.await(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("서버 시작 대기 시간 초과");
        }
        synchronized (lifecycle) {
            return listener == null ? 0 : listener.getLocalPort();
        }
    }

    /** 접속 수락과 등록 사이의 시점을 테스트에서 조정할 수 있게 한다. */
    void accepted() {
    }

    /** 등록과 스레드 시작이 완료된 시점을 테스트에서 확인한다. */
    void registered(Session session) {
    }

    /**
     * 리스너와 모든 연결을 닫고 요청 루프를 중단한다.
     * 종료는 한 번 시작하며 중복 호출도 같은 종료 작업을 최대 공통 3초 마감까지 기다린다.
     * 소켓 정리와 인터럽트를 먼저 요청하고, 잠금 밖에서 스레드 종료를 기다린다.
     */
    @Override
    public void close() {
        Thread loop;
        boolean owner;
        long deadline;
        synchronized (lifecycle) {
            owner = closing.compareAndSet(false, true);
            if (owner) {
                shutdownDeadline = System.nanoTime() + SHUTDOWN_NANOS;
            }
            deadline = shutdownDeadline;
            loop = loopThread;
        }
        if (!owner) {
            try {
                if (!closed.await(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                    log("서버 종료 완료 대기 시간 초과");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        boolean success = true;
        try {
            try {
                if (listener != null) {
                    listener.close();
                }
            } catch (IOException | RuntimeException e) {
                success = false;
                log("리스너 종료 실패 " + e.getMessage());
            }
            success = sessions.closeAll() && success;
            if (loop != null) {
                loop.interrupt();
            }
            try {
                success = sessions.awaitTermination(deadline) && success;
                if (loop != null && loop != Thread.currentThread()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining > 0) {
                        loop.join(Duration.ofNanos(remaining));
                    }
                    if (loop.isAlive()) {
                        success = false;
                        log("DispatcherLoop 종료 시간 초과");
                    }
                }
            } catch (InterruptedException e) {
                success = false;
                Thread.currentThread().interrupt();
            }
        } finally {
            listening.countDown();
            log(success ? "서버 종료 완료" : "서버 종료 중 실패 또는 시간 초과");
            closed.countDown();
        }
    }
}
