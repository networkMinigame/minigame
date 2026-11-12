package networkgame.client.contentpane.lobby;

import java.awt.Color;

/**
 * 로비 화면의 색상과 고양이 표시 크기를 모아 둔 상수 클래스.
 * 고양이 크기는 화면에 그릴 때와 지도 밖으로 나가지 않도록 이동 범위를 계산할 때 함께 쓴다.
 */
final class LobbyStyle {
    static final Color INK = new Color(0x243B32);
    static final Color PAPER = new Color(0xF8F4E8);
    static final int SPRITE_SIZE = 104;

    private LobbyStyle() {
    }
}
