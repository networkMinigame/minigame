package networkgame.server;

import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 입장자 목록을 관리한다. DispatcherLoop 스레드에서만 사용한다. */
final class PlayerManager {
    private static final int MAX_PLAYERS = 4;
    private static final int SPAWN_X = 836;
    private static final int SPAWN_Y = 470;
    private static final int SPAWN_SPACING = 120;

    private final Map<Long, PlayerInfo> players = new LinkedHashMap<>();

    /**
     * 빈 ID를 배정하고 새 플레이어를 등록한다.
     *
     * @param connectionId 입장하려는 TCP 연결의 식별자
     * @param name 입장 요청에 담긴 플레이어 이름
     * @param character 입장 요청에 담긴 캐릭터 번호
     * @return 등록한 플레이어. 이미 입장했거나 정원이 찼으면 {@code null}
     */
    PlayerInfo register(long connectionId, String name, int character) {
        if (players.containsKey(connectionId) || players.size() == MAX_PLAYERS) {
            return null;
        }

        int id = availableId();
        PlayerInfo player = new PlayerInfo(id, name, character,
                SPAWN_X + id * SPAWN_SPACING, SPAWN_Y, Direction.DOWN);
        players.put(connectionId, player);
        return player;
    }

    /**
     * 연결이 끝난 세션을 목록에서 제거한다. 입장 기록이 없으면 그대로 종료한다.
     *
     * @param connectionId 제거할 TCP 연결의 식별자
     */
    void remove(long connectionId) {
        players.remove(connectionId);
    }

    /** 현재 입장자 정보를 등록 순서대로 복사해 반환한다. */
    List<PlayerInfo> players() {
        return List.copyOf(players.values());
    }

    /** 입장한 플레이어의 연결 식별자를 등록 순서대로 모아 반환한다. */
    List<Long> connectionIds() {
        return List.copyOf(players.keySet());
    }

    /** 현재 비어 있는 ID 중 가장 작은 값을 반환한다. 빈자리가 있을 때 호출한다. */
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
