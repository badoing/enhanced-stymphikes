package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Pulses the whole screen red for a few seconds after the player takes a hit while Stymphike hunting.
 */
public class StymphikeAttackOverlay extends Overlay
{
    // Strongest the red tint gets at the peak of a pulse
    private static final int MAX_ALPHA = 110;
    // One full pulse (fade in and out) every 800ms
    private static final double PULSE_PERIOD_MS = 800.0;

    private final Client client;
    private final StymphikeTreeHighlighterPlugin plugin;

    @Inject
    public StymphikeAttackOverlay(Client client, StymphikeTreeHighlighterPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isRecentlyHit())
        {
            return null;
        }

        double wave = 0.5 * (1.0 - Math.cos(2 * Math.PI * (System.currentTimeMillis() % (long) PULSE_PERIOD_MS) / PULSE_PERIOD_MS));
        int alpha = (int) Math.round(MAX_ALPHA * wave);
        if (alpha <= 0)
        {
            return null;
        }

        graphics.setColor(new Color(255, 0, 0, alpha));
        graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        return null;
    }
}
