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
public class Server {
    private static final long SHUTDOWN_NANOS = Duration.ofSeconds(3).toNanos();
    private final int port;
    private final Object lifecycle = new Object();
    private final SessionManager sessions = new SessionManager();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final CountDownLatch listening = new CountDownLatch(1);
    private ServerSocket listener;
    private Thread loopThread;
    private long nextSessionId;
    private boolean started;

    /** 수신 포트를 지정한다. 0이면 운영체제가 빈 포트를 배정한다. */
    public Server(int port) {
        this.port = port;
    }

    /** 요청 루프를 시작하고 접속을 받는다. 반환 또는 실패 시 전체 종료를 수행한다.
     * @throws IOException 수신 소켓을 열거나 접속을 받는 중 오류가 발생한 경우
     */
    public void start() throws IOException {
        DispatcherLoop dispatcher;
        try {
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
                dispatcher = new DispatcherLoop(sessions);
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
                        Session session = new Session(socket, nextSessionId++, sessions, dispatcher);
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
     * 종료 시작을 한 번 승인하고 리스너, 모든 세션, 요청 루프를 순서대로 정리한다.
     * 자원별 실패를 기록하며 나머지 자원의 종료 요청을 계속한다.
     * DispatcherLoop 종료는 잠금 밖에서 최대 3초 기다린다.
     */
    public void close() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }

        long deadline = System.nanoTime() + SHUTDOWN_NANOS;
        ServerSocket serverSocket;
        Thread loop;
        synchronized (lifecycle) {
            serverSocket = listener;
            loop = loopThread;
        }

        boolean success = true;
        try {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException | RuntimeException e) {
                success = false;
                log("리스너 종료 실패 " + e.getMessage());
            }

            try {
                success = sessions.closeAll() && success;
            } catch (RuntimeException e) {
                success = false;
                log("세션 전체 종료 실패 " + e.getMessage());
            }

            try {
                if (loop != null) {
                    loop.interrupt();
                    if (loop != Thread.currentThread()) {
                        long remaining = deadline - System.nanoTime();
                        if (remaining > 0) {
                            loop.join(Duration.ofNanos(remaining));
                        }
                        if (loop.isAlive()) {
                            success = false;
                            log("DispatcherLoop 종료 시간 초과");
                        }
                    }
                }
            } catch (InterruptedException e) {
                success = false;
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                success = false;
                log("DispatcherLoop 종료 실패 " + e.getMessage());
            }
        } finally {
            listening.countDown();
            log(success ? "서버 종료 요청 처리 완료" : "서버 종료 중 실패 또는 시간 초과");
        }
    }
}
