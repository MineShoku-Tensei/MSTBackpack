package me.MineShoku.Backpack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Objects;

public final class MySQLDatabase extends Database {
	public MySQLDatabase(@NotNull Main plugin, @NotNull String host) throws SQLException, ClassNotFoundException {
		super(createURL(host, plugin.config().port(), Objects.requireNonNull(plugin.config().database())), plugin.config().username(), plugin.config().password());
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

	@NotNull
	protected String onConflictPrefix(@NotNull String @NotNull ... keys) {
		return "ON DUPLICATE KEY UPDATE";
	}

	@NotNull
	protected String fromConflict(@NotNull String column) {
		return "VALUES(" + column + ")";
	}

	@NotNull
	protected String functionMin() {
		return "LEAST";
	}

	@NotNull
	protected String functionMax() {
		return "GREATEST";
	}
}