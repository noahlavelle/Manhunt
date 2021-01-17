package com.noahlavelle.manhunt;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        new Manhunt(this);
        getServer().getPluginManager().registerEvents(new Manhunt.EventHandler(), this);

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[Manhunt] Plugin is enabled");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[Manhunt] Plugin is disabled");
    }
}
