package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.InventoryUtils;
import com.mineshoku.mstutils.TestUtils;
import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.models.Pair;
import com.mineshoku.mstutils.tests.MockPlayerProfile;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class BackpackDatabaseTest {
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

	@Test
	void caching() throws InterruptedException, ExecutionException {
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server),
				player3 = TestUtils.addPlayer(this.server), player4 = TestUtils.addPlayer(this.server);
		LinkedHashSet<UUID> profiles1 = TestUtils.addProfiles(player1, 1), profiles2 = TestUtils.addProfiles(player2, 2),
				profiles3 = new LinkedHashSet<>(), profiles4 = TestUtils.addProfiles(player4, 4);
		assertSame(1, profiles1.size());
		assertSame(2, profiles2.size());
		assertSame(4, profiles4.size());
		TestUtils.waitAsync(this.server);
		TestUtils.disconnect(player1);
		this.plugin.backpackDatabase().getProfiles().thenCombine(this.plugin.backpackDatabase().getPlayers(), (profilesMap, players) -> {
			TestUtils.assertSameValues(List.of(player1.getUniqueId(), player2.getUniqueId(), player4.getUniqueId()), profilesMap.keySet());
			TestUtils.assertSameValues(List.of(player1.getUniqueId(), player2.getUniqueId(), player3.getUniqueId(), player4.getUniqueId()), players);
			players.forEach(playerID -> profilesMap.putIfAbsent(playerID, new LinkedHashSet<>()));
			return profilesMap;
		}).thenAccept(map -> {
			assertSame(4, map.size());
			TestUtils.assertSameValues(profiles1, map.get(player1.getUniqueId()));
			TestUtils.assertSameValues(profiles2, map.get(player2.getUniqueId()));
			TestUtils.assertSameValues(profiles3, map.get(player3.getUniqueId()));
			TestUtils.assertSameValues(profiles4, map.get(player4.getUniqueId()));
		}).get();
		TestUtils.disconnect(player2);
		TestUtils.assertSameValues(profiles1, this.plugin.cacheListener().profiles(player1.getUniqueId()));
		TestUtils.assertSameValues(profiles2, this.plugin.cacheListener().profiles(player2.getUniqueId()));
		TestUtils.assertSameValues(profiles3, this.plugin.cacheListener().profiles(player3.getUniqueId()));
		TestUtils.assertSameValues(profiles4, this.plugin.cacheListener().profiles(player4.getUniqueId()));
	}

	@Test
	void updateExtras() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server);
		UUID playerID = player.getUniqueId();
		BackpackUtils.getExtras(playerID, null).thenAccept(info -> assertEquals(Pair.of(0, 0), info)).get();
		TestUtils.addProfiles(player, 1);
		MockPlayerProfile profile1 = TestUtils.setCurrentProfileIfNotSet(player);
		assertNotNull(profile1);
		UUID profileID = profile1.getUniqueId();
		BackpackUtils.updateExtrasPlayer(playerID, 4).
				thenCompose(v -> BackpackUtils.updateExtrasProfile(playerID, profileID, 2)).
				thenCompose(v -> BackpackUtils.getExtras(playerID, profileID)).
				thenAccept(info -> assertEquals(Pair.of(4, 2), info)).get();
	}

	@Test
	void updateItems() throws ExecutionException, InterruptedException {
		PlayerMock player = TestUtils.addPlayer(this.server);
		UUID playerID = player.getUniqueId();
		TestUtils.addProfiles(player, 1);
		MockPlayerProfile profile1 = TestUtils.setCurrentProfileIfNotSet(player);
		assertNotNull(profile1);
		UUID profileID = profile1.getUniqueId();
		BackpackUtils.getItems(playerID, profileID).thenAccept(Assertions::assertNull).get();
		ItemStack dirt = ItemStack.of(Material.DIRT), stone = ItemStack.of(Material.STONE);
		List<ItemStack> items1 = Arrays.asList(dirt, null, null, stone, null, null, dirt),
				items2 = Arrays.asList(dirt, dirt, stone, stone, dirt, dirt, stone, stone);
		LoggingManager.exceptionallyLog(this.plugin, BackpackUtils.saveItems(playerID, profileID, items1).
				thenCompose(v -> BackpackUtils.getItems(playerID, profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(playerID, profileID, items).items()).
				thenAccept(items -> TestUtils.assertSameValues(InventoryUtils.condenseItems(items1), items))).get();
		LoggingManager.exceptionallyLog(this.plugin, BackpackUtils.saveItems(playerID, profileID, items2).
				thenCompose(v -> BackpackUtils.getItems(playerID, profileID)).
				thenApply(items -> items == null ? null : InventoryUtils.condenseItems(items)).
				thenApply(items -> new BackpackInfo(playerID, profileID, items).items()).
				thenAccept(items -> TestUtils.assertSameValues(InventoryUtils.condenseItems(items2), items))).get();
	}
}