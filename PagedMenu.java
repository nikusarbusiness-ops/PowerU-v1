package de.power.bat.gui;

import de.power.Power;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Menü mit Seiten (28 Einträge pro Seite) und Navigation in der unteren Reihe. */
public abstract class PagedMenu<T> extends Menu {

    protected int page = 0;

    protected PagedMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected int rows() {
        return 6;
    }

    protected abstract List<T> items();

    protected abstract ItemStack render(T item);

    protected abstract void clicked(T item, InventoryClickEvent event);

    protected abstract Supplier<Menu> backTarget();

    protected String emptyText() {
        return "&7Hier gibt es noch nichts zu sehen.";
    }

    /** Hook für zusätzliche Items (nach der Standard-Navigation). */
    protected void extra() {
    }

    @Override
    protected void build() {
        decorate();
        List<T> all = items();
        List<Integer> slots = innerSlots();
        int perPage = slots.size();
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) perPage));
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < all.size(); i++) {
            T item = all.get(start + i);
            set(slots.get(i), render(item), event -> clicked(item, event));
        }
        if (all.isEmpty()) {
            set(22, new ItemBuilder(Material.BARRIER).name("&c&lKeine Einträge").lore(emptyText()).build());
        }
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("&e« Vorherige Seite")
                    .lore("&7Seite &f" + page + "&7/&f" + pages)
                    .build(), event -> {
                page--;
                refresh();
            });
        }
        if (page < pages - 1) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("&eNächste Seite »")
                    .lore("&7Seite &f" + (page + 2) + "&7/&f" + pages)
                    .build(), event -> {
                page++;
                refresh();
            });
        }
        back(49, backTarget());
        extra();
    }
}
