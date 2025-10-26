package me.DMan16;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record Info(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable List<@Nullable ItemStack> items, @NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
	@NotNull
	@Contract("_ -> new")
	public Info withItems(@Nullable List<@Nullable ItemStack> items) {
		return new Info(this.playerID, this.profileID, items, this.extrasProfile, this.extrasProfile);
	}
}