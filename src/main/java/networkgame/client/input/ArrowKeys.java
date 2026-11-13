package networkgame.client.input;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JFrame;

import networkgame.protocol.Direction;

/**
 * 창이 포커스를 가졌을 때 눌린 방향키를 모아 둔다. EDT에서만 쓴다.
 * <p>만들자마자 창에 키 입력이 걸리고, 창을 닫으면 저절로 떨어진다.
 */
public final class ArrowKeys {
    // 현재 눌린 방향키를 저장
    private final EnumSet<Direction> pressed = EnumSet.noneOf(Direction.class);

    // 생성자에서 키 입력을 걸어도, 키 이벤트는 생성자와 같은 EDT에서 처리되므로 생성이 끝나기 전에 들어오지 않는다.
    public ArrowKeys(JFrame window) {
        ArrowKeyBindings bindings = new ArrowKeyBindings(pressed);

        // KeyListener는 키보드 포커스를 가진 부품만 받아서, 포커스가 어디 있든 받도록 KeyboardFocusManager에 건다.
        // 여기 건 디스패처는 이 프로그램의 모든 창의 키를 받으므로 이 창이 포커스를 가졌을 때만 처리한다.
        KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyEventDispatcher dispatcher = event -> window.isFocused() && bindings.dispatchKeyEvent(event);
        focus.addKeyEventDispatcher(dispatcher);

        WindowAdapter lifecycle = new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                // 키를 누른 채 다른 창으로 가면 뗀 이벤트가 오지 않아서, 비워 두지 않으면 고양이가 계속 걷는다.
                pressed.clear();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                // 디스패처는 창이 아니라 KeyboardFocusManager에 걸려 있어서 창을 닫아도 직접 떼야 한다.
                focus.removeKeyEventDispatcher(dispatcher);
            }
        };
        window.addWindowFocusListener(lifecycle);
        window.addWindowListener(lifecycle);
    }

    /** 지금 눌려 있는 방향키. 고칠 수 없고, 키를 누르거나 떼면 같이 바뀐다. */
    public Set<Direction> pressed() {
        return Collections.unmodifiableSet(pressed);
    }
}
