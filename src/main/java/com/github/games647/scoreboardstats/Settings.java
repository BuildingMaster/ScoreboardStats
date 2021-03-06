package com.github.games647.scoreboardstats;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Managing all general configurations of this plugin.
 */
public final class Settings {

    private static boolean updateEnabled;
    private static boolean compatibilityMode;
    private static boolean pvpStats;
    private static boolean tempScoreboard;
    private static boolean hideVanished;

    private static String title;
    private static String tempTitle;
    private static String tempColor;
    private static String topType;

    private static int intervall;
    private static int saveIntervall;
    private static int topitems;
    private static int tempShow;
    private static int tempDisapper;

    //Sidebar objective can't have more than 15 items
    private static final Map<String, String> ITEMS = Maps.newHashMapWithExpectedSize(15);
    private static Set<String> disabledWorlds;

    /**
     * Get an iterator of all items in the main scoreboard
     *
     * @return an iterator of the configurated items in the main scoreboard
     */
    public static Iterator<Map.Entry<String, String>> getItems() {
        return ITEMS.entrySet().iterator();
    }

    /**
     * Check if a world is from scoreboardstats ignored
     *
     * @param world the checked world
     * @return if the world is disabled
     */
    public static boolean isActiveWorld(World world) {
        return !disabledWorlds.contains(world.getName());
    }

    /**
     * Check whether tracking of players stats is enabled
     *
     * @return whether tracking of players stats is enabled
     */
    public static boolean isPvpStats() {
        return pvpStats;
    }

    /**
     * Check whether compatibility mode that scoreboardstats should operate
     * over raw packets instead of using the bukkit api.
     *
     * @return whether compatibility mode that scoreboardstats should operate over raw packets
     */
    public static boolean isCompatibilityMode() {
        return compatibilityMode;
    }

    /**
     * Check if the temp-scoreboard is enabled
     *
     * @return if the temp-scoreboard is enabled
     */
    public static boolean isTempScoreboard() {
        return tempScoreboard;
    }

    /**
     * Check if the plugin should ignore vanished player for online counting
     *
     * @return if the plugin should ignore vanished player for online counting
     */
    public static boolean isHideVanished() {
        return hideVanished;
    }

    /**
     * Check if update checking is enabled
     *
     * @return if update checking is enabled
     */
    public static boolean isUpdateEnabled() {
        return updateEnabled;
    }

    /**
     * Get the title of the main scoreboard
     *
     * @return the title of the main scoreboard
     */
    public static String getTitle() {
        return title;
    }

    /**
     * Get the title of the temp-scoreboard
     *
     * @return the title of the temp-scoreboard
     */
    public static String getTempTitle() {
        return tempTitle;
    }

    /**
     * Get the color for items in the temp-scoreboard
     *
     * @return the color for items in the temp-scoreboard
     */
    public static String getTempColor() {
        return tempColor;
    }

    /**
     * Get the type what the temp-scoreboard should display
     *
     * @return what the temp-scoreboard should display
     */
    public static String getTopType() {
        return topType;
    }

    /**
     * Get the interval in which the items being refreshed.
     *
     * @return the interval in which the items being refreshed.
     */
    public static int getIntervall() {
        return intervall;
    }

    /**
     * Get the interval (in minutes) the stats should be saved
     *
     * @return the interval (in minutes) the stats should be saved
     */
    public static int getSaveIntervall() {
        return saveIntervall;
    }

    /**
     * Get how many items the temp-scoreboard should have
     *
     * @return how many items the temp-scoreboard should have
     */
    public static int getTopitems() {
        return topitems;
    }

    /**
     * Get the seconds after the temp-scoreboard should appear.
     *
     * @return the seconds after the temp-scoreboard should appear
     */
    public static int getTempAppear() {
        return tempShow;
    }

    /**
     * Get the seconds after the temp-scoreboard should disappear.
     *
     * @return the seconds after the temp-scoreboard should disappear
     */
    public static int getTempDisappear() {
        return tempDisapper;
    }

