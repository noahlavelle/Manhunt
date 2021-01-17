package com.noahlavelle.manhunt;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.*;

public class Manhunt implements CommandExecutor {

    private Main plugin;
    private static Map<UUID, Game> games = new HashMap<>();
    private Game game;

    public Manhunt (Main plugin) {
        this.plugin = plugin;

        plugin.getCommand("manhunt").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "[Manhunt] This command can only be run by a player");
            return true;
        }

        Player player = (Player) commandSender;

        if (!games.containsKey(player.getUniqueId())) {
            game = new Game(plugin, player);
            games.put(player.getUniqueId(), game);
            game.players.add(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "[Manhunt] Created a new game of manhunt");
        }

        if (strings.length > 0) {
            Game g = games.get(player.getUniqueId());

            if (strings[0].equalsIgnoreCase("start")) {
                for (UUID u : g.players) {
                    plugin.getServer().getPlayer(u).sendMessage(ChatColor.GREEN + "[Manhunt] The game has begun!");
                }

                g.run();
            } else {

                Player p;

                try {
                    p = plugin.getServer().getPlayer(strings[1]);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "[Manhunt] Please enter a real player");
                    return true;
                }

                switch (strings[0].toLowerCase()) {
                    case "h":
                        g.hunters.add(p.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "[Manhunt] Added " + strings[1] + " as a hunter");
                        break;
                    case "r":
                        g.runners.add(p.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "[Manhunt] Added " + strings[1] + " as a runner");
                        break;
                    default:
                        player.sendMessage(ChatColor.RED + "[Manhunt] Please specify either h (hunter) or r (runner)");
                        return true;
                }

                games.put(p.getUniqueId(), game);
                g.players.add(p.getUniqueId());
            }
        }

        return false;
    }

    public static class EventHandler implements Listener {

        @org.bukkit.event.EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();

            if (!games.containsKey(player.getUniqueId())) return;
            Game game = games.get(player.getUniqueId());

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.COMPASS) {
                CompassMeta cmeta = (CompassMeta) itemInHand.getItemMeta();
                cmeta.setLodestone(game.updateTracker(player));
                itemInHand.setItemMeta(cmeta);
            }
        }

        @org.bukkit.event.EventHandler
        public void onEntityDeath(EntityDeathEvent event) {
            if (!(event.getEntity().getKiller() instanceof Player)) return;
            if (!games.containsKey(event.getEntity().getKiller().getUniqueId())) return;

            Game game = games.get(event.getEntity().getKiller().getUniqueId());

            Entity entity = event.getEntity();
            Player killer = event.getEntity().getKiller();

            if (entity.getType() == EntityType.ENDER_DRAGON && game.runners.contains(killer.getUniqueId())) {

            }
        }

        @org.bukkit.event.EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (!games.containsKey(event.getEntity())) return;
            Game game = games.get(event.getEntity().getUniqueId());

            Player player = event.getEntity();

            if (game.runners.contains(player.getUniqueId())) {
                game.runners.remove(player);
                if (game.runners.size() == 0) {
                    // Hunters win
                }
            }
        }
    }

    public static class Game {
        private ArrayList<UUID> runners = new ArrayList<>();
        private ArrayList<UUID> hunters = new ArrayList<>();
        private ArrayList<UUID> players = new ArrayList<>();

        private Main plugin;
        private ItemStack compass;
        private CompassMeta meta;
        private Player player;

        public Game (Main plugin, Player player) {
            this.plugin = plugin;
            this.player = player;
        }

        public void run() {
            compass = new ItemStack(Material.COMPASS);
            meta = (CompassMeta) compass.getItemMeta();

            meta.setDisplayName(ChatColor.GREEN + "Player Tracker");
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setLodestoneTracked(false);

            compass.setItemMeta(meta);
            for (UUID u : hunters) {
                meta.setLodestone(updateTracker(plugin.getServer().getPlayer(u)));
                compass.setItemMeta(meta);
                plugin.getServer().getPlayer(u).getInventory().addItem(compass);
            }
        }

        public Location updateTracker(Player player) {
            List<Double> runnerDistances = new ArrayList<>();
            Player target = null;

            for (UUID u : runners) {
                if (plugin.getServer().getPlayer(u).getLocation().getWorld().equals(player.getLocation().getWorld())) {
                    runnerDistances.add(player.getLocation().distance(plugin.getServer().getPlayer(u).getLocation()));
                }
            }

            if (runnerDistances.size() == 0) {
                player.sendMessage(ChatColor.RED + "There are no runners in your dimension");
            }

            Collections.sort(runnerDistances);
            for (UUID u : runners) {
                if (plugin.getServer().getPlayer(u).getLocation().getWorld().equals(player.getLocation().getWorld()) &&
                        player.getLocation().distance(plugin.getServer().getPlayer(u).getLocation()) == runnerDistances.get(0)) {
                    target = plugin.getServer().getPlayer(u);
                }
            }

            player.sendMessage(ChatColor.GREEN + "Player Tracker is now pointing to " + target.getName());
            return target.getLocation();
        }
    }
}
