package networkgame.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;

class ClientMainTest {
    @Test
    @DisplayName("입력한 이름을 플레이어 이름으로 사용한다")
    void usesArgumentName() {
        // given
        String input = "모카";

        // when
        String name = NamePrompt.readName(() -> input);

        // then
        assertThat(name).isEqualTo("모카");
    }

    @Test
    @DisplayName("이름 앞뒤 공백을 제거한다")
    void stripsName() {
        // given
        String input = "  구름  ";

        // when
        String name = NamePrompt.readName(() -> input);

        // then
        assertThat(name).isEqualTo("구름");
    }

    @Test
    @DisplayName("이름 입력을 취소하면 접속할 이름을 반환하지 않는다")
    void cancelsName() {
        // given
        Supplier<String> input = () -> null;

        // when
        String name = NamePrompt.readName(input);

        // then
        assertThat(name).isNull();
    }

    @Test
    @DisplayName("빈 이름은 다시 입력받는다")
    void retriesBlankName() {
        // given
        Iterator<String> inputs = List.of("   ", "모카").iterator();

        // when
        String name = NamePrompt.readName(inputs::next);

        // then
        assertThat(name).isEqualTo("모카");
    }
}
