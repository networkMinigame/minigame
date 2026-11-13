package networkgame.client.contentpane.lobby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import networkgame.protocol.Direction;

/**
 * 로비에 그릴 플레이어 상태. EDT에서만 읽고 바꾼다. 네트워크 스레드는 {@code invokeLater}로 넘겨서 바꾼다.
 */
final class LobbyState {

    // playerId → Player. 목록을 들어온 순서대로 돌려주려고 LinkedHashMap을 쓴다.
    private final Map<Integer, Player> players = new LinkedHashMap<>();

    /**
     * 플레이어를 새 좌표와 방향으로 옮긴다. 이름과 캐릭터는 그대로 둔다.
     * 목록에 없는 {@code playerId}면 아무것도 하지 않는다.
     */
    void moveTo(int playerId, double x, double y, Direction direction) {
        players.computeIfPresent(playerId, (id, player) -> player.moveTo(x, y, direction));
    }

    /**
     * 플레이어를 넣는다. 같은 {@code playerId}가 이미 있으면 덮어쓰고, 목록 순서는 처음 들어온 자리를 유지한다.
     */
    void save(Player player) {
        players.put(player.playerId(), player);
    }

    /** 플레이어 한 명을 목록에서 뺀다. 목록에 없는 {@code playerId}면 아무것도 하지 않는다. */
    void removeByPlayerId(int playerId) {
        players.remove(playerId);
    }

    /**
     * 플레이어 한 명을 찾는다.
     *
     * @return {@code playerId}를 가진 플레이어. 목록에 없으면 {@code null}
     */
    Player findByPlayerId(int playerId) {
        return players.get(playerId);
    }

    /**
     * 전체 플레이어를 찾는다.
     *
     * @return 지금 목록을 복사한 리스트. 고칠 수 없고, 나중에 상태가 바뀌어도 달라지지 않는다
     */
    List<Player> findAll() {
        return List.copyOf(players.values());
    }
}
