package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.tests.TestUtils;
import com.mineshoku.mstutils.tests.mock.MMOPlayerProfile;
import com.mineshoku.mstutils.tests.mock.MMOProfileProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
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

	@NotNull
	List<String> tabComplete(@NotNull CommandSender sender, @NotNull String cmd) {
		return this.server.getCommandTabComplete(sender, cmd);
	}

	void testStreamBase(int expected, @NotNull CommandSender sender) {
		BackpackCommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(expected,
				tabComplete(sender, TestUtils.cmd(COMMAND, false, c)).size()));
	}

	@NotNull
	CommandSender console() {
		return this.server.getConsoleSender();
	}

	@Test
	void tabCompleteBase() {
		TestUtils.assertSameValues(BackpackCommandHandler.BASE, tabComplete(console(), TestUtils.cmd(COMMAND, true)));
		testStreamBase(1, console());
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider),
				player2 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.assertSameValues(BackpackCommandHandler.BASE, tabComplete(player1, TestUtils.cmd(COMMAND, true)));
		TestUtils.assertSameValues(Collections.emptyList(), tabComplete(player2, TestUtils.cmd(COMMAND, true)));
		testStreamBase(1, player1);
		testStreamBase(0, player2);
	}

	void tabCompleteSubSimpleCheck(@NotNull String sub) {
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		assertSame(0, tabComplete(player1, TestUtils.cmd(COMMAND, true, sub)).size());
	}

	@Test
	void tabCompleteHelp() {
		tabCompleteSubSimpleCheck("help");
	}

	@Test
	void tabCompleteReload() {
		tabCompleteSubSimpleCheck("reload");
	}

	void testSubAdvanced(@NotNull List<@NotNull String> list, @NotNull PlayerMock player,
						 @NotNull String sub, @NotNull Object arg) {
		TestUtils.assertSameValues(list, tabComplete(player, TestUtils.cmd(COMMAND, true, sub, arg)));
	}

	void tabCompleteSubAdvancedCheck(@NotNull String sub, boolean hasNoProfile) {
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider),
				player2 = TestUtils.addPlayer(this.server, this.profileProvider);
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.disconnect(player2, this.profileProvider);
		TestUtils.assertSameValues(List.of(BackpackCommandHandler.CURRENT, player1.getName()),
				tabComplete(player1, TestUtils.cmd(COMMAND, true, sub)));
		TestUtils.addProfiles(this.profileProvider, player1, 4);
		List<String> result = this.profileProvider.getPlayerData(player1.getUniqueId()).getProfiles().stream().
				map(MMOPlayerProfile::getUniqueId).map(UUID::toString).collect(Collectors.toList()),
				setPlayer = hasNoProfile ? List.of(BackpackCommandHandler.EXTRAS_SET_PLAYER) : Collections.emptyList();
		result.add(BackpackCommandHandler.CURRENT);
		if (hasNoProfile) {
			result.add(BackpackCommandHandler.EXTRAS_SET_PLAYER);
		}
		testSubAdvanced(result, player1, sub, BackpackCommandHandler.CURRENT);
		testSubAdvanced(setPlayer, player1, sub, player2.getName());
		testSubAdvanced(setPlayer, player1, sub, player2.getUniqueId());
		assertSame(0, tabComplete(player1, TestUtils.cmd(COMMAND, true, sub, "aaaa")).size());
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
		PlayerMock player1 = TestUtils.addPlayer(this.server, this.profileProvider),
				player2 = TestUtils.addPlayer(this.server, this.profileProvider);
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
		assertTrue(player1.performCommand(TestUtils.cmd(COMMAND, false, "open",
				BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT)));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		attachment.setPermission(BackpackCommandHandler.PERMISSION_ADVANCED, true);
		performCommandWithAsync(player1, TestUtils.cmd(COMMAND, false, "open",
				BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		TestUtils.addProfiles(this.profileProvider, player1, 1);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, TestUtils.cmd(COMMAND, false, "open",
				BackpackCommandHandler.CURRENT, BackpackCommandHandler.CURRENT));
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
	}

	@Test
	void pageItem() {
		ItemStack item = new PageItem(Material.DIRT, "<page-2><page-1><page><page+1><page+2>", null, null, null, 0).
				toItemStack(4, 10, 0, 0, 0, 0);
		Component name = item.getItemMeta().itemName();
		assertInstanceOf(TextComponent.class, name);
		assertEquals("23456", ((TextComponent) name).content());
	}
}