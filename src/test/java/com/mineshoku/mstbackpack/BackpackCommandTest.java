package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.TestUtils;
import com.mineshoku.mstutils.models.Pair;
import com.mineshoku.mstutils.tests.MockPlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class BackpackCommandTest {
	private static final String COMMAND = "backpack";
	private static final String PERMISSION = "mst.backpack.use";

	private ServerMock server;
	private MSTBackpack plugin;

	@BeforeEach
	void setUp() {
		this.server = MockBukkit.mock();
		TestUtils.loadMSTUtils();
		this.plugin = MockBukkit.load(MSTBackpack.class);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void pluginLoadsSuccessfully() {
		TestUtils.testPluginLoadsSuccessfully(this.plugin);
	}

	void testOpenInventory(@NotNull PlayerMock player, boolean backpack, @NotNull Object @NotNull ... extras) throws InterruptedException {
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, extras));
		assertSame(backpack ? InventoryType.CHEST : InventoryType.CRAFTING, player.getOpenInventory().getType());
		player.closeInventory();
	}

	@Test
	void commandRunBase() throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server);
		TestUtils.setPermission(this.plugin, player1, PERMISSION, true);
		TestUtils.addProfiles(player1, 2);
		assertNotNull(TestUtils.setCurrentProfileIfNotSet(player1));
		testOpenInventory(player1, true);
		testOpenInventory(player2, false);
	}

	@Test
	void commandRunOpen() throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server);
		UUID playerID1 = player1.getUniqueId(), playerID2 = player2.getUniqueId(), playerID3;
		do {
			playerID3 = UUID.randomUUID();
		} while (playerID3.equals(playerID1) || playerID3.equals(playerID2));
		testOpenInventory(player1, false, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT);
		TestUtils.setPermission(this.plugin, player1, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.setPermission(this.plugin, player2, PERMISSION, true);
		testOpenInventory(player1, false, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT);
		TestUtils.addProfiles(player1, 1);
		TestUtils.addProfiles(player2, 1);
		TestUtils.waitAsync(this.server);
		MockPlayerProfile profile1 = TestUtils.setCurrentProfileIfNotSet(player1), profile2 = TestUtils.setCurrentProfileIfNotSet(player2);
		assertNotNull(profile1);
		assertNotNull(profile2);
		UUID profileID1 = profile1.getUniqueId(), profileID2 = profile2.getUniqueId();
		assertNotEquals(profileID1, profileID2);
		testOpenInventory(player2, true, "open", playerID2);
		testOpenInventory(player2, true, "open", playerID2, BackpackCommandHandler.CURRENT);
		testOpenInventory(player2, true, "open", playerID2, profileID2);
		testOpenInventory(player1, false, "open");
		testOpenInventory(player1, true, "open", BackpackCommandHandler.CURRENT);
		testOpenInventory(player1, true, "open", playerID1);
		testOpenInventory(player1, true, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT);
		testOpenInventory(player1, true, "open", BackpackCommandHandler.CURRENT, profileID1);
		testOpenInventory(player1, false, "open", BackpackCommandHandler.CURRENT, profileID2);
		testOpenInventory(player1, true, "open", playerID2);
		testOpenInventory(player1, true, "open", playerID2, BackpackCommandHandler.CURRENT);
		testOpenInventory(player1, true, "open", playerID2, profileID2);
		testOpenInventory(player1, false, "open", playerID2, profileID1);
		testOpenInventory(player1, false, "open", playerID3);
		testOpenInventory(player1, false, "open", playerID3, BackpackCommandHandler.CURRENT);
		testOpenInventory(player1, false, "open", playerID3, profileID1);
		TestUtils.disconnect(player2);
		testOpenInventory(player1, false, "open", playerID2);
		testOpenInventory(player1, false, "open", playerID2, BackpackCommandHandler.CURRENT);
		testOpenInventory(player1, true, "open", playerID2, profileID2);
	}

	void checkExtras(@NotNull UUID playerID, @NotNull UUID profileID, int extrasPlayer, int extrasProfile) throws ExecutionException, InterruptedException {
		assertEquals(Pair.of(extrasPlayer, extrasProfile), this.plugin.backpackDatabase().getExtras(playerID, profileID).get());
	}

	@Test
	void commandRunUpgradeDowngrade() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server);
		TestUtils.setPermission(this.plugin, player, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.addProfiles(player, 1);
		UUID playerID = player.getUniqueId(), profileID = Objects.requireNonNull(TestUtils.setCurrentProfileIfNotSet(player)).getUniqueId();
		String playerName = player.getName();
		int playerMax = this.plugin.backpackConfig().amountExtraPlayerMax(), profileMax = this.plugin.backpackConfig().amountExtraProfileMax();
		TestUtils.waitAsync(this.server);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID));
		checkExtras(playerID, profileID, 1, 0);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerName));
		checkExtras(playerID, profileID, 2, 0);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID, BackpackCommandHandler.EXTRAS_SET_PLAYER));
		checkExtras(playerID, profileID, 3, 0);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerName, BackpackCommandHandler.EXTRAS_SET_PLAYER));
		checkExtras(playerID, profileID, 4, 0);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID, profileID));
		checkExtras(playerID, profileID, 4, 1);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerName, profileID));
		checkExtras(playerID, profileID, 4, 2);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID, profileID, 2));
		checkExtras(playerID, profileID, 4, 4);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID, profileID, 100));
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "upgrade", playerID, BackpackCommandHandler.EXTRAS_SET_PLAYER, 100));
		checkExtras(playerID, profileID, playerMax, profileMax);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID));
		checkExtras(playerID, profileID, playerMax - 1, profileMax);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerName));
		checkExtras(playerID, profileID, playerMax - 2, profileMax);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID, BackpackCommandHandler.EXTRAS_SET_PLAYER));
		checkExtras(playerID, profileID, playerMax - 3, profileMax);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerName, BackpackCommandHandler.EXTRAS_SET_PLAYER));
		checkExtras(playerID, profileID, playerMax - 4, profileMax);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID, profileID));
		checkExtras(playerID, profileID, playerMax - 4, profileMax - 1);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerName, profileID));
		checkExtras(playerID, profileID, playerMax - 4, profileMax - 2);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID, profileID, 2));
		checkExtras(playerID, profileID, playerMax - 4, profileMax - 4);
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID, profileID, 100));
		TestUtils.performCommandWithAsync(this.server, player, TestUtils.cmd(COMMAND, false, "downgrade", playerID, BackpackCommandHandler.EXTRAS_SET_PLAYER, 100));
		checkExtras(playerID, profileID, 0, 0);
	}

	@Test
	void commandRunInfo() throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server);
		TestUtils.setPermission(this.plugin, player1, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.setPermission(this.plugin, player2, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.addProfiles(player1, 1);
		UUID playerID = player1.getUniqueId(), profileID = Objects.requireNonNull(TestUtils.setCurrentProfileIfNotSet(player1)).getUniqueId();
		int maxPlayer = this.plugin.backpackConfig().amountExtraPlayerMax(), maxProfile = this.plugin.backpackConfig().amountExtraProfileMax();
		String playerName = player1.getName();
		TestUtils.waitAsync(this.server);
		Component msg0Player = this.plugin.backpackConfig().messageExtrasInfo(playerID, playerName, null, 0, 0),
				msg0Profile = this.plugin.backpackConfig().messageExtrasInfo(playerID, playerName, profileID, 0, 0),
				msgPlayer = this.plugin.backpackConfig().messageExtrasInfo(playerID, playerName, null, maxPlayer, 0),
				msgProfile = this.plugin.backpackConfig().messageExtrasInfo(playerID, playerName, profileID, maxPlayer, maxProfile);
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "info", playerID));
		assertEquals(msg0Player, player1.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "info", playerName));
		assertEquals(msg0Player, player1.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "info", playerID, profileID));
		assertEquals(msg0Profile, player1.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "info", playerName, profileID));
		assertEquals(msg0Profile, player1.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "upgrade", playerID, profileID, 100));
		TestUtils.performCommandWithAsync(this.server, player1, TestUtils.cmd(COMMAND, false, "upgrade", playerID, BackpackCommandHandler.EXTRAS_SET_PLAYER, 100));
		TestUtils.disconnect(player1);
		TestUtils.performCommandWithAsync(this.server, player2, TestUtils.cmd(COMMAND, false, "info", playerID));
		assertEquals(msgPlayer, player2.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player2, TestUtils.cmd(COMMAND, false, "info", playerName));
		assertEquals(msgPlayer, player2.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player2, TestUtils.cmd(COMMAND, false, "info", playerID, profileID));
		assertEquals(msgProfile, player2.nextComponentMessage());
		TestUtils.performCommandWithAsync(this.server, player2, TestUtils.cmd(COMMAND, false, "info", playerName, profileID));
		assertEquals(msgProfile, player2.nextComponentMessage());
	}

	@Test
	void pageItem() {
		ItemStack item = new PageItem(Material.DIRT, "<page-2><page-1><page><page+1><page+2>", null, null, null, 0).toItemStack(4, 10, 0, 0, 0, 0);
		Component name = item.getItemMeta().itemName();
		assertInstanceOf(TextComponent.class, name);
		assertEquals("23456", ((TextComponent) name).content());
	}
}