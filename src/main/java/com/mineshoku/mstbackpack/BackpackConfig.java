package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
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
	private final @Nullable String host;
	private final int port;
	private final @Nullable String database;
	private final @Nullable String username;
	private final @Nullable String password;
	private @NotNull Type type;
	private boolean condense;
	private @NonNegative int amountBase;
	private @NonNegative int amountExtraPlayerMax;
	private @NonNegative int amountExtraPlayerPer;
	private @NonNegative int amountExtraProfileMax;
	private @NonNegative int amountExtraProfilePer;
	private @NonNegative long clearTimeout;
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
	private @Nullable Component messageProfileNotSelected;
	private @Nullable Component messageOpenFail;
	private @Nullable Component messageReloaded;
	private @Nullable String commandUsage;
	private @Nullable String commandDescription;

	public BackpackConfig(@NotNull MSTBackpack plugin) {
		this.plugin = plugin;
		this.plugin.saveDefaultConfig();
		this.defaults = Objects.requireNonNull(config().getDefaults());
		reloadConfig();
		this.host = stringOrNull("mysql.host");
		this.port = intNonNegative("mysql.port");
		this.database = stringOrNull("mysql.database");
		this.username = stringOrNull("mysql.username");
		this.password = stringOrNull("mysql.password");
		recalculate();
	}

	@NotNull
	private FileConfiguration config() {
		return this.plugin.getConfig();
	}

	private void reloadConfig() {
		this.plugin.reloadConfig();
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
		this.type = Type.get(stringOrNull("backpack.type"));
		this.condense = config().getBoolean("backpack.condense", false);
		this.amountBase = intNonNegative("backpack.base");
		this.amountExtraPlayerMax = intNonNegative("backpack.extra.player.max");
		this.amountExtraPlayerPer = intNonNegative("backpack.extra.player.per");
		this.amountExtraProfileMax = intNonNegative("backpack.extra.profile.max");
		this.amountExtraProfilePer = intNonNegative("backpack.extra.profile.per");
		int clearTimeout = intNonNegative("backpack.clear_resend_seconds");
		this.clearTimeout = clearTimeout == 0 ? 0 : TimeUnit.SECONDS.toMillis(clearTimeout);
		this.menuTitle = Utils.toRichComponent(stringOrNull("backpack.menu.title"));
		this.menuBorder = config().getBoolean("backpack.menu.border", true);
		this.menuShowUnlockable = config().getBoolean("backpack.menu.show_unlockable", true);
		this.menuIndicator = pageItemFromConfig("backpack.menu.item.indicator", null);
		this.menuClose = pageItemFromConfig("backpack.menu.item.close", this.defaults);
		this.menuNext = pageItemFromConfig("backpack.menu.item.next", this.defaults);
		this.menuPrevious = pageItemFromConfig("backpack.menu.item.previous", this.defaults);
		this.menuBorderStatic = pageItemFromConfig("backpack.menu.item.border.regular", this.defaults);
		this.menuBorderUnlockable = pageItemFromConfig("backpack.menu.item.border.unlockable", this.defaults);
		this.messageCommandFailed = Utils.toRichComponent(stringOrNull("backpack.message.command_failed"));
		this.messageNotFoundPlayer = Utils.toRichComponent(stringOrNull("backpack.message.not_found.player"));
		this.messageNotFoundProfile = Utils.toRichComponent(stringOrNull("backpack.message.not_found.profile"));
		this.messageClearResend = stringOrNull("backpack.message.clear.resend");
		this.messageClearFinish = stringOrNull("backpack.message.clear.finish");
		this.messageExtrasSetPlayer = stringOrNull("backpack.message.extras_set.player");
		this.messageExtrasSetProfile = stringOrNull("backpack.message.extras_set.profile");
		this.messageProfileNotSelected = Utils.toRichComponent(stringOrNull("backpack.message.profile_not_selected"));
		this.messageOpenFail = Utils.toRichComponent(stringOrNull("backpack.message.open_fail"));
		this.messageReloaded = Utils.toRichComponent(stringOrNull("backpack.message.reloaded"));
		this.commandUsage = stringOrNull("backpack.command.usage");
		this.commandDescription = stringOrNull("backpack.command.description");
	}

	public void reload() {
		reloadConfig();
		recalculate();
	}

	@Nullable
	public String host() {
		return this.host;
	}

	public int port() {
		return this.port;
	}

	@Nullable
	public String database() {
		return this.database;
	}

	@Nullable
	public String username() {
		return this.username;
	}

	@Nullable
	public String password() {
		return this.password;
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
	private TagResolver tagResolverPlayer(@NotNull OfflinePlayer offlinePlayer) {
		return TagResolver.resolver(
				Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_NAME, Objects.requireNonNull(offlinePlayer.getName())),
				Utils.unparsedPlaceholder(PLACEHOLDER_PLAYER_ID, offlinePlayer.getUniqueId())
		);
	}

	@NotNull
	private TagResolver tagResolverPlayerProfile(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		return TagResolver.resolver(
				tagResolverPlayer(offlinePlayer),
				Utils.unparsedPlaceholder(PLACEHOLDER_PROFILE_ID, profileID)
		);
	}

	@Nullable
	public Component messageClearResend(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		return this.messageClearResend == null ? null :
				Utils.toRichComponent(this.messageClearResend, tagResolverPlayerProfile(offlinePlayer, profileID));
	}

	@Nullable
	public Component messageClearFinish(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		return this.messageClearFinish == null ? null :
				Utils.toRichComponent(this.messageClearFinish, tagResolverPlayerProfile(offlinePlayer, profileID));
	}

	@Nullable
	public Component messageExtrasSet(@NotNull OfflinePlayer offlinePlayer, @Nullable UUID profileID,
									  @NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
		return profileID == null ? (
				this.messageExtrasSetPlayer == null ? null :
						Utils.toRichComponent(this.messageExtrasSetPlayer,
								tagResolverPlayer(offlinePlayer),
								Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS, extrasPlayer),
								Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_MAX, amountExtraPlayerMax())
						)
		) : (
				this.messageExtrasSetProfile == null ? null :
						Utils.toRichComponent(this.messageExtrasSetProfile,
								tagResolverPlayerProfile(offlinePlayer, profileID),
								Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS, extrasProfile),
								Utils.unparsedPlaceholder(PLACEHOLDER_EXTRAS_MAX, amountExtraProfileMax()),
								Utils.unparsedPlaceholder(PLACEHOLDER_TOTAL, extrasPlayer + extrasProfile),
								Utils.unparsedPlaceholder(PLACEHOLDER_TOTAL_MAX, amountExtrasMax())
						)
		);
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
				lore = section.getStringList("lore").stream().
						map(str -> TextUtils.isNullOrBlank(str) ? null : str).toList();
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
					model = new NamespacedKey(arr.length < 2 ? NamespacedKey.MINECRAFT : arr[0],
							arr[arr.length < 2 ? 0 : 1]);
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