package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.TestUtils;
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

import java.util.UUID;

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

	@Test
	void pageItem() {
		ItemStack item = new PageItem(Material.DIRT, "<page-2><page-1><page><page+1><page+2>", null, null, null, 0).toItemStack(4, 10, 0, 0, 0, 0);
		Component name = item.getItemMeta().itemName();
		assertInstanceOf(TextComponent.class, name);
		assertEquals("23456", ((TextComponent) name).content());
	}
}