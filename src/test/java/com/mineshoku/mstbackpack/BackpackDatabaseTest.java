package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.models.Pair;
import com.mineshoku.mstutils.tests.TestUtils;
import com.mineshoku.mstutils.tests.mock.MMOProfileProvider;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BackpackDatabaseTest {
	private ServerMock server;
	private MSTBackpack plugin;
	private MMOProfileProvider profileProvider;

	@BeforeEach
	void setUp() {
		this.server = MockBukkit.mock();
		this.profileProvider = TestUtils.getAndRegisterProfileProvider();
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

	@Test
	void updateExtras() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server, this.profileProvider);
		UUID playerID = player.getUniqueId();
		BackpackUtils.getExtras(playerID, null).thenAccept(info -> assertEquals(Pair.of(0, 0), info)).get();
		TestUtils.addProfiles(this.profileProvider, player, 1);
		UUID profileID = this.profileProvider.getPlayerData(playerID).setCurrentProfileIfNotSet().getUniqueId();
		BackpackUtils.updateExtrasPlayer(playerID, 4).
				thenCompose(v -> BackpackUtils.updateExtrasProfile(playerID, profileID, 2)).
				thenCompose(v -> BackpackUtils.getExtras(playerID, profileID)).
				thenAccept(info -> assertEquals(Pair.of(4, 2), info)).get();
	}

	@Test
	void updateItems() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server, this.profileProvider);
		UUID playerID = player.getUniqueId();
		TestUtils.addProfiles(this.profileProvider, player, 1);
		UUID profileID = this.profileProvider.getPlayerData(playerID).setCurrentProfileIfNotSet().getUniqueId();
		BackpackUtils.getItems(playerID, profileID).thenAccept(Assertions::assertNull).get();
		ItemStack dirt = ItemStack.of(Material.DIRT), stone = ItemStack.of(Material.STONE);
		List<ItemStack> items1 = Arrays.asList(dirt, null, null, stone, null, null, dirt),
				items2 = Arrays.asList(dirt, dirt, stone, stone, dirt, dirt, stone, stone);
		BackpackUtils.saveItems(playerID, profileID, items1).
				thenCompose(v -> BackpackUtils.getItems(playerID, profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(playerID, profileID, items).items()).
				thenAccept(items -> TestUtils.assertSameValues(InventoryUtils.condenseItems(items1), items)).get();
		BackpackUtils.saveItems(playerID, profileID, items2).
				thenCompose(v -> BackpackUtils.getItems(playerID, profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(playerID, profileID, items).items()).
				thenAccept(items -> TestUtils.assertSameValues(InventoryUtils.condenseItems(items2), items)).get();
	}
}