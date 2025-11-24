package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.TestUtils;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.managers.PlayersCache;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BackpackTabCompleteTest {
	private static final String COMMAND = "backpack";

	private ServerMock server;
	private MSTBackpack plugin;

	@BeforeEach
	void setUp() {
		this.server = MockBukkit.mock();
		TestUtils.loadUtilsPlugin();
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
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server);
		TestUtils.setPermission(this.plugin, player1, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		TestUtils.assertSameValues(BackpackCommandHandler.BASE, tabComplete(player1, TestUtils.cmd(COMMAND, true)));
		TestUtils.assertSameValues(Collections.emptyList(), tabComplete(player2, TestUtils.cmd(COMMAND, true)));
		testStreamBase(1, player1);
		testStreamBase(0, player2);
	}

	void tabCompleteSubSimpleCheck(@NotNull String sub) {
		PlayerMock player1 = TestUtils.addPlayer(this.server);
		TestUtils.setPermission(this.plugin, player1, BackpackCommandHandler.PERMISSION_ADVANCED, true);
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

	void tabCompleteSubAdvanced(@NotNull List<@NotNull String> list, @NotNull PlayerMock player, @NotNull String sub, @NotNull Object arg) {
		TestUtils.assertSameValues(list, tabComplete(player, TestUtils.cmd(COMMAND, true, sub, arg)));
	}

	@NotNull
	@Unmodifiable
	List<String> collectTabComplete(@Nullable Collection<?> collection, boolean addCurrent, boolean addNoProfile) {
		Stream<String> stream = collection == null ? Stream.empty() : collection.stream().map(Object::toString);
		if (addNoProfile) {
			stream = Stream.concat(Stream.of(BackpackCommandHandler.EXTRAS_SET_PLAYER), stream);
		}
		if (addCurrent) {
			stream = Stream.concat(Stream.of(BackpackCommandHandler.CURRENT), stream);
		}
		return stream.toList();
	}

	void tabCompleteSubAdvancedCheck(@NotNull String sub, boolean hasNoProfile) throws InterruptedException {
		PlayerMock player1 = TestUtils.addPlayer(this.server), player2 = TestUtils.addPlayer(this.server), player3 = TestUtils.addPlayer(this.server);
		TestUtils.waitAsync(this.server);
		TestUtils.setPermission(this.plugin, player1, BackpackCommandHandler.PERMISSION_ADVANCED, true);
		LinkedHashSet<UUID> profiles1 = TestUtils.addProfiles(player1, 4), profiles2 = TestUtils.addProfiles(player2, 2);
		assertSame(4, profiles1.size());
		assertSame(2, profiles2.size());
		TestUtils.waitAsync(this.server);
		TestUtils.disconnect(player2);
		assertNotNull(PlayersCache.instance().byIDCached(player1.getUniqueId()));
		assertNotNull(PlayersCache.instance().byIDCached(player2.getUniqueId()));
		assertNotNull(PlayersCache.instance().byIDCached(player3.getUniqueId()));
		TestUtils.assertSameValues(collectTabComplete(List.of(player1.getName(), player2.getName(), player3.getName()), true, false),
				tabComplete(player1, TestUtils.cmd(COMMAND, true, sub)));
		List<String> results1 = collectTabComplete(profiles1, true, hasNoProfile), results2 = collectTabComplete(profiles2, false, hasNoProfile),
				setPlayerCurrent = collectTabComplete(null, true, hasNoProfile), setPlayer = collectTabComplete(null, false, hasNoProfile);
		tabCompleteSubAdvanced(results1, player1, sub, BackpackCommandHandler.CURRENT);
		tabCompleteSubAdvanced(results1, player1, sub, player1.getUniqueId());
		tabCompleteSubAdvanced(results1, player1, sub, player1.getName());
		tabCompleteSubAdvanced(results1, player1, sub, TextUtils.toLowerCase(player1.getName()));
		tabCompleteSubAdvanced(results2, player1, sub, player2.getUniqueId());
		tabCompleteSubAdvanced(results2, player1, sub, player2.getName());
		tabCompleteSubAdvanced(results2, player1, sub, TextUtils.toLowerCase(player2.getName()));
		tabCompleteSubAdvanced(setPlayerCurrent, player1, sub, player3.getUniqueId());
		tabCompleteSubAdvanced(setPlayerCurrent, player1, sub, player3.getName());
		tabCompleteSubAdvanced(setPlayerCurrent, player1, sub, TextUtils.toLowerCase(player3.getName()));
		assertTrue(tabComplete(player1, TestUtils.cmd(COMMAND, true, sub, "aaaa")).isEmpty());
		TestUtils.disconnect(player3);
		TestUtils.waitAsync(this.server);
		tabCompleteSubAdvanced(setPlayer, player1, sub, player3.getUniqueId());
		tabCompleteSubAdvanced(setPlayer, player1, sub, player3.getName());
		tabCompleteSubAdvanced(setPlayer, player1, sub, TextUtils.toLowerCase(player3.getName()));
		assertTrue(tabComplete(player1, TestUtils.cmd(COMMAND, true, sub, "aaaa")).isEmpty());
	}

	@Test
	void tabCompleteOpen() throws InterruptedException {
		tabCompleteSubAdvancedCheck("open", false);
	}

	@Test
	void tabCompleteClear() throws InterruptedException {
		tabCompleteSubAdvancedCheck("clear", false);
	}

	@Test
	void tabCompleteUpgrade() throws InterruptedException {
		tabCompleteSubAdvancedCheck("upgrade", true);
	}

	@Test
	void tabCompleteDowngrade() throws InterruptedException {
		tabCompleteSubAdvancedCheck("downgrade", true);
	}
}