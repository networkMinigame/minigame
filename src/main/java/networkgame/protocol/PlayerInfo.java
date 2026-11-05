package networkgame.protocol;

/**
 * 로비에 있는 플레이어 한 명. {@code character}는 고른 고양이 번호, {@code x}·{@code y}는 로비 좌표.
 */
public record PlayerInfo(
        int id,
        String name,
        int character,
        double x,
        double y,
        Direction direction
) {
}
