package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.tests.TestUtils;
import com.mineshoku.mstutils.tests.mock.MMOPlayerProfile;
import com.mineshoku.mstutils.tests.mock.MMOProfileProvider;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
	void tabCompleteBase() {
		TestUtils.assertSameValues(BackpackCommandHandler.BASE, this.server.getCommandTabComplete(this.server.getConsoleSender(), TestUtils.cmd(COMMAND, true)));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(this.server.getConsoleSender(), TestUtils.cmd(COMMAND, false, c)).size()));
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider), player2 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.assertSameValues(BackpackCommandHandler.BASE, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true)));
		TestUtils.assertSameValues(Collections.emptyList(), this.server.getCommandTabComplete(player2, TestUtils.cmd(COMMAND, true)));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, false, c)).size()));
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(0, this.server.getCommandTabComplete(player2, TestUtils.cmd(COMMAND, false, c)).size()));
	}

	void tabCompleteSubSimpleCheck(@NotNull String sub) {
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		assertSame(0, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub)).size());
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
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider), player2 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.disconnect(player2, this.profileProvider);
		TestUtils.assertSameValues(List.of(BackpackCommandHandler.CURRENT, player1.getName()), this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub)));
		int profiles = 4;
		TestUtils.addProfiles(this.profileProvider, player1, profiles);
		List<String> result = this.profileProvider.getPlayerData(player1.getUniqueId()).getProfiles().stream().map(MMOPlayerProfile::getUniqueId).map(UUID::toString).collect(Collectors.toList()),
				setPlayer = hasOptionNoProfile ? List.of(BackpackCommandHandler.EXTRAS_SET_PLAYER) : Collections.emptyList();
		result.add(BackpackCommandHandler.CURRENT);
		if (hasOptionNoProfile) {
			result.add(BackpackCommandHandler.EXTRAS_SET_PLAYER);
		}
		TestUtils.assertSameValues(result, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub, BackpackCommandHandler.CURRENT)));
		TestUtils.assertSameValues(setPlayer, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub, player2.getName())));
		TestUtils.assertSameValues(setPlayer, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub, player2.getUniqueId())));
		assertSame(0, this.server.getCommandTabComplete(player1, TestUtils.cmd(COMMAND, true, sub, "aaaa")).size());
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
		Thread.sleep(50);
		this.server.getScheduler().performTicks(Utils.secondsToTicks(1));
	}

	@Test
	void commandRunBase() throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider), player2 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(PERMISSION, true);
		TestUtils.addProfiles(this.profileProvider, player1, 2);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, COMMAND);
		assertNotNull(player1.getOpenInventory().getTopInventory());
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
		performCommandWithAsync(player2, COMMAND);
		assertSame(InventoryType.CRAFTING, player2.getOpenInventory().getType());
	}

	@Test
	void commandRunOpen() throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		assertTrue(player1.performCommand(TestUtils.cmd(COMMAND, false, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT)));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		performCommandWithAsync(player1, TestUtils.cmd(COMMAND, false, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		TestUtils.addProfiles(this.profileProvider, player1, 1);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, TestUtils.cmd(COMMAND, false, "open", BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT));
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
	}
}