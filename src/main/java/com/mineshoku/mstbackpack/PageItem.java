package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.MathUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.*;

import java.util.List;
import java.util.regex.Pattern;

@ApiStatus.Internal
public record PageItem(@NotNull Material material, @Nullable String name, @Nullable @Unmodifiable List<String> lore,
					   @Nullable NamespacedKey model, @Nullable @Positive Integer customModel, int slot) {
	private static final @NotNull String PLACEHOLDER_PAGE = "page";
	private static final @NotNull String PLACEHOLDER_PAGES_TOTAL = "pages";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER = "extras_player";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER_MAX = "extras_player_max";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE = "extras_profile";
	private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE_MAX = "extras_profile_max";
	private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL = "extras_total";
	private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL_MAX = "extras_total_max";
	private static final @NotNull Pattern PATTERN_PAGE = Pattern.compile("<" + PLACEHOLDER_PAGE + "([+-]\\d+)>");

	@NotNull
	@Contract("_, _, _, _, _, _ -> new")
	public ItemStack toItemStack(@Positive int currentPage, @NonNegative int totalPages,
								 @NonNegative int extrasPlayer, @NonNegative int extrasProfile,
								 @NonNegative int extrasPlayerMax, @NonNegative int extrasProfileMax) {
		ItemStack item = ItemStack.of(this.material);
		item.editMeta(meta -> {
			meta.itemName(replace(this.name, currentPage, totalPages, extrasPlayer, extrasProfile,
					extrasPlayerMax, extrasProfileMax));
			if (this.lore != null) {
				meta.lore(this.lore.stream().
						map(str -> replace(str, currentPage, totalPages, extrasPlayer, extrasProfile,
								extrasPlayerMax, extrasProfileMax)).toList());
			}
			if (this.model != null) {
				meta.setItemModel(this.model);
			}
			if (this.customModel != null) {
				meta.setCustomModelData(this.customModel);
			}
		});
		return item;
	}

	@NotNull
	private static Component replace(@Nullable String str,
									 @Positive int currentPage, @NonNegative int totalPages,
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