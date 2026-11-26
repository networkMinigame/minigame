package networkgame.client.lobby.controller;

import networkgame.client.lobby.model.Player;
import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.ClientMsg;

import java.util.function.Consumer;

/**
 * 내 고양이의 위치나 방향이 마지막으로 보낸 값과 다를 때만 MOVE를 보낸다.
 * <p>EDT에서만 부른다. 보내는 방법은 생성할 때 받아서, 소켓 없이 테스트할 수 있다.</p>
 */
final class MoveSender {
    private final Consumer<ClientMsg> send;
    private final LobbyWalkArea walkArea;

    private ClientMsg.Move lastSentMove;

    MoveSender(Consumer<ClientMsg> send, LobbyWalkArea walkArea) {
        this.send = send;
        this.walkArea = walkArea;
    }

    /** 이전 전송 위치에서 다음 프레임 위치까지 직선으로 갈 수 없으면 경유 위치를 먼저 보낸다. */
    void sendWaypointIfNeeded(Player current, Player next) {
        if (current.x() == next.x() && current.y() == next.y()) {
            return;
        }
        if (lastSentMove == null || !walkArea.canMove(lastSentMove.x(), lastSentMove.y(), next.x(), next.y())) {
            sendIfChanged(current);
        }
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
