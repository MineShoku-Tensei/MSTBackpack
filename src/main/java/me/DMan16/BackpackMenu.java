package me.DMan16;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class BackpackMenu implements Listener {
	private static final int LINE_SIZE = 9;
	private static final int INVENTORY_LINES = 6;
	private static final int INVENTORY_SIZE = LINE_SIZE * INVENTORY_LINES;
	private static final int INVENTORY_LAST_LINE_START = INVENTORY_SIZE - LINE_SIZE;
	private static final @NotNull String KEY_PAGE = "book.pageIndicator";
	private static final @NotNull String KEY_PAGE_CLOSE = "spectatorMenu.next_page";
	private static final @NotNull String KEY_PAGE_NEXT = "spectatorMenu.next_page";
	private static final @NotNull String KEY_PAGE_PREVIOUS = "spectatorMenu.previous_page";
	private static final @NotNull Component INVENTORY_NAME = text("Backpack", NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
	private static final @NotNull ItemStack ITEM_BORDER;
	private static final @NotNull ItemStack ITEM_PAGE_NEXT = ItemStack.of(Material.ARROW);
	private static final @NotNull ItemStack ITEM_PAGE_PREVIOUS = ITEM_PAGE_NEXT.clone();
	private static final @NotNull ItemStack ITEM_CLOSE;
	private static final @NotNull ItemStack ITEM_PAGE_COUNT = ItemStack.of(Material.PAPER);

	static {
		ITEM_BORDER = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
		ITEM_BORDER.editMeta(meta -> meta.itemName(Component.empty()));
		ITEM_CLOSE = ItemStack.of(Material.BARRIER);
		ITEM_CLOSE.editMeta(meta -> meta.itemName(translate(KEY_PAGE_CLOSE, NamedTextColor.RED)));
	}

	@NotNull
	protected static Component text(@NotNull Object obj, @Nullable TextColor color) {
		return noItalic(Component.text(obj.toString(), color));
	}

	@NotNull
	protected static Component translate(@NotNull String key, @Nullable TextColor color, @NotNull Component ... args) {
		return noItalic(Component.translatable(key, color, args));
	}
	@NotNull
	protected static Component noItalic(@NotNull Component component) {
		return component.decoration(TextDecoration.ITALIC, false);
	}

	@Nullable
	private static ItemStack getFromPage(@Nullable Map<@NotNull Integer, @Nullable ItemStack> page, @NonNegative int slot) {
		return page == null ? null : page.get(slot);
	}

	protected final @NotNull JavaPlugin plugin;
	protected final @NotNull Player player;
	protected final @NotNull UUID profileID;
	protected final @Positive int maxPages;
	private final @NotNull Database database;
	private final @NotNull Inventory inventory;
	protected final @NotNull Map<@NotNull Integer, @NotNull Map<@NotNull Integer, @Nullable ItemStack>> pages;

	private final @Nullable ItemStack @NotNull [] playerInitialItems;

	private @Positive int currentPage;

	private @Nullable Map<@NotNull Integer, @Nullable ItemStack> pageContent;

	public BackpackMenu(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull UUID profileID, 
						@Positive int maxPages, @NotNull Database database) throws NullPointerException, SQLException {
		this.plugin = plugin;
		this.player = player;
		this.profileID = profileID;
		this.database = database;
		this.inventory = Bukkit.createInventory(player, INVENTORY_SIZE, INVENTORY_NAME);
		this.pages = pagesFromItems(this.database.getItems(this.player.getUniqueId(), this.profileID));
		this.maxPages = maxPages;
		this.playerInitialItems = this.player.getInventory().getContents();
		fillBorder();
		setPage(1, false);
	}

	@NotNull
	private Map<@NotNull Integer, @NotNull Map<@NotNull Integer, @Nullable ItemStack>> pagesFromItems(@Nullable List<@Nullable ItemStack> items) {
		Map<Integer, Map<Integer, ItemStack>> map = new LinkedHashMap<>();
		if (items != null) {
			LinkedHashMap<Integer, ItemStack> page = new LinkedHashMap<>();
			Iterator<ItemStack> iterItems = items.iterator();
			Iterator<Integer> iterSlots = streamInside().iterator();
			while (iterItems.hasNext()) {
				ItemStack item = iterItems.next();
				if (!iterSlots.hasNext()) {
					map.put(map.size() + 1, page);
					page = new LinkedHashMap<>();
					iterSlots = streamInside().iterator();
				}
				int slot = iterSlots.next();
				if (item != null && !item.isEmpty()) {
					page.put(slot, item);
				}
			}
			if (!page.isEmpty()) {
				map.put(map.size() + 1, page);
			}
		}
		return map;
	}

	@NotNull
	protected final String randomString() {
		return System.currentTimeMillis() + "+" + ThreadLocalRandom.current().nextInt(1000000) + "=" + UUID.randomUUID();
	}

	@NotNull
	protected final ItemStack lockItem(@NotNull ItemStack item) {
		ItemStack clone = item.clone();
		clone.editMeta(meta -> meta.getPersistentDataContainer().
				set(new NamespacedKey(this.plugin, UUID.randomUUID().toString()), PersistentDataType.STRING, randomString()));
		return clone;
	}

	protected int slotClose() {
		return INVENTORY_SIZE - 5;
	}

	@NotNull
	protected ItemStack itemClose() {
		return lockItem(ITEM_CLOSE);
	}

	private void savePage() {
		if (this.pageContent == null) {
			this.pageContent = new HashMap<>();
			this.pages.put(this.currentPage, this.pageContent);
		}
		streamInside().forEach(slot -> {
			ItemStack item = this.inventory.getItem(slot);
			if (item == null) {
				this.pageContent.remove(slot);
			} else {
				this.pageContent.put(slot, item);
			}
		});
		if (this.pageContent.isEmpty()) {
			this.pageContent = null;
			this.pages.remove(this.currentPage);
		}
	}

	private void setPage(int page, boolean savePage) {
		if (page <= 0 || page > maxPages) return;
		if (savePage) {
			savePage();
		}
		this.currentPage = page;
		this.pageContent = this.pages.get(page);
		updateBorder();
		fillContent();
	}

	protected final void setPage(int page) {
		setPage(page, true);
	}

	protected final void setFirstPage() {
		setPage(1);
	}

	protected final void setLastPage() {
		setPage(maxPages);
	}

	protected final void nextPage() {
		setPage(this.currentPage + 1);
	}

	protected final void previousPage() {
		setPage(this.currentPage - 1);
	}

	private void setItem(@NonNegative @Range(from = 0, to = INVENTORY_SIZE - 1) int slot, @Nullable ItemStack item) {
		this.inventory.setItem(slot, item);
	}

	@NotNull
	protected final IntStream streamBorder() {
		return IntStream.range(0, INVENTORY_SIZE).filter(this::isBorder);
	}

	@NotNull
	protected final IntStream streamInside() {
		return IntStream.range(0, INVENTORY_SIZE).filter(slot -> !isBorder(slot));
	}

	/**
	 * {@link BackpackMenu#INVENTORY_SIZE}
	 */
	@NotNull
	protected ItemStack itemBorder(@NonNegative @Range(from = 0, to = INVENTORY_SIZE - 1) int slot) {
		return lockItem(ITEM_BORDER);
	}

	protected final void fillBorder() {
		streamBorder().forEach(i -> setItem(i, i == slotClose() ? itemClose() : itemBorder(i)));
	}

	protected int slotNext() {
		return INVENTORY_SIZE - 1;
	}

	@NotNull
	protected final String pageAppendage(int add) {
		return " (" + (this.currentPage + add) + ")";
	}

	@NotNull
	protected ItemStack itemNext() {
		ItemStack item = lockItem(ITEM_PAGE_NEXT);
		item.editMeta(meta -> meta.itemName(translate(KEY_PAGE_NEXT, NamedTextColor.GREEN).append(text(pageAppendage(1), null))));
		return item;
	}

	protected int slotPrevious() {
		return INVENTORY_SIZE - 9;
	}

	@NotNull
	protected ItemStack itemPrevious() {
		ItemStack item = lockItem(ITEM_PAGE_PREVIOUS);
		item.editMeta(meta -> meta.itemName(translate(KEY_PAGE_PREVIOUS, NamedTextColor.GOLD).append(text(pageAppendage(-1), null))));
		return item;
	}

	protected int slotPageCount() {
		return 4;
	}

	@NotNull
	protected ItemStack itemPageCount() {
		ItemStack item = lockItem(ITEM_PAGE_COUNT);
		item.setAmount(this.currentPage);
		item.editMeta(meta -> meta.itemName(translate(KEY_PAGE, NamedTextColor.LIGHT_PURPLE, text(this.currentPage, null), text(this.maxPages, null))));
		return item;
	}

	protected final void updateBorder() {
		if (isLegalBorder(slotPrevious())) {
			setItem(slotPrevious(), this.currentPage > 1 ? itemPrevious() : itemBorder(slotPrevious()));
		}
		if (isLegalBorder(slotNext())) {
			setItem(slotNext(), this.currentPage < maxPages ? itemNext() : itemBorder(slotNext()));
		}
		if (isLegalBorder(slotPageCount())) {
			setItem(slotPageCount(), itemPageCount());
		}
	}

	protected final void fillContent() {
		streamInside().forEach(slot -> setItem(slot, getFromPage(this.pageContent, slot)));
	}

	private void register() {
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
	}

	private void unregister() {
		HandlerList.unregisterAll(this);
	}

	protected final boolean isThisInventory(@NotNull Inventory inventory) {
		return inventory.equals(this.inventory);
	}

	protected final boolean isThisInventory(@NotNull InventoryEvent inventoryEvent) {
		return isThisInventory(inventoryEvent.getView().getTopInventory());
	}

	protected final boolean isSlotInInventory(int slot) {
		return slot >= 0 && slot < INVENTORY_SIZE;
	}

	protected boolean isBorder(int slot) {
		return (slot >= 0 && slot < LINE_SIZE) || (slot >= INVENTORY_LAST_LINE_START && slot < INVENTORY_SIZE) ||
				(slot + 1) % LINE_SIZE < 2;
	}

	protected boolean isLegalBorder(int slot) {
		return isSlotInInventory(slot) && isBorder(slot);
	}

	protected boolean isSlotDisallowed(int slot) {
		return isLegalBorder(slot);
	}

	protected final void close() {
		inventory.close();
	}

	/**
	 * Also registers this as a {@link Listener}
	 */
	@Nullable
	public final InventoryView openInventory() {
		InventoryView view = player.openInventory(inventory);
		if (view != null) {
			register();
		}
		return view;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!isThisInventory(event)) return;
		if (event.getRawSlots().stream().anyMatch(this::isSlotDisallowed)) {
			event.setCancelled(true);
		}
	}

	protected void saveItems(@NotNull List<@Nullable ItemStack> items) {
		try {
			this.database.saveItems(this.player.getUniqueId(), this.profileID, items);
		} catch (Exception e) {
			e.printStackTrace();
			if (this.player.isOnline()) {
				new BukkitRunnable() {
					public void run() {
						player.getInventory().setContents(playerInitialItems);
					}
				}.runTaskLater(this.plugin, 1);
			}
		}
	}

	protected final void saveExit() {
		savePage();
		List<ItemStack> items = new ArrayList<>();
		IntStream.rangeClosed(1, maxPages).mapToObj(this.pages::get).
				forEachOrdered(page -> streamInside().forEach(slot -> items.add(getFromPage(page, slot))));
		saveItems(items);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public final void onCloseSaveEvent(InventoryCloseEvent event) {
		if (isThisInventory(event)) {
			unregister();
			saveExit();
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public final void onQuitSaveEvent(PlayerQuitEvent event) {
		if (event.getPlayer().equals(this.player)) {
			unregister();
			saveExit();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!isThisInventory(event)) return;
		int slot = event.getRawSlot();
		if (!isSlotInInventory(slot)) return;
		if (isSlotDisallowed(slot)) {
			event.setCancelled(true);
		}
		if (!isLegalBorder(slot)) return;
		boolean rightClick = event.getClick().isRightClick();
		if (slot == slotClose()) {
			new BukkitRunnable() {
				public void run() {
					close();
				}
			}.runTaskLater(this.plugin, 1);
		} else if (slot == slotNext()) {
			if (rightClick) {
				setLastPage();
			} else {
				nextPage();
			}
		} else if (slot == slotPrevious()) {
			if (rightClick) {
				setFirstPage();
			} else {
				previousPage();
			}
		}
	}
}