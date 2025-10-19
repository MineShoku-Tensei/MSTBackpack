package me.DMan16;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.Objects;

public final class MySQLDatabase extends Database {
	public MySQLDatabase(@NotNull JavaPlugin plugin, @NotNull String host) throws SQLException, ClassNotFoundException, ConnectException {
		super(createURL(host, plugin.getConfig().getInt("port",0 ), Objects.requireNonNull(plugin.getConfig().getString("database", null))),
				plugin.getConfig().getString("username", null), plugin.getConfig().getString("password", null));
		prepareDatabase();
	}

	@NotNull
	private static String createURL(@NotNull String host, int port, @NotNull String database) {
		return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
	}

	@Nullable
	protected String jdbc() {
		return null;
	}

	@Override
	@NotNull
	protected String onConflictUpdateItems() {
		return "ON DUPLICATE KEY UPDATE Items=VALUES(Items)";
	}
}