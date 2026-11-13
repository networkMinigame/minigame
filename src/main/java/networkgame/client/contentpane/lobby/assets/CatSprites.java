package networkgame.client.contentpane.lobby.assets;

import networkgame.protocol.Direction;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * 고양이 한 마리의 방향별 그림. 서 있는 그림과 걷기 그림 중 지금 그릴 한 장을 고른다.
 */
final class CatSprites {
    private final Map<Direction, BufferedImage> idle;
    private final Map<Direction, List<BufferedImage>> run;

    /**
     * 파일은 읽지 않아서 테스트에서는 가짜 그림을 넣어 만들 수 있다.
     *
     * @param idle 방향마다 서 있는 그림 한 장. 모든 {@link Direction}이 있어야 한다
     * @param run  방향마다 걷기 그림 목록. 모든 {@link Direction}이 있어야 하고 목록이 비어 있으면 안 된다
     */
    CatSprites(Map<Direction, BufferedImage> idle, Map<Direction, List<BufferedImage>> run) {
        this.idle = Map.copyOf(idle);
        this.run = Map.copyOf(run);
    }

    /**
     * 지금 그릴 그림 한 장을 고른다.
     * <p>걷는 중이면 {@code tick}에 따라 걷기 그림을 차례로 돌려 주고, 마지막 그림 다음에는 첫 그림으로 돌아간다.
     *
     * @param walking {@code false}면 {@code tick}과 상관없이 서 있는 그림
     * @param tick    0 이상. 1 늘 때마다 다음 걷기 그림으로 넘어간다
     */
    BufferedImage frame(Direction direction, boolean walking, long tick) {
        if (!walking) {
            return idle.get(direction);
        }
        List<BufferedImage> frames = run.get(direction);
        return frames.get((int) (tick % frames.size()));
    }
}
