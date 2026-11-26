package networkgame.server;

import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;

import java.util.*;

/** 입장자 목록을 관리한다. DispatcherLoop 스레드에서만 사용한다. */
final class PlayerManager {
    private static final int MAX_PLAYERS = 4;
    private static final int SPAWN_X = 836;
    private static final int SPAWN_Y = 470;
    private static final int SPAWN_SPACING = 120;

    // 클라이언트 LobbyMovement와 같은 범위. 지도 map/lobby.png(1672x941)와 고양이 그림 크기(104px)로 계산한 값이다.
    // 좌우는 그림 반 폭(52)만큼 안쪽, 위는 그림 높이와 이름표(104 + 32)만큼 아래까지다.
    private static final double MIN_X = 52;
    private static final double MAX_X = 1620;
    private static final double MIN_Y = 136;
    private static final double MAX_Y = 941;

    private final Map<Long, PlayerInfo> players = new LinkedHashMap<>();

    private final Set<Long> movedSessionIds = new LinkedHashSet<>();

    /**
     * 빈 ID를 배정하고 새 플레이어를 등록한다.
     *
     * @param sessionId 입장하려는 세션의 식별자
     * @param name 입장 요청에 담긴 플레이어 이름
     * @param character 입장 요청에 담긴 캐릭터 번호
     * @return 등록한 플레이어. 이미 입장했거나 정원이 찼으면 {@code null}
     */
    PlayerInfo register(long sessionId, String name, int character) {
        if (players.containsKey(sessionId) || players.size() == MAX_PLAYERS) {
            return null;
        }

        int id = availableId();
        PlayerInfo player = new PlayerInfo(id, name, character,
                SPAWN_X + id * SPAWN_SPACING, SPAWN_Y, Direction.DOWN);
        players.put(sessionId, player);
        return player;
    }

    /**
     * 입장한 플레이어의 위치와 방향을 바꾸고 MOVED를 보낼 대상으로 표시한다.
     */
    void move(long sessionId, double x, double y, Direction direction) {
        PlayerInfo playerInfo = players.get(sessionId);
        if (playerInfo == null) {
            return;
        }

        PlayerInfo movePlayerInfo = new PlayerInfo(
                playerInfo.id(),
                playerInfo.name(),
                playerInfo.character(),
                Math.clamp(x, MIN_X, MAX_X),
                Math.clamp(y, MIN_Y, MAX_Y),
                direction
        );
        if (movePlayerInfo.equals(playerInfo)) {
            return;
        }
        players.put(sessionId, movePlayerInfo);

        movedSessionIds.add(sessionId);
    }

    /**
     * 연결이 끝난 세션을 목록에서 제거한다. 입장 기록이 없으면 그대로 종료한다.
     *
     * @param sessionId 제거할 세션의 식별자
     * @return 제거한 플레이어. 이미 나갔거나 입장하지 않은 세션이면 {@code null}
     */
    PlayerInfo remove(long sessionId) {
        movedSessionIds.remove(sessionId);
        return players.remove(sessionId);
    }

    /** 입장 가능한지 검사한다. 정원 초과를 이름 중복보다 우선 판정한다. */
    networkgame.protocol.RejectReason rejectionReason(String name) {
        if (players.size() >= MAX_PLAYERS) {
            return networkgame.protocol.RejectReason.FULL;
        }
        Collection<PlayerInfo> joinedPlayers = players.values();
        for (PlayerInfo player : joinedPlayers) {
            String joinedName = player.name();
            if (joinedName.equals(name)) {
                return networkgame.protocol.RejectReason.NAME_TAKEN;
            }
        }
        return null;
    }

    /** 현재 입장자 정보를 등록 순서대로 복사해 반환한다. */
    List<PlayerInfo> players() {
        return List.copyOf(players.values());
    }

    /** 입장한 플레이어의 세션 식별자를 등록 순서대로 모아 반환한다. */
    List<Long> sessionIds() {
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

    /**
     * {@link #clearMoved()}뒤로 위치가 바뀐 플레이어를 세션 식별자와 함께 바뀐 순서대로 돌려준다.
     * @return
     */
    Map<Long, PlayerInfo> movedPlayerInfos() {
        Map<Long, PlayerInfo> movedPlayerInfos = new LinkedHashMap<>();
        for (Long movedSessionId : movedSessionIds) {
            movedPlayerInfos.put(movedSessionId, players.get(movedSessionId));
        }
        return movedPlayerInfos;
    }

    /**
     * 위치가 바뀌었다는 표시를 모두 지운다. MOVED를 보낸 뒤 호출한다.
     */
    void clearMoved() {
        movedSessionIds.clear();
    }
}
