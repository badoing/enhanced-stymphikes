package com.stymphiketreehighlighter;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/**
 * Highlights the Stymphike hunting cycle:
 * <ol>
 *     <li>No tree baited: Stymphike trees are green.</li>
 *     <li>Tree baited: that tree loses its highlight and the hiding bushes turn green.</li>
 *     <li>Hidden in a bush: all highlights are hidden, the screen darkens and the timer runs.</li>
 *     <li>Stymphike speared: the tree is unbaited again and the cycle repeats.</li>
 * </ol>
 */
@Slf4j
@PluginDescriptor(
        name = "Enhanced Stymphikes",
        description = "Highlights Stymphike trees and hiding bushes through the hunting cycle, with a timer while you hide.",
        tags = {"hunter", "stymphike", "vampyrium", "highlight", "trap", "timer"}
)
public class StymphikeTreeHighlighterPlugin extends Plugin
{
    /** Stymphike tree and its variants. */
    private static final Set<Integer> TREE_IDS = Set.of(
            ObjectID.HUNTING_STYMPHIKE_TREE_0,
            ObjectID.HUNTING_STYMPHIKE_TREE_1,
            ObjectID.HUNTING_STYMPHIKE_TREE_2,
            ObjectID.HUNTING_STYMPHIKE_TREE_3,
            ObjectID.HUNTING_STYMPHIKE_TREE_4,
            ObjectID.HUNTING_STYMPHIKE_TREE_LETVEK,
            ObjectID.HUNTING_STYMPHIKE_TREE_NO_LETVEK
    );

    /** Stymphike hiding bush and its variants. */
    private static final Set<Integer> HIDE_BUSH_IDS = Set.of(
            ObjectID.HUNTING_STYMPHIKE_BUSH,
            ObjectID.HUNTING_STYMPHIKE_BUSH_EMPTY,
            ObjectID.HUNTING_STYMPHIKE_BUSH_HIDDEN
    );

    /** How long the hiding timer and dark screen last. */
    private static final long HIDE_DURATION_MS = 38_000;

    /** Game message shown once the bait is actually placed in the tree. */
    private static final String BAITED_MESSAGE = "you bait the tree";

    private static final String BAIT_ACTION = "bait";
    private static final String HIDE_ACTION = "hide-in";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private StymphikeConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private StymphikeTreeOverlay overlay;

    @Inject
    private StymphikeHidingOverlay hidingOverlay;

    @Inject
    private StymphikeStatsOverlay statsOverlay;

    /** Every Stymphike tree in the scene. */
    @Getter
    private final Map<WorldPoint, GameObject> trees = new HashMap<>();

    /** Every hiding bush in the scene. */
    @Getter
    private final Map<WorldPoint, GameObject> hideBushes = new HashMap<>();

    /** Trees that are currently baited. */
    private final Set<WorldPoint> baitedTrees = new HashSet<>();

    /** Tree the player clicked Bait on, waiting for the game to confirm the bait is in. */
    private WorldPoint pendingBaitTree;

    /** The most recently baited tree; the timer is drawn over it. */
    @Getter
    private WorldPoint activeTree;

    /** True while the player is actually hidden in a bush, until the Stymphike is speared. */
    @Getter
    private boolean hiding;

    /** The bush the player chose to hide in, used to place the timer if no tree was baited. */
    @Getter
    private WorldPoint hideBush;

    /** When hiding started (System.currentTimeMillis), or 0 if no timer is running. */
    private long hideStartMs;

    /** Stymphikes speared since the plugin started. */
    @Getter
    private int spearCount;

    /** Hunter XP gained since the plugin started. */
    @Getter
    private long xpGained;

    /** When the first spear or Hunter XP drop happened, or 0 before any activity. */
    private long statsStartMs;

    /** Last known Hunter XP, or -1 until the first reading after login. */
    private int lastHunterXp = -1;

