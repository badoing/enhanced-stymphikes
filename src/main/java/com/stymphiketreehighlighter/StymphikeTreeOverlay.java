package com.stymphiketreehighlighter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class StymphikeTreeOverlay extends Overlay
{
    private final Client client;
    private final StymphikeTreeHighlighterPlugin plugin;
    private final StymphikeConfig config;

    @Inject
    public StymphikeTreeOverlay(
            Client client,
            StymphikeTreeHighlighterPlugin plugin,
            StymphikeConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // While hiding in a bush every highlight is hidden (StymphikeHidingOverlay takes over)
        if (plugin.isHiding())
        {
            return null;
        }

        double flash = computeFlashMultiplier();
        Stroke stroke = new BasicStroke(config.borderWidth());
        int plane = client.getTopLevelWorldView().getPlane();

        // Unbaited trees are highlighted; baited trees get no colour
        for (Map.Entry<WorldPoint, GameObject> tree : plugin.getTrees().entrySet())
        {
            WorldPoint wp = tree.getKey();
            if (wp.getPlane() == plane && !plugin.isBaited(wp))
            {
                renderObject(graphics, wp, tree.getValue(), config.treeColor(), flash, stroke);
            }
        }

        // Bushes are highlighted only once a tree has been baited (time to go and hide)
        if (!plugin.hasBaitedTree())
        {
            return null;
        }
        for (Map.Entry<WorldPoint, GameObject> bush : plugin.getHideBushes().entrySet())
        {
            WorldPoint wp = bush.getKey();
            if (wp.getPlane() == plane)
            {
                renderObject(graphics, wp, bush.getValue(), config.bushColor(), flash, stroke);
            }
        }

        return null;
    }

    private void renderObject(Graphics2D graphics, WorldPoint wp, GameObject obj, Color baseColor, double flash, Stroke stroke)
    {
        LocalPoint lp = obj != null ? obj.getLocalLocation() : LocalPoint.fromWorld(client, wp);
        if (lp == null)
        {
            return;
        }

        int strokeAlpha = clampAlpha((int) Math.round(baseColor.getAlpha() * flash));
        int fillAlpha = clampAlpha((int) Math.round(config.fillOpacity() * flash));
        Color strokeColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), strokeAlpha);
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), fillAlpha);

        if (config.drawTileOutline())
        {
            int size = obj != null ? Math.max(obj.sizeX(), obj.sizeY()) : 1;
            Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, lp, size);
            if (tilePoly != null)
            {
                OverlayUtil.renderPolygon(graphics, tilePoly, strokeColor, fillColor, stroke);
            }
        }

        if (config.drawObjectHull() && obj != null)
        {
            Shape hull = obj.getConvexHull();
            if (hull != null)
            {
                OverlayUtil.renderPolygon(graphics, hull, strokeColor, fillColor, stroke);
            }
        }
    }

    private double computeFlashMultiplier()
    {
        if (!config.flashEnabled())
        {
            return 1.0;
        }

        double frequency = config.flashSpeed() * 0.0015;
        double wave = 0.5 * (1.0 + Math.sin(System.currentTimeMillis() * frequency * Math.PI));
        return 0.25 + (0.75 * wave);
    }

    private static int clampAlpha(int value)
    {
        return Math.max(0, Math.min(255, value));
    }
}
