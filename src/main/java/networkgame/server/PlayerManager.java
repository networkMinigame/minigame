package networkgame.server;

import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.RejectReason;

import networkgame.protocol.Direction;
import networkgame.protocol.PlayerInfo;

import java.util.*;

/** 입장자 목록을 관리한다. DispatcherLoop 스레드에서만 사용한다. */
final class PlayerManager {
    private static final int MAX_PLAYERS = 4;

    /** 입장 시 배정할 고양이 번호 순서. 0~3은 각각 player1~player4 그림에 대응한다. */
    private static final int[] CHARACTER_ORDER = {0, 1, 2, 3};
    private static final int SPAWN_X = 836;
    private static final int SPAWN_Y = 470;
    private static final int SPAWN_SPACING = 120;

    private final LobbyWalkArea walkArea = LobbyWalkArea.standard();

    private final Map<Long, PlayerInfo> players = new LinkedHashMap<>();

    private final Set<Long> movedSessionIds = new LinkedHashSet<>();

    /**
     * 빈 ID와 사용하지 않는 첫 고양이 번호를 배정하고 새 플레이어를 등록한다.
     *
     * @param sessionId 입장하려는 세션의 식별자
     * @param name 입장 요청에 담긴 플레이어 이름
     * @return 등록한 플레이어. 이미 입장했거나 정원이 찼으면 {@code null}
     */
    PlayerInfo register(long sessionId, String name) {
        if (players.containsKey(sessionId) || players.size() == MAX_PLAYERS) {
            return null;
        }

        int id = availableId();
        int character = availableCharacter();
        PlayerInfo player = new PlayerInfo(id, name, character,
                SPAWN_X + id * SPAWN_SPACING, SPAWN_Y, Direction.DOWN);
        players.put(sessionId, player);
        return player;
    }

    /**
     * 마지막으로 승인한 위치부터 요청한 좌표까지 흙길 위에 있을 때만 이동을 반영한다.
     * 위치나 방향이 바뀌면 MOVED를 보낼 대상으로 표시한다.
     */
    void move(long sessionId, double x, double y, Direction direction) {
        PlayerInfo playerInfo = players.get(sessionId);
        if (playerInfo == null) {
            return;
        }
        if (!walkArea.canMove(playerInfo.x(), playerInfo.y(), x, y)) {
            return;
        }

        PlayerInfo movePlayerInfo = new PlayerInfo(
                playerInfo.id(),
                playerInfo.name(),
                playerInfo.character(),
                x,
                y,
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
    RejectReason rejectionReason(String name) {
        if (players.size() >= MAX_PLAYERS) {
            return RejectReason.FULL;
        }
        Collection<PlayerInfo> joinedPlayers = players.values();
        for (PlayerInfo player : joinedPlayers) {
            String joinedName = player.name();
            if (joinedName.equals(name)) {
                return RejectReason.NAME_TAKEN;
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

    /**
     * 배정 순서에서 현재 사용하지 않는 첫 고양이 번호를 반환한다.
     * 빈자리가 있을 때 호출하며, 플레이어 목록은 변경하지 않는다.
     *
     * @throws IllegalStateException 배정 가능한 고양이가 없는 경우
     */
    private int availableCharacter() {
        Collection<PlayerInfo> joinedPlayers = players.values();
        for (int character : CHARACTER_ORDER) {
            boolean used = false;
            for (PlayerInfo player : joinedPlayers) {
                if (player.character() == character) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return character;
            }
        }
        throw new IllegalStateException("배정 가능한 고양이가 없습니다.");
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
