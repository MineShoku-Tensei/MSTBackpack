package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.models.Pair;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BackpackUtils {
	private BackpackUtils() {}

	@NotNull
	public static CompletableFuture<@Nullable List<@Nullable ItemStack>> getItems(@NotNull UUID playerID, @NotNull UUID profileID) {
		return MSTBackpack.instance().database().getInfo(playerID, profileID).thenApply(BackpackInfo::items);
	}

	@NotNull
	public static CompletableFuture<Void> saveItems(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable List<@Nullable ItemStack> items) {
		return MSTBackpack.instance().database().saveItems(new BackpackInfo(playerID, profileID, 0, 0, items));
	}

	@NotNull
	private static CompletableFuture<Void> updateExtrasAsync(@NotNull UUID playerID, @Nullable UUID profileID, int delta) {
		return MSTBackpack.instance().database().updateExtrasAsync(playerID, profileID, delta);
	}

	@NotNull
	public static CompletableFuture<Void> updateExtrasPlayer(@NotNull UUID playerID, int delta) {
		return updateExtrasAsync(playerID, null, delta);
	}

	@NotNull
	public static CompletableFuture<Void> updateExtrasProfile(@NotNull UUID playerID, @NotNull UUID profileID, int delta) {
		return updateExtrasAsync(playerID, profileID, delta);
	}

	@NotNull
	public static CompletableFuture<@NotNull Pair<@NotNull @NonNegative Integer, @NotNull @NonNegative Integer>>
	getExtras(@NotNull UUID playerID, @Nullable UUID profileID) {
		return MSTBackpack.instance().database().getExtras(playerID, profileID);
	}
}