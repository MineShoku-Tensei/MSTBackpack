package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.managers.LoggingManager;
import org.bukkit.Bukkit;
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
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;

public final class BackpackMenu implements Listener {
	private final @NotNull MSTBackpack plugin;
	private final @NotNull Player player;
	private final @NotNull BackpackInfo info;
	private final BackpackConfig.@NotNull Type type;
	private final boolean topBorder;
	private @Nullable @NonNegative Integer slotClose;
	private @Nullable @NonNegative Integer slotNext;
	private @Nullable @NonNegative Integer slotPrevious;
	private final @Positive int inventorySize;
	private final @Positive int maxPages;
	private final @NotNull Inventory inventory;
	private final @NotNull TreeMap<@NotNull Integer, @NotNull TreeMap<@NotNull Integer, @Nullable ItemStack>> pagesItems;
	private final @NotNull @Unmodifiable Set<@NotNull Integer> staticBorders;
	private final @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean>> extraBorders;
	private final @Nullable ItemStack @NotNull [] playerInitialItems;
	private @Positive int currentPage;
	private @Nullable TreeMap<@NotNull Integer, @Nullable ItemStack> pageItems;
	private @Nullable @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean> extraBorder;

	public BackpackMenu(@NotNull MSTBackpack plugin, @NotNull Player player, @NotNull BackpackInfo info) throws NullPointerException {
		this.plugin = plugin;
		this.player = player;
		this.info = info;
		this.type = this.plugin.backpackConfig().type();
		this.topBorder = this.plugin.backpackConfig().menuBorder();
		boolean menuShowUnlockable = this.plugin.backpackConfig().menuShowUnlockable();
		int amount = this.plugin.backpackConfig().calculateAmount(info.extrasPlayer(), info.extrasProfile()),
				borders = this.topBorder ? 2 : 1, totalSlots = totalSlots(amount, this.type, this.topBorder),
				totalSlotsMax = totalSlots(this.plugin.backpackConfig().amountTotalMax(), this.type, this.topBorder),
				maxSlotsPerPage = InventoryUtils.slotsFromLines(InventoryUtils.MAX_LINES - borders);
		this.pagesItems = itemsPages(this.info.items(), this.topBorder, maxSlotsPerPage, this.plugin.backpackConfig().condense());
		this.maxPages = maxPages(totalSlots, totalSlotsMax, menuShowUnlockable, maxSlotsPerPage);
		int lines = menuShowUnlockable || this.maxPages > 1 ? InventoryUtils.MAX_LINES : Math.ceilDiv(totalSlots, InventoryUtils.LINE_SLOTS) + borders;
		this.inventory = InventoryUtils.inventory(this.player, lines, this.plugin.backpackConfig().menuTitle());
		this.inventorySize = this.inventory.getSize();
		this.extraBorders = extraBorders(this.topBorder, this.inventorySize, this.maxPages, totalSlots, totalSlotsMax, maxSlotsPerPage);
		this.staticBorders = staticBorders(this.topBorder, this.inventorySize);
		this.playerInitialItems = this.player.getInventory().getContents();
		setPage(1, false);
	}

	@NonNegative
	private static int totalSlots(@NonNegative int total, BackpackConfig.@NotNull Type type, boolean topBorder) {
		return switch (type) {
			case PAGES -> Math.multiplyExact(total, InventoryUtils.slotsFromLines(InventoryUtils.MAX_LINES - (topBorder ? 2 : 1)));
			case LINES -> Math.multiplyExact(total, InventoryUtils.LINE_SLOTS);
			case SLOTS -> total;
		};
	}

	@NotNull
	@Unmodifiable
	@Contract("_, _ -> new")
	private static Set<@NotNull Integer> staticBorders(boolean topBorder, @Positive int inventorySize) {
		TreeSet<Integer> set = new TreeSet<>();
		if (topBorder) {
			for (int i = 0; i < InventoryUtils.LINE_SLOTS; i++) {
				set.add(i);
			}
		}
		for (int i = inventorySize - InventoryUtils.LINE_SLOTS; i < inventorySize; i++) {
			set.add(i);
		}
		return Collections.unmodifiableNavigableSet(set);
	}

