package kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents;

import kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.events.RegionEnterEvent;
import kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.events.RegionLeaveEvent;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

import java.util.*;

/**
 *
 * @author mewin
 */
public class WGRegionEventsListener implements Listener
{
    private final WorldGuardWrapper wgPlugin = WorldGuardWrapper.getInstance();
    private final JavaPlugin plugin;
    
    private final Map<Player, Set<IWrappedRegion>> playerRegions;
    
    public WGRegionEventsListener(JavaPlugin plugin)
    {
        this.plugin = plugin;
        playerRegions = new HashMap<>();
    }

    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e)
    {
        leave(e);
    }
    private static final Map<Player,Long> DELAY = new HashMap<>();
    private void leave(PlayerEvent e) {
        Set<IWrappedRegion> regions = playerRegions.remove(e.getPlayer());
        if (regions != null)
        {
            for(IWrappedRegion region : regions)
            {
                plugin.getServer().getPluginManager().callEvent(
                        new RegionLeaveEvent(region, e.getPlayer(), MovementWay.DISCONNECT, e)
                );
            }
        }
        DELAY.remove(e.getPlayer());
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        long after;
        if (DELAY.getOrDefault(e.getPlayer(),-200L) + 200 <= (after = System.currentTimeMillis())) {
            DELAY.put(e.getPlayer(),after);
            updateRegions(e.getPlayer(), MovementWay.MOVE, e.getTo(), e);
        }
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e)
    {
        updateRegions(e.getPlayer(), MovementWay.TELEPORT, e.getTo(), e);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        updateRegions(e.getPlayer(), MovementWay.SPAWN, e.getPlayer().getLocation(), e);
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e)
    {
        updateRegions(e.getPlayer(), MovementWay.SPAWN, e.getRespawnLocation(), e);
    }

    private void updateRegions(final Player player, final MovementWay movement, Location to, final PlayerEvent event)
    {
        Set<IWrappedRegion> newRegion, oldRegion;
        newRegion = wgPlugin.getRegions(to);
        oldRegion = playerRegions.get(player);

        if (newRegion != null) {
            if (oldRegion != null)
                newRegion
                        .stream()
                        .filter(i -> {
                            String id = i.getId();
                            for (IWrappedRegion t : oldRegion) {
                                if (id.equals(t.getId())) return false;
                            }
                            return true;
                        })
                        .forEach(i -> EvtUtil.call(new RegionEnterEvent(i, player, movement, event)));
            else newRegion.forEach(i -> EvtUtil.call(new RegionEnterEvent(i, player, movement, event)));
        }

        if (oldRegion != null) {
            if (newRegion != null)
                oldRegion
                        .stream()
                        .filter(i -> {
                            String id = i.getId();
                            for (IWrappedRegion t : newRegion) {
                                if (id.equals(t.getId())) return false;
                            }
                            return true;
                        })
                        .forEach(i -> EvtUtil.call(new RegionLeaveEvent(i, player, movement, event)));
            else oldRegion.forEach(i -> EvtUtil.call(new RegionLeaveEvent(i, player, movement, event)));
        }

        playerRegions.put(player,newRegion);
    }
}