    @Provides
    StymphikeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(StymphikeConfig.class);
    }

    @Override
    protected void startUp()
    {
        resetCycle();
        resetStats();
        overlayManager.add(overlay);
        overlayManager.add(hidingOverlay);
        overlayManager.add(statsOverlay);
        // Pick up trees and bushes that were already loaded when the plugin was turned on
        clientThread.invoke(this::scanLoadedScene);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        overlayManager.remove(hidingOverlay);
        overlayManager.remove(statsOverlay);
        resetCycle();
        resetStats();
    }

    private void resetStats()
    {
        spearCount = 0;
        xpGained = 0;
        statsStartMs = 0;
        lastHunterXp = -1;
    }

    public boolean hasStatsSession()
    {
        return statsStartMs != 0;
    }

    /** Rate per hour since the first activity. */
    public long perHour(long amount)
    {
        if (statsStartMs == 0)
        {
            return 0;
        }
        // Treat the first minute as a full minute so the rate doesn't spike right after the first spear
        long elapsedMs = Math.max(System.currentTimeMillis() - statsStartMs, 60_000L);
        return amount * 3_600_000L / elapsedMs;
    }

    private void markStatsActivity()
    {
        if (statsStartMs == 0)
        {
            statsStartMs = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.HUNTER)
        {
            return;
        }

        int xp = event.getXp();
        if (lastHunterXp >= 0 && xp > lastHunterXp)
        {
            markStatsActivity();
            xpGained += xp - lastHunterXp;
        }
        lastHunterXp = xp;
    }

    private void resetCycle()
    {
        trees.clear();
        hideBushes.clear();
        baitedTrees.clear();
        pendingBaitTree = null;
        activeTree = null;
        hideBush = null;
        stopHiding();
    }

    public boolean isBaited(WorldPoint tree)
    {
        return baitedTrees.contains(tree);
    }

    public boolean hasBaitedTree()
    {
        return !baitedTrees.isEmpty();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
        {
            resetCycle();
            // Re-read the XP baseline after logging back in (could be a different account)
            lastHunterXp = -1;
        }
        else if (state == GameState.LOADING)
        {
            // The scene is being rebuilt; objects will be re-sent through GameObjectSpawned
            trees.clear();
            hideBushes.clear();
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        if (isTree(obj))
        {
            trees.put(obj.getWorldLocation(), obj);
        }
        else if (isHideBush(obj))
        {
            hideBushes.put(obj.getWorldLocation(), obj);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject obj = event.getGameObject();
        WorldPoint wp = obj.getWorldLocation();
        trees.remove(wp, obj);
        hideBushes.remove(wp, obj);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!isGameObjectAction(event.getMenuAction()))
        {
            return;
        }

        String option = Text.removeTags(event.getMenuOption()).trim().toLowerCase();
        boolean bait = BAIT_ACTION.equals(option);
        boolean hide = HIDE_ACTION.equals(option);
        if (!bait && !hide)
        {
            return;
        }

        GameObject clicked = findSceneObject(event.getId(), event.getParam0(), event.getParam1());
        if (clicked == null)
        {
            return;
        }
        WorldPoint wp = clicked.getWorldLocation();

        if (bait && isTree(clicked))
        {
            // Only remember the tree; it counts as baited once the game confirms it (see onChatMessage)
            trees.put(wp, clicked);
            pendingBaitTree = wp;
            log.debug("Bait clicked on tree at {}", wp);
        }
        else if (hide)
        {
            // Only remember the bush; hiding starts once the character is actually in it
            hideBush = wp;
            log.debug("Hide-in clicked on bush at {}", wp);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (pendingBaitTree == null || event.getType() != ChatMessageType.MESBOX)
        {
            return;
        }

        // "You bait the tree with a letvek to try and lure a stymphike."
        String msg = Text.removeTags(event.getMessage()).toLowerCase();
        if (msg.startsWith(BAITED_MESSAGE))
        {
            // Bait is actually in: tree loses its highlight and the bushes light up
            baitedTrees.add(pendingBaitTree);
            activeTree = pendingBaitTree;
            pendingBaitTree = null;
            stopHiding();
            log.debug("Stymphike tree baited at {}", activeTree);
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        Actor actor = event.getActor();
        if (actor == null || actor != client.getLocalPlayer())
        {
            return;
        }

        int anim = actor.getAnimation();
        if (anim == AnimationID.HUMAN_STYMPHIKE_IDLE_START || anim == AnimationID.HUMAN_STYMPHIKE_RUSTLE)
        {
            // Settling into the bush (or rustling while waiting, if the start was missed)
            startHiding();
        }
        else if (anim == AnimationID.HUMAN_STAKE2 && hiding)
        {
            // Spear thrust from the bush
            log.debug("Stymphike speared");
            markStatsActivity();
            spearCount++;
            completeCycle();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if (!hiding)
        {
            return;
        }

        NPC npc = event.getNpc();
        String name = npc.getName();
        if (name != null && Text.removeTags(name).toLowerCase().contains("stymphike"))
        {
            log.debug("Stymphike despawned while hiding");
            completeCycle();
        }
    }

    private void startHiding()
    {
        if (hiding)
        {
            return;
        }
        hiding = true;
        hideStartMs = System.currentTimeMillis();
        log.debug("Hidden in bush, timer started");
    }

    /** Stymphike speared (or gone): stop hiding, and the tree is unbaited again. */
    private void completeCycle()
    {
        if (activeTree != null)
        {
            baitedTrees.remove(activeTree);
        }
        stopHiding();
    }

    private void stopHiding()
    {
        hiding = false;
        hideStartMs = 0;
    }

    /**
     * Seconds left on the hiding timer, rounded up, or -1 if there is no running timer.
     */
    public int getTimerSecondsRemaining()
    {
        if (!hiding || hideStartMs == 0)
        {
            return -1;
        }

        long remainingMs = HIDE_DURATION_MS - (System.currentTimeMillis() - hideStartMs);
        if (remainingMs <= 0)
        {
            return -1;
        }
        return (int) ((remainingMs + 999) / 1000);
    }

    public GameObject getTreeObject(WorldPoint wp)
    {
        return trees.get(wp);
    }

    private static boolean isGameObjectAction(MenuAction action)
    {
        return action == MenuAction.GAME_OBJECT_FIRST_OPTION
                || action == MenuAction.GAME_OBJECT_SECOND_OPTION
                || action == MenuAction.GAME_OBJECT_THIRD_OPTION
                || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
                || action == MenuAction.GAME_OBJECT_FIFTH_OPTION;
    }

    private static boolean isTree(GameObject obj)
    {
        return obj != null && TREE_IDS.contains(obj.getId());
    }

    private boolean isHideBush(GameObject obj)
    {
        if (obj == null)
        {
            return false;
        }

        if (HIDE_BUSH_IDS.contains(obj.getId()))
        {
            return true;
        }

        // Fallback for bush variants not in the list: anything offering Hide-in
        ObjectComposition comp = client.getObjectDefinition(obj.getId());
        if (comp == null)
        {
            return false;
        }
        if (hasHideAction(comp))
        {
            return true;
        }
        if (comp.getImpostorIds() != null)
        {
            ObjectComposition impostor = comp.getImpostor();
            return impostor != null && hasHideAction(impostor);
        }
        return false;
    }

    private static boolean hasHideAction(ObjectComposition comp)
    {
        String[] actions = comp.getActions();
        if (actions == null)
        {
            return false;
        }
        for (String act : actions)
        {
            if (act != null && HIDE_ACTION.equalsIgnoreCase(act))
            {
                return true;
            }
        }
        return false;
    }

    private GameObject findSceneObject(int objectId, int sceneX, int sceneY)
    {
        Scene scene = client.getTopLevelWorldView().getScene();
        if (scene == null)
        {
            return null;
        }

        Tile[][][] tiles = scene.getTiles();
        int plane = client.getTopLevelWorldView().getPlane();
        if (plane < 0 || plane >= tiles.length
                || sceneX < 0 || sceneX >= tiles[plane].length
                || sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
        {
            return null;
        }

        Tile tile = tiles[plane][sceneX][sceneY];
        if (tile == null)
        {
            return null;
        }

        for (GameObject obj : tile.getGameObjects())
        {
            if (obj != null && obj.getId() == objectId)
            {
                return obj;
            }
        }
        return null;
    }

    /** One-off scan used only when the plugin is enabled mid-session. */
    private void scanLoadedScene()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Scene scene = client.getTopLevelWorldView().getScene();
        if (scene == null)
        {
            return;
        }

        Tile[][] planeTiles = scene.getTiles()[client.getTopLevelWorldView().getPlane()];
        for (Tile[] column : planeTiles)
        {
            for (Tile tile : column)
            {
                if (tile == null)
                {
                    continue;
                }
                for (GameObject obj : tile.getGameObjects())
                {
                    if (isTree(obj))
                    {
                        trees.put(obj.getWorldLocation(), obj);
                    }
                    else if (isHideBush(obj))
                    {
                        hideBushes.put(obj.getWorldLocation(), obj);
                    }
                }
            }
        }
    }
}
