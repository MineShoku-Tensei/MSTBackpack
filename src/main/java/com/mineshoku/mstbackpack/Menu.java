package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Menu implements Listener {
	protected final @NotNull Main plugin;
	protected final @NotNull Player player;
	protected final @NotNull Info info;
	protected final Config.@NotNull Type type;
	protected final boolean topBorder;
	private @Nullable @NonNegative Integer slotClose;
	private @Nullable @NonNegative Integer slotNext;
	private @Nullable @NonNegative Integer slotPrevious;
	private final @Positive int inventorySize;
	protected final @Positive int maxPages;
	private final @NotNull Inventory inventory;
	protected final @NotNull TreeMap<@NotNull Integer, @NotNull TreeMap<@NotNull Integer, @Nullable ItemStack>> pagesContent;
	private final @NotNull @Unmodifiable Set<@NotNull Integer> staticBorders;
	private final @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean>> extraBorders;
	private final @Nullable ItemStack @NotNull [] playerInitialItems;
	private @Positive int currentPage;
	private @Nullable TreeMap<@NotNull Integer, @Nullable ItemStack> pageContent;
	private @Nullable @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean> extraBorder;

	public Menu(@NotNull Main plugin, @NotNull Player player, @NotNull Info info) throws NullPointerException {
		this.plugin = plugin;
		this.player = player;
		this.info = info;
		this.type = this.plugin.config().type();
		this.topBorder = this.plugin.config().menuBorder();
		boolean menuShowUnlockable = this.plugin.config().menuShowUnlockable();
		int totalSlots = totalSlots(this.plugin.config().calculateAmount(info.extrasPlayer(), info.extrasProfile()), this.type, this.topBorder),
				totalSlotsPossibleWithExtras = totalSlots(this.plugin.config().amountTotalMax(), this.type, this.topBorder),
				maxSlotsPerPage = InventoryUtils.slotsFromLines(InventoryUtils.INVENTORY_MAX_LINES - (this.topBorder ? 2 : 1));
		this.pagesContent = itemsPages(this.info.items(), this.topBorder, maxSlotsPerPage, this.plugin.config().condense());
		this.maxPages = maxPages(totalSlots, totalSlotsPossibleWithExtras, menuShowUnlockable, maxSlotsPerPage);
		int lines = menuShowUnlockable || this.maxPages > 1 ? InventoryUtils.INVENTORY_MAX_LINES :
				Math.ceilDiv(totalSlots, InventoryUtils.LINE_SLOTS) + (this.topBorder ? 2 : 1);
		this.inventory = InventoryUtils.inventory(this.player, lines, this.plugin.config().menuTitle());
		this.inventorySize = this.inventory.getSize();
		this.extraBorders = extraBorders(this.topBorder, this.inventorySize, this.maxPages,
				totalSlots, totalSlotsPossibleWithExtras, maxSlotsPerPage);
		this.staticBorders = staticBorders(this.topBorder, this.inventorySize);
		this.playerInitialItems = this.player.getInventory().getContents();
		setPage(1, false);
	}

	@NonNegative
	private static int totalSlots(@NonNegative int total, Config.@NotNull Type type, boolean topBorder) {
		return switch (type) {
			case PAGES -> Math.multiplyExact(total, InventoryUtils.slotsFromLines(InventoryUtils.INVENTORY_MAX_LINES - (topBorder ? 2 : 1)));
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
			int pageSlot = Math.toIntExact(slot % maxSlotsPerPage) + firstLine,
					page = Math.toIntExact(Math.ceilDiv(slot + 1, maxSlotsPerPage));
			map.computeIfAbsent(page, i -> new TreeMap<>()).put(pageSlot, slot < totalSlotsPossibleWithExtras);
		}
		TreeMap<Integer, NavigableMap<Integer, Boolean>> clone = new TreeMap<>();
		map.forEach((page, set) -> clone.put(page, Collections.unmodifiableNavigableMap(set)));
		return Collections.unmodifiableNavigableMap(clone);
	}

	@NotNull
	@Contract("_ -> new")
	protected final ItemStack lockItem(@NotNull ItemStack item) {
		ItemStack clone = item.clone();
		clone.editMeta(meta -> Utils.setPDCString(meta, Utils.newNamespacedKey(this.plugin, UUID.randomUUID()), TextUtils.randomString()));
		return clone;
	}

	@Nullable
	private ItemStack getFromPage(@NonNegative int slot) {
		return this.pageContent == null ? null : this.pageContent.get(slot);
	}

	private void savePage() {
		if (this.pageContent == null) {
			this.pageContent = new TreeMap<>();
			this.pagesContent.put(this.currentPage, this.pageContent);
		}
		runInside(slot -> {
			ItemStack item = this.inventory.getItem(slot);
			if (item == null) {
				this.pageContent.remove(slot);
			} else {
				this.pageContent.put(slot, item);
			}
		});
		if (this.pageContent.isEmpty()) {
			this.pageContent = null;
			this.pagesContent.remove(this.currentPage);
		}
	}

	private void setPage(int page, boolean savePage) {
		if (page <= 0 || page > this.maxPages) return;
		if (savePage) {
			savePage();
		}
		this.currentPage = page;
		this.pageContent = this.pagesContent.get(page);
		this.extraBorder = this.extraBorders.get(page);
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
		setPage(this.maxPages);
	}

	protected final void nextPage() {
		setPage(this.currentPage + 1);
	}

	protected final void previousPage() {
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

	protected final void runInside(@NotNull Consumer<@NotNull @NonNegative Integer> run) {
		for (int slot = 0; slot < this.inventorySize; slot++) {
			if (!isBorder(slot)) {
				run.accept(slot);
			}
		}
	}

	@NotNull
	private ItemStack fromConfigPageItem(Config.@NotNull PageItem pageItem) {
		return pageItem.toItemStack(this.currentPage, this.maxPages, this.info.extrasPlayer(), this.info.extrasProfile(),
				this.plugin.config().amountExtraPlayerMax(), this.plugin.config().amountExtraProfileMax());
	}

	@NonNegative
	private int calculateSlot(Config.@NotNull PageItem pageItem) {
		int calculated = pageItem.slot();
		while (calculated < 0) {
			calculated += this.inventorySize;
		}
		while (calculated >= this.inventorySize) {
			calculated -= this.inventorySize;
		}
		return calculated;
	}

	protected final void updateBorder() {
		Config.PageItem menuIndicator = this.plugin.config().menuIndicator();
		Config.PageItem menuClose = this.plugin.config().menuClose();
		Config.PageItem menuNext = this.plugin.config().menuNext();
		Config.PageItem menuPrevious = this.plugin.config().menuPrevious();
		Config.PageItem menuBorderStatic = this.plugin.config().menuBorderStatic();
		Config.PageItem menuBorderUnlockable = this.plugin.config().menuBorderUnlockable();
		ItemStack borderStatic = fromConfigPageItem(menuBorderStatic), borderUnlockable = fromConfigPageItem(menuBorderUnlockable);
		int slotClose = calculateSlot(menuClose), slotNext = calculateSlot(menuNext), slotPrevious = calculateSlot(menuPrevious), slotIndicator;
		this.slotClose = isLegalBorder(slotClose) ? slotClose : null;
		this.slotNext = isLegalBorder(slotNext) ? slotNext : null;
		this.slotPrevious = isLegalBorder(slotPrevious) ? slotPrevious : null;
		this.staticBorders.forEach(slot -> setItem(slot, lockItem(borderStatic)));
		if (this.extraBorder != null) {
			this.extraBorder.forEach((slot, unlockable) -> setItem(slot, lockItem(unlockable ? borderUnlockable : borderStatic)));
		}
		if (menuIndicator != null && isLegalBorder(slotIndicator = calculateSlot(menuIndicator))) {
			setItem(slotIndicator, lockItem(fromConfigPageItem(menuIndicator)));
		}
		if (this.slotPrevious != null) {
			setItem(this.slotPrevious, lockItem(this.currentPage > 1 ? fromConfigPageItem(menuPrevious) : borderStatic));
		}
		if (this.slotNext != null) {
			setItem(this.slotNext, lockItem(this.currentPage < this.maxPages ? fromConfigPageItem(menuNext) : borderStatic));
		}
		if (this.slotClose != null) {
			setItem(this.slotClose, lockItem(fromConfigPageItem(menuClose)));
		}
	}

	protected final void fillContent() {
		runInside(slot -> setItem(slot, getFromPage(slot)));
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
		return slot >= 0 && slot < this.inventorySize;
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

	protected final void saveExit() {
		savePage();
		List<ItemStack> items = new ArrayList<>(), temp = new ArrayList<>();
		int startSlot = this.topBorder ? InventoryUtils.LINE_SLOTS : 0, endSlots = this.inventorySize - InventoryUtils.LINE_SLOTS;
		for (int page = 1; page <= (this.pagesContent.isEmpty() ? 0 : this.pagesContent.lastKey()); page++) {
			Map<Integer, ItemStack> pageContent = this.pagesContent.get(page);
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
		Info saveInfo = this.info.withItems(this.plugin.config().condense() ? InventoryUtils.condenseItems(items) : items);
		Utils.sendToSync(CompletableFuture.supplyAsync(() -> {
			try {
				this.plugin.database().saveItems(saveInfo);
				return true;
			} catch (Exception e) {
				Utils.logException(e);
				return false;
			}
		}), this.plugin, success -> {
			if (!this.player.isOnline()) return;
			if (success) {
				try {
					this.player.saveData();
				} catch (Exception ignored) {}
			} else {
				this.player.getInventory().setContents(this.playerInitialItems);
			}
		});
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
		if (Objects.equals(slot, this.slotClose)) {
			new BukkitRunnable() {
				public void run() {
					close();
				}
			}.runTaskLater(this.plugin, 1);
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