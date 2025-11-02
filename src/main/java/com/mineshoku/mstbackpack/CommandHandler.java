package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.Pair;
import com.mineshoku.mstutils.ProfileUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class CommandHandler implements CommandExecutor, TabCompleter {
	public static final @NotNull @Unmodifiable List<@NotNull String> BASE = List.of("help", "reload", "open", "clear", "upgrade", "downgrade");
	public static final @NotNull String PERMISSION_ADVANCED = "mst.backpack.advanced";
	public static final @NotNull String EXTRAS_SET_PLAYER = "-";
	public static final @NotNull String EXTRAS_CURRENT = ".";
	private static final @Positive int INDEX_OPEN = BASE.indexOf("open");
	private static final @Positive int INDEX_CLEAR = BASE.indexOf("clear");
	private static final @Positive int INDEX_UPGRADE = BASE.indexOf("upgrade");
	private static final @Positive int INDEX_DOWNGRADE = BASE.indexOf("downgrade");
	private static final @NotNull Component HELP = Utils.toRichComponent("""
			<gold>reload - reload config file
			open <player> <profile> - open player's inventory (use '.' for current player/profile)
			upgrade <player> <profile> <n> - +n extras (use '.' for current player/profile; use '-' for profile to use on player)
			downgrade <player> <profile> <n> - -n extras (use '.' for current player/profile; use '-' for profile to use on player)
			""");

	private final @NotNull PluginCommand command;
	private final @NotNull Main plugin;
	private final @NotNull Map<@NotNull UUID, @NotNull ClearInfo> timesPlayers = new HashMap<>();
	private final @NotNull Map<@NotNull String, @NotNull ClearInfo> timesOthers = new HashMap<>();

	public CommandHandler(@NotNull Main plugin) {
		this.command = Objects.requireNonNull(plugin.getCommand("backpack"));
		this.command.setExecutor(this);
		this.command.setTabCompleter(this);
		this.plugin = plugin;
	}

	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
		boolean advanced = sender.hasPermission(PERMISSION_ADVANCED);
		UUID playerID, profileID;
		Player player;
		if (args.length > 0 && advanced) {
			int idx = BASE.indexOf(TextUtils.toLowerCase(args[0]));
			if (idx == 1) {
				try {
					this.plugin.reload();
					Utils.sendMessage(sender, this.plugin.config().messageReloaded());
				} catch (Exception e) {
					Utils.logException(this.plugin, null, e);
					Utils.sendMessage(sender, this.plugin.config().messageCommandFailed());
				}
				return true;
			}
			if (idx <= 0 || args.length < 3) {
				Utils.sendMessage(sender, HELP);
				return true;
			}
			boolean useCurrentPlayer = args[1].equals(EXTRAS_CURRENT);
			if ((idx == INDEX_OPEN || useCurrentPlayer) && !(sender instanceof Player)) return true;
			OfflinePlayer offlinePlayer = useCurrentPlayer ? (Player) sender : Utils.offlinePlayerCached(args[1]);
			if (offlinePlayer == null) {
				Utils.sendMessage(sender, this.plugin.config().messageNotFoundPlayer());
				return true;
			}
			playerID = offlinePlayer.getUniqueId();
			if ((idx == INDEX_UPGRADE || idx == INDEX_DOWNGRADE) && args[2].equals(EXTRAS_SET_PLAYER)) {
				profileID = null;
			} else {
				if (args[2].equals(EXTRAS_CURRENT)) {
					profileID = ProfileUtils.getCurrentProfileID(playerID);
				} else {
					UUID id = Utils.getUUID(args[2]);
					profileID = id == null || !ProfileUtils.hasPlayerProfile(playerID, id) ? null : id;
				}
				if (profileID == null) {
					noProfile(sender);
					return true;
				}
			}
			if (idx != INDEX_OPEN) {
				if (idx == INDEX_CLEAR) {
					if (profileID == null) {
						noProfile(sender);
						return true;
					}
					long now = System.currentTimeMillis();
					long clearTimeout = this.plugin.config().clearTimeout();
					if (clearTimeout != 0) {
						ClearInfo info = (sender instanceof Player p) ? this.timesPlayers.remove(p.getUniqueId()) : this.timesOthers.remove(sender.getName());
						if (info == null || (now - info.time() > this.plugin.config().clearTimeout())) {
							info = new ClearInfo(playerID, profileID, now);
							if (sender instanceof Player p) {
								this.timesPlayers.put(p.getUniqueId(), info);
							} else {
								this.timesOthers.put(sender.getName(), info);
							}
							Utils.sendMessage(sender, this.plugin.config().messageClearResend(offlinePlayer, profileID));
						} else {
							Utils.sendToSync(CompletableFuture.supplyAsync(() -> {
								try {
									this.plugin.database().saveItems(new Info(playerID, profileID, null, 0, 0));
									return true;
								} catch (Exception e) {
									Utils.logException(this.plugin, null, e);
									return false;
								}
							}) , this.plugin, success -> {
								Component msg = success ? this.plugin.config().messageClearFinish(offlinePlayer, profileID) :
										this.plugin.config().messageCommandFailed();
								Utils.sendMessage(sender, msg);
							});
						}
					}
				} else {
					int amount = 1;
					if (args.length > 3) {
						try {
							amount = Integer.parseInt(args[3]);
						} catch (Exception e) {
							Utils.sendMessage(sender, HELP);
							return true;
						}
					}
					amount = Math.abs(amount);
					int delta = idx == INDEX_DOWNGRADE ? Math.negateExact(amount) : amount,
							playerMax = this.plugin.config().amountExtraPlayerMax(),
							profileMax = this.plugin.config().amountExtraProfileMax();
					Utils.sendToSync(CompletableFuture.supplyAsync(() -> {
						try {
							if (profileID == null) {
								this.plugin.database().updateExtrasPlayer(playerID, delta, playerMax);
							} else {
								this.plugin.database().updateExtrasProfile(playerID, profileID, delta, profileMax);
							}
							return true;
						} catch (Exception e) {
							Utils.logException(this.plugin, null, e);
							return false;
						}
					}).thenApplyAsync(success -> {
						Pair<Integer, Integer> extras = null;
						if (success) {
							try {
								extras = this.plugin.database().getExtras(playerID, profileID);
							} catch (Exception e) {
								Utils.logException(this.plugin, null, e);
							}
						}
						return extras;
					}) , this.plugin, extras -> {
						if (extras == null) {
							Utils.sendMessage(sender, this.plugin.config().messageCommandFailed());
						} else if (profileID == null) {
							Utils.sendMessage(sender, this.plugin.config().messageExtrasSetPlayer(offlinePlayer, extras.first()));
						} else {
							Utils.sendMessage(sender, this.plugin.config().messageExtrasSetProfile(offlinePlayer, profileID, extras.first(), extras.second()));
						}
					});
				}
				return true;
			}
			player = (Player) sender;
		} else if (!(sender instanceof Player onlinePlayer)) return true;
		else {
			player = onlinePlayer;
			playerID = player.getUniqueId();
			profileID = ProfileUtils.getCurrentProfileID(playerID);
			if (profileID == null) {
				Utils.sendMessage(player, this.plugin.config().messageProfileNotSelected());
				return true;
			}
		}
		if (profileID == null) {
			noProfile(sender);
			return true;
		}
		Utils.sendToSync(CompletableFuture.supplyAsync(() -> {
			try {
				return this.plugin.database().getInfo(playerID, profileID);
			} catch (Exception e) {
				Utils.logException(this.plugin, null, e);
				return null;
			}
		}), this.plugin, info -> {
			boolean failed = info == null;
			if (!failed) {
				try {
					failed = new Menu(this.plugin, player, info).openInventory() == null;
				} catch (Exception e) {
					Utils.logException(this.plugin, null, e);
				}
			}
			if (failed) {
				Utils.sendMessage(player, this.plugin.config().messageOpenFail());
			}
		});
		return true;
	}

	@NotNull
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
		if (!sender.hasPermission(PERMISSION_ADVANCED)) return new ArrayList<>();
		if (args.length == 0) return BASE;
		if (args.length == 1) {
			return BASE.stream().filter(cmd -> Utils.checkTabComplete(args[0], cmd)).toList();
		}
		int idx = BASE.indexOf(TextUtils.toLowerCase(args[0]));
		if (idx < 2) return new ArrayList<>();
		if (args.length == 2) {
			Stream<String> stream = Bukkit.getOnlinePlayers().stream().map(Player::getName);
			if (sender instanceof Player) {
				stream = Stream.concat(Stream.of(EXTRAS_CURRENT), stream);
			}
			return stream.filter(cmd -> Utils.checkTabComplete(args[1], cmd)).toList();
		}
		if (args.length == 3) {
			OfflinePlayer offlinePlayer = args[1].equals(EXTRAS_CURRENT) ? ((sender instanceof Player p) ? p : null) : Utils.offlinePlayerCached(args[1]);
			if (offlinePlayer == null) return new ArrayList<>();
			UUID playerID = offlinePlayer.getUniqueId();
			List<UUID> profileIDs = ProfileUtils.getProfileIDs(playerID);
			Stream<String> special = Stream.empty(), profiles = profileIDs == null ? Stream.empty() : profileIDs.stream().map(UUID::toString);
			if (idx == INDEX_UPGRADE || idx == INDEX_DOWNGRADE) {
				special = offlinePlayer.isOnline() ? Stream.of(EXTRAS_CURRENT, EXTRAS_SET_PLAYER) : Stream.of(EXTRAS_SET_PLAYER);
			} else if ((idx == INDEX_OPEN || idx == INDEX_CLEAR) && offlinePlayer.isOnline()) {
				special = Stream.of(EXTRAS_CURRENT);
			}
			return Stream.concat(special, profiles).filter(cmd -> Utils.checkTabComplete(args[2], cmd)).toList();
		}
		return new ArrayList<>();
	}

	private void noProfile(@NotNull CommandSender sender) {
		Utils.sendMessage(sender, this.plugin.config().messageNotFoundProfile());
	}

	public void reload() {
		this.command.setUsage(this.plugin.config().commandUsage());
		this.command.setDescription(this.plugin.config().commandDescription());
	}

	private record ClearInfo(@NotNull UUID playerID, @NotNull UUID profileID, @Positive long time) {}
}