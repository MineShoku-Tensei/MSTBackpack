package me.DMan16;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;

public final class Config {
	private static final @NotNull String PLACEHOLDER_PLAYER_ID = "%player_id%";
	private static final @NotNull String PLACEHOLDER_PLAYER_NAME = "%player_name%";
	private static final @NotNull String PLACEHOLDER_PROFILE_ID = "%profile_id%";
	private static final @NotNull String PLACEHOLDER_EXTRAS = "%extras%";
	private static final @NotNull String PLACEHOLDER_EXTRAS_MAX = "%extras_max%";
	private static final @NotNull String PLACEHOLDER_TOTAL = "%total%";
	private static final @NotNull String PLACEHOLDER_TOTAL_MAX = "%total_max%";

	private final @NotNull Main plugin;
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

	public Config(@NotNull Main plugin) {
		this.plugin = plugin;
		this.plugin.saveDefaultConfig();
		this.defaults = Objects.requireNonNull(config().getDefaults());
		reloadConfig();
		this.host = getStringOrNull("mysql.host");
		this.port = getIntNonNegative("mysql.port");
		this.database = getStringOrNull("mysql.database");
		this.username = getStringOrNull("mysql.username");
		this.password = getStringOrNull("mysql.password");
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
	private String getStringOrNull(@NotNull String path) {
		return config().getString(path, null);
	}

	@NonNegative
	private int getIntNonNegative(@NotNull String path) {
		return Math.max(config().getInt(path), 0);
	}

	private void recalculate() {
		this.type = Type.get(getStringOrNull("backpack.type"));
		this.condense = config().getBoolean("backpack.condense", false);
		this.amountBase = getIntNonNegative("backpack.base");
		this.amountExtraPlayerMax = getIntNonNegative("backpack.extra.player.max");
		this.amountExtraPlayerPer = getIntNonNegative("backpack.extra.player.per");
		this.amountExtraProfileMax = getIntNonNegative("backpack.extra.profile.max");
		this.amountExtraProfilePer = getIntNonNegative("backpack.extra.profile.per");
		int clearTimeout = getIntNonNegative("backpack.clear_resend_seconds");
		this.clearTimeout = clearTimeout == 0 ? 0 : TimeUnit.SECONDS.toMillis(clearTimeout);
		this.menuTitle = Utils.toComponentMiniMessage(getStringOrNull("backpack.menu.title"));
		this.menuBorder = config().getBoolean("backpack.menu.border", true);
		this.menuShowUnlockable = config().getBoolean("backpack.menu.show_unlockable", true);
		this.menuIndicator = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.indicator"), null);
		this.menuClose = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.close"), this.defaults);
		this.menuNext = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.next"), this.defaults);
		this.menuPrevious = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.previous"), this.defaults);
		this.menuBorderStatic = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.border.regular"), this.defaults);
		this.menuBorderUnlockable = PageItem.fromConfig(config().getConfigurationSection("backpack.menu.item.border.unlockable"), this.defaults);
		this.messageCommandFailed = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.command_failed"));
		this.messageNotFoundPlayer = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.not_found.player"));
		this.messageNotFoundProfile = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.not_found.profile"));
		this.messageClearResend = getStringOrNull("backpack.message.clear.resend");
		this.messageClearFinish = getStringOrNull("backpack.message.clear.finish");
		this.messageExtrasSetPlayer = getStringOrNull("backpack.message.extras_set.player");
		this.messageExtrasSetProfile = getStringOrNull("backpack.message.extras_set.profile");
		this.messageProfileNotSelected = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.profile_not_selected"));
		this.messageOpenFail = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.open_fail"));
		this.messageReloaded = Utils.toComponentMiniMessage(getStringOrNull("backpack.message.reloaded"));
		this.commandUsage = getStringOrNull("backpack.command.usage");
		this.commandDescription = getStringOrNull("backpack.command.description");
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
	private int amountExtrasMax() {
		return calculateExtras(amountExtraPlayerMax(), amountExtraProfileMax());
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

	@Nullable
	private String placeholdersPlayer(@Nullable String str, @NotNull OfflinePlayer offlinePlayer) {
		str = Utils.replacePlaceholders(str, PLACEHOLDER_PLAYER_ID, offlinePlayer.getUniqueId());
		str = Utils.replacePlaceholders(str, PLACEHOLDER_PLAYER_NAME, Objects.requireNonNull(offlinePlayer.getName()));
		return str;
	}

	@Nullable
	private String placeholdersPlayerProfile(@Nullable String str, @NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		str = placeholdersPlayer(str, offlinePlayer);
		str = Utils.replacePlaceholders(str, PLACEHOLDER_PROFILE_ID, profileID);
		return str;
	}

	@Nullable
	public Component messageClearResend(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		return Utils.toComponentMiniMessage(placeholdersPlayerProfile(this.messageClearResend, offlinePlayer, profileID));
	}

	@Nullable
	public Component messageClearFinish(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID) {
		return Utils.toComponentMiniMessage(placeholdersPlayerProfile(this.messageClearFinish, offlinePlayer, profileID));
	}

	@Nullable
	public Component messageExtrasSetPlayer(@NotNull OfflinePlayer offlinePlayer, @NonNegative int extrasPlayer) {
		String msg = placeholdersPlayer(this.messageExtrasSetPlayer, offlinePlayer);
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_EXTRAS, extrasPlayer);
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_EXTRAS_MAX, amountExtraPlayerMax());
		return Utils.toComponentMiniMessage(msg);
	}

	@Nullable
	public Component messageExtrasSetProfile(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID profileID, @NonNegative int extrasPlayer, @NonNegative int extrasProfile) {
		String msg = placeholdersPlayerProfile(this.messageExtrasSetProfile, offlinePlayer, profileID);
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_EXTRAS, extrasProfile);
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_EXTRAS_MAX, amountExtraProfileMax());
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_TOTAL, calculateExtras(extrasPlayer, extrasProfile));
		msg = Utils.replacePlaceholders(msg, PLACEHOLDER_TOTAL_MAX, amountExtrasMax());
		return Utils.toComponentMiniMessage(msg);
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

	public enum Type {
		PAGES,
		LINES,
		SLOTS;

		public static Type get(@Nullable String type) {
			if (type == null) return PAGES;
			try {
				return Type.valueOf(Utils.toUpperCase(type));
			} catch (IllegalArgumentException e) {
				return PAGES;
			}
		}
	}

	public record PageItem(@NotNull Material material, @Nullable String name, @Nullable @Unmodifiable List<String> lore, @Nullable NamespacedKey model, @Nullable @Positive Integer customModel, int slot) {
		private static final @NotNull String PLACEHOLDER_PAGE = "%page(?:([+-])(\\d+))?%";
		private static final @NotNull String PLACEHOLDER_PAGES_TOTAL = "%pages%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER = "%extras_player%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_PLAYER_MAX = "%extras_player_max%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE = "%extras_profile%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_PROFILE_MAX = "%extras_profile_max%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL = "%extras_total%";
		private static final @NotNull String PLACEHOLDER_EXTRAS_TOTAL_MAX = "%extras_total_max%";

		@NotNull
		@Contract("_, _, _, _, _, _ -> new")
		public ItemStack toItemStack(@Positive int currentPage, @NonNegative int totalPages, @NonNegative int extrasPlayer, @NonNegative int extrasProfile, @NonNegative int extrasPlayerMax, @NonNegative int extrasProfileMax) {
			ItemStack item = ItemStack.of(this.material);
			item.editMeta(meta -> {
				meta.itemName(placeholdersPages(this.name, currentPage, totalPages, extrasPlayer, extrasProfile, extrasPlayerMax, extrasProfileMax));
				meta.lore(this.lore == null ? null : this.lore.stream().map(str -> str == null ? Component.empty() : placeholdersPages(str, currentPage, totalPages, extrasPlayer, extrasProfile, extrasPlayerMax, extrasProfileMax)).toList());
				meta.setItemModel(this.model);
				meta.setCustomModelData(this.customModel);
			});
			return item;
		}

		@Nullable
		@Contract("_, !null -> new")
		private static PageItem fromConfig(ConfigurationSection section, @Nullable Configuration defaults) {
			Objects.requireNonNull(section);
			String type = section.getString("type", null);
			Material material;
			if (type == null || (material = Material.getMaterial(Utils.toUpperCase(type.replace(" ", "_")))) == null) {
				if (defaults == null) return null;
				material = Objects.requireNonNull(Material.getMaterial(Utils.toUpperCase(Objects.requireNonNull(defaults.getString("type")))));
			}
			int slot;
			if (section.contains("slot", true)) {
				slot = section.getInt("slot");
			} else if (defaults != null) {
				slot = defaults.getInt("slot");
			} else return null;
			String name = section.getString("name", "");
			if (name.isBlank()) {
				name = null;
			}
			List<String> lore = null;
			if (section.contains("lore", true)) {
				if (section.isList("lore")) {
					lore = section.getStringList("lore").stream().map(str -> str == null || str.isBlank() ? null : str).toList();
				} else {
					String l = section.getString("lore", null);
					lore = l == null || l.isBlank() ? null : List.of(l);
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

		@NotNull
		private static Component placeholdersPages(@Nullable String str, @Positive int currentPage, @NonNegative int totalPages, @NonNegative int extrasPlayer, @NonNegative int extrasProfile, @NonNegative int extrasPlayerMax, @NonNegative int extrasProfileMax) {
			if (str == null) return Component.empty();
			int pages = Math.max(totalPages, 1);
			Function<Matcher, Integer> replace = (Matcher matcher) -> {
				String sign = matcher.group(1);
				String number = matcher.group(2);
				int replacement = currentPage;
				if (sign != null && number != null) {
					int delta = Integer.parseInt(number);
					replacement += sign.equals("+") ? delta : -delta;
				}
				return Math.clamp(replacement, 1, pages);
			};
			str = Utils.replacePlaceholdersComplex(str, PLACEHOLDER_PAGE, replace);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_PAGES_TOTAL, totalPages);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_PLAYER, extrasPlayer);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_PLAYER_MAX, extrasPlayerMax);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_PROFILE, extrasProfile);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_PROFILE_MAX, extrasProfileMax);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_TOTAL, extrasPlayer + extrasProfile);
			str = Utils.replacePlaceholders(str, PLACEHOLDER_EXTRAS_TOTAL_MAX, extrasPlayerMax + extrasProfileMax);
			return Utils.toComponentMiniMessage(str);
		}
	}
}