package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@EqualsAndHashCode
public class LocationStudio {
    private static final Map<Player,PlayerRecord> RECORD_MAP = new HashMap<>();
    private final List<CurrentRecord> records = new ArrayList<>();
    private final int max;

    private static final ArrangeStrategy INDEXED_ARRANGE = (player,studio,key) -> {
        for (CurrentRecord record : studio.records) {
            if (record.join(player, key)) return true;
        }
        return false;
    };
    private static final ArrangeStrategy MIN_ARRANGE = (player, studio, key) -> Collections.min(studio.records).join(player,key);

    public static Optional<PlayerRecord> getPlayerRecord(Player player) {
        PlayerRecord record;
        return ((record = RECORD_MAP.get(player)) != null) ? Optional.of(record) : Optional.empty();
    }
    public boolean join(Player player, String key) {
        quit(player);
        return strategy.arrange(player,this,key);
    }
    public static void quit(Player player) {
        getPlayerRecord(player).ifPresent(PlayerRecord::cancel);
    }
    public static void quitWithoutBack(Player player) {
        getPlayerRecord(player).ifPresent(PlayerRecord::cancelWithoutBack);
    }
    public Optional<Location> getLocation(Player player, String key) {
        return records.stream().filter(l -> l.contains(player)).findFirst().map(l -> l.getLocation(key));
    }
    public boolean inRecord(Player player) {
        return records.stream().anyMatch(r -> r.contains(player));
    }

    public static Optional<LocationStudio> fromConfig(String name, CutsceneManager manager, ConfigurationSection section) {
        try {
            return Optional.of(new LocationStudio(name,manager,section));
        } catch (Exception e) {
            CutsceneMaker.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private final CutsceneManager manager;
    private final String name;
    private final ArrangeStrategy strategy;
    private final Map<String,Location> locationMap = new HashMap<>();
    private LocationStudio(String name, CutsceneManager manager, ConfigurationSection section) {
        this.name = name;
        this.manager = manager;
        strategy = (ConfigUtil.getString(section,"arrange").orElse("indexed").equals("indexed")) ? INDEXED_ARRANGE : MIN_ARRANGE;
        max = Math.max(
                ConfigUtil.getInt(section,"Max").orElse(1),
                1
        );
        List<String> loc = ConfigUtil.getStringList(section,"Locations").orElseThrow(() -> new NullPointerException("Syntax error: unable to find the section \"Locations\""));
        loc.forEach(s -> {
            Location t = manager.getLocations().getValue(s);
            if (t == null) CutsceneMaker.warn("The Location named \"" +s + "\" doesn't exist!");
            else locationMap.put(s,t);
        });

        ConfigurationSection rec = ConfigUtil.getConfig(section,"Records").orElseThrow(() -> new NullPointerException("Syntax error: unable to find the section \"Records\""));
        int i = 0;
        for (String c : rec.getKeys(false)) {
            ConfigurationSection t = ConfigUtil.getConfig(rec,c).orElse(null);
            if (t != null) {
                try {
                    records.add(new CurrentRecord(i++, t));
                } catch (Exception e) {
                    CutsceneMaker.warn(e.getMessage());
                }
            }
        }
    }

    @EqualsAndHashCode
    private class CurrentRecord implements Comparable<CurrentRecord> {
        private final Map<String, Location> recordLocation = new HashMap<>();
        @EqualsAndHashCode.Exclude
        private final IWrappedRegion region;
        @EqualsAndHashCode.Exclude
        private final Set<Player> players = new HashSet<>();
        private final int num;
        private CurrentRecord(int num, ConfigurationSection section) {
            this.num = num;
            String worldName = ConfigUtil.getString(section,"World").orElseThrow(() -> new NullPointerException("World value cannot be null!"));
            World world = Objects.requireNonNull(
                            Bukkit.getWorld(worldName),
                            "The world named \"" + worldName + "\" doesn't exist!"
            );
            String reg = ConfigUtil.getString(section,"Region").orElseThrow(() -> new NullPointerException("Region value cannot be null!"));
            region = Objects.requireNonNull(
                    WorldGuardWrapper
                            .getInstance()
                            .getRegions(world)
                            .get(reg),
                    "The Region named \"" + reg + "\" doesn't exist!"
            );
            locationMap.forEach((k,v) -> {
                Location loc = v.clone();
                loc.setWorld(world);
                recordLocation.put(k,loc);
            });

        }

        private void check() {
            players.removeIf(player -> !region.contains(player.getLocation()));
        }
        private boolean contains(Player player) {
            if (!players.contains(player)) return false;
            if (region.contains(player.getLocation())) return true;
            else {
                remove(player);
                return false;
            }
        }
        private Location getLocation(String key) {
            return locationMap.get(key);
        }
        private boolean join(Player player, String key) {
            check();
            if (isFull() || contains(player)) return false;
            Location loc;
            if ((loc = getLocation(Objects.requireNonNull(key,"key value not null!"))) == null) return false;
            players.add(Objects.requireNonNull(player,"player value cannot be null!"));
            RECORD_MAP.put(player,new PlayerRecord(
                    player,
                    LocationStudio.this,
                    this,
                    player.getLocation()
            ));
            player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            return true;
        }
        private boolean isFull() {
            return players.size() >= max;
        }
        private void remove(Player player) {
            players.remove(player);
            RECORD_MAP.remove(player);
        }

        @Override
        public int compareTo(@NotNull CurrentRecord o) {
            return Integer.compare(players.size(),o.players.size());
        }
    }


    private interface ArrangeStrategy {
        boolean arrange(Player player, LocationStudio studio, String key);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    public static class PlayerRecord {
        private final Player player;
        private final LocationStudio studio;
        private final CurrentRecord record;
        private final Location beforeLocation;

        public Location getLocation(@NotNull String key) {
            return record.getLocation(Objects.requireNonNull(key));
        }
        public int getRecordNumber() {
            return record.num;
        }
        public boolean isValid() {
            return record.contains(player);
        }
        public String getName() {
            return studio.name;
        }
        public void cancelWithoutBack() {
            record.remove(player);
        }
        public void cancel() {
            cancelWithoutBack();
            player.teleport(beforeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }
}
