package networkgame.client;

import java.util.function.Supplier;
import javax.swing.JOptionPane;

/** 서버에 연결하기 전에 플레이어 이름만 입력받는다. EDT에서 호출한다. */
final class NamePrompt {
    private NamePrompt() {}

    /** 취소하거나 창을 닫으면 null을 반환한다. 빈 이름은 다시 입력받는다. */
    static String show() {
        return readName(() -> JOptionPane.showInputDialog(
                null, "이름을 입력해 주세요.", "플레이어 이름",
                JOptionPane.QUESTION_MESSAGE));
    }

    /** 앞뒤 공백을 제거하며 빈 입력은 재시도한다. 취소는 그대로 호출자에게 전달한다. */
    static String readName(Supplier<String> input) {
        while (true) {
            String name = input.get();
            if (name == null) {
                return null;
            }
            if (!name.isBlank()) {
                return name.strip();
            }
        }
    }
}
