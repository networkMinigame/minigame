package networkgame.client.contentpane.lobby.assets;

import networkgame.protocol.Direction;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 리소스 폴더의 파일 배치대로 로비 그림을 읽어 {@link LobbyAssets}를 만든다. 파일 이름 규칙은 여기만 안다.
 */
public final class LobbyAssetLoader {
    private static final String MAP = "map/lobby.png";
    private static final int CAT_COUNT = 4;
    private static final int RUN_FRAMES = 16;

    private LobbyAssetLoader() {
    }

    /**
     * 지도와 고양이 4마리의 그림을 읽는다. 그림이 수백 장이라 EDT가 아닌 스레드에서 부른다.
     *
     * @throws IOException 그림 파일이 없거나 이미지로 읽을 수 없는 경우
     */
    public static LobbyAssets load() throws IOException {
        List<CatSprites> catSprites = new ArrayList<>();

        for (int character = 0; character < CAT_COUNT; character++) {
            catSprites.add(loadCat(character));
        }
        return new LobbyAssets(ResourceImages.read(MAP), catSprites);
    }

    private static CatSprites loadCat(int character) throws IOException {
        String base = basePath(character);
        Map<Direction, BufferedImage> idle = new EnumMap<>(Direction.class);
        Map<Direction, List<BufferedImage>> run = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            idle.put(direction, loadIdle(base, direction));
            run.put(direction, loadRun(base, direction));
        }
        return new CatSprites(idle, run);
    }

    // 파일 폴더는 player1부터 시작해서 로비의 고양이 번호(0부터)에 1을 더한다.
    // 예: 0번 고양이 → characters/round-cats/player1/player1
    private static String basePath(int character) {
        String player = "player" + (character + 1);
        return "characters/round-cats/" + player + "/" + player;
    }

    // 예: player1_idle_left_01.png
    private static BufferedImage loadIdle(String base, Direction direction) throws IOException {
        return ResourceImages.read(base + "_idle_" + facing(direction) + "_01.png");
    }

    // 예: player1_run_left_01.png ~ player1_run_left_16.png
    private static List<BufferedImage> loadRun(String base, Direction direction) throws IOException {
        List<BufferedImage> frames = new ArrayList<>(RUN_FRAMES);
        for (int frame = 1; frame <= RUN_FRAMES; frame++) {
            frames.add(ResourceImages.read(base + "_run_" + facing(direction) + "_"
                    + String.format(Locale.ROOT, "%02d.png", frame)));
        }
        return frames;
    }

    // 파일 이름에는 방향이 소문자로 들어간다. 터키어 등 다른 언어 설정에서도 같은 글자가 나오게 Locale.ROOT를 쓴다.
    private static String facing(Direction direction) {
        return direction.name().toLowerCase(Locale.ROOT);
    }
}
