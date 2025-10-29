package me.MineShoku.Backpack.Mock;

import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.data.LocationMap;
import fr.phoenixdevt.profiles.data.ProfileAttributable;
import fr.phoenixdevt.profiles.placeholder.PlaceholderCache;
import me.MineShoku.Backpack.NotFullyImplemented;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MMOPlayerProfile implements PlayerProfile<MMOProfile>, NotFullyImplemented {
	private final @NotNull UUID ID;

	public MMOPlayerProfile(@NotNull UUID ID) {
		this.ID = ID;
	}

	@NotNull
	public UUID getUniqueId() {
		return this.ID;
	}

	public double getHealth() {
		return notImplemented();
	}

	public int getFoodLevel() {
		return notImplemented();
	}

	public float getSaturation() {
		return notImplemented();
	}

	public float getExperience() {
		return notImplemented();
	}

	public int getLevel() {
		return notImplemented();
	}

	public int getAirLevel() {
		return notImplemented();
	}

	@NotNull
	public LocationMap getLocationMap() {
		return notImplemented();
	}

	@Nullable
	public Location getLocation() {
		return notImplemented();
	}

	@NotNull
	public LocationMap getTeleportLocationMap() {
		return notImplemented();
	}

	@NotNull
	public Location findLastLocation() {
		return notImplemented();
	}

	@NotNull
	public LocationMap getRespawnLocationMap() {
		return notImplemented();
	}

	@Nullable
	public Location getRespawnLocation() {
		return notImplemented();
	}

	@NotNull
	public LocationMap getCompassTargetMap() {
		return notImplemented();
	}

	@Nullable
	public Location getCompassTarget() {
		return notImplemented();
	}

	@NotNull
	public ItemStack[] getInventory() {
		return notImplemented();
	}

	@NotNull
	public ItemStack[] getEnderChest() {
		return notImplemented();
	}

	@NotNull
	public List<PotionEffect> getPotionEffects() {
		return notImplemented();
	}

	public double getBalance() {
		return notImplemented();
	}

	public long getLastTimePlayed() {
		return notImplemented();
	}

	public long getTimePlayed() {
		return notImplemented();
	}

	@NotNull
	public GameMode getGameMode() {
		return notImplemented();
	}

	@NotNull
	public ProfileAttributable getAttributes() {
		return notImplemented();
	}

	@NotNull
	public PlaceholderCache getPlaceholderCache() {
		return notImplemented();
	}

	public void applyToPlayer(MMOProfile mmoProfile) {
		notImplemented();
	}

	public void validateApplication(MMOProfile mmoProfile) {
		notImplemented();
	}
}