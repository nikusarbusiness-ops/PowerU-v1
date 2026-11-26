package de.power.bat.utils;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final List<String> lore = new ArrayList<>();
    private String name;
    private boolean glow;
    private OfflinePlayer skullOwner;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            lore.add(line);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        lore.addAll(lines);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemBuilder head(OfflinePlayer player) {
        this.skullOwner = player;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (name != null) {
            meta.setDisplayName(Colors.color(name));
        }
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(Colors.color(line));
            }
            meta.setLore(colored);
        }
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (skullOwner != null && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(skullOwner);
        }
        item.setItemMeta(meta);
        return item;
    }
}
