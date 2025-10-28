package me.MineShoku.Backpack.Mock;

import fr.phoenixdevt.profiles.ProfileDataModule;
import fr.phoenixdevt.profiles.ProfileProvider;
import fr.phoenixdevt.profiles.placeholder.PlaceholderProcessor;
import me.MineShoku.Backpack.NotFullyImplemented;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MMOProfileProvider implements ProfileProvider<MMOProfile>, NotFullyImplemented {
	private @NotNull final HashMap<@NotNull UUID, @NotNull MMOProfile> profiles = new HashMap<>();

	public void registerModule(@NotNull ProfileDataModule profileDataModule) {
		notImplemented();
	}

	public void registerPlaceholders(@NotNull PlaceholderProcessor placeholderProcessor) {
		notImplemented();
	}

	@NotNull
	public Collection<ProfileDataModule> getModules() {
		return notImplemented();
	}

	@NotNull
	public Collection<PlaceholderProcessor> getPlaceholders() {
		return List.of();
	}

	@NotNull
	public MMOProfile getPlayerData(UUID uuid) {
		return Objects.requireNonNull(this.profiles.get(Objects.requireNonNull(uuid)));
	}

	public void addPlayerData(@NotNull MMOProfile profileList) {
		this.profiles.put(profileList.getUniqueId(), profileList);
	}

	public void removePlayerData(@NotNull UUID uuid) {
		this.profiles.remove(uuid);
	}
}