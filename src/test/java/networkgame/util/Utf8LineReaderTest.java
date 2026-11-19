package networkgame.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class Utf8LineReaderTest {
    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    @DisplayName("줄 구분자가 달라도 한글 메시지와 빈 줄을 구분한다")
    void readsUtf8Lines(String separator) throws IOException {
        // given
        byte[] bytes = ("모카😺" + separator + separator + "구름" + separator).getBytes(UTF_8);
        Utf8LineReader reader = new Utf8LineReader(new ByteArrayInputStream(bytes));

        // when
        String first = reader.readLine();
        String empty = reader.readLine();
        String second = reader.readLine();

        // then
        assertThat(first).isEqualTo("모카😺");
        assertThat(empty).isEmpty();
        assertThat(second).isEqualTo("구름");
        assertThat(reader.readLine()).isNull();
    }

    @Test
    @DisplayName("한글 바이트가 나뉘어 도착해도 EOF 앞의 마지막 줄을 복원한다")
    void readsFragmentedFinalLine() throws IOException {
        // given
        ByteArrayInputStream input = new ByteArrayInputStream("별😺".getBytes(UTF_8)) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                return super.read(buffer, offset, Math.min(length, 1));
            }
        };
        Utf8LineReader reader = new Utf8LineReader(input);

        // when
        String line = reader.readLine();

        // then
        assertThat(line).isEqualTo("별😺");
        assertThat(reader.readLine()).isNull();
    }

    @Test
    @DisplayName("빈 스트림에서는 메시지 없이 EOF를 반환한다")
    void readsEmptyStream() throws IOException {
        // given
        Utf8LineReader reader = new Utf8LineReader(new ByteArrayInputStream(new byte[0]));

        // when
        String line = reader.readLine();

        // then
        assertThat(line).isNull();
    }
}
