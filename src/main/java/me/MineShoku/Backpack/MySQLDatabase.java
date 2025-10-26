package me.MineShoku.Backpack;

import org.checkerframework.checker.index.qual.NonNegative;
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
	protected String onConflictUpdateItems() {
		return onConflictUpdate(COLUMN_ITEMS + "=VALUES(" + COLUMN_ITEMS + ")", COLUMN_PLAYER_ID, COLUMN_PROFILE_ID);
	}

	@NotNull
	protected String onConflictUpdateExtras(@NonNegative int max, boolean includeProfileID) {
		String[] keys = includeProfileID ? new String[]{COLUMN_PLAYER_ID, COLUMN_PROFILE_ID} : new String[]{COLUMN_PLAYER_ID};
		return onConflictUpdate(COLUMN_EXTRAS + "=LEAST(GREATEST(" + TABLE_PLAYERS + "." + COLUMN_EXTRAS + " + VALUES(" + COLUMN_EXTRAS + "), 0), " + max + ")", keys);
	}
}