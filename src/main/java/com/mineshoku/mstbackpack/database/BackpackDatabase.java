package com.mineshoku.mstbackpack.database;

import com.mineshoku.mstbackpack.BackpackInfo;
import com.mineshoku.mstbackpack.MSTBackpack;
import com.mineshoku.mstutils.database.Database;
import com.mineshoku.mstutils.managers.ExecutorManager;
import com.mineshoku.mstutils.models.Pair;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class BackpackDatabase extends Database {
	protected static final @NotNull String TABLE_PLAYERS = "MSTBackpackPlayers";
	protected static final @NotNull String TABLE_PROFILES = "MSTBackpackProfiles";
	protected static final @NotNull String COLUMN_PLAYER_ID = "PlayerUUID";
	protected static final @NotNull String COLUMN_PROFILE_ID = "ProfileUUID";
	protected static final @NotNull String COLUMN_ITEMS = "Items";
	protected static final @NotNull String COLUMN_EXTRAS = "Extras";
	private static final byte @NotNull [] NULL_ITEMS_BYTES = ItemStack.serializeItemsAsBytes(Collections.emptyList());
	private static final @NotNull String STATEMENT_GET_EXTRAS_PLAYER = new SelectBuilder(TABLE_PLAYERS).
			columns(COLUMN_EXTRAS).where(COLUMN_PLAYER_ID).build();
	private static final @NotNull String STATEMENT_GET_EXTRAS_PROFILE = new SelectBuilder(TABLE_PROFILES).
			columns(COLUMN_EXTRAS).where(COLUMN_PLAYER_ID, COLUMN_PROFILE_ID).build();
	private static final @NotNull String STATEMENT_GET_INFO = new SelectBuilder(TABLE_PROFILES).
			columns(COLUMN_ITEMS, COLUMN_EXTRAS).where(COLUMN_PLAYER_ID, COLUMN_PROFILE_ID).build();

	protected final @NotNull MSTBackpack plugin;
	private final @NotNull String statementSaveItems;
	private final @NotNull String statementSaveExtrasPlayer;
	private final @NotNull String statementSaveExtrasProfile;

	protected BackpackDatabase(@NotNull MSTBackpack plugin, @NotNull String url,
							   @Nullable String username, @Nullable String password) throws ClassNotFoundException {
		super(url, "MSTBackpack", username, password);
		this.plugin = plugin;
		this.statementSaveItems = new InsertBuilder(TABLE_PROFILES).
				columns(COLUMN_PLAYER_ID, COLUMN_PROFILE_ID, COLUMN_ITEMS).
				conflictUpdate(COLUMN_ITEMS + "=" + fromConflict(COLUMN_ITEMS), COLUMN_PLAYER_ID, COLUMN_PROFILE_ID).
				build();
		String prefix = COLUMN_EXTRAS + "=" + functionMin() + "(" + functionMax() + "(",
				suffix = "." + COLUMN_EXTRAS + " + " + fromConflict(COLUMN_EXTRAS) + ", ?), ?)";
		this.statementSaveExtrasPlayer = new InsertBuilder(TABLE_PLAYERS).columns(COLUMN_PLAYER_ID, COLUMN_EXTRAS).
				conflictUpdate(prefix + TABLE_PLAYERS + suffix, COLUMN_PLAYER_ID).
				build();
		this.statementSaveExtrasProfile = new InsertBuilder(TABLE_PROFILES).
				columns(COLUMN_PLAYER_ID, COLUMN_PROFILE_ID, COLUMN_ITEMS, COLUMN_EXTRAS).
				conflictUpdate(prefix + TABLE_PROFILES + suffix, COLUMN_PLAYER_ID, COLUMN_PROFILE_ID).
				build();
	}

	protected final void prepareDatabase() throws SQLException {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.execute(new TableBuilder(TABLE_PLAYERS).
					column(COLUMN_PLAYER_ID, "VARCHAR(36)", false, "PRIMARY KEY").
					column(COLUMN_EXTRAS, "INT UNSIGNED", false, "DEFAULT 0").
					build()
			);
			statement.execute(new TableBuilder(TABLE_PROFILES).
					column(COLUMN_PLAYER_ID, "VARCHAR(36)", false).
					column(COLUMN_PROFILE_ID, "VARCHAR(36)", false).
					column(COLUMN_ITEMS, "LONGBLOB", false).
					column(COLUMN_EXTRAS, "INT UNSIGNED", false, "DEFAULT 0").
					primaryKeys(COLUMN_PLAYER_ID, COLUMN_PROFILE_ID).
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
	private int getExtrasProfile(@NotNull Connection connection,
								 @NotNull UUID playerID, @NotNull UUID profileID) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_EXTRAS_PROFILE)) {
			statement.setString(1, playerID.toString());
			statement.setString(2, profileID.toString());
			try (ResultSet result = statement.executeQuery()) {
				return result.next() ? result.getInt(1) : 0;
			}
		}
	}

	@NotNull
	public final CompletableFuture<@NotNull BackpackInfo> getInfo(@NotNull UUID playerID, @NotNull UUID profileID) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection()) {
				ItemStack[] items = null;
				int extrasProfile = 0;
				try (PreparedStatement statement = connection.prepareStatement(STATEMENT_GET_INFO)) {
					statement.setString(1, playerID.toString());
					statement.setString(2, profileID.toString());
					try (ResultSet result = statement.executeQuery()) {
						if (result.next()) {
							Object bytes = result.getObject(1);
							if (bytes != null) {
								items = ItemStack.deserializeItemsFromBytes(result.getBytes(1));
							}
							extrasProfile = result.getInt(2);
						}
					}
				}
				int extrasPlayer = getExtrasPlayer(connection, playerID);
				return new BackpackInfo(playerID, profileID, extrasPlayer, extrasProfile, items);
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<Void> saveItems(@NotNull BackpackInfo info) {
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = getConnection()) {
				try (PreparedStatement statement = connection.prepareStatement(this.statementSaveItems)) {
					statement.setString(1, info.playerID().toString());
					statement.setString(2, info.profileID().toString());
					List<ItemStack> items = info.items();
					statement.setBytes(3, items == null ? NULL_ITEMS_BYTES : ItemStack.serializeItemsAsBytes(items));
					statement.executeUpdate();
				}
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
		}, ExecutorManager.instance().database);
	}

	@NotNull
	public final CompletableFuture<Void> updateExtras(@NotNull UUID playerID, @Nullable UUID profileID, int delta) {
		if (delta == 0) return CompletableFuture.completedFuture(null);
		int maxPlayer = this.plugin.backpackConfig().amountExtraPlayerMax(),
				maxProfile = this.plugin.backpackConfig().amountExtraProfileMax();
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
	public final CompletableFuture<@NotNull Pair<@NotNull @NonNegative Integer, @NotNull @NonNegative Integer>>
	getExtras(@NotNull UUID playerID, @Nullable UUID profileID) {
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
}