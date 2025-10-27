package me.MineShoku.Backpack;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

public abstract class Database {
	private static final @NotNull String POOL_NAME = "MSTBackpack";
	protected static final @NotNull String TABLE_PLAYERS = "MSTBackpackPlayers";
	protected static final @NotNull String TABLE_PROFILES = "MSTBackpackProfiles";
	protected static final @NotNull String COLUMN_PLAYER_ID = "PlayerUUID";
	protected static final @NotNull String COLUMN_PROFILE_ID = "ProfileUUID";
	protected static final @NotNull String COLUMN_ITEMS = "Items";
	protected static final @NotNull String COLUMN_EXTRAS = "Extras";
	private static final byte @NotNull [] NULL_ITEMS_BYTES = ItemStack.serializeItemsAsBytes(Collections.emptyList());
	private static final @NotNull String STATEMENT_GET_EXTRAS_PLAYER = "SELECT " + COLUMN_EXTRAS + " FROM " + TABLE_PLAYERS + " WHERE " + COLUMN_PLAYER_ID + "=?;";
	private static final @NotNull String STATEMENT_GET_EXTRAS_PROFILE = "SELECT " + COLUMN_EXTRAS + " FROM " + TABLE_PROFILES + " WHERE " + COLUMN_PLAYER_ID + "=? AND " + COLUMN_PROFILE_ID + "=?;";
	private static final @NotNull String STATEMENT_GET_INFO = "SELECT " + COLUMN_ITEMS + ", " + COLUMN_EXTRAS + " FROM " + TABLE_PROFILES + " WHERE " + COLUMN_PLAYER_ID + "=? AND " + COLUMN_PROFILE_ID + "=?;";

	private final @NotNull HikariConfig hikariConfig;
	private final @NotNull String statementSaveItems;
	private final @NotNull String statementSaveExtrasPlayer;
	private final @NotNull String statementSaveExtrasProfile;
	private @Nullable HikariDataSource hikari;

