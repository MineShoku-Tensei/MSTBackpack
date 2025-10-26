package me.DMan16;

import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public final class LocalDatabase extends Database {
	private final @NotNull File file;

	public LocalDatabase(@NotNull Main plugin) throws ClassNotFoundException, IOException, SQLException {
		super(createURL(createFile(plugin)), null, null);
		this.file = createFile(plugin);
		initiateDatabase();
		prepareDatabase();
	}

	@NotNull
	private static File createFile(@NotNull JavaPlugin plugin) {
		return new File(plugin.getDataFolder(), "database.db");
	}

	@NotNull
	protected String jdbc() {
		return "org.sqlite.JDBC";
	}

	@NotNull
	private static String createURL(@NotNull File file) {
		return "jdbc:sqlite:" + file;
	}

	private void initiateDatabase() throws IOException {
		if (!this.file.exists() && !this.file.createNewFile()) {
			throw new IOException("Couldn't create local DB file \"" + file + "\"");
		}
	}

	@NotNull
	protected String onConflictPrefix(@NotNull String @NotNull ... keys) {
		return "ON CONFLICT(" + String.join(", ", keys) + ") DO UPDATE SET";
	}

	@NotNull
	protected String onConflictUpdateItems() {
		return onConflictUpdate(COLUMN_ITEMS + "=excluded." + COLUMN_ITEMS, COLUMN_PLAYER_ID, COLUMN_PROFILE_ID);
	}

	@NotNull
	protected String onConflictUpdateExtras(@NonNegative int max, boolean includeProfileID) {
		String[] keys = includeProfileID ? new String[]{COLUMN_PLAYER_ID, COLUMN_PROFILE_ID} : new String[]{COLUMN_PLAYER_ID};
		return onConflictUpdate(COLUMN_EXTRAS + "=MIN(MAX(" + TABLE_PLAYERS + "." + COLUMN_EXTRAS + " + excluded." + COLUMN_EXTRAS + "), 0), " + max + ")", keys);
	}
}