package com.mineshoku.mstbackpack;

import com.mineshoku.mstbackpack.mock.MMOPlayerProfile;
import com.mineshoku.mstbackpack.mock.MMOProfile;
import com.mineshoku.mstbackpack.mock.MMOProfileProvider;
import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.MSTUtils;
import com.mineshoku.mstutils.TestUtils;
import com.mineshoku.mstutils.models.Pair;
import fr.phoenixdevt.profiles.ProfileProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BackpackCommandTest {
	private static final String COMMAND = "backpack";
	private static final String PERMISSION = "mst.backpack.use";

	private ServerMock server;
	private MSTBackpack plugin;
	private MMOProfileProvider profileProvider;

	@BeforeEach
	void setUp() {
		this.server = MockBukkit.mock();
		this.profileProvider = new MMOProfileProvider();
		Bukkit.getServicesManager().register(ProfileProvider.class, this.profileProvider, MockBukkit.createMockPlugin("MMOProfiles"), ServicePriority.Normal);
		MockBukkit.loadWith(MSTUtils.class, TestUtils.PLUGIN_DESCRIPTION_UTILS);
		this.plugin = MockBukkit.load(MSTBackpack.class);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void pluginLoadsSuccessfully() {
		assertNotNull(this.plugin, "Plugin should be loaded by MockBukkit");
		assertTrue(this.plugin.isEnabled(), "Plugin should be enabled after load");
	}

	@NotNull
	PlayerMock addPlayer() {
		PlayerMock player = this.server.addPlayer();
		this.profileProvider.addPlayerData(new MMOProfile(player));
		return player;
	}

	void addProfiles(PlayerMock player, int amount) {
		for (int i = 0; i < amount; i++) {
			this.profileProvider.getPlayerData(player.getUniqueId()).addProfile(new MMOPlayerProfile(UUID.randomUUID()));
		}
	}

	void disconnect(@NotNull PlayerMock player) {
		this.profileProvider.removePlayerData(player.getUniqueId());
		player.disconnect();
	}

	String cmd(boolean endSpace, Object @NotNull ... extras) {
		String str = COMMAND;
		if (extras.length > 0) {
			str += " " + Arrays.stream(extras).map(Object::toString).collect(Collectors.joining(" "));
		}
		if (endSpace) {
			str += " ";
		}
		return str;
	}

	<V> void assertSameValues(@Nullable Collection<V> expectedList, @Nullable Collection<V> actualList) {
		if (expectedList == null || actualList == null) {
			assertSame(expectedList, actualList);
		} else {
			assertSame(expectedList.size(), actualList.size());
			assertTrue(actualList.containsAll(expectedList));
		}
	}

	@Test
	void tabCompleteBase() {
		assertSameValues(BackpackCommandHandler.BASE, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(true)));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(false, c)).size()));
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		assertSameValues(BackpackCommandHandler.BASE, this.server.getCommandTabComplete(player1, cmd(true)));
		assertSameValues(Collections.emptyList(), this.server.getCommandTabComplete(player2, cmd(true)));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(player1, cmd(false, c)).size()));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(0, this.server.getCommandTabComplete(player2, cmd(false, c)).size()));
	}

	void tabCompleteSubSimpleCheck(@NotNull String sub) {
		PlayerMock player1 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		assertSame(0, this.server.getCommandTabComplete(player1, cmd(true, sub)).size());
	}

	@Test
	void tabCompleteHelp() {
		tabCompleteSubSimpleCheck("help");
	}

	@Test
	void tabCompleteReload() {
		tabCompleteSubSimpleCheck("reload");
	}

	void tabCompleteSubAdvancedCheck(@NotNull String sub, boolean hasOptionNoProfile) {
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		disconnect(player2);
		assertSameValues(List.of(BackpackCommandHandler.EXTRAS_CURRENT, player1.getName()), this.server.getCommandTabComplete(player1, cmd(true, sub)));
		int profiles = 4;
		addProfiles(player1, profiles);
		List<String> result = this.profileProvider.getPlayerData(player1.getUniqueId()).getProfiles().stream().map(MMOPlayerProfile::getUniqueId).map(UUID::toString).collect(Collectors.toList()),
				setPlayer = hasOptionNoProfile ? List.of(BackpackCommandHandler.EXTRAS_SET_PLAYER) : Collections.emptyList();
		result.add(BackpackCommandHandler.EXTRAS_CURRENT);
		if (hasOptionNoProfile) {
			result.add(BackpackCommandHandler.EXTRAS_SET_PLAYER);
		}
		assertSameValues(result, this.server.getCommandTabComplete(player1, cmd(true, sub, BackpackCommandHandler.EXTRAS_CURRENT)));
		assertSameValues(setPlayer, this.server.getCommandTabComplete(player1, cmd(true, sub, player2.getName())));
		assertSameValues(setPlayer, this.server.getCommandTabComplete(player1, cmd(true, sub, player2.getUniqueId())));
		assertSame(0, this.server.getCommandTabComplete(player1, cmd(true, sub, "aaaa")).size());
	}

	@Test
	void tabCompleteOpen() {
		tabCompleteSubAdvancedCheck("open", false);
	}

	@Test
	void tabCompleteClear() {
		tabCompleteSubAdvancedCheck("clear", false);
	}

	@Test
	void tabCompleteUpgrade() {
		tabCompleteSubAdvancedCheck("upgrade", true);
	}

	@Test
	void tabCompleteDowngrade() {
		tabCompleteSubAdvancedCheck("downgrade", true);
	}

	void performCommandWithAsync(@NotNull PlayerMock player, @NotNull String command) throws InterruptedException {
		assertTrue(player.performCommand(command));
		Thread.sleep(100);
		this.server.getScheduler().performOneTick();
	}

	@Test
	void commandRunBase() throws InterruptedException {
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(PERMISSION, true);
		addProfiles(player1, 2);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, COMMAND);
		assertNotNull(player1.getOpenInventory().getTopInventory());
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
		performCommandWithAsync(player2, COMMAND);
		assertSame(InventoryType.CRAFTING, player2.getOpenInventory().getType());
	}

	@Test
	void commandRunOpen() throws InterruptedException {
		PlayerMock player1 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		assertTrue(player1.performCommand(cmd(false, "open", ".", ".")));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		performCommandWithAsync(player1, cmd(false, "open", ".", "."));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		addProfiles(player1, 1);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, cmd(false, "open", ".", "."));
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
	}

	@Test
	void databaseTest() throws ExecutionException, InterruptedException {
		PlayerMock player = addPlayer();
		BackpackUtils.getExtras(player.getUniqueId(), null).
				thenAccept(info -> assertEquals(Pair.of(0, 0), info)).get();
		addProfiles(player, 1);
		MMOProfile data = this.profileProvider.getPlayerData(player.getUniqueId());
		data.setCurrentProfileIfNotSet();
		UUID profileID = Objects.requireNonNull(data.getCurrent()).getUniqueId();
		BackpackUtils.updateExtrasPlayer(player.getUniqueId(), 4).
				thenCompose(v -> BackpackUtils.updateExtrasProfile(player.getUniqueId(), profileID, 2)).
				thenCompose(v -> BackpackUtils.getExtras(player.getUniqueId(), profileID)).
				thenAccept(info -> assertEquals(Pair.of(4, 2), info)).get();
		BackpackUtils.getItems(player.getUniqueId(), profileID).thenAccept(Assertions::assertNull).get();
		List<ItemStack> items1 = Arrays.asList(ItemStack.of(Material.DIRT), null, null, ItemStack.of(Material.GRASS_BLOCK), null, null, ItemStack.of(Material.DIRT)),
				items2 = new BackpackInfo(player.getUniqueId(), profileID, 0, 0, InventoryUtils.condenseItems(items1)).items();
		BackpackUtils.getItems(player.getUniqueId(), profileID).thenAccept(Assertions::assertNull).get();
		BackpackUtils.saveItems(player.getUniqueId(), profileID, items1).
				thenCompose(v -> BackpackUtils.getItems(player.getUniqueId(), profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(player.getUniqueId(), profileID, 0, 0, items).items()).
				thenAccept(items -> assertSameValues(items2, items)).get();
	}
}