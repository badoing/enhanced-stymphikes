package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * RuneScape-styled warning shown when the player walks out of the bush before the Stymphike is speared.
 */
public class StymphikeLeftBushOverlay extends OverlayPanel
{
    // Same RuneScape interface colours as the stats box, with a red title for the warning
    private static final Color RS_BACKGROUND = new Color(62, 53, 41, 225);
    private static final Color RS_RED = new Color(255, 48, 48);
    private static final Color RS_YELLOW = new Color(255, 255, 0);

    private final StymphikeTreeHighlighterPlugin plugin;
    private final StymphikeConfig config;

    @Inject
    public StymphikeLeftBushOverlay(StymphikeTreeHighlighterPlugin plugin, StymphikeConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_CENTER);
        setPriority(PRIORITY_HIGH);
        panelComponent.setBackgroundColor(RS_BACKGROUND);
        panelComponent.setPreferredSize(new Dimension(230, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showLeftBushWarning() || !plugin.isLeftBush())
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeBoldFont());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("You left the bush!")
                .color(RS_RED)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Re-enter the bush to auto-spear it,")
                .leftColor(RS_YELLOW)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("or spear the Stymphike manually as soon as it lands for reduced XP.")
                .leftColor(Color.WHITE)
                .build());

        return super.render(graphics);
    }
}
