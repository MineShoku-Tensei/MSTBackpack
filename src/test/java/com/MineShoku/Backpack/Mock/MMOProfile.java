package com.MineShoku.Backpack.Mock;

import fr.phoenixdevt.profiles.ProfileList;
import com.MineShoku.Backpack.NotFullyImplemented;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class MMOProfile implements ProfileList<MMOPlayerProfile>, NotFullyImplemented {
	private final @NotNull Player player;
	private final @NotNull LinkedHashMap<@NotNull UUID, @NotNull MMOPlayerProfile> profiles = new LinkedHashMap<>();
	private @Nullable MMOPlayerProfile currentProfile;
	
	public MMOProfile(@NotNull Player player) {
		this.player = player;
	}

	@NotNull
	public UUID getUniqueId() {
		return this.player.getUniqueId();
	}

	@NotNull
	public Player getPlayer() {
		return this.player;
	}

	@NotNull
	public List<MMOPlayerProfile> getProfiles() {
		return List.copyOf(this.profiles.values());
	}

	@Nullable
	public MMOPlayerProfile getCurrent() {
		return this.currentProfile;
	}

	@Nullable
	public MMOPlayerProfile getProfile(UUID uuid) {
		return this.profiles.get(uuid);
	}

	public void addProfile(@NotNull MMOPlayerProfile playerProfile) {
		this.profiles.put(playerProfile.getUniqueId(), playerProfile);
	}

	public void setCurrentProfile(@Nullable UUID uuid) {
		this.currentProfile = getProfile(uuid);
	}

	public void setCurrentProfileIfNotSet() {
		setCurrentProfile(this.profiles.values().iterator().next().getUniqueId());
	}
}