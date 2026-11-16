package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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

    private static final Color AUTO_SPEAR_GREEN = new Color(0, 255, 0);
    private static final Color SPEAR_NOW_RED = new Color(255, 48, 48);

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
        // Countdown lasts 38 seconds from settling into the bush and keeps running if you leave it;
        // once the Stymphike lands, the spear prompt replaces it
        if (plugin.getTimerSecondsRemaining() < 0 && !plugin.isStymphikeLanded())
        {
            return null;
        }

        // The dark screen is only shown while actually in the bush
        if (plugin.isHiding() && config.darkenScreen() && !plugin.isDarkScreenClosed())
        {
            graphics.setColor(new Color(0, 0, 0, config.darkenOpacity()));
            Area dark = new Area(new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
            if (config.undarkenChatBox())
            {
                // Cut the chat box out of the dark layer so it stays readable
                Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
                if (chatbox != null && !chatbox.isHidden())
                {
                    dark.subtract(new Area(chatbox.getBounds()));
                }
            }
            graphics.fill(dark);
        }

        if (config.showTimer())
        {
            renderTimer(graphics);
        }
        return null;
    }

    private void renderTimer(Graphics2D graphics)
    {
        String text;
        Color color;
        if (plugin.isStymphikeLanded() && plugin.isHiding() && plugin.isReenteredBush())
        {
            // Back in the bush after leaving: the spear happens on the next rustle, which may be a while.
            // (If the player never left, the spear is immediate, so the normal countdown is shown instead.)
            int spearIn = plugin.getAutoSpearSeconds();
            if (spearIn < 0)
            {
                return;
            }
            text = spearIn == 0 ? "Spearing!" : "Auto-spear in " + spearIn + "s";
            color = AUTO_SPEAR_GREEN;
        }
        else if (plugin.isStymphikeLanded() && plugin.isLeftBush())
        {
            // Out of the bush: it flies off in about 3 seconds unless speared manually
            text = "Spear now!";
            color = SPEAR_NOW_RED;
        }
        else
        {
            int secondsLeft = plugin.getTimerSecondsRemaining();
            if (secondsLeft < 0)
            {
                return;
            }
            text = Integer.toString(secondsLeft);
            color = Color.WHITE;
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

        // Set the font first so the text is centred correctly for the chosen size
        graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont((float) config.timerSize()));
        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, lp, text, TIMER_Z_OFFSET);
        if (textLocation != null)
        {
            OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
        }
    }
}
