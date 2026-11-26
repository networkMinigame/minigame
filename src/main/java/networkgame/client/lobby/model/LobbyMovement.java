package networkgame.client.lobby.model;

import networkgame.lobby.LobbyWalkArea;
import networkgame.protocol.Direction;

import java.util.Set;

/**
 * 눌린 방향키로 한 프레임 뒤 내 고양이의 위치·방향을 계산한다.
 * <p>Swing·{@link LobbyState}를 모르는 순수 계산이라 창 없이 테스트할 수 있다.
 */
public final class LobbyMovement {
    // 한 프레임(16ms)에 움직이는 거리, 지도 픽셀 단위라 초당 약 300px이다.
    static final double STEP = 5;

    private final LobbyWalkArea walkArea;

    public LobbyMovement(LobbyWalkArea walkArea) {
        this.walkArea = walkArea;
    }

    /**
     * 한 프레임 뒤의 플레이어를 계산한다.
     * <p>키를 다 뗐거나 반대 방향을 같이 눌렀으면 그 자리에서 바라보던 방향 그대로 멈춘다.
     * 이동 선분이 흙길을 벗어나면 그 자리에 선 채로 입력한 방향을 본다.
     *
     * @param pressed 지금 눌려 있는 방향키
     * @return 한 프레임 뒤의 플레이어. 바뀐 게 없으면 {@code player}와 같은 값
     */
    public Player next(Player player, Set<Direction> pressed) {
        Offset offset = Offset.of(pressed);
        if (offset.isZero()) {
            return player.stop();
        }
        double step = STEP / offset.length();
        double x = player.x() + offset.dx() * step;
        double y = player.y() + offset.dy() * step;
        if (!walkArea.canMove(player.x(), player.y(), x, y)) {
            return player.moveTo(player.x(), player.y(), offset.facing());
        }
        return player.moveTo(x, y, offset.facing());
    }

    /** 눌린 방향키들의 이동량 합. 반대 방향을 같이 누르면 서로 지워진다. */
    private record Offset(int dx, int dy) {

        static Offset of(Set<Direction> pressed) {
            int dx = 0;
            int dy = 0;
            for (Direction direction : pressed) {
                dx += direction.dx();
                dy += direction.dy();
            }
            return new Offset(dx, dy);
        }

        boolean isZero() {
            return dx == 0 && dy == 0;
        }

        // 대각선이면 √2라서, 이 값으로 나누면 상하좌우로 갈 때와 같은 속도가 된다.
        double length() {
            return Math.hypot(dx, dy);
        }

        // 대각선으로 갈 때는 좌우 방향 그림을 쓴다.
        Direction facing() {
            if (dx != 0) {
                return dx < 0 ? Direction.LEFT : Direction.RIGHT;
            }
            return dy < 0 ? Direction.UP : Direction.DOWN;
        }
    }
}
