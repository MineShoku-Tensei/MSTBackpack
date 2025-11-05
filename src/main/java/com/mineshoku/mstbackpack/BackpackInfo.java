package com.mineshoku.mstbackpack;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApiStatus.Internal
public record BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID,
						   @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						   @Nullable List<@Nullable ItemStack> items) {
	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID,
						@NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						@Nullable List<@Nullable ItemStack> items) {
		this.playerID = playerID;
		this.profileID = profileID;
		this.items = fixItems(items);
		this.extrasPlayer = extrasPlayer;
		this.extrasProfile = extrasProfile;
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID,
						@NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						@Nullable ItemStack @Nullable ... items) {
		this(playerID, profileID, extrasPlayer, extrasProfile, items == null ? null : Arrays.asList(items));
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable ItemStack @Nullable ... items) {
		this(playerID, profileID, 0, 0, items);
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable List<@Nullable ItemStack> items) {
		this(playerID, profileID, 0, 0, items);
	}

	@Nullable
	@Contract("null -> null")
	private static List<@Nullable ItemStack> fixItems(@Nullable List<@Nullable ItemStack> items) {
		if (items == null || items.isEmpty()) return null;
		items = items.stream().map(item -> item == null || item.isEmpty() ? null : item).collect(Collectors.toList());
		while (items.getLast() == null) {
			items.removeLast();
		}
		return items.isEmpty() ? null : items;
	}

	@NotNull
	@Contract("_ -> new")
	public BackpackInfo withItems(@Nullable List<@Nullable ItemStack> items) {
		return new BackpackInfo(this.playerID, this.profileID, this.extrasProfile, this.extrasProfile, items);
	}
}