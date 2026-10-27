package networkgame.util;

import static networkgame.util.Log.log;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogTest {
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    @BeforeEach
    void captureOut() {
        System.setOut(new PrintStream(captured, true));
    }

    @AfterEach
    void restoreOut() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("로그를 남기면 호출한 시각, 스레드 번호와 이름, 메시지가 한 줄로 나온다")
    void printsTimeThreadAndMessage() throws InterruptedException {
        // given
        Thread reader = new Thread(() -> log("receiveQueue에 넣음 CHAT hi"), "Reader-1");
        LocalTime before = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);

        // when
        reader.start();
        reader.join();
        LocalTime after = LocalTime.now();

        // then
        String line = captured.toString().strip();
        Matcher m = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[#(\\d+) Reader-1] receiveQueue에 넣음 CHAT hi").matcher(line);
        assertThat(m.matches()).as(line).isTrue();
        assertThat(LocalTime.parse(m.group(1))).isBetween(before, after);
        assertThat(Long.parseLong(m.group(2))).isEqualTo(reader.threadId());
    }
}
