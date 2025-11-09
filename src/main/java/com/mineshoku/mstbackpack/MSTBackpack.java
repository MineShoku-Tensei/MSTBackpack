package com.mineshoku.mstbackpack;

import com.mineshoku.mstutils.managers.LoggingManager;
import com.mineshoku.mstutils.managers.MMOProfilesManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

public class MSTBackpack extends JavaPlugin {
	private static MSTBackpack instance;

	private BackpackConfig backpackConfig;
	private BackpackDatabase backpackDatabase;
	private BackpackCache backpackCache;
	private BackpackCommandHandler backpackCommandHandler;

	public MSTBackpack() {
		if (instance != null) throw new IllegalStateException("MSTBackpack main already present?");
		instance = this;
	}

	@Override
	public void onEnable() {
		Objects.requireNonNull(MMOProfilesManager.instance());
		this.backpackConfig = new BackpackConfig(this);
		try {
			this.backpackDatabase = BackpackDatabase.create(this);
		} catch (Exception e) {
			LoggingManager.instance().severe(this, e);
			getLogger().severe("Disabling plugin");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		this.backpackCache = new BackpackCache(this);
		this.backpackCommandHandler = new BackpackCommandHandler(this);
	}

	@Override
	public void onDisable() {
		if (this.backpackCache != null) {
			this.backpackCache.shutdown();
		}
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

	public BackpackCache cacheListener() {
		return this.backpackCache;
	}

	public BackpackCommandHandler backpackCommandHandler() {
		return this.backpackCommandHandler;
	}

	public void reload() {
		backpackConfig().reload();
		backpackCommandHandler().reload();
	}
}