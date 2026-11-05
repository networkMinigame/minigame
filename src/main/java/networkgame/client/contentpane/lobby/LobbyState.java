package networkgame.client.contentpane.lobby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Swing 이벤트 디스패치 스레드에서 관리하는 로비의 플레이어 상태. */
final class LobbyState {
    enum Direction { DOWN, LEFT, RIGHT, UP }

    record Player(int playerId, String name, int character, double x, double y,
                  Direction direction, boolean walking) {
    }

    private final Map<Integer, Player> players = new LinkedHashMap<>();

    void put(Player player) {
        players.put(player.playerId(), player);
    }

    void remove(int playerId) {
        players.remove(playerId);
    }

    Player player(int playerId) {
        return players.get(playerId);
    }

    void move(int playerId, double x, double y, Direction direction, boolean walking) {
        Player player = players.get(playerId);
        if (player != null) {
            put(new Player(playerId, player.name(), player.character(), x, y, direction, walking));
        }
    }

    List<Player> players() {
        return List.copyOf(players.values());
    }
}
