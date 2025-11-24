package com.mineshoku.mstbackpack;

import com.google.common.base.Preconditions;
import com.mineshoku.mstutils.Utils;
import com.mineshoku.mstutils.database.Database;
import com.mineshoku.mstutils.database.LocalDatabaseType;
import com.mineshoku.mstutils.database.MSTUtilsDatabase;
import com.mineshoku.mstutils.database.MySQLDatabaseType;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.models.Pair;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract sealed class BackpackDatabase extends Database permits BackpackDatabase.Local, BackpackDatabase.MySQL {
	private static final @NotNull String TABLE_PLAYERS = "mstbackpack_players";
	private static final @NotNull String TABLE_PROFILES = "mstbackpack_profiles";
	private static final @NotNull String COLUMN_PLAYER_ID = "player_uuid";
	private static final @NotNull String COLUMN_PROFILE_ID = "profile_uuid";
	private static final @NotNull String COLUMN_ITEMS = "items";
	private static final @NotNull String COLUMN_EXTRAS = "extras";
	private static final byte @NotNull [] NULL_ITEMS_BYTES = ItemStack.serializeItemsAsBytes(Collections.emptyList());
	private static final @NotNull String STATEMENT_GET_PLAYERS = new SelectBuilder(TABLE_PLAYERS).column(COLUMN_PLAYER_ID).build();
	private static final @NotNull String STATEMENT_GET_PROFILES = new SelectBuilder(TABLE_PROFILES).column(COLUMN_PLAYER_ID).column(COLUMN_PROFILE_ID).build();
	private static final @NotNull String STATEMENT_REMOVE_PROFILES = new DeleteBuilder(TABLE_PROFILES).where(COLUMN_PLAYER_ID).where(COLUMN_PROFILE_ID).build();
	private static final @NotNull String STATEMENT_GET_EXTRAS_PLAYER = new SelectBuilder(TABLE_PLAYERS).column(COLUMN_EXTRAS).where(COLUMN_PLAYER_ID).build();
	private static final @NotNull String STATEMENT_GET_EXTRAS_PROFILE = new SelectBuilder(TABLE_PROFILES).column(COLUMN_EXTRAS).
			where(COLUMN_PLAYER_ID).where(COLUMN_PROFILE_ID).build();
	private static final @NotNull String STATEMENT_GET_INFO = new SelectBuilder(TABLE_PROFILES).column(COLUMN_ITEMS).column(COLUMN_EXTRAS).
			where(COLUMN_PLAYER_ID).where(COLUMN_PROFILE_ID).build();

	private final @NotNull MSTBackpack plugin;
	private final @NotNull String statementInsertPlayer;
	private final @NotNull String statementInsertProfile;
	private final @NotNull String statementSaveItems;
	private final @NotNull String statementSaveExtrasPlayer;
	private final @NotNull String statementSaveExtrasProfile;

	@NotNull
	private static HikariConfig fromUtils() {
		HikariConfig config = MSTUtilsDatabase.instance().hikariConfig();
		config.setPoolName("MSTBackpack");
		return config;
	}

	private BackpackDatabase(@NotNull MSTBackpack plugin) throws ClassNotFoundException, SQLException {
		super(fromUtils());
		this.plugin = plugin;
		this.statementInsertPlayer = new InsertBuilder(this, TABLE_PLAYERS).column(COLUMN_PLAYER_ID).ignoreConflict(true).build();
		this.statementInsertProfile = new InsertBuilder(this, TABLE_PROFILES).ignoreConflict(true).
				column(COLUMN_PLAYER_ID).column(COLUMN_PROFILE_ID).column(COLUMN_ITEMS).build();
		this.statementSaveItems = new InsertBuilder(this, TABLE_PROFILES).
				column(COLUMN_PLAYER_ID).column(COLUMN_PROFILE_ID).column(COLUMN_ITEMS).
				conflictUpdate(new InsertBuilder.ConflictUpdateBuilder(this, COLUMN_ITEMS + "=" + fromConflict(COLUMN_ITEMS)).
						keyColumn(COLUMN_PLAYER_ID).keyColumn(COLUMN_PROFILE_ID)).build();
		String prefix = COLUMN_EXTRAS + "=" + functionMin() + "(" + functionMax() + "(",
				suffix = "." + COLUMN_EXTRAS + " + " + fromConflict(COLUMN_EXTRAS) + ", ?), ?)";
		this.statementSaveExtrasPlayer = new InsertBuilder(this, TABLE_PLAYERS).column(COLUMN_PLAYER_ID).column(COLUMN_EXTRAS).
				conflictUpdate(new InsertBuilder.ConflictUpdateBuilder(this, prefix + TABLE_PLAYERS + suffix).keyColumn(COLUMN_PLAYER_ID)).
				build();
		this.statementSaveExtrasProfile = new InsertBuilder(this, TABLE_PROFILES).
				column(COLUMN_PLAYER_ID).column(COLUMN_PROFILE_ID).column(COLUMN_ITEMS).column(COLUMN_EXTRAS).
				conflictUpdate(new InsertBuilder.ConflictUpdateBuilder(this, prefix + TABLE_PROFILES + suffix).
						keyColumn(COLUMN_PLAYER_ID).keyColumn(COLUMN_PROFILE_ID)).
				build();
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.execute(new TableBuilder(TABLE_PLAYERS).
					column(new TableBuilder.ColumnBuilder(COLUMN_PLAYER_ID, "VARCHAR(36)").nullable(false).primaryKey(true)).
					column(new TableBuilder.ColumnBuilder(COLUMN_EXTRAS, "INT").typeExtra("UNSIGNED").nullable(false).defaultValue(0)).
					build()
			);
			statement.execute(new TableBuilder(TABLE_PROFILES).
					column(new TableBuilder.ColumnBuilder(COLUMN_PLAYER_ID, "VARCHAR(36)").nullable(false).primaryKey(true)).
					column(new TableBuilder.ColumnBuilder(COLUMN_PROFILE_ID, "VARCHAR(36)").nullable(false).primaryKey(true)).
					column(new TableBuilder.ColumnBuilder(COLUMN_ITEMS, "LONGBLOB").nullable(false)).
					column(new TableBuilder.ColumnBuilder(COLUMN_EXTRAS, "INT").typeExtra("UNSIGNED").nullable(false).defaultValue(0)).
					build()
			);
		}
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

	@ApiStatus.Internal
	public void insertPlayer(@NotNull UUID playerID) {
		LoggingManager.exceptionallyLogError(this.plugin, CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(this.statementInsertPlayer)) {
				statement.setString(1, playerID.toString());
				statement.executeUpdate();
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database));
	}

	@ApiStatus.Internal
	public void insertProfiles(@NotNull Map<@NotNull UUID, @NotNull Set<@NotNull UUID>> profilesMap) {
		if (profilesMap.isEmpty()) return;
		LoggingManager.exceptionallyLogError(this.plugin, CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(this.statementInsertProfile)) {
				for (Map.Entry<UUID, Set<UUID>> entry : profilesMap.entrySet()) {
					for (UUID profileID : entry.getValue()) {
						statement.setString(1, entry.getKey().toString());
						statement.setString(2, profileID.toString());
						statement.setBytes(3, NULL_ITEMS_BYTES);
						statement.addBatch();
					}
				}
				statement.executeBatch();
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database));
	}

	@ApiStatus.Internal
	public CompletableFuture<Void> removeProfiles(@NotNull Map<@NotNull UUID, @NotNull Set<@NotNull UUID>> profilesMap) {
		if (profilesMap.isEmpty()) return CompletableFuture.completedFuture(null);
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(STATEMENT_REMOVE_PROFILES)) {
				for (Map.Entry<UUID, Set<UUID>> entry : profilesMap.entrySet()) {
					for (UUID profileID : entry.getValue()) {
						statement.setString(1, entry.getKey().toString());
						statement.setString(2, profileID.toString());
						statement.addBatch();
					}
				}
				statement.executeBatch();
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@ApiStatus.Internal
	@NotNull
	public final CompletableFuture<@NotNull LinkedHashSet<@NotNull UUID>> getPlayers() {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_PLAYERS);
				 ResultSet result = statement.executeQuery()) {
				LinkedHashSet<UUID> ids = new LinkedHashSet<>();
				while (result.next()) {
					UUID playerID = Utils.getUUID(result.getString(1));
					if (playerID != null) {
						ids.add(playerID);
					}
				}
				return ids;
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@ApiStatus.Internal
	@NotNull
	public final CompletableFuture<@NotNull LinkedHashMap<@NotNull UUID, @NotNull LinkedHashSet<@NotNull UUID>>> getProfiles() {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_PROFILES);
				 ResultSet result = statement.executeQuery()) {
				LinkedHashMap<UUID, LinkedHashSet<UUID>> map = new LinkedHashMap<>();
				while (result.next()) {
					UUID playerID = Utils.getUUID(result.getString(1)), profileID = Utils.getUUID(result.getString(2));
					if (playerID != null && profileID != null) {
						map.computeIfAbsent(playerID, k -> new LinkedHashSet<>()).add(profileID);
					}
				}
				return map;
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<@NotNull BackpackInfo> getInfo(@NotNull UUID playerID, @NotNull UUID profileID) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection()) {
				ItemStack[] items = new ItemStack[0];
				int extrasProfile = 0;
				boolean inDB = false;
				try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_INFO)) {
					statement.setString(1, playerID.toString());
					statement.setString(2, profileID.toString());
					try (ResultSet result = statement.executeQuery()) {
						if (result.next()) {
							inDB = true;
							Object bytes = result.getObject(1);
							if (bytes != null) {
								items = ItemStack.deserializeItemsFromBytes(result.getBytes(1));
							}
							extrasProfile = result.getInt(2);
						}
					}
				}
				int extrasPlayer = getExtrasPlayer(connection, playerID);
				return new BackpackInfo(playerID, profileID, extrasPlayer, extrasProfile, inDB, items);
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<Void> saveItems(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable Collection<@Nullable ItemStack> items) {
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(this.statementSaveItems)) {
				statement.setString(1, playerID.toString());
				statement.setString(2, profileID.toString());
				statement.setBytes(3, items == null ? NULL_ITEMS_BYTES : ItemStack.serializeItemsAsBytes(items));
				statement.executeUpdate();
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<Void> saveItems(@NotNull UUID playerID, @NotNull UUID profileID, @Nullable ItemStack @NotNull ... items) {
		return saveItems(playerID, profileID, Arrays.asList(items));
	}

	@NotNull
	public final CompletableFuture<Void> updateExtras(@NotNull UUID playerID, @Nullable UUID profileID, int delta) {
		if (delta == 0) return CompletableFuture.completedFuture(null);
		int maxPlayer = this.plugin.backpackConfig().snapshot().amountExtraPlayerMax(),
				maxProfile = this.plugin.backpackConfig().snapshot().amountExtraProfileMax();
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection()) {
				if (profileID == null) {
					try (PreparedStatement statement = connection.prepareStatement(this.statementSaveExtrasPlayer)) {
						statement.setString(1, playerID.toString());
						statement.setInt(2, delta);
						statement.setInt(3, 0);
						statement.setInt(4, maxPlayer);
						statement.executeUpdate();
					}
				} else {
					try (PreparedStatement statement = connection.prepareStatement(this.statementSaveExtrasProfile)) {
						statement.setString(1, playerID.toString());
						statement.setString(2, profileID.toString());
						statement.setBytes(3, NULL_ITEMS_BYTES);
						statement.setInt(4, delta);
						statement.setInt(5, 0);
						statement.setInt(6, maxProfile);
						statement.executeUpdate();
					}
				}
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<@NotNull Pair<@NotNull @NonNegative Integer, @NotNull @NonNegative Integer>> getExtras(@NotNull UUID playerID,
																														  @Nullable UUID profileID) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection()) {
				int extrasPlayer = getExtrasPlayer(connection, playerID),
						extrasProfile = profileID == null ? 0 : getExtrasProfile(connection, playerID, profileID);
				return Pair.of(extrasPlayer, extrasProfile);
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	protected static final class Local extends BackpackDatabase implements LocalDatabaseType {
		private Local(@NotNull MSTBackpack plugin) throws ClassNotFoundException, SQLException {
			super(plugin);
		}
	}

	protected static final class MySQL extends BackpackDatabase implements MySQLDatabaseType {
		private MySQL(@NotNull MSTBackpack plugin) throws ClassNotFoundException, SQLException {
			super(plugin);
		}
	}

	@NotNull
	@ApiStatus.Internal
	public static BackpackDatabase create(@NotNull MSTBackpack plugin) throws ClassNotFoundException, SQLException {
		Preconditions.checkState(plugin.backpackDatabase() == null, "Backpack database already initialized");
		return (MSTUtilsDatabase.instance() instanceof LocalDatabaseType) ? new Local(plugin) : new MySQL(plugin);
	}
}