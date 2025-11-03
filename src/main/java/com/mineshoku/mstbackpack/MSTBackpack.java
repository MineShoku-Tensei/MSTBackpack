package com.mineshoku.mstbackpack;

import com.mineshoku.mstbackpack.database.BackpackDatabase;
import com.mineshoku.mstbackpack.database.BackpackLocalDB;
import com.mineshoku.mstbackpack.database.BackpackMySQL;
import com.mineshoku.mstutils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MSTBackpack extends JavaPlugin {
	private static MSTBackpack instance;

	private BackpackConfig config;
	private BackpackDatabase database;
	private BackpackCommandHandler commandHandler;

	public void onLoad() {
		if (instance != null) throw new IllegalStateException("MSTBackpack main already present?");
		instance = this;
	}

	public void onEnable() {
		boolean failed = false;
		this.config = new BackpackConfig(this);
		try {
			String host = this.config.host();
			if (host == null || host.isBlank()) {
				this.database = new BackpackLocalDB(this);
			} else {
				try {
					this.database = new BackpackMySQL(this, host);
				} catch (Exception e) {
					failed = true;
					getLogger().severe("Failed connecting to MySQL DB!");
				}
			}
		} catch (Exception e) {
			failed = true;
			Utils.logException(this, e);
		}
		if (failed) {
			getLogger().severe("Disabling plugin");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		this.commandHandler = new BackpackCommandHandler(this);
	}

	public void onDisable() {
		instance = null;
	}

	static MSTBackpack instance() {
		return instance;
	}

	public BackpackConfig config() {
		return this.config;
	}

	public BackpackDatabase database() {
		return this.database;
	}

	public BackpackCommandHandler commandHandler() {
		return this.commandHandler;
	}

	public void reload() {
		config().reload();
		commandHandler().reload();
	}
}