	protected Database(@NotNull String url, @Nullable String username, @Nullable String password) throws ClassNotFoundException {
		String jdbc = jdbc();
		if (jdbc != null) {
			Class.forName(jdbc);
		}
		this.hikariConfig = createHikariConfig(url, username, password);
		this.statementSaveItems = "INSERT INTO " + TABLE_PROFILES +
				" (" + COLUMN_PLAYER_ID + ", " + COLUMN_PROFILE_ID + ", " + COLUMN_ITEMS + ") VALUES (?, ?, ?) " +
				onConflictUpdate(COLUMN_ITEMS + "=" + fromConflict(COLUMN_ITEMS), COLUMN_PLAYER_ID, COLUMN_PROFILE_ID) + ";";
		this.statementSaveExtrasPlayer = "INSERT INTO " + TABLE_PLAYERS +
				" (" + COLUMN_PLAYER_ID + ", " + COLUMN_EXTRAS + ") VALUES (?, ?) " +
				onConflictUpdate(COLUMN_EXTRAS + "=" + functionMin() + "(" + functionMax() + "(" + TABLE_PLAYERS + "." + COLUMN_EXTRAS + " + " + fromConflict(COLUMN_EXTRAS) + ", ?), ?)", COLUMN_PLAYER_ID) + ";";
		this.statementSaveExtrasProfile = "INSERT INTO " + TABLE_PROFILES +
				" (" + COLUMN_PLAYER_ID + ", " + COLUMN_PROFILE_ID + ", " + COLUMN_ITEMS + ", " + COLUMN_EXTRAS + ") VALUES (?, ?, ?, ?) " +
				onConflictUpdate(COLUMN_EXTRAS + "=" + functionMin() + "(" + functionMax() + "(" + TABLE_PROFILES + "." + COLUMN_EXTRAS + " + " + fromConflict(COLUMN_EXTRAS) + ", ?), ?)", COLUMN_PLAYER_ID, COLUMN_PROFILE_ID) + ";";
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
			statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYERS + " (" +
					COLUMN_PLAYER_ID + " VARCHAR(36) NOT NULL PRIMARY KEY, " +
					COLUMN_EXTRAS + " INT UNSIGNED NOT NULL DEFAULT 0" +
					");");
			statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PROFILES + " (" +
					COLUMN_PLAYER_ID + " VARCHAR(36) NOT NULL, " +
					COLUMN_PROFILE_ID + " VARCHAR(36) NOT NULL, " +
					COLUMN_ITEMS + " LONGBLOB NOT NULL, " +
					COLUMN_EXTRAS + " INT UNSIGNED NOT NULL DEFAULT 0, " +
					"PRIMARY KEY (" + COLUMN_PLAYER_ID + ", " + COLUMN_PROFILE_ID + ")" +
					");");
		}
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

	@NonNegative
	private int getExtrasPlayer(@NotNull Connection connection, @NotNull UUID playerID) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_EXTRAS_PLAYER)) {
			statement.setString(1, playerID.toString());
			try (ResultSet result = statement.executeQuery()) {
				return result.next() ? result.getInt(1) : 0;
			}
		}
	}

	@NonNegative
	private int getExtrasProfile(@NotNull Connection connection, @NotNull UUID playerID, @NotNull UUID profileID) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_EXTRAS_PROFILE)) {
			statement.setString(1, playerID.toString());
			statement.setString(2, profileID.toString());
			try (ResultSet result = statement.executeQuery()) {
				return result.next() ? result.getInt(1) : 0;
			}
		}
	}

	@Nullable
	public final Info getInfo(@NotNull UUID playerID, @NotNull UUID profileID) throws SQLException {
		try (Connection connection = getConnection()) {
			List<ItemStack> items = null;
			int extrasProfile = 0;
			try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_INFO)) {
				statement.setString(1, playerID.toString());
				statement.setString(2, profileID.toString());
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						Object bytes = result.getObject(1);
						if (bytes != null) {
							items = Arrays.stream(ItemStack.deserializeItemsFromBytes(result.getBytes(1))).
									map(item -> item.isEmpty() ? null : item).toList();
							if (items.stream().noneMatch(Objects::nonNull)) {
								items = null;
							}
						}
						extrasProfile = result.getInt(2);
					}
				}
			}
			int extrasPlayer = getExtrasPlayer(connection, playerID);
			return new Info(playerID, profileID, items, extrasPlayer, extrasProfile);
		}
	}

	@NotNull protected abstract String onConflictPrefix(@NotNull String @NotNull ... keys);

	@NotNull
	protected final String onConflictUpdate(@NotNull String updateLogic, @NotNull String @NotNull ... keys) {
		return onConflictPrefix(keys) + " " + updateLogic;
	}

	@NotNull protected abstract String fromConflict(@NotNull String column);

	@NotNull protected abstract String functionMin();

	@NotNull protected abstract String functionMax();

	public final void saveItems(@NotNull Info info) throws SQLException {
		try (Connection connection = getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(this.statementSaveItems)) {
				statement.setString(1, info.playerID().toString());
				statement.setString(2, info.profileID().toString());
				statement.setBytes(3, info.items() == null ? NULL_ITEMS_BYTES : ItemStack.serializeItemsAsBytes(info.items()));
				statement.executeUpdate();
			}
		}
	}

	public final void updateExtrasPlayer(@NotNull UUID playerID, int delta, @NonNegative int max) throws SQLException {
		if (delta == 0) return;
		try (Connection connection = getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(this.statementSaveExtrasPlayer)) {
				statement.setString(1, playerID.toString());
				statement.setInt(2, delta);
				statement.setInt(3, 0);
				statement.setInt(4, max);
				statement.executeUpdate();
			}
		}
	}

	public final void updateExtrasProfile(@NotNull UUID playerID, @NotNull UUID profileID, int delta, @NonNegative int max) throws SQLException {
		if (delta == 0) return;
		try (Connection connection = getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(this.statementSaveExtrasProfile)) {
				statement.setString(1, playerID.toString());
				statement.setString(2, profileID.toString());
				statement.setBytes(3, NULL_ITEMS_BYTES);
				statement.setInt(4, delta);
				statement.setInt(5, 0);
				statement.setInt(6, max);
				statement.executeUpdate();
			}
		}
	}

	@NotNull
	public final Pair<@NotNull @NonNegative Integer, @NotNull @NonNegative Integer>
	getExtras(@NotNull UUID playerID, @Nullable UUID profileID) throws SQLException {
		try (Connection connection = getConnection()) {
			int extrasPlayer = getExtrasPlayer(connection, playerID),
					extrasProfile = profileID == null ? 0 : getExtrasProfile(connection, playerID, profileID);
			return new Pair<>(extrasPlayer, extrasProfile);
		}
	}
}
