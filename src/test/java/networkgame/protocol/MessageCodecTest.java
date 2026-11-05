package networkgame.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

class MessageCodecTest {

    // JSON의 큰따옴표를 전부 이스케이프하면 읽기 어려워서 작은따옴표로 쓰고 여기서 바꾼다.
    private static String json(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    @Nested
    @DisplayName("클라이언트 메시지")
    class ClientMessages {

        @Test
        @DisplayName("JOIN은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsJoin() {
            // given
            ClientMsg join = new ClientMsg.Join("Kim", 2);

            // when
            String line = MessageCodec.encode(join);
            ClientMsg decoded = MessageCodec.decodeClient(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'JOIN','name':'Kim','character':2}"));
            assertThat(decoded).isEqualTo(join);
        }

        @Test
        @DisplayName("CHAT은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsChat() {
            // given
            ClientMsg chat = new ClientMsg.Chat("안녕");

            // when
            String line = MessageCodec.encode(chat);
            ClientMsg decoded = MessageCodec.decodeClient(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'CHAT','text':'안녕'}"));
            assertThat(decoded).isEqualTo(chat);
        }

        @Test
        @DisplayName("PONG은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsPong() {
            // given
            ClientMsg pong = new ClientMsg.Pong(1_730_000_000_000L);

            // when
            String line = MessageCodec.encode(pong);
            ClientMsg decoded = MessageCodec.decodeClient(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'PONG','t':1730000000000}"));
            assertThat(decoded).isEqualTo(pong);
        }

        @Test
        @DisplayName("MOVE는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsMove() {
            // given
            ClientMsg move = new ClientMsg.Move(780.5, 560.0, Direction.RIGHT);

            // when
            String line = MessageCodec.encode(move);
            ClientMsg decoded = MessageCodec.decodeClient(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'MOVE','x':780.5,'y':560.0,'direction':'RIGHT'}"));
            assertThat(decoded).isEqualTo(move);
        }

        @Test
        @DisplayName("READY는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsReady() {
            // given
            ClientMsg ready = new ClientMsg.Ready(true);

            // when
            String line = MessageCodec.encode(ready);
            ClientMsg decoded = MessageCodec.decodeClient(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'READY','ready':true}"));
            assertThat(decoded).isEqualTo(ready);
        }
    }

    @Nested
    @DisplayName("서버 메시지")
    class ServerMessages {

        @Test
        @DisplayName("WELCOME은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsWelcome() {
            // given
            PlayerInfo kim = new PlayerInfo(0, "Kim", 2, 770.0, 560.0, Direction.DOWN);
            PlayerInfo lee = new PlayerInfo(1, "Lee", 4, 910.0, 560.0, Direction.LEFT);
            ServerMsg welcome = new ServerMsg.Welcome(1, List.of(kim, lee));

            // when
            String line = MessageCodec.encode(welcome);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'WELCOME','id':1,'players':["
                    + "{'id':0,'name':'Kim','character':2,'x':770.0,'y':560.0,'direction':'DOWN'},"
                    + "{'id':1,'name':'Lee','character':4,'x':910.0,'y':560.0,'direction':'LEFT'}]}"));
            assertThat(decoded).isEqualTo(welcome);
        }

        @Test
        @DisplayName("REJECT는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsReject() {
            // given
            ServerMsg reject = new ServerMsg.Reject(RejectReason.FULL);

            // when
            String line = MessageCodec.encode(reject);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'REJECT','reason':'FULL'}"));
            assertThat(decoded).isEqualTo(reject);
        }

