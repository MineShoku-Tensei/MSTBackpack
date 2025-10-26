package me.DMan16;

import com.zaxxer.hikari.pool.HikariPool;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	private Config config;
	private Database database;
	private CommandHandler commandHandler;

	public void onEnable() {
		boolean failed = false;
		this.config = new Config(this);
		try {
			String host = this.config.host();
			if (host == null || host.isBlank()) {
				this.database = new LocalDatabase(this);
			} else {
				try {
					this.database = new MySQLDatabase(this, host);
				} catch (HikariPool.PoolInitializationException e) {
					failed = true;
					getLogger().severe("Failed connecting to MySQL DB!");
				}
			}
		} catch (Exception e) {
			failed = true;
			getLogger().severe("Backpack command executor error: ");
			Utils.logException(e);
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

	public Database database() {
		return this.database;
	}

	public CommandHandler commandHandler() {
		return this.commandHandler;
	}

	public void reload() {
		config().reload();
		commandHandler().reload();
	}

	public void onDisable() {
		if (database() != null) {
			database().close();
		}
	}
}