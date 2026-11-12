package networkgame.client.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.protocol.Direction;

class ArrowKeyBindingsTest {
    @Test
    @DisplayName("네 방향키를 누르면 각각의 방향이 눌린 상태가 된다")
    void pressesAllDirections() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        int[] keyCodes = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};

        // when
        for (int keyCode : keyCodes) {
            bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                    keyCode, KeyEvent.CHAR_UNDEFINED));
        }

        // then
        assertThat(pressed).containsExactlyInAnyOrder(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT);
    }

    @Test
    @DisplayName("눌린 네 방향키를 모두 놓으면 눌린 방향이 남지 않는다")
    void releasesAllDirections() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.allOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        int[] keyCodes = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};

        // when
        for (int keyCode : keyCodes) {
            bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0,
                    keyCode, KeyEvent.CHAR_UNDEFINED));
        }

        // then
        assertThat(pressed).isEmpty();
    }

    @Test
    @DisplayName("방향키를 누르면 그 방향이 눌린 상태가 된다")
    void pressesDirection() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        KeyEvent press = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = bindings.dispatchKeyEvent(press);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(Direction.RIGHT);
    }

    @Test
    @DisplayName("오른쪽·위 방향키를 누른 채 오른쪽 방향키를 놓으면 오른쪽 이동만 멈춘다")
    void releasesDirectionWithoutModifiers() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = bindings.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(Direction.UP);
    }

    @Test
    @DisplayName("Shift를 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithShift() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, InputEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = bindings.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(Direction.UP);
    }

    @Test
    @DisplayName("Ctrl을 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithCtrl() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1, InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = bindings.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(Direction.UP);
    }

    @Test
    @DisplayName("Shift와 Ctrl을 함께 누른 채 오른쪽 방향키를 놓아도 오른쪽 이동만 멈춘다")
    void releasesDirectionWithShiftAndCtrl() {
        // given
        JPanel source = new JPanel();
        Set<Direction> pressed = EnumSet.noneOf(Direction.class);
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        bindings.dispatchKeyEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        KeyEvent release = new KeyEvent(source, KeyEvent.KEY_RELEASED, 1,
                InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        // when
        boolean handled = bindings.dispatchKeyEvent(release);

        // then
        assertThat(handled).isTrue();
        assertThat(pressed).containsExactly(Direction.UP);
    }
}
