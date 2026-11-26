package de.power.bat.gui;

import de.power.Lang;
import de.power.Power;
import de.power.Settings;
import de.power.bat.utils.Colors;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Basisklasse aller Power-GUIs: dunkles Design mit gelb/orange/roten Akzenten. */
public abstract class Menu implements InventoryHolder {

    protected final Power plugin;
    protected final Player viewer;
    protected Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(Power plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    protected abstract String title();

    protected abstract int rows();

    protected abstract void build();

    public void open() {
        inventory = Bukkit.createInventory(this, rows() * 9, Lang.tr(viewer, Colors.color(title())));
        actions.clear();
        build();
        Lang.translate(inventory, viewer);
        viewer.openInventory(inventory);
    }

    /** Baut das Menü neu auf, ohne es zu schließen. */
    public void refresh() {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        actions.clear();
        build();
        Lang.translate(inventory, viewer);
        viewer.updateInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getSlot());
        if (action != null) {
            if (Settings.isOn(viewer.getUniqueId(), "sounds")) {
                viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            }
            action.accept(event);
        }
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    /** Dunkler Hintergrund mit gelb/orangenem Rahmen und roten Ecken. */
    protected void decorate() {
        int rows = rows();
        for (int i = 0; i < rows * 9; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean edgeRow = row == 0 || row == rows - 1;
            boolean edgeCol = col == 0 || col == 8;
            Material material;
            if (edgeRow && edgeCol) {
                material = Material.RED_STAINED_GLASS_PANE;
            } else if (edgeRow || edgeCol) {
                material = (row + col) % 2 == 0 ? Material.ORANGE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
            } else {
                material = Material.BLACK_STAINED_GLASS_PANE;
            }
            inventory.setItem(i, new ItemBuilder(material).name(" ").build());
        }
    }

    /** Alle Slots im Inneren (ohne Rahmen). */
    protected List<Integer> innerSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < rows() - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    /** Öffnet ein anderes Menü im nächsten Tick (vermeidet Probleme beim Wechsel innerhalb von Klick-Events). */
    protected void navigate(Supplier<Menu> next) {
        Bukkit.getScheduler().runTask(plugin, () -> next.get().open());
    }

    protected void back(int slot, Supplier<Menu> target) {
        set(slot, new ItemBuilder(Material.ARROW)
                .name("&e« Zurück")
                .lore("&7Zurück zum vorherigen Menü")
                .build(), event -> navigate(target));
    }

    protected ItemStack locked(String name, String reason) {
        return new ItemBuilder(Material.GRAY_DYE)
                .name("&8" + Colors.strip(name))
                .lore("&c✘ " + reason)
                .build();
    }

    protected ItemStack filler(Material material) {
        return new ItemBuilder(material).name(" ").build();
    }
}
