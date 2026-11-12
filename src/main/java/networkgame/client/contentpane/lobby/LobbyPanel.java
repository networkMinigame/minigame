package networkgame.client.contentpane.lobby;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import javax.swing.JPanel;

import networkgame.client.contentpane.lobby.LobbyState.Player;

final class LobbyPanel extends JPanel {
    private final LobbyState state;
    private final LobbyAssets assets;
    private long animationTick;

    LobbyPanel(LobbyState state, LobbyAssets assets) {
        this.state = state;
        this.assets = assets;
        setBackground(LobbyStyle.INK);
        setPreferredSize(new Dimension(1000, 563));
    }

    void renderFrame() {
        animationTick++;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            double scale = Math.min((double) getWidth() / assets.map.getWidth(),
                    (double) getHeight() / assets.map.getHeight());
            canvas.translate((getWidth() - assets.map.getWidth() * scale) / 2,
                    (getHeight() - assets.map.getHeight() * scale) / 2);
            canvas.scale(scale, scale);
            canvas.clipRect(0, 0, assets.map.getWidth(), assets.map.getHeight());
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.drawImage(assets.map, 0, 0, null);
            canvas.setFont(getFont().deriveFont(java.awt.Font.BOLD, 22f));
            state.findAll().stream().sorted(Comparator.comparingDouble(Player::y))
                    .forEach(player -> drawPlayer(canvas, player));
        } finally {
            canvas.dispose();
        }
    }

    private void drawPlayer(Graphics2D canvas, Player player) {
        int size = LobbyStyle.SPRITE_SIZE;
        int x = (int) Math.round(player.x());
        int y = (int) Math.round(player.y());
        canvas.drawImage(assets.sprite(player.character(), player.direction(), player.walking(),
                animationTick / 3), x - size / 2, y - size, size, size, null);

        int labelWidth = canvas.getFontMetrics().stringWidth(player.name()) + 24;
        int labelY = y - size - 27;
        canvas.setColor(new Color(36, 59, 50, 225));
        canvas.fillRoundRect(x - labelWidth / 2, labelY, labelWidth, 30, 16, 16);
        canvas.setColor(LobbyStyle.PAPER);
        canvas.drawString(player.name(), x - labelWidth / 2 + 12, labelY + 23);
    }
}
