package com.MineShoku.Backpack.Database;

import com.MineShoku.Backpack.Main;
import com.MineShoku.Utils.Database.MySQLDatabase;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public final class MySQL extends Database implements MySQLDatabase {
	public MySQL(@NotNull Main plugin, @NotNull String host) throws ClassNotFoundException, SQLException {
		super(plugin, MySQLDatabase.createURL(host, plugin.config().port(), Objects.requireNonNull(plugin.config().database()), false),
				plugin.config().username(), plugin.config().password());
		prepareDatabase();
	}
}