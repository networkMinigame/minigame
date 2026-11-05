package networkgame.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 클라이언트가 서버로 보내는 메시지. 한 줄에 JSON 하나로 보내고, {@code type} 필드로 어떤 record인지 구분한다.
 * 보낸 사람은 넣지 않는다. 서버 Reader가 메시지를 받은 소켓을 보고 붙인다.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public sealed interface ClientMsg {

    // 하위 record는 Jackson이 sealed 선언에서 찾아서 @JsonSubTypes가 필요 없다.
    // 대신 @JsonTypeName을 빠뜨리면 type이 클래스 이름(ClientMsg$Join)으로 나가므로 record마다 붙인다.

    /** 접속 직후 한 번 보낸다. {@code character}는 고른 고양이 번호. */
    @JsonTypeName("JOIN")
    record Join(String name, int character) implements ClientMsg {
    }

    @JsonTypeName("CHAT")
    record Chat(String text) implements ClientMsg {
    }

    /** 받은 {@link ServerMsg.Ping}의 {@code t}를 그대로 돌려보낸다. */
    @JsonTypeName("PONG")
    record Pong(long t) implements ClientMsg {
    }

    /** 로비에서 내 고양이의 위치와 바라보는 방향. */
    @JsonTypeName("MOVE")
    record Move(double x, double y, Direction direction) implements ClientMsg {
    }

    @JsonTypeName("READY")
    record Ready(boolean ready) implements ClientMsg {
    }
}
