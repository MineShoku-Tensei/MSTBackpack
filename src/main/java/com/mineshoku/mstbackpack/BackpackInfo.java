package com.mineshoku.mstbackpack;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApiStatus.Internal
public record BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						   boolean inDB, @Nullable @Unmodifiable List<@Nullable ItemStack> items) {
	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						boolean inDB, @Nullable List<@Nullable ItemStack> items) {
		this.playerID = playerID;
		this.profileID = profileID;
		this.extrasPlayer = extrasPlayer;
		this.extrasProfile = extrasProfile;
		this.inDB = inDB;
		this.items = fixItems(items);
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
						boolean inDB, @Nullable ItemStack @NotNull ... items) {
		this(playerID, profileID, extrasPlayer, extrasProfile, inDB, Arrays.asList(items));
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable ItemStack @NotNull ... items) {
		this(playerID, profileID, 0, 0, true, items);
	}

	public BackpackInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable List<@Nullable ItemStack> items) {
		this(playerID, profileID, 0, 0, true, items);
	}

	@Nullable
	@Unmodifiable
	@Contract("null -> null")
	private static List<@Nullable ItemStack> fixItems(@Nullable List<@Nullable ItemStack> items) {
		if (items == null || items.isEmpty()) return null;
		items = items.stream().map(item -> item == null || item.isEmpty() ? null : item).collect(Collectors.toList());
		while (items.getLast() == null) {
			items.removeLast();
		}
		return items.isEmpty() ? null : Collections.unmodifiableList(items);
	}
}