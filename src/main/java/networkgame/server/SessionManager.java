package networkgame.server;

import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/** 입장자 목록을 관리한다. DispatcherLoop 스레드에서만 사용한다. */
final class SessionManager {
    private static final int MAX_PLAYERS = 4;
    private static final int SPAWN_X = 836;
    private static final int SPAWN_Y = 470;
    private static final int SPAWN_SPACING = 120;

    private final Map<BlockingQueue<ServerMsg>, PlayerInfo> players = new LinkedHashMap<>();

    /** 새 플레이어를 등록한다. 이미 입장했거나 정원이 찼으면 null을 반환한다. */
    PlayerInfo register(BlockingQueue<ServerMsg> sendQueue, String name, int character) {
        if (players.containsKey(sendQueue) || players.size() == MAX_PLAYERS) {
            return null;
        }

        int id = availableId();
        PlayerInfo player = new PlayerInfo(id, name, character,
                SPAWN_X + id * SPAWN_SPACING, SPAWN_Y, Direction.DOWN);
        players.put(sendQueue, player);
        return player;
    }

    void remove(BlockingQueue<ServerMsg> sendQueue) {
        players.remove(sendQueue);
    }

    List<PlayerInfo> players() {
        return List.copyOf(players.values());
    }

    List<BlockingQueue<ServerMsg>> sendQueues() {
        return List.copyOf(players.keySet());
    }

    private int availableId() {
        boolean[] used = new boolean[MAX_PLAYERS];
        for (PlayerInfo player : players.values()) {
            used[player.id()] = true;
        }
        for (int id = 0; id < MAX_PLAYERS; id++) {
            if (!used[id]) {
                return id;
            }
        }
        throw new IllegalStateException("빈 플레이어 ID가 없습니다.");
    }
}
