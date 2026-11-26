package networkgame.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClientMainTest {
    @Test
    @DisplayName("첫 실행 인자를 플레이어 이름으로 사용한다")
    void usesArgumentName() {
        // given
        String[] args = {"모카"};

        // when
        String name = ClientMain.playerName(args);

        // then
        assertThat(name).isEqualTo("모카");
    }

    @Test
    @DisplayName("이름 앞뒤 공백을 제거한다")
    void stripsName() {
        // given
        String[] args = {"  구름  "};

        // when
        String name = ClientMain.playerName(args);

        // then
        assertThat(name).isEqualTo("구름");
    }
}
