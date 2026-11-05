package networkgame.client;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class KeyCommandInvoker implements KeyEventDispatcher {
    private record Commands(Runnable press, Runnable release) {
    }

    private final Map<Integer, Commands> bindings = new HashMap<>();

    void bind(int keyCode, Runnable press, Runnable release) {
        bindings.put(keyCode, new Commands(press, release));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Match the key code so modifier changes between press and release don't break the pair.
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