    private final ScoreboardStats plugin;

    Settings(ScoreboardStats instance) {
        this.plugin = instance;
    }

    /**
     * Load the configuration file in memory and convert it into simple variables
     */
    public void loadConfig() {
        //Creates a default config and/or load it
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        final FileConfiguration config = plugin.getConfig();

        //Load all normal scoreboard variables
        loaditems(config.getConfigurationSection("Scoreboard.Items"));
//        setCompatibilityMode(config);

        updateEnabled = config.getBoolean("pluginUpdate");
        hideVanished = config.getBoolean("hide-vanished");
        pvpStats = config.getBoolean("enable-pvpstats");

        //This set only changes after another call to loadConfig so this set can be immutable
        disabledWorlds = ImmutableSet.copyOf(config.getStringList("disabled-worlds"));
        intervall = config.getInt("Scoreboard.Update-delay");
        saveIntervall = config.getInt("PvPStats-SaveIntervall");
        title = ChatColor.translateAlternateColorCodes('&', checkLength(Lang.getReplaced(config.getString("Scoreboard.Title")), 32));

        tempScoreboard = config.getBoolean("Temp-Scoreboard-enabled") && pvpStats;

        topitems = checkItems(config.getInt("Temp-Scoreboard.Items"));

        tempShow = config.getInt("Temp-Scoreboard.Intervall-show");
        tempDisapper = config.getInt("Temp-Scoreboard.Intervall-disappear");

        topType = config.getString("Temp-Scoreboard.Type");

        tempColor = ChatColor.translateAlternateColorCodes('&', config.getString("Temp-Scoreboard.Color"));
        tempTitle = ChatColor.translateAlternateColorCodes('&',
                checkLength(Lang.getReplaced(config.getString("Temp-Scoreboard.Title")), 32));
    }

    private String checkLength(String check, int limit) {
        if (check.length() > limit) {
            //If the string check is longer cut it down
            final String cut = check.substring(0, limit);
            //We are couting from 0 so plus 1
            plugin.getLogger().warning(Lang.get("tooLongName", cut, limit));

            return cut;
        }

        return check;
    }

    private int checkItems(int input) {
        if (input >= 16) {
            //Only 15 items per sidebar objective are allowed
            plugin.getLogger().warning(Lang.get("tooManyItems"));
            return 16 - 1;
        }

        if (input <= 0) {
            plugin.getLogger().warning(Lang.get("notEnoughItems", "tempscoreboard"));
            return 5;
        }

        return input;
    }

    private void loaditems(ConfigurationSection config) {
        if (!ITEMS.isEmpty()) {
            //clear all existing items
            ITEMS.clear();
        }

        final Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            //Only 15 items per sidebar objective are allowed
            if (ITEMS.size() == 16 - 1) {
                plugin.getLogger().warning(Lang.get("tooManyItems"));
                break;
            }

            final String name = ChatColor.translateAlternateColorCodes('&', checkLength(Lang.getReplaced(key), 16));
            //Prevent case-sensitive mistakes
            final String variable = config.getString(key).toLowerCase(Locale.ENGLISH);
            if (variable.startsWith("%") && variable.endsWith("%")) {
                ITEMS.put(name, variable);
            } else {
                //Prevent user mistakes
                plugin.getLogger().info("The variables has to contains % at the beginning and one % on the end");
            }
        }

        if (ITEMS.isEmpty()) {
            plugin.getLogger().info(Lang.get("notEnoughItems", "scoreboard"));
        }
    }

    private boolean setCompatibilityMode(ConfigurationSection config) {
        final boolean active = config.getBoolean("compatibilityMode");
        if (active) {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                return true;
            } else {
                plugin.getLogger().info("You need to have the plugin called ProtocolLib to activate compatibilityMode");
            }
        }

        return false;
    }
}
