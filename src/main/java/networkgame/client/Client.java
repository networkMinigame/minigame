package networkgame.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static networkgame.util.Log.log;

public class Client {

    private final Socket socket;
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private final Thread readThread;
    private final Thread writeThread;
    private final Thread inputThread;
    private final AtomicBoolean closed = new AtomicBoolean();

    public Client(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.readThread = new Thread(new ReadHandler(this, socket), "readHandler");
        this.writeThread = new Thread(new WriteHandler(this, socket, sendQueue), "writeHandler");
        this.inputThread = new Thread(this::readConsole, "input");

        // 콘솔 입력 대기는 끊을 방법이 없다. 데몬으로 두면 연결이 끝났을 때 엔터를 기다리지 않고 프로세스와 같이 끝난다.
        inputThread.setDaemon(true);

        log("서버 연결 " + host + ":" + port);
    }

    public void start() throws InterruptedException {
        readThread.start();
        writeThread.start();
        inputThread.start();

        readThread.join();
        writeThread.join();
    }

    private void readConsole() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isBlank()) {
                continue;
            }
            sendQueue.add(line);
        }

        close();
    }

    // Reader, Writer, 콘솔 입력이 동시에 부를 수 있어서 CAS로 한 번만 통과시킨다.
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            log("소켓 닫기 실패 " + e.getMessage());
        }

        // sendQueue.take()에서 기다리는 Writer는 소켓을 닫아도 안 깨어나서 interrupt로 깨운다.
        writeThread.interrupt();

        log("서버 연결 종료");
    }
}
