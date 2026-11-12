package networkgame.client.contentpane.lobby;

import networkgame.protocol.Direction;

import java.util.Set;

/**
 * 눌린 방향키로 한 프레임 뒤 내 고양이의 위치·방향을 계산한다.
 * <p>Swing·{@link LobbyState}를 모르는 순수 계산이라 창 없이 테스트할 수 있다.
 */
final class LobbyMovement {
    // 한 프레임(16ms)에 움직이는 거리, 지도 픽셀 단위라 초당 약 300px이다.
    static final double STEP = 5;

    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    /**
     * 지도 크기로 고양이가 움직일 수 있는 범위를 정한다.
     * <p>좌표는 고양이 발밑 가운데라서, 좌우는 그림 반 폭만큼 안쪽까지, 위쪽은 머리 위 이름표까지 지도 안에 들어오게 한다.
     *
     * @param mapWidth  지도 그림의 너비(px)
     * @param mapHeight 지도 그림의 높이(px)
     */
    LobbyMovement(int mapWidth, int mapHeight) {
        int size = LobbyStyle.SPRITE_SIZE;
        this.minX = size / 2.0;
        this.maxX = mapWidth - size / 2.0;
        this.minY = size + 32;
        this.maxY = mapHeight;
    }

    /**
     * 한 프레임 뒤의 플레이어를 계산한다.
     * <p>키를 다 뗐거나 반대 방향을 같이 눌렀으면 그 자리에서 바라보던 방향 그대로 멈춘다.
     * 지도 범위 끝에서 바깥쪽 키를 누르면 그 자리에 선 채로 그 방향을 본다.
     *
     * @param pressed 지금 눌려 있는 방향키
     * @return 한 프레임 뒤의 플레이어. 바뀐 게 없으면 {@code player}와 같은 값
     */
    Player next(Player player, Set<Direction> pressed) {
        Offset offset = Offset.of(pressed);
        if (offset.isZero()) {
            return player.stop();
        }
        double step = STEP / offset.length();
        double x = Math.clamp(player.x() + offset.dx() * step, minX, maxX);
        double y = Math.clamp(player.y() + offset.dy() * step, minY, maxY);
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
