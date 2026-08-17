package com.nulvora.friends.paper.gui;

import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.cache.FriendCache;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class FriendsGUI implements Listener {

    private final NulvoraFriendsPaper plugin;
    private static final String TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Tus Amigos";

    public FriendsGUI(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<FriendCache.CachedFriend> friends = plugin.friendCache().getFriends(player.getUniqueId());
        int size = Math.min(((friends.size() / 9) + 1) * 9 + 9, 54);

        Inventory gui = Bukkit.createInventory(null, size, TITLE);

        if (friends.isEmpty()) {
            ItemStack empty = new ItemStack(Material.PAPER);
            ItemMeta emptyMeta = empty.getItemMeta();
            emptyMeta.setDisplayName(ChatColor.RED + "No tienes amigos");
            emptyMeta.setLore(List.of(
                ChatColor.GRAY + "Usa " + ChatColor.YELLOW + "/amigos add <jugador>" + ChatColor.GRAY + " para enviar una solicitud."
            ));
            empty.setItemMeta(emptyMeta);
            gui.setItem(4, empty);
        } else {
            for (int i = 0; i < friends.size() && i < size - 9; i++) {
                FriendCache.CachedFriend friend = friends.get(i);
                gui.setItem(i, createFriendItem(friend));
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createFriendItem(FriendCache.CachedFriend friend) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(friend.name());
        meta.setDisplayName(ChatColor.GREEN + friend.name());

        List<String> lore = new java.util.ArrayList<>();
        if (friend.online()) {
            String serverName = plugin.getConfig().getString("server-names." + friend.server(), friend.server());
            lore.add(ChatColor.GREEN + "En linea - " + ChatColor.YELLOW + serverName);
            lore.add("");
            lore.add(ChatColor.GRAY + "Click izquierdo: unirse al servidor");
            lore.add(ChatColor.GRAY + "Click derecho: eliminar amigo");
        } else {
            lore.add(ChatColor.GRAY + "Desconectado");
            lore.add("");
            lore.add(ChatColor.GRAY + "Click derecho: eliminar amigo");
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwner() == null) return;

        String friendName = meta.getOwner();

        if (event.isRightClick()) {
            player.performCommand("amigos eliminar " + friendName);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 5L);
        } else if (event.isLeftClick()) {
            player.performCommand("amigos join " + friendName);
            player.closeInventory();
        }
    }
}
