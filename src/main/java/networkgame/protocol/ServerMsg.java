package networkgame.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

/**
 * 서버가 클라이언트로 보내는 메시지. 형식은 {@link ClientMsg}와 같다.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public sealed interface ServerMsg {

    /** JOIN한 본인에게만 보낸다. {@code id}는 받는 사람의 playerId, {@code players}는 본인을 포함한 현재 접속자. */
    @JsonTypeName("WELCOME")
    record Welcome(int id, List<PlayerInfo> players) implements ServerMsg {
        // 같은 메시지 객체를 여러 sendQueue에 넣으면 Writer 스레드들이 같이 읽는다. 바뀌지 않는 목록으로 복사해 둔다.
        public Welcome {
            players = List.copyOf(players);
        }
    }

    /** 입장 거절. 보낸 뒤 서버가 연결을 닫는다. */
    @JsonTypeName("REJECT")
    record Reject(RejectReason reason) implements ServerMsg {
    }

    /** 새로 들어온 사람을 기존 접속자에게 알린다. */
    @JsonTypeName("JOINED")
    record Joined(PlayerInfo player) implements ServerMsg {
    }

    @JsonTypeName("LEFT")
    record Left(int id) implements ServerMsg {
    }

    /** {@code from}은 보낸 사람의 playerId. 서버가 채운다. */
    @JsonTypeName("CHAT")
    record Chat(int from, String text) implements ServerMsg {
    }

    /** {@code t}는 서버가 보낸 시각. 클라이언트는 해석하지 않고 {@link ClientMsg.Pong}으로 그대로 돌려준다. */
    @JsonTypeName("PING")
    record Ping(long t) implements ServerMsg {
    }

    /** 다른 사람의 로비 위치와 바라보는 방향. */
    @JsonTypeName("MOVED")
    record Moved(int id, double x, double y, Direction direction) implements ServerMsg {
    }

    /** 준비한 사람의 playerId 목록. */
    @JsonTypeName("READY_STATE")
    record ReadyState(List<Integer> readyIds) implements ServerMsg {
        public ReadyState {
            readyIds = List.copyOf(readyIds);
        }
    }

    /** 전원 준비가 끝나 게임을 시작한다. */
    @JsonTypeName("START")
    record Start() implements ServerMsg {
    }
}
