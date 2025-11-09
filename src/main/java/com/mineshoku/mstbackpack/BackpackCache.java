package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.managers.MMOProfilesManager;
import fr.phoenixdevt.profiles.event.ProfileCreateEvent;
import fr.phoenixdevt.profiles.event.ProfileRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public final class BackpackCache implements Listener {
	private final @NotNull MSTBackpack plugin;
	private final @NotNull Map<@NotNull UUID, @NotNull LinkedHashSet<@NotNull UUID>> cache = new HashMap<>();
	private final @NotNull BukkitTask schedule;

	public BackpackCache(@NotNull MSTBackpack plugin) {
		if (plugin.cacheListener() != null) throw new IllegalStateException("Backpack cache listener already initialized");
		this.plugin = plugin;
		this.schedule = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::update, 0, Utils.secondsToTicks(TimeUnit.MINUTES.toSeconds(1)));
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
	}

	@ApiStatus.Internal
	public void shutdown() {
		this.schedule.cancel();
		HandlerList.unregisterAll(this);
	}

	private void update() {
		this.plugin.backpackDatabase().getProfiles().exceptionally(e -> {
			LoggingManager.instance().log(this.plugin, e);
			return new LinkedHashMap<>();
		}).thenCombine(this.plugin.backpackDatabase().getPlayers().exceptionally(e -> {
			LoggingManager.instance().log(this.plugin, e);
			return new LinkedHashSet<>();
		}), (profilesMap, players) -> {
			players.forEach(playerID -> profilesMap.putIfAbsent(playerID, new LinkedHashSet<>()));
			return profilesMap;
		}).thenAcceptAsync(profilesMap -> {
			Map<UUID, Set<UUID>> remove = new HashMap<>(), insert = new HashMap<>();
			profilesMap.forEach((playerID, profiles) -> {
				LinkedHashSet<UUID> profilesOnline = MMOProfilesManager.instance().getProfileIDs(playerID);
				if (profilesOnline == null) {
					LinkedHashSet<UUID> existing = this.cache.computeIfAbsent(playerID, u -> new LinkedHashSet<>()), local = new LinkedHashSet<>(existing);
					existing.addAll(profiles);
					local.removeAll(profiles);
					if (!local.isEmpty()) {
						insert.put(playerID, local);
					}
				} else {
					LinkedHashSet<UUID> existing = this.cache.put(playerID, new LinkedHashSet<>(profilesOnline));
					if (existing == null) {
						insert.put(playerID, new LinkedHashSet<>(profilesOnline));
					} else {
						existing.removeAll(profilesOnline);
						if (!existing.isEmpty()) {
							remove.put(playerID, existing);
						}
					}
				}
			});
			if (this.plugin.backpackConfig().removeBackpackProfileDelete()) {
				LoggingManager.exceptionallyLog(this.plugin, this.plugin.backpackDatabase().removeProfiles(remove));
			}
			this.plugin.backpackDatabase().insertProfiles(insert);
		}, ExecutorManager.mainThreadExecutor(this.plugin));
	}

	private void addPlayer(@NotNull UUID playerID) {
		Set<UUID> profiles = MMOProfilesManager.instance().getProfileIDs(playerID);
		if (!this.cache.containsKey(playerID)) {
			this.plugin.backpackDatabase().insertPlayer(playerID);
		}
		this.cache.put(playerID, profiles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(profiles));
		if (profiles == null) return;
		this.plugin.backpackDatabase().insertProfiles(Map.of(playerID, profiles));
	}

	private void addProfile(@NotNull UUID playerID, @NotNull UUID profileID) {
		if (!MMOProfilesManager.instance().hasPlayerProfile(playerID, profileID)) return;
		this.cache.computeIfAbsent(playerID, u -> new LinkedHashSet<>()).add(profileID);
		this.plugin.backpackDatabase().insertProfiles(Map.of(playerID, Set.of(profileID)));
	}

	private void removeProfile(@NotNull UUID playerID, @NotNull UUID profileID) {
		if (MMOProfilesManager.instance().hasPlayerProfile(playerID, profileID)) return;
		this.cache.getOrDefault(playerID, new LinkedHashSet<>()).remove(profileID);
		LoggingManager.exceptionallyLog(this.plugin, this.plugin.backpackDatabase().removeProfiles(Map.of(playerID, Set.of(profileID))));
	}

	@Nullable
	@Unmodifiable
	public SequencedSet<@NotNull UUID> profiles(@NotNull UUID playerID) {
		LinkedHashSet<UUID> profiles = MMOProfilesManager.instance().getProfileIDs(playerID);
		if (profiles == null) {
			profiles = this.cache.get(playerID);
		} else {
			LinkedHashSet<UUID> insert = new LinkedHashSet<>(profiles), old = this.cache.put(playerID, new LinkedHashSet<>(profiles));
			if (old != null) {
				insert.removeAll(old);
			}
			if (!insert.isEmpty()) {
				this.plugin.backpackDatabase().insertProfiles(Map.of(playerID, insert));
			}
		}
		return profiles == null ? null : Collections.unmodifiableSequencedSet(profiles);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLogin(PlayerJoinEvent event) {
		addPlayer(event.getPlayer().getUniqueId());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onProfileCreate(ProfileCreateEvent event) {
		UUID playerID = event.getPlayer().getUniqueId(), profileID = event.getProfile().getUniqueId();
		Bukkit.getScheduler().runTaskLater(this.plugin, () -> addProfile(playerID, profileID), 1);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onProfileRemove(ProfileRemoveEvent event) {
		UUID playerID = event.getPlayer().getUniqueId(), profileID = event.getProfile().getUniqueId();
		Bukkit.getScheduler().runTaskLater(this.plugin, () -> removeProfile(playerID, profileID), 1);
	}
}