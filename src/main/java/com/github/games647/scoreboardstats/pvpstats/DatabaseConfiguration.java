package com.github.games647.scoreboardstats.pvpstats;

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import com.github.games647.scoreboardstats.Lang;
import com.github.games647.scoreboardstats.ScoreboardStats;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Configuration for the sql database.
 */
/* package */ class DatabaseConfiguration {

    private final ScoreboardStats plugin;

    private ServerConfig serverConfig;

    DatabaseConfiguration(ScoreboardStats instance) {
        plugin = instance;
    }

    /**
     * Gets the sql configuration
     *
     * @return the server config
     */
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    void loadConfiguration() throws InvalidConfigurationException {
        final ServerConfig databaseConfig = new ServerConfig();
        databaseConfig.setRegister(false);
        databaseConfig.addClass(PlayerStats.class);
        //Give the database a specific name
        databaseConfig.setName(plugin.getName());
        databaseConfig.setValidateOnSave(true);

        final DataSourceConfig sqlConfig = getSqlConfig(databaseConfig);
        //set a correct path

        validatePath();

        sqlConfig.setUrl(replaceUrlString(sqlConfig.getUrl()));
        sqlConfig.setHeartbeatSql("SELECT 1");

        serverConfig = databaseConfig;
    }

    private DataSourceConfig getSqlConfig(ServerConfig serverConfig) {
        FileConfiguration sqlConfig;
        DataSourceConfig config;

        final File file = new File(plugin.getDataFolder(), "sql.yml");
        //Check if the file exists. If so load the settings form there
        if (file.exists()) {
            sqlConfig = YamlConfiguration.loadConfiguration(file);
            config = new DataSourceConfig();

            config.setUsername(sqlConfig.getString("SQL-Settings.Username"));
            config.setPassword(sqlConfig.getString("SQL-Settings.Password"));
            config.setIsolationLevel(TransactionIsolation.getLevel(sqlConfig.getString("SQL-Settings.Isolation")));
            config.setDriver(sqlConfig.getString("SQL-Settings.Driver"));
            config.setUrl(sqlConfig.getString("SQL-Settings.Url"));

            serverConfig.setDataSourceConfig(config);
        } else {
            //Create a new configuration based on the default settings form bukkit.yml
            plugin.saveResource("sql.yml", false);
            plugin.getServer().configureDbConfig(serverConfig);

            config = serverConfig.getDataSourceConfig();

            sqlConfig = YamlConfiguration.loadConfiguration(file);
            sqlConfig.set("SQL-Settings.Username", config.getUsername());
            sqlConfig.set("SQL-Settings.Password", config.getPassword());
            sqlConfig.set("SQL-Settings.Isolation", TransactionIsolation.getLevelDescription(config.getIsolationLevel()));
            sqlConfig.set("SQL-Settings.Driver", config.getDriver());
            sqlConfig.set("SQL-Settings.Url", config.getUrl());
            try {
                sqlConfig.save(file);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, Lang.get("databaseConfigSaveError"), ex);
            }
        }

        config.setWaitTimeoutMillis(sqlConfig.getInt("SQL-Settings.Timeout"));

        return config;
    }

    private String replaceUrlString(String input) {
        String result = input.replaceAll("\\{DIR\\}", plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + '/');
        result = result.replaceAll("\\{NAME\\}", plugin.getDescription().getName().replaceAll("[^\\w-]", ""));

        return result;
    }

    private void validatePath() throws InvalidConfigurationException {
        final String path = plugin.getDataFolder().getPath();
        if (!path.matches("[\\p{L}0-9-" + StringEscapeUtils.escapeJava(File.separator) + ":]+")) {
            throw new InvalidConfigurationException("The path to your craftbukkit.jar is invalid format. "
                    + "The non-latin characters aren't allowed, because these occures a bug in java 6."
                    + "Please use normal characters instead of this: "
                    + path);
        }
    }
}
