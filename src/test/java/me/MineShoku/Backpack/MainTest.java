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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
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

	@Test
	void tabCompleteCorrect() {
		assertLinesMatch(CommandHandler.BASE, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(true)));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(this.server.getConsoleSender(), cmd(false, c)).size()));
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
		assertLinesMatch(CommandHandler.BASE, this.server.getCommandTabComplete(player1, cmd(true)));
		assertLinesMatch(Collections.emptyList(), this.server.getCommandTabComplete(player2, cmd(true)));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(1, this.server.getCommandTabComplete(player1, cmd(false, c)).size()));
		CommandHandler.BASE.stream().map(str -> str.charAt(0)).forEach(c -> assertSame(0, this.server.getCommandTabComplete(player2, cmd(false, c)).size()));
		List<String> withPlayers = CommandHandler.BASE.subList(2, CommandHandler.BASE.size());
		CommandHandler.BASE.subList(0, 2).forEach(sub -> assertSame(0, this.server.getCommandTabComplete(player1, cmd(true, sub)).size()));
		disconnect(player2);
		withPlayers.forEach(sub -> assertSame(2, this.server.getCommandTabComplete(player1, cmd(true, sub)).size()));
		withPlayers.forEach(sub -> assertSame(2, this.server.getCommandTabComplete(player1, cmd(true, sub)).size()));
		addProfiles(player1, 4);
		List<String> withoutPlayer = withPlayers.subList(0, 2), withPlayer = withPlayers.subList(2, withPlayers.size());
		withoutPlayer.forEach(sub -> assertSame(5, this.server.getCommandTabComplete(player1, cmd(true, sub, ".")).size()));
		withoutPlayer.forEach(sub -> assertSame(0, this.server.getCommandTabComplete(player1, cmd(true, sub, player2.getName())).size()));
		withPlayer.forEach(sub -> assertSame(0, this.server.getCommandTabComplete(player1, cmd(true, sub, "aaaa")).size()));
		withPlayer.forEach(sub -> assertSame(6, this.server.getCommandTabComplete(player1, cmd(true, sub, ".")).size()));
		withPlayer.forEach(sub -> assertSame(1, this.server.getCommandTabComplete(player1, cmd(true, sub, player2.getUniqueId())).size()));
	}

	void performCommandWithAsync(@NotNull PlayerMock player, @NotNull String command) throws InterruptedException {
		assertTrue(player.performCommand(command));
		Thread.sleep(100);
		this.server.getScheduler().performOneTick();
	}

	@Test
	void commandRunsCorrectly() throws InterruptedException {
		PlayerMock player1 = addPlayer(), player2 = addPlayer();
		PermissionAttachment attachment = player1.addAttachment(this.plugin);
		attachment.setPermission(PERMISSION, true);
		addProfiles(player1, 2);
		MMOProfile playerData = this.profileProvider.getPlayerData(player1.getUniqueId());
		playerData.setCurrentProfile(playerData.getProfiles().getFirst().getUniqueId());
		performCommandWithAsync(player1, COMMAND);
		assertNotNull(player1.getOpenInventory().getTopInventory());
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
		player1.closeInventory();
		performCommandWithAsync(player2, COMMAND);
		assertSame(InventoryType.CRAFTING, player2.getOpenInventory().getType());
		assertTrue(player1.performCommand(cmd(false, "open", ".", ".")));
		assertSame(InventoryType.CRAFTING, player1.getOpenInventory().getType());
		attachment.setPermission(CommandHandler.PERMISSION_ADVANCED, true);
		performCommandWithAsync(player1, cmd(false, "open", ".", "."));
		assertSame(InventoryType.CHEST, player1.getOpenInventory().getType());
	}
}