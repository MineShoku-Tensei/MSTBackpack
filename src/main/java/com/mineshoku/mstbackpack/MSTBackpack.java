package com.mineshoku.mstbackpack;

import com.mineshoku.mstbackpack.database.BackpackDatabase;
import com.mineshoku.mstbackpack.database.BackpackLocalDB;
import com.mineshoku.mstbackpack.database.BackpackMySQL;
import com.mineshoku.mstutils.TextUtils;
import com.mineshoku.mstutils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

public class MSTBackpack extends JavaPlugin {
	private static MSTBackpack instance;

	private BackpackConfig backpackConfig;
	private BackpackDatabase backpackDatabase;
	private BackpackCommandHandler backpackCommandHandler;

	public MSTBackpack() {
		if (instance != null) throw new IllegalStateException("MSTBackpack main already present?");
		instance = this;
	}

	@Override
	public void onEnable() {
		boolean failed = false;
		this.backpackConfig = new BackpackConfig(this);
		try {
			String host = this.backpackConfig.host();
			if (TextUtils.isNullOrBlank(host)) {
				this.backpackDatabase = new BackpackLocalDB(this);
			} else {
				try {
					this.backpackDatabase = new BackpackMySQL(this, host);
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
		this.backpackCommandHandler = new BackpackCommandHandler(this);
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	@ApiStatus.Internal
	static MSTBackpack instance() {
		return instance;
	}

	public BackpackConfig backpackConfig() {
		return this.backpackConfig;
	}

	public BackpackDatabase backpackDatabase() {
		return this.backpackDatabase;
	}

	public BackpackCommandHandler backpackCommandHandler() {
		return this.backpackCommandHandler;
	}

	public void reload() {
		backpackConfig().reload();
		backpackCommandHandler().reload();
	}
}