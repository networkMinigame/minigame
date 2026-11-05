package networkgame.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KeyCommandInvokerTest {
    @ParameterizedTest
    @ValueSource(ints = {0, InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK})
    @DisplayName("보조 키를 누른 채 방향키를 놓으면 해당 방향의 이동만 멈춘다")
    void releasesDirectionRegardlessOfModifiers(int modifiers) {
        // given
        JPanel source = new JPanel();
        Set<Integer> pressed = new HashSet<>();
        KeyCommandInvoker invoker = new KeyCommandInvoker();
        invoker.bind(KeyEvent.VK_RIGHT, () -> pressed.add(KeyEvent.VK_RIGHT),
                () -> pressed.remove(KeyEvent.VK_RIGHT));
        invoker.bind(KeyEvent.VK_UP, () -> pressed.add(KeyEvent.VK_UP),
                () -> pressed.remove(KeyEvent.VK_UP));
        invoker.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        invoker.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        assertThat(pressed).containsExactlyInAnyOrder(KeyEvent.VK_RIGHT, KeyEvent.VK_UP);

        // when
        boolean handled = invoker.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_RELEASED, 1,
                modifiers, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_UP);
    }
}
