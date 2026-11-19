package networkgame.protocol;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 메시지와 한 줄 JSON 문자열을 서로 바꾼다.
 * 바꾼 문자열에는 줄바꿈이 없어서(문자열 안의 줄바꿈은 {@code \n}으로 이스케이프된다) 그대로 한 줄로 보내면 된다.
 */
public final class MessageCodec {

    // Jackson 3의 JsonMapper는 만든 뒤 설정이 바뀌지 않아서 Reader·Writer·DispatcherLoop 스레드가 하나를 같이 써도 된다.
    // 기본 설정은 잘못 만든 메시지를 조용히 받아 준다. 아래 설정으로 그런 줄을 받을 때 예외가 나게 한다.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            // 필드가 빠진 줄. 기본값이면 {"type":"JOIN","name":"Kim"}이 character=0으로 들어온다.
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            // 필드 값이 null인 줄. 기본값이면 "name":null이 그대로 null로 들어온다.
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            // int·long 같은 기본형 필드가 null인 줄. Jackson 3에서는 기본으로 켜져 있지만, 버전이 바뀌어도 유지되게 적어 둔다.
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            // record에 없는 필드가 든 줄. 기본값이면 무시해서, 필드 이름 오타("charater")가 0으로 조용히 들어온다.
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private MessageCodec() {
    }

    public static String encode(ClientMsg message) {
        return MAPPER.writeValueAsString(message);
    }

    public static String encode(ServerMsg message) {
        return MAPPER.writeValueAsString(message);
    }

    /**
     * 서버가 보낸 한 줄을 메시지로 바꾼다.
     *
     * @param line 줄바꿈을 뗀 한 줄 ({@code readLine()} 결과)
     * @throws JacksonException JSON이 깨졌거나, 줄 전체가 {@code null}이거나, 모르는 {@code type}이거나, 필드가 빠졌거나 null이거나 남는 경우
     */
    public static ServerMsg decodeServer(String line) {
        return decode(line, ServerMsg.class);
    }

    /**
     * 클라이언트가 보낸 한 줄을 메시지로 바꾼다.
     *
     * @param line 줄바꿈을 뗀 한 줄 ({@code readLine()} 결과)
     * @throws JacksonException JSON이 깨졌거나, 줄 전체가 {@code null}이거나, 모르는 {@code type}이거나, 필드가 빠졌거나 null이거나 남는 경우
     */
    public static ClientMsg decodeClient(String line) {
        return decode(line, ClientMsg.class);
    }

    public static <T> T decode(String line, Class<T> type) {
        T message = MAPPER.readValue(line, type);

        if(message == null) {
            throw MismatchedInputException.from(
                    null,
                    type,
                    "메시지 자리에 null이 왔습니다."
            );
        }

        return message;
    }
}
