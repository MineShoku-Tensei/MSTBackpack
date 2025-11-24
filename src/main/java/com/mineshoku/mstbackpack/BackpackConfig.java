package com.mineshoku.mstbackpack;

import com.google.common.base.Preconditions;
import com.mineshoku.mstutils.MathUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.models.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BackpackConfig extends PluginConfig<MSTBackpack, BackpackConfig.Info> {
	public BackpackConfig(@NotNull MSTBackpack plugin) {
		super(plugin, "config.yml");
		saveDefaultConfig();
		recalculate();
	}

	@Override
	protected void testBeforeInit(@NotNull MSTBackpack plugin, @NotNull String resource) {
		Preconditions.checkState(plugin.backpackConfig() == null, "Config handler already initialized");
	}

	@Override
	protected void recalculate() {
		this.snapshot = new Info(Utils.thisOrThat(Type.get(stringNoDefault("type")), Type.PAGES), config().getBoolean("condense", false),
				intNonNegative("base"), intNonNegative("extra.player.max"), intNonNegative("extra.player.per"),
				intNonNegative("extra.profile.max"), intNonNegative("extra.profile.per"),
				MathUtils.thisOrThatMinimum(Utils.parseShortDurationMillis(config().get("clear_resend_timeout", null), TimeUnit.SECONDS), 0L),
				config().getBoolean("remove_backpack_profile_delete", true), Utils.toRichComponent(stringNoDefault("menu.title")),
				config().getBoolean("menu.border", true), config().getBoolean("menu.show_unlockable", true),
				pageItemFromConfig("menu.item.indicator", false), pageItemFromConfig("menu.item.close", true), pageItemFromConfig("menu.item.next", true),
				pageItemFromConfig("menu.item.previous", true), pageItemFromConfig("menu.item.border.regular", true),
				pageItemFromConfig("menu.item.border.unlockable", true), Utils.toRichComponent(stringNoDefault("message.command_failed")),
				Utils.toRichComponent(stringNoDefault("message.not_found.player")), Utils.toRichComponent(stringNoDefault("message.not_found.profile")),
				stringNoDefault("message.clear.resend"), stringNoDefault("message.clear.finish"), stringNoDefault("message.extras.set.player"),
				stringNoDefault("message.extras.set.profile"), stringNoDefault("message.extras.info.player"), stringNoDefault("message.extras.info.profile"),
				Utils.toRichComponent(stringNoDefault("message.profile_not_selected")), Utils.toRichComponent(stringNoDefault("message.open_fail")),
				Utils.toRichComponent(stringNoDefault("message.reloaded")), stringNoDefault("command.usage"), stringNoDefault("command.description"));
	}
	
	@Nullable
	@Contract("_, true -> new")
	private BackpackMenuItem pageItemFromConfig(@NotNull String sectionName, boolean useDefaults) {
		ConfigurationSection section = config().getConfigurationSection(sectionName);
		Objects.requireNonNull(section);
		ConfigurationSection sectionDefaults = useDefaults ? Objects.requireNonNull(section.getDefaultSection()) : null;
		Material material = Objects.requireNonNull(Utils.getMaterialOrDefault(section.getString("type", null),
				sectionDefaults == null ? null : sectionDefaults.getString("type", null)));
		int slot;
		if (section.contains("slot", true)) {
			slot = section.getInt("slot");
		} else if (sectionDefaults != null) {
			slot = sectionDefaults.getInt("slot");
		} else return null;
		String name = section.getString("name", "");
		if (name.isBlank()) {
			name = null;
		}
		List<String> lore = null;
		if (section.contains("lore", true)) {
			if (section.isList("lore")) {
				lore = section.getStringList("lore").stream().map(str -> TextUtils.isNullOrBlank(str) ? null : str).toList();
			} else {
				String l = section.getString("lore", null);
				lore = TextUtils.isNullOrBlank(l) ? null : List.of(l);
			}
		}
		NamespacedKey model;
		Integer customModel;
		if (section.contains("model", true)) {
			if (section.isInt("model")) {
				model = null;
				int m = section.getInt("model");
				customModel = m <= 0 ? null : m;
			} else {
				customModel = null;
				String m = section.getString("model", null);
				if (m == null) {
					model = null;
				} else {
					String[] arr = m.split(":", 2);
					model = new NamespacedKey(arr.length < 2 ? NamespacedKey.MINECRAFT : arr[0], arr[arr.length < 2 ? 0 : 1]);
				}
			}
		} else {
			model = null;
			customModel = null;
		}
		ItemStack item = ItemStack.of(material);
		item.editMeta(meta -> {
			if (model != null) {
				meta.setItemModel(model);
			}
			if (customModel != null) {
				meta.setCustomModelData(customModel);
			}
		});
		return new BackpackMenuItem(item, name, lore, slot);
	}

	public enum Type {
		PAGES,
		LINES,
		SLOTS;

		@Nullable
		@Contract(value = "null -> null", pure = true)
		public static Type get(@Nullable String name) {
			return Utils.enumFromName(Type.class, name);
		}
	}

	public record Info(@NotNull Type type, boolean condense, @NonNegative int amountBase, @NonNegative int amountExtraPlayerMax,
					   @NonNegative int amountExtraPlayerPer, @NonNegative int amountExtraProfileMax, @NonNegative int amountExtraProfilePer,
					   @NonNegative long clearTimeoutMillis, boolean removeBackpackProfileDelete, @Nullable Component menuTitle, boolean menuBorder,
					   boolean menuShowUnlockable, @Nullable BackpackMenuItem menuIndicator, @NotNull BackpackMenuItem menuClose, @NotNull BackpackMenuItem menuNext,
					   @NotNull BackpackMenuItem menuPrevious, @NotNull BackpackMenuItem menuBorderStatic, @NotNull BackpackMenuItem menuBorderUnlockable,
					   @Nullable Component messageCommandFailed, @Nullable Component messageNotFoundPlayer, @Nullable Component messageNotFoundProfile,
					   @Nullable String messageClearResend, @Nullable String messageClearFinish, @Nullable String messageExtrasSetPlayer,
					   @Nullable String messageExtrasSetProfile, @Nullable String messageExtrasInfoPlayer, @Nullable String messageExtrasInfoProfile,
					   @Nullable Component messageProfileNotSelected, @Nullable Component messageOpenFail, @Nullable Component messageReloaded,
					   @Nullable String commandUsage, @Nullable String commandDescription) {
		private static final @NotNull String PLACEHOLDER_PLAYER_ID = "player_id";
		private static final @NotNull String PLACEHOLDER_PLAYER_NAME = "player_name";
		private static final @NotNull String PLACEHOLDER_PROFILE_ID = "profile_id";
		private static final @NotNull String PLACEHOLDER_EXTRAS = "extras";
		private static final @NotNull String PLACEHOLDER_EXTRAS_MAX = "extras_max";
		private static final @NotNull String PLACEHOLDER_TOTAL = "total";
		private static final @NotNull String PLACEHOLDER_TOTAL_MAX = "total_max";

		@NonNegative
		public int amountExtraPlayerMax() {
			return this.amountExtraPlayerMax;
		}

		@NonNegative
		public int amountExtraProfileMax() {
			return this.amountExtraProfileMax;
		}

		@NonNegative
		public int amountExtrasMax() {
			return amountExtraPlayerMax() + amountExtraProfileMax();
		}

		@NonNegative
		private int calculateExtras(@NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
			int player = Math.min(extrasPlayer, amountExtraPlayerMax());
			int profile = Math.min(extrasProfile, amountExtraProfileMax());
			return (player * this.amountExtraPlayerPer) + (profile * this.amountExtraProfilePer);
		}

		@NonNegative
		public int calculateAmount(@NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
			return this.amountBase + calculateExtras(extrasPlayer, extrasProfile);
		}

		@NonNegative
		public int amountTotalMax() {
			return calculateAmount(amountExtraPlayerMax(), amountExtraProfileMax());
		}

		@NotNull
		private TagResolver tagResolverPlayer(@NotNull UUID playerID, @NotNull String playerName) {
			return TagResolver.resolver(Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_NAME, playerName), Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_ID, playerID));
		}

		@NotNull
		private TagResolver tagResolverPlayerProfile(@NotNull UUID playerID, @NotNull String playerName, @NotNull UUID profileID) {
			return TagResolver.resolver(tagResolverPlayer(playerID, playerName), Utils.unparsedPlaceholder(PLACEHOLDER_PROFILE_ID, profileID));
		}

		@Nullable
		public Component messageClearResend(@NotNull UUID playerID, @NotNull String playerName, @NotNull UUID profileID) {
			return this.messageClearResend == null ? null :
					Utils.toRichComponent(this.messageClearResend, tagResolverPlayerProfile(playerID, playerName, profileID));
		}

		@Nullable
		public Component messageClearFinish(@NotNull UUID playerID, @NotNull String playerName, @NotNull UUID profileID) {
			return this.messageClearFinish == null ? null :
					Utils.toRichComponent(this.messageClearFinish, tagResolverPlayerProfile(playerID, playerName, profileID));
		}

		@Nullable
		public Component messageExtrasSet(@NotNull UUID playerID, @NotNull String playerName, @Nullable UUID profileID) {
			return profileID == null ? (this.messageExtrasSetPlayer == null ? null :
					Utils.toRichComponent(this.messageExtrasSetPlayer, tagResolverPlayer(playerID, playerName))) :
					(this.messageExtrasSetProfile == null ? null :
							Utils.toRichComponent(this.messageExtrasSetProfile, tagResolverPlayerProfile(playerID, playerName, profileID)));
		}

		@Nullable
		public Component messageExtrasInfo(@NotNull UUID playerID, @NotNull String playerName, @Nullable UUID profileID,
										   @NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
			return profileID == null ? (this.messageExtrasInfoPlayer == null ? null :
					Utils.toRichComponent(this.messageExtrasInfoPlayer, tagResolverPlayer(playerID, playerName),
							Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS, extrasPlayer),
							Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_MAX, amountExtraPlayerMax()))) :
					(this.messageExtrasInfoProfile == null ? null :
							Utils.toRichComponent(this.messageExtrasInfoProfile, tagResolverPlayerProfile(playerID, playerName, profileID),
									Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS, extrasProfile),
									Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_MAX, amountExtraProfileMax()),
									Utils.unparsedPlaceholder(PLACEHOLDER_TOTAL, extrasPlayer + extrasProfile),
									Utils.unparsedPlaceholder(PLACEHOLDER_TOTAL_MAX, amountExtrasMax())));
		}
	}
}