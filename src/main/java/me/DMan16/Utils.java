package me.DMan16;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
	public static final int LINE_SLOTS = 9;
	public static final int INVENTORY_MAX_LINES = 6;
	public static final int INVENTORY_MAX_SIZE = slotsFromLines(INVENTORY_MAX_LINES);

	public static void logException(@NotNull Throwable throwable) {
		throwable.printStackTrace();
	}

	@Positive
	public static int slotsFromLines(@Positive @Range(from = 1, to = INVENTORY_MAX_LINES) int lines) {
		return lines * LINE_SLOTS;
	}

	@NotNull
	public static String toLowerCase(@NotNull String str) {
		return str.toLowerCase(Locale.ENGLISH);
	}

	@NotNull
	public static String toUpperCase(@NotNull String str) {
		return str.toUpperCase(Locale.ENGLISH);
	}

	@NotNull
	@Contract(" -> new")
	public static String randomString() {
		return System.currentTimeMillis() + "+" + ThreadLocalRandom.current().nextInt(1000000) + "=" + UUID.randomUUID();
	}

	@NotNull
	@Contract("_, _, _ -> new")
	public static Inventory inventory(@NotNull InventoryHolder owner, @Positive int inventorySize, @Nullable Component title) {
		return title == null ? Bukkit.createInventory(owner, inventorySize) : Bukkit.createInventory(owner, inventorySize, title);
	}

	@Nullable
	public static UUID getUUID(@NotNull String str) {
		try {
			return UUID.fromString(str);
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	public static OfflinePlayer offlinePlayerCached(@NotNull String str) {
		OfflinePlayer p;
		UUID ID = getUUID(str);
		if (ID == null) {
			p = Bukkit.getOfflinePlayerIfCached(str);
		} else {
			p = Bukkit.getOfflinePlayer(ID);
			if (p.getName() == null || p.getName().isEmpty()) {
				p = null;
			}
		}
		return p;
	}

	public static void sendMessage(@NotNull CommandSender sender, @Nullable Component msg) {
		if (msg != null) {
			sender.sendMessage(msg);
		}
	}

	public static boolean containsTabComplete(@NotNull String arg1, @NotNull String arg2) {
		return arg1.isEmpty() || toUpperCase(arg2).contains(toUpperCase(arg1));
	}

	@NotNull
	@Contract("_ -> new")
	public static List<@NotNull ItemStack> condenseItems(@NotNull List<@Nullable ItemStack> items) {
		Inventory inv = Bukkit.createInventory(null, INVENTORY_MAX_SIZE);
		Collection<ItemStack> currentItems = items.stream().filter(Objects::nonNull).toList();
		List<ItemStack> finalItems = new ArrayList<>();
		while (!currentItems.isEmpty()) {
			inv.clear();
			currentItems = inv.addItem(currentItems.toArray(new ItemStack[]{})).values();
			for (ItemStack item : inv.getContents()) {
				if (item != null && !item.isEmpty()) {
					finalItems.add(item);
				}
			}
		}
		return finalItems;
	}

	public static <V> void sendToSync(@NotNull CompletableFuture<@Nullable V> cf, @NotNull JavaPlugin plugin, @NotNull Consumer<V> run) {
		cf.thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> run.accept(result)));
	}

	public static int hashCode(@NotNull Object @NotNull ... objects) {
		return Arrays.hashCode(objects);
	}

	@Nullable
	public static Component toComponentMiniMessage(@Nullable String str) {
		return MiniMessage.miniMessage().deserializeOrNull(str);
	}


	@Nullable
	@Contract("null, _, _ -> null; !null, _, _ -> !null")
	public static String replacePlaceholdersComplex(@Nullable String str, @NotNull String regex, @NotNull Function<@NotNull Matcher, ? extends @NotNull Object> replace) {
		if (str == null) return null;
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(str);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			Object replacement = Objects.requireNonNull(replace.apply(matcher));
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	@Nullable
	@Contract("null, _, _ -> null; !null, _, _ -> !null")
	public static String replacePlaceholders(@Nullable String str, @NotNull String regex, @NotNull Object replacement) {
		return replacePlaceholdersComplex(str, regex, matcher -> replacement);
	}
}