package networkgame.client.contentpane.lobby.assets;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import networkgame.protocol.Direction;

class CatSpritesTest {
    @Test
    @DisplayName("멈춰 있으면 tick과 상관없이 그 방향의 서 있는 그림을 고른다")
    void choosesIdleWhenStopped() {
        // given
        BufferedImage idleLeft = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Map<Direction, BufferedImage> idle = new EnumMap<>(Direction.class);
        Map<Direction, List<BufferedImage>> run = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            idle.put(direction, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
            run.put(direction, List.of(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));
        }
        idle.put(Direction.LEFT, idleLeft);
        CatSprites cat = new CatSprites(idle, run);

        // when
        BufferedImage frame = cat.frame(Direction.LEFT, false, 7);

        // then
        assertThat(frame).isSameAs(idleLeft);
    }

    @Test
    @DisplayName("걷는 중이면 tick마다 다음 걷기 그림을 고르고 마지막 다음엔 처음으로 돌아간다")
    void cyclesRunFramesWhileWalking() {
        // given
        BufferedImage first = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage second = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Map<Direction, BufferedImage> idle = new EnumMap<>(Direction.class);
        Map<Direction, List<BufferedImage>> run = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            idle.put(direction, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
            run.put(direction, List.of(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));
        }
        run.put(Direction.RIGHT, List.of(first, second));
        CatSprites cat = new CatSprites(idle, run);

        // when
        List<BufferedImage> frames = List.of(
                cat.frame(Direction.RIGHT, true, 0),
                cat.frame(Direction.RIGHT, true, 1),
                cat.frame(Direction.RIGHT, true, 2));

        // then
        assertThat(frames).containsExactly(first, second, first);
    }
}
