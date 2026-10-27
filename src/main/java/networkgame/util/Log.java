package networkgame.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Log {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    // 시각을 잠금 안에서 찍어야 출력 순서와 시각 순서가 같다. 잠금 밖에서 찍으면 먼저 찍은 스레드가 나중에 출력될 수 있다.
    public static synchronized void log(String message) {
        Thread thread = Thread.currentThread();
        System.out.println(LocalTime.now().format(TIME)
                + " [#" + thread.threadId() + " " + thread.getName() + "] " + message);
    }
}
