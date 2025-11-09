package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BackpackConfig {
	private static final @NotNull String PLACEHOLDER_PLAYER_ID = "player_id";
	private static final @NotNull String PLACEHOLDER_PLAYER_NAME = "player_name";
	private static final @NotNull String PLACEHOLDER_PROFILE_ID = "profile_id";
	private static final @NotNull String PLACEHOLDER_EXTRAS = "extras";
	private static final @NotNull String PLACEHOLDER_EXTRAS_MAX = "extras_max";
	private static final @NotNull String PLACEHOLDER_TOTAL = "total";
	private static final @NotNull String PLACEHOLDER_TOTAL_MAX = "total_max";

	private final @NotNull MSTBackpack plugin;
	private final @NotNull Configuration defaults;
	private @NotNull Type type;
	private boolean condense;
	private @NonNegative int amountBase;
	private @NonNegative int amountExtraPlayerMax;
	private @NonNegative int amountExtraPlayerPer;
	private @NonNegative int amountExtraProfileMax;
	private @NonNegative int amountExtraProfilePer;
	private @NonNegative long clearTimeout;
	private boolean removeBackpackProfileDelete;
	private @Nullable Component menuTitle;
	private boolean menuBorder;
	private boolean menuShowUnlockable;
	private @Nullable PageItem menuIndicator;
	private @NotNull PageItem menuClose;
	private @NotNull PageItem menuNext;
	private @NotNull PageItem menuPrevious;
	private @NotNull PageItem menuBorderStatic;
	private @NotNull PageItem menuBorderUnlockable;
	private @Nullable Component messageCommandFailed;
	private @Nullable Component messageNotFoundPlayer;
	private @Nullable Component messageNotFoundProfile;
	private @Nullable String messageClearResend;
	private @Nullable String messageClearFinish;
	private @Nullable String messageExtrasSetPlayer;
	private @Nullable String messageExtrasSetProfile;
	private @Nullable String messageExtrasInfoPlayer;
	private @Nullable String messageExtrasInfoProfile;
	private @Nullable Component messageProfileNotSelected;
	private @Nullable Component messageOpenFail;
	private @Nullable Component messageReloaded;
	private @Nullable String commandUsage;
	private @Nullable String commandDescription;

	public BackpackConfig(@NotNull MSTBackpack plugin) {
		if (plugin.backpackConfig() != null) throw new IllegalStateException("Config handler already initialized");
		this.plugin = plugin;
		this.plugin.saveDefaultConfig();
		this.defaults = Objects.requireNonNull(config().getDefaults());
		recalculate();
	}

	@NotNull
	private FileConfiguration config() {
		return this.plugin.getConfig();
	}

	@Nullable
	private String stringOrNull(@NotNull String path) {
		return config().getString(path, null);
	}

	@NonNegative
	private int intNonNegative(@NotNull String path) {
		return Math.max(config().getInt(path), 0);
	}

	private void recalculate() {
		this.type = Type.get(stringOrNull("type"));
		this.condense = config().getBoolean("condense", false);
		this.amountBase = intNonNegative("base");
		this.amountExtraPlayerMax = intNonNegative("extra.player.max");
		this.amountExtraPlayerPer = intNonNegative("extra.player.per");
		this.amountExtraProfileMax = intNonNegative("extra.profile.max");
		this.amountExtraProfilePer = intNonNegative("extra.profile.per");
		int clearTimeout = intNonNegative("clear_resend_seconds");
		this.clearTimeout = clearTimeout == 0 ? 0 : TimeUnit.SECONDS.toMillis(clearTimeout);
		this.removeBackpackProfileDelete = config().getBoolean("remove_backpack_profile_delete", true);
		this.menuTitle = Utils.toRichComponent(stringOrNull("menu.title"));
		this.menuBorder = config().getBoolean("menu.border", true);
		this.menuShowUnlockable = config().getBoolean("menu.show_unlockable", true);
		this.menuIndicator = pageItemFromConfig("menu.item.indicator", null);
		this.menuClose = pageItemFromConfig("menu.item.close", this.defaults);
		this.menuNext = pageItemFromConfig("menu.item.next", this.defaults);
		this.menuPrevious = pageItemFromConfig("menu.item.previous", this.defaults);
		this.menuBorderStatic = pageItemFromConfig("menu.item.border.regular", this.defaults);
		this.menuBorderUnlockable = pageItemFromConfig("menu.item.border.unlockable", this.defaults);
		this.messageCommandFailed = Utils.toRichComponent(stringOrNull("message.command_failed"));
		this.messageNotFoundPlayer = Utils.toRichComponent(stringOrNull("message.not_found.player"));
		this.messageNotFoundProfile = Utils.toRichComponent(stringOrNull("message.not_found.profile"));
		this.messageClearResend = stringOrNull("message.clear.resend");
		this.messageClearFinish = stringOrNull("message.clear.finish");
		this.messageExtrasSetPlayer = stringOrNull("message.extras.set.player");
		this.messageExtrasSetProfile = stringOrNull("message.extras.set.profile");
		this.messageExtrasInfoPlayer = stringOrNull("message.extras.info.player");
		this.messageExtrasInfoProfile = stringOrNull("message.extras.info.profile");
		this.messageProfileNotSelected = Utils.toRichComponent(stringOrNull("message.profile_not_selected"));
		this.messageOpenFail = Utils.toRichComponent(stringOrNull("message.open_fail"));
		this.messageReloaded = Utils.toRichComponent(stringOrNull("message.reloaded"));
		this.commandUsage = stringOrNull("command.usage");
		this.commandDescription = stringOrNull("command.description");
	}

	public void reload() {
		this.plugin.reloadConfig();
		recalculate();
	}

	@NotNull
	public Type type() {
		return this.type;
	}

	public boolean condense() {
		return this.condense;
	}

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

	@NonNegative
	public long clearTimeout() {
		return this.clearTimeout;
	}

	public boolean removeBackpackProfileDelete() {
		return this.removeBackpackProfileDelete;
	}

	@Nullable
	public Component menuTitle() {
		return this.menuTitle;
	}

	public boolean menuBorder() {
		return this.menuBorder;
	}

	public boolean menuShowUnlockable() {
		return this.menuShowUnlockable;
	}

	@Nullable
	public PageItem menuIndicator() {
		return this.menuIndicator;
	}

	@NotNull
	public PageItem menuClose() {
		return this.menuClose;
	}

	@NotNull
	public PageItem menuNext() {
		return this.menuNext;
	}

	@NotNull
	public PageItem menuPrevious() {
		return this.menuPrevious;
	}

	@NotNull
	public PageItem menuBorderStatic() {
		return this.menuBorderStatic;
	}

	@NotNull
	public PageItem menuBorderUnlockable() {
		return this.menuBorderUnlockable;
	}

	@Nullable
	public Component messageNotFoundPlayer() {
		return this.messageNotFoundPlayer;
	}

	@Nullable
	public Component messageCommandFailed() {
		return this.messageCommandFailed;
	}

	@Nullable
	public Component messageNotFoundProfile() {
		return this.messageNotFoundProfile;
	}

	@NotNull
	private TagResolver tagResolverPlayer(@NotNull UUID playerID, @NotNull String playerName) {
		return TagResolver.resolver(Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_NAME, playerName),Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_ID, playerID));
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
				Utils.toRichComponent(this.messageExtrasSetPlayer,tagResolverPlayer(playerID, playerName))) :
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

	@Nullable
	public Component messageProfileNotSelected() {
		return this.messageProfileNotSelected;
	}

	@Nullable
	public Component messageOpenFail() {
		return this.messageOpenFail;
	}

	@Nullable
	public Component messageReloaded() {
		return this.messageReloaded;
	}

	@NotNull
	public String commandUsage() {
		return this.commandUsage == null ? "" : this.commandUsage;
	}

	@NotNull
	public String commandDescription() {
		return this.commandDescription == null ? "" : this.commandDescription;
	}
	
	@Nullable
	@Contract("_, !null -> new")
	private PageItem pageItemFromConfig(@NotNull String sectionName, @Nullable Configuration defaults) {
		ConfigurationSection section = config().getConfigurationSection(sectionName);
		ConfigurationSection sectionDefaults = defaults == null ? null : defaults.getConfigurationSection(sectionName);
		Objects.requireNonNull(section);
		String type = section.getString("type", null);
		Material material;
		if (type == null || (material = Material.getMaterial(TextUtils.toUpperCase(type.replace(" ", "_")))) == null) {
			if (sectionDefaults == null) return null;
			String typeDefault = Objects.requireNonNull(TextUtils.toUpperCase(sectionDefaults.getString("type", null)));
			material = Objects.requireNonNull(Material.getMaterial(typeDefault));
		}
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
		NamespacedKey model = null;
		Integer customModel = null;
		if (section.contains("model", true)) {
			if (section.isInt("model")) {
				customModel = section.getInt("model");
				if (customModel <= 0) {
					customModel = null;
				}
			} else {
				String m = section.getString("model", null);
				if (m != null) {
					String[] arr = m.split(":", 2);
					model = new NamespacedKey(arr.length < 2 ? NamespacedKey.MINECRAFT : arr[0], arr[arr.length < 2 ? 0 : 1]);
				}
			}
		}
		return new PageItem(material, name, lore, model, customModel, slot);
	}

	public enum Type {
		PAGES,
		LINES,
		SLOTS;

		public static Type get(@Nullable String type) {
			if (type == null) return PAGES;
			try {
				return Type.valueOf(TextUtils.toUpperCase(type));
			} catch (IllegalArgumentException e) {
				return PAGES;
			}
		}
	}
}