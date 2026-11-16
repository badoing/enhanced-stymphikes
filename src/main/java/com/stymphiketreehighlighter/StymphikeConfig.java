package com.stymphiketreehighlighter;

import java.awt.Color;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("stymphiketreehighlighter")
public interface StymphikeConfig extends Config
{
    @Alpha
    @ConfigItem(
            keyName = "treeColor",
            name = "Tree Colour",
            description = "Highlight colour of Stymphike Trees.",
            position = 1
    )
    default Color treeColor()
    {
        return new Color(0, 255, 0, 255);
    }

    @Alpha
    @ConfigItem(
            keyName = "bushColor",
            name = "Bush Colour",
            description = "Highlight Bush Colour",
            position = 2
    )
    default Color bushColor()
    {
        return new Color(0, 170, 0, 255);
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            keyName = "fillOpacity",
            name = "Fill Opacity",
            description = "Opacity of the interior highlight fill (0 = outline only, 255 = solid).",
            position = 3
    )
    default int fillOpacity()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "flashEnabled",
            name = "Enable Flashing",
            description = "Pulse/flash the highlight opacity on tracked trees.",
            position = 4
    )
    default boolean flashEnabled()
    {
        return false;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "flashSpeed",
            name = "Flash Speed",
            description = "Speed of the highlight pulse when flashing is enabled (1 = slow, 20 = fast).",
            position = 5
    )
    default int flashSpeed()
    {
        return 6;
    }

    @ConfigItem(
            keyName = "drawTileOutline",
            name = "Draw Tile Outline",
            description = "Highlight the ground tile footprint in addition to or instead of the bush hull.",
            position = 6
    )
    default boolean drawTileOutline()
    {
        return true;
    }

    @ConfigItem(
            keyName = "drawObjectHull",
            name = "Draw Hull",
            description = "Highlight the 3D clickbox/convex hull of trees and bushes.",
            position = 7
    )
    default boolean drawObjectHull()
    {
        return true;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(
            keyName = "borderWidth",
            name = "Outline Width",
            description = "Stroke thickness in pixels for tile and hull outlines.",
            position = 8
    )
    default int borderWidth()
    {
        return 2;
    }

    @ConfigItem(
            keyName = "showTimer",
            name = "Show Hiding Timer",
            description = "Show a 38 second countdown over the baited tree after you hide in a bush.",
            position = 9
    )
    default boolean showTimer()
    {
        return true;
    }

    @Range(min = 10, max = 60)
    @ConfigItem(
            keyName = "timerSize",
            name = "Timer Size",
            description = "Font size of the hiding countdown.",
            position = 10
    )
    default int timerSize()
    {
        return 16;
    }

    @ConfigItem(
            keyName = "darkenScreen",
            name = "Darken Screen While Hiding",
            description = "Darken the whole screen for 38 seconds after you hide in a bush.",
            position = 11
    )
    default boolean darkenScreen()
    {
        return true;
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            keyName = "darkenOpacity",
            name = "Darken Amount",
            description = "How dark the screen gets while hiding (0 = none, 255 = black).",
            position = 12
    )
    default int darkenOpacity()
    {
        return 150;
    }

    @ConfigItem(
            keyName = "undarkenChatBox",
            name = "Undarken Chat Box",
            description = "Keep the chat box bright while the rest of the screen is darkened.",
            position = 13
    )
    default boolean undarkenChatBox()
    {
        return false;
    }

    @ConfigItem(
            keyName = "flashWhenHit",
            name = "Flash Red When Hit",
            description = "Flash the screen red for a few seconds if you take a hit while Stymphike hunting.",
            position = 17
    )
    default boolean flashWhenHit()
    {
        return true;
    }

    @ConfigItem(
            keyName = "closeDarkScreenKey",
            name = "Close Dark Screen Key",
            description = "Press this key while hiding to close the dark screen. The timer keeps running.",
            position = 14
    )
    default Keybind closeDarkScreenKey()
    {
        return new Keybind(KeyEvent.VK_ESCAPE, 0);
    }

    @ConfigItem(
            keyName = "showStats",
            name = "Show Stats Box",
            description = "Show Stymphikes speared and Hunter XP, with hourly rates, in the top left.",
            position = 15
    )
    default boolean showStats()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showLeftBushWarning",
            name = "Left Bush Warning",
            description = "Show a pop-up if you leave the bush before the Stymphike is speared.",
            position = 16
    )
    default boolean showLeftBushWarning()
    {
        return true;
    }
}