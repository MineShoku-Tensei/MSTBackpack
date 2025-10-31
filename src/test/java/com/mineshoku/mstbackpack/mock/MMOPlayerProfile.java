package com.mineshoku.mstbackpack.mock;

import com.mineshoku.mstutils.Utils;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.data.LocationMap;
import fr.phoenixdevt.profiles.data.ProfileAttributable;
import fr.phoenixdevt.profiles.placeholder.PlaceholderCache;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MMOPlayerProfile implements PlayerProfile<MMOProfile> {
	private final @NotNull UUID ID;

	public MMOPlayerProfile(@NotNull UUID ID) {
		this.ID = ID;
	}

	@NotNull
	public UUID getUniqueId() {
		return this.ID;
	}

	public double getHealth() {
		return Utils.throwUnimplemented();
	}

	public int getFoodLevel() {
		return Utils.throwUnimplemented();
	}

	public float getSaturation() {
		return Utils.throwUnimplemented();
	}

	public float getExperience() {
		return Utils.throwUnimplemented();
	}

	public int getLevel() {
		return Utils.throwUnimplemented();
	}

	public int getAirLevel() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public LocationMap getLocationMap() {
		return Utils.throwUnimplemented();
	}

	@Nullable
	public Location getLocation() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public LocationMap getTeleportLocationMap() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public Location findLastLocation() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public LocationMap getRespawnLocationMap() {
		return Utils.throwUnimplemented();
	}

	@Nullable
	public Location getRespawnLocation() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public LocationMap getCompassTargetMap() {
		return Utils.throwUnimplemented();
	}

	@Nullable
	public Location getCompassTarget() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public ItemStack[] getInventory() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public ItemStack[] getEnderChest() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public List<PotionEffect> getPotionEffects() {
		return Utils.throwUnimplemented();
	}

	public double getBalance() {
		return Utils.throwUnimplemented();
	}

	public long getLastTimePlayed() {
		return Utils.throwUnimplemented();
	}

	public long getTimePlayed() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public GameMode getGameMode() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public ProfileAttributable getAttributes() {
		return Utils.throwUnimplemented();
	}

	@NotNull
	public PlaceholderCache getPlaceholderCache() {
		return Utils.throwUnimplemented();
	}

	public void applyToPlayer(MMOProfile mmoProfile) {
		Utils.throwUnimplemented();
	}

	public void validateApplication(MMOProfile mmoProfile) {
		Utils.throwUnimplemented();
	}
}