package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.menus.MenuItem;
import com.mineshoku.mstutils.menus.MenuPagesBasic;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.*;

import java.util.*;

public final class BackpackMenuPages extends MenuPagesBasic<MSTBackpack, BackpackMenuPages.MenuInfo> {
	private @Nullable @NonNegative Integer slotClose;
	private @Nullable @NonNegative Integer slotNext;
	private @Nullable @NonNegative Integer slotPrevious;
	private final @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean>> extraBorders;
	private @Nullable @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean> extraBorder;

	public BackpackMenuPages(@NotNull MSTBackpack plugin, @NotNull BackpackInfo info, @NotNull Player player) {
		super(plugin, new MenuInfo(plugin, info), player, true);
		this.extraBorders = extraBorders();
	}

	@Override
	@NotNull
	protected Set<@NotNull Integer> getStaticBorders() {
		TreeSet<Integer> set = new TreeSet<>();
		if (info().topBorder) {
			for (int i = 0; i < InventoryUtils.LINE_SLOTS; i++) {
				set.add(i);
			}
		}
		for (int i = this.inventorySize - InventoryUtils.LINE_SLOTS; i < this.inventorySize; i++) {
			set.add(i);
		}
		return set;
	}

	@Override
	@Positive
	public int maxPages() {
		return info().maxPages;
	}

	@Override
	@Positive
	@Range(from = 1, to = 6)
	protected int lines() {
		return info().lines;
	}

	@Override
	@NotNull
	protected Component title() {
		return Utils.thisOrThat(this.plugin.backpackConfig().snapshot().menuTitle(), Component.empty());
	}

