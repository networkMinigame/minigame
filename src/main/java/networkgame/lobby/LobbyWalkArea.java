package networkgame.lobby;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.BitSet;
import javax.imageio.ImageIO;

/** 원본 지도 좌표에서 고양이 발밑 가운데가 통과할 수 있는 영역이다. */
public final class LobbyWalkArea {
    private static final String MASK_PATH = "/map/lobby-walkable.png";
    private static final int WALKABLE_PIXEL = 0xFFFFFFFF;

    private final int width;
    private final int height;
    private final BitSet walkable;

    private LobbyWalkArea(BufferedImage mask) {
        width = mask.getWidth();
        height = mask.getHeight();
        walkable = new BitSet(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask.getRGB(x, y) == WALKABLE_PIXEL) {
                    walkable.set(y * width + x);
                }
            }
        }
    }

    /** 클라이언트와 서버가 각각 시작할 때 같은 리소스를 읽어 판정에 사용한다. */
    public static LobbyWalkArea standard() {
        return Standard.AREA;
    }

    public boolean contains(double x, double y) {
        // 범위 비교는 비정상 좌표인 NaN과 무한대도 거른다.
        return x >= 0 && x < width && y >= 0 && y < height && isWalkable((int) x, (int) y);
    }

    /** 출발점부터 목적지까지 선분이 지나는 픽셀이 모두 흙길인지 검사한다. */
    public boolean canMove(double fromX, double fromY, double toX, double toY) {
        if (!contains(fromX, fromY) || !contains(toX, toY)) {
            return false;
        }

        int x = (int) fromX;
        int y = (int) fromY;
        int endX = (int) toX;
        int endY = (int) toY;
        int stepX = Integer.compare(endX, x);
        int stepY = Integer.compare(endY, y);
        double dx = Math.abs(toX - fromX);
        double dy = Math.abs(toY - fromY);

        while (x != endX || y != endY) {
            // 선분이 다음 세로·가로 픽셀 경계에 도달하는 비율을 비교한다.
            double crossingX = x == endX ? Double.POSITIVE_INFINITY
                    : (stepX > 0 ? x + 1 - fromX : fromX - x) / dx;
            double crossingY = y == endY ? Double.POSITIVE_INFINITY
                    : (stepY > 0 ? y + 1 - fromY : fromY - y) / dy;
            if (crossingX <= crossingY) {
                x += stepX;
            }
            if (crossingY <= crossingX) {
                y += stepY;
            }
            if (!isWalkable(x, y)) {
                return false;
            }
        }
        return true;
    }

    private boolean isWalkable(int x, int y) {
        return walkable.get(y * width + x);
    }

    private static LobbyWalkArea loadMask() {
        try (InputStream resource = LobbyWalkArea.class.getResourceAsStream(MASK_PATH)) {
            if (resource == null) {
                throw new IllegalStateException("로비 이동 영역 파일을 찾을 수 없습니다: " + MASK_PATH);
            }
            BufferedImage mask = ImageIO.read(resource);
            if (mask == null) {
                throw new IllegalStateException("로비 이동 영역 그림을 읽을 수 없습니다: " + MASK_PATH);
            }
            return new LobbyWalkArea(mask);
        } catch (IOException error) {
            throw new UncheckedIOException("로비 이동 영역을 읽지 못했습니다", error);
        }
    }

    private static final class Standard {
        private static final LobbyWalkArea AREA = loadMask();
    }
}
