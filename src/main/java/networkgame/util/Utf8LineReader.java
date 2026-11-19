package networkgame.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/** 바이트 입력에서 줄을 구분하고 완성된 줄을 UTF-8 문자열로 변환한다. 단일 Reader에서 사용한다. */
public final class Utf8LineReader {
    private final BufferedInputStream input;
    private boolean skipLineFeed;

    /** 세션이 관리하는 입력 스트림을 버퍼링한다. 스트림 종료는 세션이 맡는다. */
    public Utf8LineReader(InputStream input) {
        this.input = new BufferedInputStream(input);
    }

    /**
     * LF, CR 또는 CRLF까지 읽어 줄 구분자를 제외한 문자열을 반환한다.
     * UTF-8 문자의 바이트가 여러 번에 나뉘어 들어와도 한 줄을 모은 뒤 변환한다.
     * EOF 직전의 마지막 줄도 반환하며, 읽을 바이트가 없으면 null을 반환한다.
     *
     * @throws IOException 입력 스트림에서 읽는 중 오류가 발생한 경우
     */
    public String readLine() throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (skipLineFeed) {
                skipLineFeed = false;
                if (value == '\n') {
                    continue;
                }
            }
            if (value == '\n' || value == '\r') {
                skipLineFeed = value == '\r';
                return line.toString(UTF_8);
            }
            line.write(value);
        }
        return line.size() == 0 ? null : line.toString(UTF_8);
    }
}
