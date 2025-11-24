package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.MathUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.menus.MenuItem;
import com.mineshoku.mstutils.menus.MenuPages;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@ApiStatus.Internal
public final class BackpackMenuItem extends MenuItem.Unique<MSTBackpack, BackpackMenuPages.MenuInfo> {
	private static final @NotNull String PLACEHOLDER_PAGE = "page";
	private static final @NotNull String PLACEHOLDER_PAGES_TOTAL = "pages";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER = "extras_player";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER_MAX = "extras_player_max";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE = "extras_profile";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE_MAX = "extras_profile_max";
	private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL = "extras_total";
	private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL_MAX = "extras_total_max";
	private static final @NotNull Pattern PATTERN_PAGE = Pattern.compile("<" + PLACEHOLDER_PAGE + "([+-]\\d+)>");

	private final @NotNull ItemStack item;
	private final @Nullable String name;
	private final @Nullable @Unmodifiable List<String> lore;
	private final int slot;

	public BackpackMenuItem(@NotNull ItemStack item, @Nullable String name, @Nullable List<String> lore, int slot) {
		this.item = item.clone();
		this.name = name;
		this.lore = lore == null ? null : Collections.unmodifiableList(lore);
		this.slot = slot;
	}

	public int slot() {
		return slot;
	}

	@Override
	@NotNull
	protected ItemStack toItemStackNotUnique(@NotNull MenuPages<MSTBackpack, BackpackMenuPages.MenuInfo> menuPages) {
		int currentPage = menuPages.currentPage(), totalPages = menuPages.maxPages(), extrasPlayer = menuPages.info().backpackInfo().extrasPlayer(),
				extrasProfile = menuPages.info().backpackInfo().extrasProfile(),
				extrasPlayerMax = menuPages.plugin().backpackConfig().snapshot().amountExtraPlayerMax(),
				extrasProfileMax = menuPages.plugin().backpackConfig().snapshot().amountExtraProfileMax();
		ItemStack clone = this.item.clone();
		clone.editMeta(meta -> {
			meta.itemName(replace(this.name, currentPage, totalPages, extrasPlayer, extrasProfile, extrasPlayerMax, extrasProfileMax));
			if (this.lore != null) {
				meta.lore(this.lore.stream().
						map(str -> replace(str, currentPage, totalPages, extrasPlayer, extrasProfile, extrasPlayerMax, extrasProfileMax)).toList());
			}
		});
		return clone;
	}

	@ApiStatus.Internal
	@NotNull
	public static Component replace(@Nullable String str, @Positive int currentPage, @NonNegative int totalPages,
									 @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
									 @NonNegative int extrasPlayerMax, @NonNegative int extrasProfileMax) {
		if (TextUtils.isNullOrBlank(str)) return Component.empty();
		str = TextUtils.replacePlaceholdersComplex(str, PATTERN_PAGE, matcher -> {
			Integer delta = MathUtils.getInteger(matcher.group(1));
			return delta == null ? null : String.valueOf(Math.clamp(currentPage + delta, 1, Math.max(totalPages, 1)));
		});
		return Utils.toRichComponent(str,
				Utils.unparsedPlaceholder(PLACEHOLDER_PAGE, currentPage),
				Utils.unparsedPlaceholder(PLACEHOLDER_PAGES_TOTAL, totalPages),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_PLAYER, extrasPlayer),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_PLAYER_MAX, extrasPlayerMax),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_PROFILE, extrasProfile),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_PROFILE_MAX, extrasProfileMax),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_TOTAL, extrasPlayer + extrasProfile),
				Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_TOTAL_MAX, extrasPlayerMax + extrasProfileMax)
		);
	}
}