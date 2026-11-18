package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

/**
 * RuneScape-styled box in the top left showing Stymphikes speared and Hunter XP, with hourly rates.
 */
public class StymphikeStatsOverlay extends OverlayPanel
{
    // Classic RuneScape interface colours: dark brown panel, orange title, yellow values
    private static final Color RS_BACKGROUND = new Color(62, 53, 41, 225);
    private static final Color RS_ORANGE = new Color(255, 152, 31);
    private static final Color RS_YELLOW = new Color(255, 255, 0);

    private static final long UPDATE_INTERVAL_MS = 1000;

    private final StymphikeTreeHighlighterPlugin plugin;
    private final StymphikeConfig config;

    // Last values shown, refreshed once per UPDATE_INTERVAL_MS
    private long lastUpdateMs;
    private String spearedText = "0";
    private String spearedPerHourText = "0";
    private String xpGainedText = "0.00k";
    private String xpPerHourText = "0.00k";
    private String goalLabel = "Left to lvl:";
    private String spearsToGoalText = "-";

    @Inject
    public StymphikeStatsOverlay(StymphikeTreeHighlighterPlugin plugin, StymphikeConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setBackgroundColor(RS_BACKGROUND);
        panelComponent.setPreferredSize(new Dimension(150, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Only pop up once there is something to show
        if (!config.showStats() || !plugin.hasStatsSession())
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeBoldFont());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Stymphike Hunting")
                .color(RS_ORANGE)
                .build());

        // Recalculate the numbers once a second rather than every frame
        long now = System.currentTimeMillis();
        if (now - lastUpdateMs >= UPDATE_INTERVAL_MS)
        {
            lastUpdateMs = now;
            spearedText = QuantityFormatter.formatNumber(plugin.getSpearCount());
            spearedPerHourText = QuantityFormatter.formatNumber(plugin.perHour(plugin.getSpearCount()));
            xpGainedText = formatThousands(plugin.getXpGained());
            xpPerHourText = formatThousands(plugin.perHour(plugin.getXpGained()));
            if (config.showSpearsToGoal())
            {
                int spearsLeft = plugin.getSpearsToGoal();
                goalLabel = "Left to " + plugin.getGoalLabel() + ":";
                // Unknown until one full bait-to-spear cycle has been measured
                spearsToGoalText = spearsLeft < 0 ? "-" : QuantityFormatter.formatNumber(spearsLeft);
            }
        }

        addLine("Speared:", spearedText);
        addLine("Speared/hr:", spearedPerHourText);
        addLine("XP gained:", xpGainedText);
        addLine("XP/hr:", xpPerHourText);
        if (config.showSpearsToGoal())
        {
            addLine(goalLabel, spearsToGoalText);
        }

        return super.render(graphics);
    }

    /** Shows XP in thousands with two decimals, e.g. 100,290 -> "100.29k". */
    private static String formatThousands(long value)
    {
        return String.format(Locale.ROOT, "%,.2fk", value / 1000.0);
    }

    private void addLine(String label, String value)
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(label)
                .leftColor(Color.WHITE)
                .right(value)
                .rightColor(RS_YELLOW)
                .build());
    }
}
