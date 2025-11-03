package com.mineshoku.mstbackpack.database;

import com.mineshoku.mstbackpack.MSTBackpack;
import com.mineshoku.mstutils.database.MySQLDatabase;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public final class BackpackMySQL extends BackpackDatabase implements MySQLDatabase {
	public BackpackMySQL(@NotNull MSTBackpack plugin, @NotNull String host) throws ClassNotFoundException, SQLException {
		super(plugin, MySQLDatabase.createURL(host, plugin.config().port(), Objects.requireNonNull(plugin.config().database()), false),
				plugin.config().username(), plugin.config().password());
		prepareDatabase();
	}
}