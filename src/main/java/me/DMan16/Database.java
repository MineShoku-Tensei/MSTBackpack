package me.DMan16;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class Database {
	private static final @NotNull String POOL_NAME = "MSTBackpack";

	private final @NotNull HikariConfig hikariConfig;
	private @Nullable HikariDataSource hikari;

	protected Database(@NotNull String url, @Nullable String username, @Nullable String password) throws ClassNotFoundException {
		String jdbc = jdbc();
		if (jdbc != null) {
			Class.forName(jdbc);
		}
		this.hikariConfig = createHikariConfig(url, username, password);
	}

	@Nullable protected abstract String jdbc();

	@NotNull
	protected HikariConfig createHikariConfig(@NotNull String url, @Nullable String username, @Nullable String password) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setPoolName(POOL_NAME);
		if (username != null) {
			hikariConfig.setUsername(username);
		}
		if (password != null) {
			hikariConfig.setPassword(password);
		}
		hikariConfig.setJdbcUrl(url);
		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
		hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
		hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
		hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
		hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
		hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
		hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
		hikariConfig.setMaximumPoolSize(20);
		return hikariConfig;
	}

	protected final void prepareDatabase() throws SQLException {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE IF NOT EXISTS " + table() + " (" + """
                        PlayerUUID VARCHAR(36) NOT NULL,
                        ProfileUUID VARCHAR(36) NOT NULL,
                        Items LONGBLOB NOT NULL,
                        PRIMARY KEY (PlayerUUID, ProfileUUID)
                    );
                    """);
		}
	}

	@NotNull
	protected final String table() {
		return "MSTBackpack";
	}

	public final void close() {
		if (hikari == null) return;
		hikari.close();
		hikari = null;
	}

	private void connect() {
		if (this.hikari == null || this.hikari.isClosed()) {
			this.hikari = new HikariDataSource(this.hikariConfig);
		}
	}

	@NotNull
	public final Connection getConnection() throws SQLException {
		connect();
		assert hikari != null;
		return hikari.getConnection();
	}

	@Nullable
	public List<@Nullable ItemStack> getItems(@NotNull UUID playerID, @NotNull UUID profileID) throws SQLException {
		try (Connection connection = getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("SELECT Items FROM " + table() +
					" WHERE PlayerUUID=? AND ProfileUUID=?;")) {
				statement.setString(1, playerID.toString());
				statement.setString(2, profileID.toString());
				try (ResultSet result = statement.executeQuery()) {
					return result.next() ? Arrays.asList(ItemStack.deserializeItemsFromBytes(result.getBytes(1))) : null;
				}
			}
		}
	}

	@NotNull protected abstract String onConflictUpdateItems();

	public final void saveItems(@NotNull UUID playerID, @NotNull UUID profileID, @NotNull List<@Nullable ItemStack> items) throws SQLException {
		try (Connection connection = getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table() +
					" (PlayerUUID, ProfileUUID, Items) VALUES (?, ?, ?) " + onConflictUpdateItems() + ";")) {
				byte[] bytes = ItemStack.serializeItemsAsBytes(items);
				statement.setString(1, playerID.toString());
				statement.setString(2, profileID.toString());
				statement.setBytes(3, bytes);
				statement.executeUpdate();
			}
		}
	}
}