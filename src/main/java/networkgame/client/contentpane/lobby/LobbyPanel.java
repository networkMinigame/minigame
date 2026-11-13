package networkgame.client.contentpane.lobby;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import javax.swing.JPanel;

import networkgame.client.contentpane.lobby.assets.LobbyAssets;

/**
 * 로비 지도와 플레이어들을 그린다. {@link LobbyState}를 읽기만 하고 바꾸지 않는다.
 * <p>지도는 비율을 유지한 채 패널에 꽉 차게 늘리거나 줄이고, 남는 곳은 배경색으로 둔다.
 */
final class LobbyPanel extends JPanel {
    // 처음 창 크기. 지도 그림(1672×941)과 같은 16:9 비율이다.
    private static final Dimension PREFERRED_SIZE = new Dimension(1000, 563);
    // 화면 갱신 3번(약 48ms)마다 걷기 그림을 한 장 넘긴다.
    private static final int FRAMES_PER_SPRITE = 3;

    private static final float NAME_FONT_SIZE = 22f;
    // 이름표는 머리 위 27px에서 시작해서 높이가 30px이라 머리에 3px 걸친다.
    private static final int LABEL_RISE = 27;
    private static final int LABEL_HEIGHT = 30;
    private static final int LABEL_PADDING_X = 12;
    private static final int LABEL_BASELINE = 23;
    private static final int LABEL_CORNER = 16;
    // INK에 투명도 225를 준 색.
    private static final Color LABEL_BACKGROUND = new Color(36, 59, 50, 225);

    private final LobbyState state;
    private final LobbyAssets assets;
    private long animationTick;

    LobbyPanel(LobbyState state, LobbyAssets assets) {
        this.state = state;
        this.assets = assets;
        setBackground(LobbyStyle.INK);
        setPreferredSize(PREFERRED_SIZE);
    }

    /** 걷기 그림을 한 단계 넘기고 다시 그려 달라고 요청한다. 실제로 그리는 건 Swing이 나중에 한다. */
    void renderFrame() {
        animationTick++;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            fitMapToPanel(canvas);
            canvas.drawImage(assets.map(), 0, 0, null);
            drawPlayers(canvas);
        } finally {
            canvas.dispose();
        }
    }

    // 이후에 그리는 좌표를 지도 픽셀 좌표 그대로 쓸 수 있게, 지도가 비율을 유지한 채 패널 가운데에 꽉 차도록 옮기고 늘린다.
    private void fitMapToPanel(Graphics2D canvas) {
        int mapWidth = assets.map().getWidth();
        int mapHeight = assets.map().getHeight();
        double scale = Math.min((double) getWidth() / mapWidth, (double) getHeight() / mapHeight);
        canvas.translate((getWidth() - mapWidth * scale) / 2, (getHeight() - mapHeight * scale) / 2);
        canvas.scale(scale, scale);
        canvas.clipRect(0, 0, mapWidth, mapHeight);
        // 늘리거나 줄인 그림과 이름표 모서리가 계단처럼 보이지 않게 한다.
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    // 앞(아래)에 선 고양이가 뒤 고양이를 가리도록 y가 작은 순서대로 그린다.
    private void drawPlayers(Graphics2D canvas) {
        canvas.setFont(getFont().deriveFont(Font.BOLD, NAME_FONT_SIZE));
        state.findAll().stream()
                .sorted(Comparator.comparingDouble(Player::y))
                .forEach(player -> {
                    drawSprite(canvas, player);
                    drawNameLabel(canvas, player);
                });
    }

    // 좌표가 고양이 발밑 가운데라서 그림의 왼쪽 위는 (x - 반 폭, y - 높이)다.
    private void drawSprite(Graphics2D canvas, Player player) {
        int size = LobbyStyle.SPRITE_SIZE;
        int x = (int) Math.round(player.x());
        int y = (int) Math.round(player.y());
        canvas.drawImage(assets.sprite(player.character(), player.direction(), player.walking(),
                animationTick / FRAMES_PER_SPRITE), x - size / 2, y - size, size, size, null);
    }

    private void drawNameLabel(Graphics2D canvas, Player player) {
        int x = (int) Math.round(player.x());
        int y = (int) Math.round(player.y());
        int width = canvas.getFontMetrics().stringWidth(player.name()) + LABEL_PADDING_X * 2;
        int left = x - width / 2;
        int top = y - LobbyStyle.SPRITE_SIZE - LABEL_RISE;
        canvas.setColor(LABEL_BACKGROUND);
        canvas.fillRoundRect(left, top, width, LABEL_HEIGHT, LABEL_CORNER, LABEL_CORNER);
        canvas.setColor(LobbyStyle.PAPER);
        canvas.drawString(player.name(), left + LABEL_PADDING_X, top + LABEL_BASELINE);
    }
}
