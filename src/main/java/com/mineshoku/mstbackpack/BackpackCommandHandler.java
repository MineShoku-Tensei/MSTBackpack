package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.MathUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.managers.MMOProfilesManager;
import com.mineshoku.mstutils.managers.PlayersCache;
import com.mineshoku.mstutils.models.Pair;
import com.mineshoku.mstutils.models.PlayerInfo;
import com.mineshoku.mstutils.models.StopCompletableFutureException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public final class BackpackCommandHandler implements CommandExecutor, TabCompleter {
	public static final @NotNull @Unmodifiable List<@NotNull String> BASE = List.of("help", "reload", "open", "clear", "upgrade", "downgrade", "info");
	public static final @NotNull String PERMISSION_ADVANCED = "mst.backpack.advanced";
	public static final @NotNull String EXTRAS_SET_PLAYER = "-";
	public static final @NotNull String CURRENT = ".";
	private static final @Positive int INDEX_OPEN = BASE.indexOf("open");
	private static final @Positive int INDEX_CLEAR = BASE.indexOf("clear");
	private static final @Positive int INDEX_UPGRADE = BASE.indexOf("upgrade");
	private static final @Positive int INDEX_DOWNGRADE = BASE.indexOf("downgrade");
	private static final @Positive int INDEX_INFO = BASE.indexOf("info");
	private static final @NotNull String HELP_NO_PROFILE = "no profile defaults to current profile";
	private static final @NotNull String HELP_SET_PLAYER = "no profile defaults to player";
	private static final @NotNull String HELP1 = "Backpack command help";
	private static final @NotNull String HELP2 = "Use '.' for current player/profile";
	private static final @NotNull String HELP3 = "";
	private static final @NotNull String HELP_RELOAD = "reload - reload config file";
	private static final @NotNull String HELP_OPEN = "open <player> <optional:profile> - open player's backpack (" + HELP_NO_PROFILE + ")";
	private static final @NotNull String HELP_CLEAR = "clear <player> <optional:profile> - clear player's backpack(" + HELP_NO_PROFILE + ")";
	private static final @NotNull String HELP_UPGRADE = "upgrade <player> <optional:profile> <n> - +n extras (" + HELP_SET_PLAYER + ")";
	private static final @NotNull String HELP_DOWNGRADE = "downgrade <player> <optional:profile> <n> - -n extras (" + HELP_SET_PLAYER + ")";
	private static final @NotNull String HELP_INFO = "info <player> <optional:profile> <n> - player info (" + HELP_SET_PLAYER + ")";
	private static final @NotNull Component HELP;

	static {
		List<TextComponent> helps = Stream.of(HELP1, HELP2, HELP3, HELP_RELOAD, HELP_OPEN, HELP_CLEAR, HELP_UPGRADE, HELP_DOWNGRADE, HELP_INFO).
				map(Component::text).toList();
		HELP = Component.join(JoinConfiguration.builder().separator(Component.newline()).build(), helps).color(NamedTextColor.GOLD);
	}

	private final @NotNull PluginCommand command;
	private final @NotNull MSTBackpack plugin;
	private final @NotNull Map<@NotNull UUID, @NotNull ClearInfo> timesPlayers = new HashMap<>();
	private final @NotNull Map<@NotNull String, @NotNull ClearInfo> timesOthers = new HashMap<>();

	public BackpackCommandHandler(@NotNull MSTBackpack plugin) {
		if (plugin.backpackCommandHandler() != null) throw new IllegalStateException("Command handler already initialized");
		this.plugin = plugin;
		this.command = Objects.requireNonNull(plugin.getCommand("backpack"));
		this.command.setExecutor(this);
		this.command.setTabCompleter(this);
	}

	private void openBackpack(@NotNull Player player, @NotNull UUID playerID, @NotNull UUID profileID, boolean requireInDB) {
		this.plugin.backpackDatabase().getInfo(playerID, profileID).thenAcceptAsync(info -> {
			if (requireInDB && !info.inDB()) return;
			try {
				if (new BackpackMenu(this.plugin, player, info).openInventory() == null) {
					Utils.sendMessage(player, this.plugin.backpackConfig().messageOpenFail());
				}
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.mainThreadExecutor(this.plugin)).exceptionally(e -> {
			LoggingManager.instance().log(this.plugin, e);
			Utils.sendMessage(player, this.plugin.backpackConfig().messageOpenFail());
			return null;
		});
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
		boolean advanced = sender.hasPermission(PERMISSION_ADVANCED);
		Optional<Player> senderPlayer = Optional.ofNullable((sender instanceof Player p) ? p : null);
		if (args.length > 0 && advanced) {
			int idx = BASE.indexOf(TextUtils.toLowerCase(args[0]));
			if (idx == 1) {
				try {
					this.plugin.reload();
					Utils.sendMessage(sender, this.plugin.backpackConfig().messageReloaded());
				} catch (Exception e) {
					LoggingManager.instance().log(this.plugin, e);
					Utils.sendMessage(sender, this.plugin.backpackConfig().messageCommandFailed());
				}
				return true;
			}
			if (idx <= 0) {
				Utils.sendMessage(sender, HELP);
				return true;
			}
			String playerSelector, profileSelector;
			if (args.length < 3) {
				if (args.length == 1) {
					Utils.sendMessage(sender, HELP);
					return true;
				}
				profileSelector = idx == INDEX_OPEN || idx == INDEX_CLEAR ? CURRENT : EXTRAS_SET_PLAYER;
			} else {
				profileSelector = args[2];
			}
			playerSelector = args[1];
			Integer extrasAmount = (idx == INDEX_UPGRADE || idx == INDEX_DOWNGRADE) && args.length > 3 ? MathUtils.getInteger(args[3]) : Integer.valueOf(1);
			if (extrasAmount == null) {
				Utils.sendMessage(sender, HELP);
				return true;
			}
			boolean currentPlayer = CURRENT.equals(playerSelector), currentProfile = CURRENT.equals(profileSelector),
					extrasSetPlayer = EXTRAS_SET_PLAYER.equals(profileSelector);
			if ((idx == INDEX_OPEN || currentPlayer) && senderPlayer.isEmpty()) return true;
			UUID pID = currentPlayer ? ((Player) sender).getUniqueId() : Utils.getUUID(playerSelector);
			(pID == null ? PlayersCache.instance().byName(playerSelector) : PlayersCache.instance().byID(pID)).thenApplyAsync(info -> {
				if (info == null) {
					Utils.sendMessage(sender, this.plugin.backpackConfig().messageNotFoundPlayer());
					throw new StopCompletableFutureException();
				}
				return info;
			}).thenApplyAsync(info -> {
				assert info != null;
				Player player = Bukkit.getPlayer(info.id());
				UUID profileID;
				if (idx != INDEX_OPEN && idx != INDEX_CLEAR && extrasSetPlayer) {
					profileID = null;
				} else if (currentProfile) {
					profileID = MMOProfilesManager.instance().getCurrentProfileID(info.id());
					if (profileID == null) {
						Utils.sendMessage(sender, player == null ? this.plugin.backpackConfig().messageNotFoundProfile() :
								this.plugin.backpackConfig().messageProfileNotSelected());
						throw new StopCompletableFutureException();
					}
				} else {
					profileID = Utils.getUUID(profileSelector);
					if (profileID == null) {
						Utils.sendMessage(sender, this.plugin.backpackConfig().messageNotFoundProfile());
						throw new StopCompletableFutureException();
					}
				}
				if (idx == INDEX_OPEN) {
					boolean isCurrentPlayer = currentPlayer || senderPlayer.get().getUniqueId().equals(info.id()),
							isCurrentProfile = currentProfile ||
									profileID.equals(MMOProfilesManager.instance().getCurrentProfileID(senderPlayer.get().getUniqueId()));
					openBackpack(senderPlayer.get(), info.id(), profileID, !isCurrentPlayer || !isCurrentProfile);
					throw new StopCompletableFutureException();
				}
				if (idx == INDEX_CLEAR) {
					long now = System.currentTimeMillis();
					Optional<UUID> senderID = senderPlayer.map(Player::getUniqueId);
					String senderName = sender.getName();
					ClearInfo clearInfo = senderID.isEmpty() ? this.timesOthers.remove(senderName) : this.timesPlayers.remove(senderID.get());
					if (this.plugin.backpackConfig().clearTimeout() == 0 || clearInfo == null ||
							(now - clearInfo.time() > this.plugin.backpackConfig().clearTimeout())) {
						clearInfo = new ClearInfo(info.id(), profileID, now);
						if (senderID.isEmpty()) {
							this.timesOthers.put(senderName, clearInfo);
						} else {
							this.timesPlayers.put(senderID.get(), clearInfo);
						}
						Utils.sendMessage(sender, this.plugin.backpackConfig().messageClearResend(info.id(), info.name(), profileID));
					} else {
						this.plugin.backpackDatabase().saveItems(info.id(), profileID).whenComplete((ignored, e) -> {
							Component msg;
							if (e == null) {
								msg = this.plugin.backpackConfig().messageClearFinish(info.id(), info.name(), profileID);
							} else {
								LoggingManager.instance().log(this.plugin, e);
								msg = this.plugin.backpackConfig().messageCommandFailed();
							}
							Utils.sendMessage(sender, msg);
						});
					}
					throw new StopCompletableFutureException();
				}
				if (idx == INDEX_INFO) {
					LoggingManager.exceptionallyLog(this.plugin, this.plugin.backpackDatabase().getExtras(info.id(), profileID));
					throw new StopCompletableFutureException();
				}
				return Pair.of(info, profileID);
			}, ExecutorManager.mainThreadExecutor(this.plugin)).thenCompose(pair -> {
				PlayerInfo info = pair.first();
				UUID profileID = pair.second();
				assert info != null;
				if (idx == INDEX_INFO) return this.plugin.backpackDatabase().getExtras(info.id(), profileID).
						thenAccept(extras -> Utils.sendMessage(sender, plugin.backpackConfig().messageExtrasInfo(info.id(), info.name(), profileID,
								extras.first(), extras.second())));
				if (idx != INDEX_UPGRADE && idx != INDEX_DOWNGRADE) throw new RuntimeException("Missed backpack condition");
				int delta = idx == INDEX_DOWNGRADE ? Math.negateExact(Math.abs(extrasAmount)) : Math.abs(extrasAmount);
				return this.plugin.backpackDatabase().updateExtras(info.id(), profileID, delta).
						thenCompose(v -> this.plugin.backpackDatabase().getExtras(info.id(), profileID)).
						thenAccept(extras -> {
							Utils.sendMessage(sender, this.plugin.backpackConfig().messageExtrasSet(info.id(), info.name(), profileID));
							Utils.sendMessage(sender, this.plugin.backpackConfig().messageExtrasInfo(info.id(), info.name(), profileID,
									extras.first(), extras.second()));
						});
			}).exceptionally(e -> {
				LoggingManager.instance().log(this.plugin, e);
				if (!(LoggingManager.initialCause(e) instanceof StopCompletableFutureException)) {
					Utils.sendMessage(sender, plugin.backpackConfig().messageCommandFailed());
				}
				return null;
			});
			return true;
		}
		if (senderPlayer.isEmpty()) return true;
		UUID playerID = senderPlayer.get().getUniqueId(), profileID = MMOProfilesManager.instance().getCurrentProfileID(playerID);
		if (profileID == null) {
			Utils.sendMessage(sender, this.plugin.backpackConfig().messageProfileNotSelected());
		} else {
			openBackpack(senderPlayer.get(), playerID, profileID, false);
		}
		return true;
	}

	@Override
	@NotNull
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
		if (!sender.hasPermission(PERMISSION_ADVANCED)) return new ArrayList<>();
		if (args.length == 0) return BASE;
		if (args.length == 1) return BASE.stream().filter(cmd -> Utils.checkTabComplete(args[0], cmd)).toList();
		int idx = BASE.indexOf(TextUtils.toLowerCase(args[0]));
		if (idx < 2) return new ArrayList<>();
		if (args.length == 2) {
			Stream<String> stream = Bukkit.getOnlinePlayers().stream().map(Player::getName);
			stream = Stream.concat(stream, PlayersCache.instance().getCached().values().stream()).distinct();
			if (sender instanceof Player) {
				stream = Stream.concat(Stream.of(CURRENT), stream);
			}
			return stream.filter(cmd -> Utils.checkTabComplete(args[1], cmd)).toList();
		}
		if (args.length == 3) {
			UUID playerID;
			if (args[1].equals(CURRENT)) {
				playerID = (sender instanceof Player p) ? p.getUniqueId() : null;
			} else {
				UUID id = Utils.getUUID(args[1]);
				playerID = id == null ? PlayersCache.instance().byNameCached(args[1]) : (PlayersCache.instance().byIDCached(id) == null ? null : id);
			}
			if (playerID == null) return new ArrayList<>();
			SequencedSet<UUID> profileIDs = this.plugin.cacheListener().profiles(playerID);
			Stream<String> special = Stream.empty(), profiles = profileIDs == null ? Stream.empty() : profileIDs.stream().map(UUID::toString);
			boolean isOnline = Bukkit.getPlayer(playerID) != null;
			if (idx == INDEX_UPGRADE || idx == INDEX_DOWNGRADE) {
				special = isOnline ? Stream.of(CURRENT, EXTRAS_SET_PLAYER) : Stream.of(EXTRAS_SET_PLAYER);
			} else if ((idx == INDEX_OPEN || idx == INDEX_CLEAR) && isOnline) {
				special = Stream.of(CURRENT);
			}
			return Stream.concat(special, profiles).filter(cmd -> Utils.checkTabComplete(args[2], cmd)).toList();
		}
		return new ArrayList<>();
	}

	public void reload() {
		this.command.setUsage(this.plugin.backpackConfig().commandUsage());
		this.command.setDescription(this.plugin.backpackConfig().commandDescription());
	}

	private record ClearInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Positive long time) {}
}