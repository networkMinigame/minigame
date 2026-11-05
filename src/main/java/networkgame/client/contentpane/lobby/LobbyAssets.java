package networkgame.client.contentpane.lobby;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;

import networkgame.client.contentpane.lobby.LobbyState.Direction;

public final class LobbyAssets {
    private static final int CHARACTER_COUNT = 4;
    private static final int WALK_FRAMES = 16;

    final BufferedImage map = read("map/lobby.png");
    private final BufferedImage[][][] sprites =
            new BufferedImage[CHARACTER_COUNT][Direction.values().length][WALK_FRAMES + 1];

    public LobbyAssets() throws IOException {
        for (int character = 0; character < CHARACTER_COUNT; character++) {
            String player = "player" + (character + 1);
            String base = "characters/round-cats/" + player + "/" + player;
            for (Direction direction : Direction.values()) {
                String facing = direction.name().toLowerCase(Locale.ROOT);
                BufferedImage[] frames = sprites[character][direction.ordinal()];
                frames[0] = read(base + "_idle_" + facing + "_01.png");
                for (int frame = 1; frame <= WALK_FRAMES; frame++) {
                    frames[frame] = read(base + "_run_" + facing + "_"
                            + String.format(Locale.ROOT, "%02d.png", frame));
                }
            }
        }
    }

    BufferedImage sprite(int character, Direction direction, boolean walking, long tick) {
        int frame = walking ? 1 + (int) (tick % WALK_FRAMES) : 0;
        return sprites[character][direction.ordinal()][frame];
    }

    private static BufferedImage read(String path) throws IOException {
        try (InputStream stream = LobbyAssets.class.getResourceAsStream("/" + path)) {
            BufferedImage image = stream == null ? null : ImageIO.read(stream);
            if (image == null) {
                throw new IOException("이미지를 읽을 수 없습니다. " + path);
            }
            return image;
        }
    }
}
