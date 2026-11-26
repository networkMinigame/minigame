package networkgame.client.lobby.controller;

import networkgame.client.lobby.model.Player;
import networkgame.protocol.ClientMsg;

import java.util.function.Consumer;

/**
 * 내 고양이의 위치나 방향이 마지막으로 보낸 값과 다를 때만 MOVE를 보낸다.
 * <p>EDT에서만 부른다. 보내는 방법은 생성할 때 받아서, 소켓 없이 테스트할 수 있다.</p>
 */
final class MoveSender {
    private final Consumer<ClientMsg> send;

    private ClientMsg.Move lastSentMove;

    public MoveSender(Consumer<ClientMsg> send) {
        this.send = send;
    }

    void sendIfChanged(Player localPlayer) {
        ClientMsg.Move move = new ClientMsg.Move(
                localPlayer.x(),
                localPlayer.y(),
                localPlayer.direction()
        );
        if (move.equals(lastSentMove)) {
            return;
        }

        send.accept(move);
        lastSentMove = move;
    }
}
