package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Shown only while hiding in a bush: darkens the whole screen and draws the countdown on top.
 */
public class StymphikeHidingOverlay extends Overlay
{
    // Height above the ground the countdown text is drawn at, so it sits over the tree
    private static final int TIMER_Z_OFFSET = 150;

    private final Client client;
    private final StymphikeTreeHighlighterPlugin plugin;
    private final StymphikeConfig config;

    @Inject
    public StymphikeHidingOverlay(
            Client client,
            StymphikeTreeHighlighterPlugin plugin,
            StymphikeConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        // Above widgets so the whole screen, interfaces included, is darkened
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Dark screen and timer last 38 seconds from settling into the bush
        if (!plugin.isHiding() || plugin.getTimerSecondsRemaining() < 0)
        {
            return null;
        }

        if (config.darkenScreen())
        {
            graphics.setColor(new Color(0, 0, 0, config.darkenOpacity()));
            graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        }

        if (config.showTimer())
        {
            renderTimer(graphics);
        }
        return null;
    }

    private void renderTimer(Graphics2D graphics)
    {
        int secondsLeft = plugin.getTimerSecondsRemaining();
        if (secondsLeft < 0)
        {
            return;
        }

        // Show the countdown over the baited tree; fall back to the bush being hidden in
        WorldPoint anchor = plugin.getActiveTree() != null ? plugin.getActiveTree() : plugin.getHideBush();
        if (anchor == null || anchor.getPlane() != client.getTopLevelWorldView().getPlane())
        {
            return;
        }

        GameObject tree = plugin.getTreeObject(anchor);
        LocalPoint lp = tree != null ? tree.getLocalLocation() : LocalPoint.fromWorld(client, anchor);
        if (lp == null)
        {
            return;
        }

        String text = Integer.toString(secondsLeft);
        // Set the font first so the text is centred correctly for the chosen size
        graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont((float) config.timerSize()));
        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, lp, text, TIMER_Z_OFFSET);
        if (textLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.WHITE);
        }
    }
}
