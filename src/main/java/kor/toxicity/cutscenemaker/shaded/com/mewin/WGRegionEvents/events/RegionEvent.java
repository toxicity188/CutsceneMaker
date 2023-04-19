package kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.events;

import kor.toxicity.cutscenemaker.events.ICutsceneEvent;
import kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.MovementWay;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

/**
 *
 * @author mewin
 */
public abstract class RegionEvent extends PlayerEvent implements ICutsceneEvent {

    private static final HandlerList handlerList = new HandlerList();
    
    private final IWrappedRegion region;
    private final MovementWay movement;
    public PlayerEvent parentEvent;

    public RegionEvent(IWrappedRegion region, Player player, MovementWay movement, PlayerEvent parent)
    {
        super(player);
        this.region = region;
        this.movement = movement;
        this.parentEvent = parent;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    
    public IWrappedRegion getRegion()
    {
        return region;
    }
    
    public static HandlerList getHandlerList()
    {
        return handlerList;
    }
    
    public MovementWay getMovementWay()
    {
        return this.movement;
    }

    /**
     * retrieves the event that has been used to create this event
     * @see PlayerMoveEvent
     * @see PlayerTeleportEvent
     * @see PlayerQuitEvent
     * @see org.bukkit.event.player.PlayerKickEvent
     * @see PlayerJoinEvent
     * @see PlayerRespawnEvent
     * @return 
     */
    public PlayerEvent getParentEvent()
    {
        return parentEvent;
    }
}
