package me.DMan16;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public final class LocalDatabase extends Database {
	private final @NotNull File file;

	public LocalDatabase(@NotNull JavaPlugin plugin) throws ClassNotFoundException, IOException, SQLException {
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
		if (!this.file.exists()) {
			if (!this.file.createNewFile()) {
				throw new IOException("Couldn't create local DB file \"" + file + "\"");
			}
		}
	}

	@Override
	@NotNull
	protected String onConflictUpdateItems() {
		return "ON CONFLICT(PlayerUUID, ProfileUUID) DO UPDATE SET Items=excluded.Items";
	}
}