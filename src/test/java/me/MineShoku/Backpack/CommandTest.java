package me.MineShoku.Backpack;

import fr.phoenixdevt.profiles.ProfileProvider;
import me.MineShoku.Backpack.Mock.MMOPlayerProfile;
import me.MineShoku.Backpack.Mock.MMOProfile;
import me.MineShoku.Backpack.Mock.MMOProfileProvider;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {
	private static final String COMMAND = "backpack";
	private static final String PERMISSION = "mst.backpack.use";

	private ServerMock server;
	private Main plugin;
	private MMOProfileProvider profileProvider;

	@BeforeEach
	void setUp() {
		this.server = MockBukkit.mock();
		this.profileProvider = new MMOProfileProvider();
		Bukkit.getServicesManager().register(ProfileProvider.class, this.profileProvider, MockBukkit.createMockPlugin("MMOProfiles"), ServicePriority.Normal);
		this.plugin = MockBukkit.load(Main.class);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void pluginLoadsSuccessfully() {
		assertNotNull(this.plugin, "Plugin should be loaded by MockBukkit");
		assertTrue(this.plugin.isEnabled(), "Plugin should be enabled after load");
		assertSame(this.plugin.getServer(), Bukkit.getServer(), "Bukkit server should be same as plugin's");
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

	void assertSameValues(@NotNull Collection<String> expectedList, @NotNull Collection<String> actualList) {
		assertSame(expectedList.size(), actualList.size());
		assertTrue(actualList.containsAll(expectedList));
	}

	@Test
	void tabCompleteBase() {
		assertSameValues(CommandHandler.BASE, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(true)));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(false, c)).size()));
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
		assertSameValues(CommandHandler.BASE, this.server.getCommandTabComplete(player1, cmd(true)));
		assertSameValues(Collections.emptyList(), this.server.getCommandTabComplete(player2, cmd(true)));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(player1, cmd(false, c)).size()));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(0, this.server.getCommandTabComplete(player2, cmd(false, c)).size()));
	}

	void tabCompleteSubSimpleCheck(@NotNull String sub) {
		PlayerMock player1 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
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
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
		disconnect(player2);
		assertSameValues(List.of(CommandHandler.EXTRAS_CURRENT, player1.getName()), this.server.getCommandTabComplete(player1, cmd(true, sub)));
		int profiles = 4;
		addProfiles(player1, profiles);
		List<String> result = this.profileProvider.getPlayerData(player1.getUniqueId()).getProfiles().stream().map(MMOPlayerProfile::getUniqueId).map(UUID::toString).collect(Collectors.toList()),
				setPlayer = hasOptionNoProfile ? List.of(CommandHandler.EXTRAS_SET_PLAYER) : Collections.emptyList();
		result.add(CommandHandler.EXTRAS_CURRENT);
		if (hasOptionNoProfile) {
			result.add(CommandHandler.EXTRAS_SET_PLAYER);
		}
		assertSameValues(result, this.server.getCommandTabComplete(player1, cmd(true, sub, CommandHandler.EXTRAS_CURRENT)));
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
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
		performCommandWithAsync(player1, cmd(false, "open", ".", "."));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		addProfiles(player1, 1);
		this.profileProvider.getPlayerData(player1.getUniqueId()).setCurrentProfileIfNotSet();
		performCommandWithAsync(player1, cmd(false, "open", ".", "."));
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
	}
}
