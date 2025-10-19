package me.DMan16;

import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.ProfileList;
import fr.phoenixdevt.profiles.ProfileProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

public final class BackpackCommand implements CommandExecutor {
    private static final @NotNull String PERMISSION_EXTRA = "mst.backpack.extra.";
    private static final @Positive int PERMISSION_EXTRA_AMOUNT = 10;
    private static final @Positive int PERMISSION_EXTRA_BASE = 5;
    private static final @Positive int PERMISSION_EXTRA_PAGES_PER = 1;

	private final @NotNull JavaPlugin plugin;
	private final @NotNull Database database;
    private final @NotNull ProfileProvider<?> provider;

	public BackpackCommand(@NotNull JavaPlugin plugin, @NotNull Database database) throws SQLException, IOException, ClassNotFoundException {
		PluginCommand command = plugin.getCommand("backpack");
		assert command != null;
        command.setExecutor(this);
        this.plugin = plugin;
        this.database = database;
        this.provider = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(ProfileProvider.class)).getProvider();
        IntStream.rangeClosed(1, PERMISSION_EXTRA_AMOUNT).mapToObj(i -> PERMISSION_EXTRA + i).
                map(perm -> new Permission(perm, PermissionDefault.FALSE)).
                forEach(perm -> perm.addParent(PERMISSION_EXTRA + "*", true));
        Bukkit.reloadPermissions();
    }

	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
		if (!(sender instanceof Player player)) return true;
		UUID playerUUID = player.getUniqueId();
        ProfileList<?> playerData = this.provider.getPlayerData(playerUUID);
        PlayerProfile<?> currentProfile = playerData.getCurrent();
		if (currentProfile == null) {
            player.sendMessage(Component.text("Profile not found", NamedTextColor.RED));
            return true;
        }
        UUID profileUUID = currentProfile.getUniqueId();
        int maxPages = PERMISSION_EXTRA_BASE + (int) IntStream.rangeClosed(1, PERMISSION_EXTRA_AMOUNT).
                mapToObj(i -> PERMISSION_EXTRA + i).filter(player::hasPermission).count() * PERMISSION_EXTRA_PAGES_PER;
		try {
			Objects.requireNonNull(new BackpackMenu(this.plugin, player, profileUUID, maxPages, this.database).openInventory());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}