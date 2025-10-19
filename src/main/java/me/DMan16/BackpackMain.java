package me.DMan16;

import com.zaxxer.hikari.pool.HikariPool;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackpackMain extends JavaPlugin {
    private Database database;

    public void onEnable() {
        boolean failed = false;
        try {
            saveDefaultConfig();
            reloadConfig();
            String host = getConfig().getString("host", null);
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
            new BackpackCommand(this, this.database);
        } catch (Exception e) {
            failed = true;
            getLogger().severe("Backpack command executor error: ");
            e.printStackTrace();
        }
        if (failed) {
            getLogger().severe("Disabling plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public void onDisable() {
        if (this.database != null) {
            this.database.close();
        }
    }
}