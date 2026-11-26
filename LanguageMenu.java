package de.power.gui;

import de.power.Lang;
import de.power.Power;
import de.power.bat.gui.Menu;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Sprach-Auswahl (/language und beim ersten Einloggen). */
public class LanguageMenu extends Menu {

    private final boolean first;
    private boolean chosen;

    public LanguageMenu(Power plugin, Player viewer, boolean first) {
        super(plugin, viewer);
        this.first = first;
    }

    public boolean isFirst() { return first; }

    public boolean isChosen() { return chosen; }

    @Override
    protected String title() { return "&8Language &7/ &8Sprache"; }

    @Override
    protected int rows() {
        int n = Lang.available().size();
        return Math.min(6, Math.max(3, 2 + (int) Math.ceil(n / 7.0)));
    }

    @Override
    protected void build() {
        decorate();
        List<Integer> slots = innerSlots();
        String current = Lang.code(viewer.getUniqueId());
        int i = 0;
        for (Lang.Info info : new ArrayList<>(Lang.available())) {
            if (i >= slots.size()) break;
            boolean sel = info.code.equals(current) && Lang.hasChosen(viewer.getUniqueId());
            set(slots.get(i++), new ItemBuilder(info.icon)
                    .name("&6&l" + info.name)
                    .lore(sel ? "&a✔ Ausgewählt" : "&eKlicke zum Auswählen")
                    .glow(sel).build(), e -> choose(info));
        }
    }

    private void choose(Lang.Info info) {
        Lang.set(viewer.getUniqueId(), info.code);
        chosen = true;
        viewer.sendMessage(Lang.tr(viewer, "§8[§bPower§8] §7Sprache gesetzt: §f" + info.name));
        if (first) {
            viewer.sendMessage(Lang.tr(viewer, "§8[§bPower§8] §7Du kannst die Sprache jederzeit mit §b/language §7ändern."));
            Bukkit.getScheduler().runTask(plugin, () -> viewer.closeInventory());
        } else {
            refresh();
        }
    }

    /** Vom Close-Event: Wer das Menue beim ersten Login ohne Auswahl schliesst, bekommt die Standardsprache. */
    public void onClose() {
        if (first && !chosen && !Lang.hasChosen(viewer.getUniqueId())) {
            Lang.set(viewer.getUniqueId(), Lang.defaultCode());
            viewer.sendMessage(Lang.tr(viewer, "§8[§bPower§8] §7Standardsprache gesetzt. Ändern mit §b/language§7."));
        }
    }
}
