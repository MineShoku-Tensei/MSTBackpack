package com.mineshoku.mstbackpack.database;

import com.mineshoku.mstbackpack.Main;
import com.mineshoku.mstutils.database.LocalDatabase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;

public final class Local extends Database implements LocalDatabase {
	public Local(@NotNull Main plugin) throws ClassNotFoundException, IOException, SQLException {
		super(plugin, LocalDatabase.createURL(LocalDatabase.getFile(plugin)), null, null);
		LocalDatabase.initiateDatabase(LocalDatabase.getFile(plugin));
		prepareDatabase();
	}
}