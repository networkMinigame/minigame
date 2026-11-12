package networkgame.client.contentpane.lobby.assets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** 클래스패스 리소스에서 그림을 읽는다. */
final class ResourceImages {

    private ResourceImages() {
    }

    /**
     * @param path 리소스 루트 기준 경로. 앞에 {@code /}를 붙이지 않는다
     * @throws IOException 파일이 없거나 이미지로 읽을 수 없는 경우
     */
    static BufferedImage read(String path) throws IOException {
        try (InputStream stream = ResourceImages.class.getResourceAsStream("/" + path)) {
            BufferedImage image = stream == null ? null : ImageIO.read(stream);
            if (image == null) {
                throw new IOException("이미지를 읽을 수 없습니다. " + path);
            }
            return image;
        }
    }
}
