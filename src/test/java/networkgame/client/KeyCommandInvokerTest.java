package networkgame.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyCommandInvokerTest {
    @Test
    @DisplayName("방향키를 누르면 그 방향이 눌린 상태가 된다")
    void pressesDirection() {
        // given
        JPanel source = new JPanel();
        Set<Integer> pressed = new HashSet<>();
        KeyCommandInvoker invoker = new KeyCommandInvoker();
        invoker.bind(KeyEvent.VK_RIGHT, () -> pressed.add(KeyEvent.VK_RIGHT),
                () -> pressed.remove(KeyEvent.VK_RIGHT));
        KeyEvent press = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = invoker.dispatchKeyEvent(press);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_RIGHT);
    }

    @Test
    @DisplayName("오른쪽·위 방향키를 누른 채 오른쪽 방향키를 놓으면 오른쪽 이동만 멈춘다")
    void releasesDirectionWithoutModifiers() {
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
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = invoker.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_UP);
    }

    @Test
    @DisplayName("Shift를 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithShift() {
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
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, InputEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = invoker.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_UP);
    }

    @Test
    @DisplayName("Ctrl을 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithCtrl() {
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
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = invoker.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_UP);
    }

    @Test
    @DisplayName("Shift와 Ctrl을 함께 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithShiftAndCtrl() {
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
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1,
                InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = invoker.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(KeyEvent.VK_UP);
    }
}
