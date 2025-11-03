package com.mineshoku.mstbackpack.database;

import com.mineshoku.mstbackpack.MSTBackpack;
import com.mineshoku.mstutils.database.LocalDatabase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;

public final class BackpackLocalDB extends BackpackDatabase implements LocalDatabase {
	public BackpackLocalDB(@NotNull MSTBackpack plugin) throws ClassNotFoundException, IOException, SQLException {
		super(plugin, LocalDatabase.createURL(LocalDatabase.getFile(plugin)), null, null);
		LocalDatabase.initiateDatabase(LocalDatabase.getFile(plugin));
		prepareDatabase();
	}
}