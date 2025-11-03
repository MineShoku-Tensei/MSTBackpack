package com.mineshoku.mstbackpack;

import com.mineshoku.mstbackpack.database.DatabaseImpl;
import com.mineshoku.mstbackpack.database.LocalImpl;
import com.mineshoku.mstbackpack.database.MySQLImpl;
import com.mineshoku.mstutils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	private Config config;
	private DatabaseImpl database;
	private CommandHandler commandHandler;

	public void onEnable() {
		boolean failed = false;
		this.config = new Config(this);
		try {
			String host = this.config.host();
			if (host == null || host.isBlank()) {
				this.database = new LocalImpl(this);
			} else {
				try {
					this.database = new MySQLImpl(this, host);
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
		this.commandHandler = new CommandHandler(this);
	}

	public Config config() {
		return this.config;
	}

	public DatabaseImpl database() {
		return this.database;
	}

	public CommandHandler commandHandler() {
		return this.commandHandler;
	}

	public void reload() {
		config().reload();
		commandHandler().reload();
	}
}