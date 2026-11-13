package networkgame.client.input;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Set;

import networkgame.protocol.Direction;

/** 방향키의 누름·뗌 동작을 등록하고, 키 이벤트에 맞춰 눌린 방향 목록을 갱신한다. EDT에서만 쓴다. */
final class ArrowKeyBindings implements KeyEventDispatcher {
    private record Commands(Runnable press, Runnable release) {
    }

    private final Map<Integer, Commands> bindings;

    ArrowKeyBindings(Set<Direction> pressed) {
        bindings = Map.of(
                KeyEvent.VK_UP, new Commands(() -> pressed.add(Direction.UP), () -> pressed.remove(Direction.UP)),
                KeyEvent.VK_DOWN, new Commands(() -> pressed.add(Direction.DOWN), () -> pressed.remove(Direction.DOWN)),
                KeyEvent.VK_LEFT, new Commands(() -> pressed.add(Direction.LEFT), () -> pressed.remove(Direction.LEFT)),
                KeyEvent.VK_RIGHT, new Commands(() -> pressed.add(Direction.RIGHT), () -> pressed.remove(Direction.RIGHT)));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 누름과 뗌 사이에 Shift·Ctrl 상태가 달라져도 같은 키로 처리한다.
        Commands commands = bindings.get(event.getKeyCode());
        if (commands == null) {
            return false;
        }
        switch (event.getID()) {
            case KeyEvent.KEY_PRESSED -> commands.press().run();
            case KeyEvent.KEY_RELEASED -> commands.release().run();
            default -> { return false; }
        }
        return true;
    }
}