	@NotNull
	@Contract("_, _, _, _ -> new")
	private static TreeMap<@NotNull Integer, @NotNull TreeMap<@NotNull Integer, @Nullable ItemStack>>
	itemsPages(@Nullable List<@Nullable ItemStack> items, boolean topBorder, @Positive int maxSlotsPerPage, boolean condense) {
		TreeMap<Integer, TreeMap<Integer, ItemStack>> map = new TreeMap<>();
		if (items == null || items.isEmpty()) return map;
		if (condense) {
			items = InventoryUtils.condenseItems(items);
		}
		int startSlot = topBorder ? InventoryUtils.LINE_SLOTS : 0;
		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack item = items.get(slot);
			if (item != null) {
				int page = Math.ceilDiv(slot + 1, maxSlotsPerPage), pageSlot = startSlot + (slot % maxSlotsPerPage);
				map.computeIfAbsent(page, i -> new TreeMap<>()).put(pageSlot, item);
			}
		}
		return map;
	}

	private static int maxPages(int totalSlots, int totalSlotsMax, boolean menuShowUnlockable, @Positive int maxSlotsPerPage) {
		int useSlots = menuShowUnlockable ? totalSlotsMax : totalSlots;
		return useSlots < maxSlotsPerPage ? 1 : Math.ceilDiv(useSlots, maxSlotsPerPage);
	}

	@NotNull
	@Unmodifiable
	@Contract("_, _, _, _, _, _ -> new")
	private static NavigableMap<@NotNull Integer, @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean>>
	extraBorders(boolean topBorder, @Positive int inventorySize, @Positive int maxPages, @Positive int totalSlots,
				 @NonNegative int totalSlotsPossibleWithExtras, @Positive int maxSlotsPerPage) {
		Map<Integer, TreeMap<Integer, Boolean>> map = new HashMap<>();
		int firstLine = topBorder ? InventoryUtils.LINE_SLOTS : 0, totalInventorySlots = inventorySize * maxPages;
		for (long slot = totalSlots; slot < totalInventorySlots; slot++) {
			int pageSlot = Math.toIntExact(slot % maxSlotsPerPage) + firstLine, page = Math.toIntExact(Math.ceilDiv(slot + 1, maxSlotsPerPage));
			map.computeIfAbsent(page, i -> new TreeMap<>()).put(pageSlot, slot < totalSlotsPossibleWithExtras);
		}
		TreeMap<Integer, NavigableMap<Integer, Boolean>> clone = new TreeMap<>();
		map.forEach((page, set) -> clone.put(page, Collections.unmodifiableNavigableMap(set)));
		return Collections.unmodifiableNavigableMap(clone);
	}

	@Nullable
	private ItemStack getFromPage(@NonNegative int slot) {
		return this.pageItems == null ? null : this.pageItems.get(slot);
	}

	private void savePage() {
		if (this.pageItems == null) {
			this.pageItems = new TreeMap<>();
			this.pagesItems.put(this.currentPage, this.pageItems);
		}
		runInside(slot -> {
			ItemStack item = this.inventory.getItem(slot);
			if (item == null) {
				this.pageItems.remove(slot);
			} else {
				this.pageItems.put(slot, item);
			}
		});
		if (this.pageItems.isEmpty()) {
			this.pageItems = null;
			this.pagesItems.remove(this.currentPage);
		}
	}

	private void setPage(int page, boolean savePage) {
		if (page <= 0 || page > this.maxPages) return;
		if (savePage) {
			savePage();
		}
		this.currentPage = page;
		this.pageItems = this.pagesItems.get(page);
		this.extraBorder = this.extraBorders.get(page);
		updateBorder();
		fillContent();
	}

	private void setPage(int page) {
		setPage(page, true);
	}

	private void setFirstPage() {
		setPage(1);
	}

	private void setLastPage() {
		setPage(this.maxPages);
	}

	private void nextPage() {
		setPage(this.currentPage + 1);
	}

	private void previousPage() {
		setPage(this.currentPage - 1);
	}

	private void setItem(@NonNegative int slot, @Nullable ItemStack item) {
		this.inventory.setItem(slot, item);
	}

	private boolean isStaticBorder(int slot) {
		return this.staticBorders.contains(slot);
	}

	@Nullable
	private Boolean isExtraPageBorder(int slot) {
		if (isStaticBorder(slot) || this.extraBorder == null) return null;
		return this.extraBorder.get(slot);
	}

	private boolean isBorder(int slot) {
		return isStaticBorder(slot) || isExtraPageBorder(slot) != null;
	}

	private void runInside(@NotNull Consumer<@NotNull @NonNegative Integer> run) {
		for (int slot = 0; slot < this.inventorySize; slot++) {
			if (isBorder(slot)) continue;
			run.accept(slot);
		}
	}

	@NotNull
	private ItemStack fromConfigPageItem(@NotNull PageItem pageItem) {
		return pageItem.toItemStack(this.currentPage, this.maxPages, this.info.extrasPlayer(), this.info.extrasProfile(),
				this.plugin.backpackConfig().amountExtraPlayerMax(), this.plugin.backpackConfig().amountExtraProfileMax());
	}

	@NonNegative
	private int calculateSlot(@NotNull PageItem pageItem) {
		int calculated = pageItem.slot();
		while (calculated < 0) {
			calculated += this.inventorySize;
		}
		while (calculated >= this.inventorySize) {
			calculated -= this.inventorySize;
		}
		return calculated;
	}

	private void updateBorder() {
		PageItem menuIndicator = this.plugin.backpackConfig().menuIndicator();
		PageItem menuClose = this.plugin.backpackConfig().menuClose();
		PageItem menuNext = this.plugin.backpackConfig().menuNext();
		PageItem menuPrevious = this.plugin.backpackConfig().menuPrevious();
		PageItem menuBorderStatic = this.plugin.backpackConfig().menuBorderStatic();
		PageItem menuBorderUnlockable = this.plugin.backpackConfig().menuBorderUnlockable();
		ItemStack borderStatic = fromConfigPageItem(menuBorderStatic), borderUnlockable = fromConfigPageItem(menuBorderUnlockable);
		int slotIndicator, slotClose = calculateSlot(menuClose), slotNext = calculateSlot(menuNext), slotPrevious = calculateSlot(menuPrevious);
		this.slotClose = isLegalBorder(slotClose) ? slotClose : null;
		this.slotNext = isLegalBorder(slotNext) ? slotNext : null;
		this.slotPrevious = isLegalBorder(slotPrevious) ? slotPrevious : null;
		this.staticBorders.forEach(slot -> setItem(slot, Utils.uniquifyItem(borderStatic)));
		if (this.extraBorder != null) {
			this.extraBorder.forEach((slot, unlockable) -> setItem(slot, Utils.uniquifyItem(unlockable ? borderUnlockable : borderStatic)));
		}
		if (menuIndicator != null && isLegalBorder(slotIndicator = calculateSlot(menuIndicator))) {
			setItem(slotIndicator, Utils.uniquifyItem(fromConfigPageItem(menuIndicator)));
		}
		if (this.slotPrevious != null) {
			setItem(this.slotPrevious, Utils.uniquifyItem(this.currentPage > 1 ? fromConfigPageItem(menuPrevious) : borderStatic));
		}
		if (this.slotNext != null) {
			setItem(this.slotNext, Utils.uniquifyItem(this.currentPage < this.maxPages ? fromConfigPageItem(menuNext) : borderStatic));
		}
		if (this.slotClose != null) {
			setItem(this.slotClose, Utils.uniquifyItem(fromConfigPageItem(menuClose)));
		}
	}

	private void fillContent() {
		runInside(slot -> setItem(slot, getFromPage(slot)));
	}

	private void register() {
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
	}

	private void unregister() {
		HandlerList.unregisterAll(this);
	}

	private boolean isThisInventory(@NotNull Inventory inventory) {
		return inventory.equals(this.inventory);
	}

	private boolean isThisInventory(@NotNull InventoryEvent inventoryEvent) {
		return isThisInventory(inventoryEvent.getView().getTopInventory());
	}

	private boolean isSlotInInventory(int slot) {
		return slot >= 0 && slot < this.inventorySize;
	}

	private boolean isLegalBorder(int slot) {
		return isSlotInInventory(slot) && isBorder(slot);
	}

	private boolean isSlotDisallowed(int slot) {
		return isLegalBorder(slot);
	}

	private void close() {
		inventory.close();
	}

	/**
	 * Also registers this as a {@link Listener}
	 */
	@Nullable
	public InventoryView openInventory() {
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

	private void saveExit() {
		savePage();
		List<ItemStack> items = new ArrayList<>(), temp = new ArrayList<>();
		int startSlot = this.topBorder ? InventoryUtils.LINE_SLOTS : 0, endSlots = this.inventorySize - InventoryUtils.LINE_SLOTS;
		for (int page = 1; page <= (this.pagesItems.isEmpty() ? 0 : this.pagesItems.lastKey()); page++) {
			Map<Integer, ItemStack> pageContent = this.pagesItems.get(page);
			for (int slot = startSlot; slot < endSlots; slot++) {
				ItemStack item = pageContent == null ? null : pageContent.get(slot);
				if (item == null) {
					temp.add(null);
				} else {
					items.addAll(temp);
					temp = new ArrayList<>();
					items.add(item);
				}
			}
		}
		if (this.plugin.backpackConfig().condense()) {
			items = InventoryUtils.condenseItems(items);
		}
		this.plugin.backpackDatabase().saveItems(this.info.playerID(), this.info.profileID(), items).handle((ignored, e) -> {
			if (e == null) return true;
			LoggingManager.instance().log(this.plugin, e);
			return false;
		}).thenAcceptAsync(success -> {
			if (!this.player.isOnline()) return;
			if (success) {
				try {
					this.player.saveData();
				} catch (Exception e) {
					LoggingManager.instance().log(this.plugin, e);
				}
			} else {
				this.player.getInventory().setContents(this.playerInitialItems);
			}
		}, ExecutorManager.mainThreadExecutor(this.plugin));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onCloseSaveEvent(InventoryCloseEvent event) {
		if (!isThisInventory(event)) return;
		unregister();
		saveExit();
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onQuitSaveEvent(PlayerQuitEvent event) {
		if (!event.getPlayer().equals(this.player)) return;
		unregister();
		saveExit();
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
		if (Objects.equals(slot, this.slotClose)) {
			Bukkit.getScheduler().runTaskLater(this.plugin, this::close, 1);
		} else if (Objects.equals(slot, this.slotNext)) {
			if (rightClick) {
				setLastPage();
			} else {
				nextPage();
			}
		} else if (Objects.equals(slot, this.slotPrevious)) {
			if (rightClick) {
				setFirstPage();
			} else {
				previousPage();
			}
		}
	}
}