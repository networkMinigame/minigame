package networkgame.client.lobby.view.assets;

import networkgame.protocol.Direction;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 로비에서 쓰는 지도와 고양이 그림. 파일은 읽지 않는다. 만드는 일은 {@link LobbyAssetLoader}가 맡는다.
 */
public final class LobbyAssets {
    private final BufferedImage map;
    private BufferedImage arena;
    private final List<CatSprites> cats;

    /** @param cats 고양이 번호({@code character}) 순서대로 */
    LobbyAssets(BufferedImage map, List<CatSprites> cats) {
        this.map = map;
        this.cats = List.copyOf(cats);
    }

    /** 로딩 단계에서 경기장 이미지를 설정한다. EDT에 전달하기 전에 호출한다. */
    void setArena(BufferedImage arena) {
        this.arena = arena;
    }

    /** 어깨빵 경기장 이미지. */
    public BufferedImage arena() {
        return arena;
    }

    /** 로비 지도. 이 그림의 픽셀 좌표가 플레이어 좌표({@code x}, {@code y})의 기준이다. */
    public BufferedImage map() {
        return map;
    }

    /**
     * 지금 그릴 고양이 그림 한 장을 고른다.
     *
     * @param character 고양이 번호. 0 이상, 고양이 수 미만
     */
    public BufferedImage sprite(int character, Direction direction, boolean walking, long tick) {
        return cats.get(character).frame(direction, walking, tick);
    }
}