	@Override
	@NotNull
	protected TreeMap<@NotNull Integer, @NotNull TreeMap<@NotNull Integer, @Nullable MenuItem<MSTBackpack, MenuInfo>>> initialPagesContent() {
		TreeMap<Integer, TreeMap<Integer, MenuItem<MSTBackpack, MenuInfo>>> map = new TreeMap<>();
		List<ItemStack> items = info().backpackInfo.items();
		if (items == null || items.isEmpty()) return map;
		if (this.plugin.backpackConfig().snapshot().condense()) {
			items = InventoryUtils.condenseItems(items);
		}
		int startSlot = info().topBorder ? InventoryUtils.LINE_SLOTS : 0;
		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack item = items.get(slot);
			if (item != null) {
				int page = Math.ceilDiv(slot + 1, info().maxSlotsPerPage), pageSlot = startSlot + (slot % info().maxSlotsPerPage);
				map.computeIfAbsent(page, i -> new TreeMap<>()).put(pageSlot, MenuItem.fixed(item));
			}
		}
		return map;
	}

	@NotNull
	@Unmodifiable
	private NavigableMap<@NotNull Integer, @NotNull @Unmodifiable NavigableMap<@NotNull Integer, @NotNull Boolean>> extraBorders() {
		Map<Integer, TreeMap<Integer, Boolean>> map = new HashMap<>();
		int firstLine = info().topBorder ? InventoryUtils.LINE_SLOTS : 0, totalInventorySlots = this.inventorySize * maxPages();
		for (long slot = info().totalSlots; slot < totalInventorySlots; slot++) {
			int pageSlot = Math.toIntExact(slot % info().maxSlotsPerPage) + firstLine,
					page = Math.toIntExact(Math.ceilDiv(slot + 1, info().maxSlotsPerPage));
			map.computeIfAbsent(page, i -> new TreeMap<>()).put(pageSlot, slot < info().totalSlotsMax);
		}
		TreeMap<Integer, NavigableMap<Integer, Boolean>> clone = new TreeMap<>();
		map.forEach((page, set) -> clone.put(page, Collections.unmodifiableNavigableMap(set)));
		return Collections.unmodifiableNavigableMap(clone);
	}

	@Override
	protected void setPageUpdate(@Positive int previousPage) {
		super.setPageUpdate(previousPage);
		this.extraBorder = this.extraBorders.get(currentPage());
	}

	@Nullable
	private Boolean isExtraPageBorder(int slot) {
		if (isStaticBorder(slot) || this.extraBorder == null) return null;
		return this.extraBorder.get(slot);
	}

	@Override
	protected boolean isBorder(int slot) {
		return isStaticBorder(slot) || isExtraPageBorder(slot) != null;
	}

	@Override
	@Nullable
	@NonNegative
	protected Integer slotClose() {
		return this.slotClose;
	}

	@Override
	@Nullable
	@NonNegative
	protected Integer slotNext() {
		return this.slotNext;
	}

	@Override
	@Nullable
	@NonNegative
	protected Integer slotPrevious() {
		return this.slotPrevious;
	}

	@Override
	@NotNull
	protected BackpackMenuItem itemClose() {
		return this.plugin.backpackConfig().snapshot().menuClose();
	}

	@Override
	@NotNull
	protected BackpackMenuItem itemNext() {
		return this.plugin.backpackConfig().snapshot().menuNext();
	}

	@Override
	@NotNull
	protected BackpackMenuItem itemPrevious() {
		return this.plugin.backpackConfig().snapshot().menuPrevious();
	}

	@Override
	@NotNull
	protected BackpackMenuItem borderStatic() {
		return this.plugin.backpackConfig().snapshot().menuBorderStatic();
	}

	@Override
	protected void updateBorder() {
		this.slotClose = calculateInnerSlot(itemClose().slot());
		this.slotNext = calculateInnerSlot(itemNext().slot());
		this.slotPrevious = calculateInnerSlot(itemPrevious().slot());
		if (this.extraBorder != null) {
			this.extraBorder.forEach((slot, unlockable) -> setMenuItem(slot, unlockable && info().menuShowUnlockable ?
					this.plugin.backpackConfig().snapshot()::menuBorderUnlockable : null, this::borderStatic));
		}
		this.staticBorders.forEach(slot -> setMenuItem(slot, null, this::borderStatic));
		setMenuItem(Utils.applyNotNull(this.plugin.backpackConfig().snapshot().menuIndicator(), BackpackMenuItem::slot),
				this.plugin.backpackConfig().snapshot()::menuIndicator, this::borderStatic);
		setMenuItem(slotPrevious(), () -> currentPage() > 1 ? itemPrevious() : null, this::borderStatic);
		setMenuItem(slotNext(), () -> currentPage() < maxPages() ? itemNext() : null, this::borderStatic);
		setMenuItem(slotClose(), this::itemClose, this::borderStatic);
	}

	@Override
	protected void saveExit() {
		savePage(currentPage());
		List<ItemStack> items = new ArrayList<>(), temp = new ArrayList<>();
		int startSlot = info().topBorder ? InventoryUtils.LINE_SLOTS : 0, endSlots = this.inventorySize - InventoryUtils.LINE_SLOTS;
		for (int page = 1; page <= (this.pagesContent.isEmpty() ? 0 : this.pagesContent.lastKey()); page++) {
			TreeMap<Integer, MenuItem<MSTBackpack, MenuInfo>> pageContent = this.pagesContent.get(page);
			for (int slot = startSlot; slot < endSlots; slot++) {
				MenuItem<MSTBackpack, MenuInfo> menuItem = pageContent == null ? null : pageContent.get(slot);
				if (menuItem == null) {
					temp.add(null);
				} else {
					items.addAll(temp);
					temp = new ArrayList<>();
					items.add(menuItem.toItemStack(this));
				}
			}
		}
		if (this.plugin.backpackConfig().snapshot().condense()) {
			items = InventoryUtils.condenseItems(items);
		}
		this.plugin.backpackDatabase().saveItems(info().backpackInfo().playerID(), info().backpackInfo().profileID(), items).handle((ignored, e) -> {
			if (e == null) return true;
			LoggingManager.instance().logError(this.plugin, e);
			return false;
		}).thenAcceptAsync(success -> {
			if (!this.player.isOnline()) return;
			if (success) {
				try {
					this.player.saveData();
				} catch (Exception e) {
					LoggingManager.instance().logError(this.plugin, e);
				}
			} else {
				this.player.getInventory().setContents(this.playerInitialItems);
			}
		}, ExecutorManager.mainThreadExecutor(this.plugin));
	}

	@Override
	public void onInventoryClick(@NotNull InventoryClickEvent event) {
		if (!isThisInventory(event)) return;
		int slot = event.getRawSlot();
		if (!isSlotInInventory(slot)) return;
		if (isSlotDisallowed(slot)) {
			event.setCancelled(true);
		}
		if (!isLegalBorder(slot)) return;
		if (Objects.equals(slot, this.slotClose)) {
			Bukkit.getScheduler().runTaskLater(this.plugin, () -> player().closeInventory(), 1);
		} else if (Objects.equals(slot, this.slotNext)) {
			if (event.getClick().isRightClick()) {
				setLastPage();
			} else {
				nextPage();
			}
		} else if (Objects.equals(slot, this.slotPrevious)) {
			if (event.getClick().isRightClick()) {
				setFirstPage();
			} else {
				previousPage();
			}
		}
	}

	@ApiStatus.Internal
	public static final class MenuInfo {
		private final @NotNull BackpackInfo backpackInfo;
		private final boolean topBorder;
		private final BackpackConfig.@NotNull Type type;
		private final boolean menuShowUnlockable;
		private final @NonNegative int totalSlots;
		private final @NonNegative int totalSlotsMax;
		private final @Positive int maxSlotsPerPage;
		private final @Positive int maxPages;
		private final @Positive int lines;

		private MenuInfo(@NotNull MSTBackpack plugin, @NotNull BackpackInfo backpackInfo) {
			this.backpackInfo = backpackInfo;
			this.type = plugin.backpackConfig().snapshot().type();
			this.topBorder = plugin.backpackConfig().snapshot().menuBorder();
			this.menuShowUnlockable = plugin.backpackConfig().snapshot().menuShowUnlockable();
			int amount = plugin.backpackConfig().snapshot().calculateAmount(backpackInfo.extrasPlayer(), backpackInfo.extrasProfile()),
					borders = this.topBorder ? 2 : 1;
			this.totalSlots = totalSlots(amount);
			this.totalSlotsMax = totalSlots(plugin.backpackConfig().snapshot().amountTotalMax());
			this.maxSlotsPerPage = InventoryUtils.slotsFromLines(InventoryUtils.MAX_LINES - borders);
			int useSlots = this.menuShowUnlockable ? this.totalSlotsMax : this.totalSlots;
			this.maxPages = useSlots < this.maxSlotsPerPage ? 1 : Math.ceilDiv(useSlots, this.maxSlotsPerPage);
			this.lines = this.menuShowUnlockable || this.maxPages > 1 ? InventoryUtils.MAX_LINES :
					Math.ceilDiv(this.totalSlots, InventoryUtils.LINE_SLOTS) + borders;
		}

		@NotNull
		public BackpackInfo backpackInfo() {
			return this.backpackInfo;
		}

		@NonNegative
		private int totalSlots(@NonNegative int total) {
			return switch (this.type) {
				case PAGES -> Math.multiplyExact(total, InventoryUtils.slotsFromLines(InventoryUtils.MAX_LINES - (this.topBorder ? 2 : 1)));
				case LINES -> Math.multiplyExact(total, InventoryUtils.LINE_SLOTS);
				case SLOTS -> total;
			};
		}
	}
}