        @Test
        @DisplayName("JOINED는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsJoined() {
            // given
            PlayerInfo lee = new PlayerInfo(1, "Lee", 4, 910.0, 560.0, Direction.LEFT);
            ServerMsg joined = new ServerMsg.Joined(lee);

            // when
            String line = MessageCodec.encode(joined);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'JOINED','player':"
                    + "{'id':1,'name':'Lee','character':4,'x':910.0,'y':560.0,'direction':'LEFT'}}"));
            assertThat(decoded).isEqualTo(joined);
        }

        @Test
        @DisplayName("LEFT는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsLeft() {
            // given
            ServerMsg left = new ServerMsg.Left(1);

            // when
            String line = MessageCodec.encode(left);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'LEFT','id':1}"));
            assertThat(decoded).isEqualTo(left);
        }

        @Test
        @DisplayName("CHAT은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsChat() {
            // given
            ServerMsg chat = new ServerMsg.Chat(0, "안녕");

            // when
            String line = MessageCodec.encode(chat);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'CHAT','from':0,'text':'안녕'}"));
            assertThat(decoded).isEqualTo(chat);
        }

        @Test
        @DisplayName("줄바꿈이 든 CHAT도 한 줄로 나가고, 되돌리면 줄바꿈이 그대로 있다")
        void keepsMultilineChatOnOneLine() {
            // given
            ServerMsg chat = new ServerMsg.Chat(0, "첫 줄\r\n둘째 줄");

            // when
            String line = MessageCodec.encode(chat);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).doesNotContain("\n", "\r");
            assertThat(decoded).isEqualTo(chat);
        }

        @Test
        @DisplayName("PING은 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsPing() {
            // given
            ServerMsg ping = new ServerMsg.Ping(1_730_000_000_000L);

            // when
            String line = MessageCodec.encode(ping);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'PING','t':1730000000000}"));
            assertThat(decoded).isEqualTo(ping);
        }

        @Test
        @DisplayName("MOVED는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsMoved() {
            // given
            ServerMsg moved = new ServerMsg.Moved(0, 780.5, 560.0, Direction.RIGHT);

            // when
            String line = MessageCodec.encode(moved);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'MOVED','id':0,'x':780.5,'y':560.0,'direction':'RIGHT'}"));
            assertThat(decoded).isEqualTo(moved);
        }

        @Test
        @DisplayName("READY_STATE는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsReadyState() {
            // given
            ServerMsg readyState = new ServerMsg.ReadyState(List.of(0, 1));

            // when
            String line = MessageCodec.encode(readyState);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'READY_STATE','readyIds':[0,1]}"));
            assertThat(decoded).isEqualTo(readyState);
        }

        @Test
        @DisplayName("START는 정해 둔 JSON 한 줄로 바뀌고, 되돌리면 같은 값이다")
        void roundTripsStart() {
            // given
            ServerMsg start = new ServerMsg.Start();

            // when
            String line = MessageCodec.encode(start);
            ServerMsg decoded = MessageCodec.decodeServer(line);

            // then
            assertThat(line).isEqualTo(json("{'type':'START'}"));
            assertThat(decoded).isEqualTo(start);
        }
    }

    @Nested
    @DisplayName("잘못된 줄을 읽으면")
    class MalformedLines {

        @Test
        @DisplayName("JSON이 아니면 예외가 난다")
        void rejectsNonJson() {
            // given
            String line = "hello";

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("JSON이 중간에 끊겼으면 예외가 난다")
        void rejectsTruncatedJson() {
            // given
            String line = json("{'type':'JOIN',");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("모르는 type이면 예외가 난다")
        void rejectsUnknownType() {
            // given
            String line = json("{'type':'NOPE'}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("type이 없으면 예외가 난다")
        void rejectsMissingType() {
            // given
            String line = json("{'name':'Kim','character':2}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("필드가 빠졌으면 0으로 채우지 않고 예외가 난다")
        void rejectsMissingField() {
            // given
            String line = json("{'type':'JOIN','name':'Kim'}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("문자열 필드가 null이면 예외가 난다")
        void rejectsNullStringField() {
            // given
            String line = json("{'type':'JOIN','name':null,'character':2}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("숫자 필드가 null이면 예외가 난다")
        void rejectsNullNumberField() {
            // given
            String line = json("{'type':'JOIN','name':'Kim','character':null}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("record에 없는 필드가 있으면 무시하지 않고 예외가 난다")
        void rejectsUnknownField() {
            // given
            String line = json("{'type':'JOIN','name':'Kim','character':2,'admin':true}");

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("클라이언트 메시지 자리에 JSON null이 오면 null을 돌려주지 않고 예외가 난다")
        void rejectsNullClientMessage() {
            // given
            String line = "null";

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeClient(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("서버 메시지 자리에 JSON null이 오면 null을 돌려주지 않고 예외가 난다")
        void rejectsNullServerMessage() {
            // given
            String line = "null";

            // when
            Throwable thrown = catchThrowable(() -> MessageCodec.decodeServer(line));

            // then
            assertThat(thrown).isInstanceOf(JacksonException.class);
        }
    }
}
