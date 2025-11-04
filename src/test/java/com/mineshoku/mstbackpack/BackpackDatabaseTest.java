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
		BackpackUtils.getExtras(player.getUniqueId(), null).thenAccept(info -> assertEquals(Pair.of(0, 0), info)).get();
		TestUtils.addProfiles(this.profileProvider, player, 1);
		UUID profileID = this.profileProvider.getPlayerData(player.getUniqueId()).setCurrentProfileIfNotSet().getUniqueId();
		BackpackUtils.updateExtrasPlayer(player.getUniqueId(), 4).
				thenCompose(v -> BackpackUtils.updateExtrasProfile(player.getUniqueId(), profileID, 2)).
				thenCompose(v -> BackpackUtils.getExtras(player.getUniqueId(), profileID)).
				thenAccept(info -> assertEquals(Pair.of(4, 2), info)).get();
	}

	@Test
	void updateItems() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server, this.profileProvider);
		TestUtils.addProfiles(this.profileProvider, player, 1);
		UUID profileID = this.profileProvider.getPlayerData(player.getUniqueId()).setCurrentProfileIfNotSet().getUniqueId();
		BackpackUtils.getItems(player.getUniqueId(), profileID).thenAccept(Assertions::assertNull).get();
		List<ItemStack> items1 = Arrays.asList(ItemStack.of(Material.DIRT), null, null, ItemStack.of(Material.STONE), null, null, ItemStack.of(Material.DIRT)),
				items2 = new BackpackInfo(player.getUniqueId(), profileID, 0, 0, InventoryUtils.condenseItems(items1)).items();
		BackpackUtils.saveItems(player.getUniqueId(), profileID, items1).
				thenCompose(v -> BackpackUtils.getItems(player.getUniqueId(), profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(player.getUniqueId(), profileID, 0, 0, items).items()).
				thenAccept(items -> TestUtils.assertSameValues(items2, items)).get();
	}
}