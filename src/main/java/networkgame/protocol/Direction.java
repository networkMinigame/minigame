package networkgame.protocol;

/**
 * 고양이가 바라보는 방향. 걷기 그림이 네 방향뿐이라 대각선은 없다.
 * <p>{@code dx}·{@code dy}는 이 방향으로 한 칸 갈 때의 좌표 변화다. 화면 좌표라 아래쪽이 +y다.
 * JSON에는 이름({@code "LEFT"})만 나가고 이 값은 나가지 않는다.
 */
public enum Direction {
    DOWN(0, 1),
    UP(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }
}
