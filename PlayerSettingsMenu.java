package de.power.gui;

import de.power.Power;
import de.power.Settings;
import de.power.bat.gui.Menu;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /settings - Einstellungen fuer das gesamte Plugin. */
public class PlayerSettingsMenu extends Menu {

    public PlayerSettingsMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() { return "&8Einstellungen"; }

    @Override
    protected int rows() { return 5; }

    @Override
    protected void build() {
        decorate();
        UUID id = viewer.getUniqueId();
        List<Integer> slots = innerSlots();
        int i = 0;
        for (Settings.Def d : Settings.DEFS) {
            if (i >= slots.size()) break;
            int slot = slots.get(i++);
            if (d.isAction()) {
                String cur = de.power.Lang.info(de.power.Lang.code(id)) == null ? "?" : de.power.Lang.info(de.power.Lang.code(id)).name;
                set(slot, new ItemBuilder(d.icon).name(d.name).lore(d.desc)
                        .lore("", "&7Aktuell: &f" + cur, "", "&eKlicke zum Ändern").build(),
                        e -> navigate(() -> new LanguageMenu(plugin, viewer, false)));
                continue;
            }
            String cur = Settings.get(id, d.key);
            List<String> lore = new ArrayList<>(d.desc);
            lore.add("");
            for (String opt : d.options) lore.add((opt.equals(cur) ? "&a▶ " : "&8   ") + Settings.optionName(opt));
            lore.add("");
            lore.add("&eLinksklick: &7nächste Option");
            lore.add("&eRechtsklick: &7vorherige Option");
            boolean glow = "ON".equals(cur) || "ALL".equals(cur);
            set(slot, new ItemBuilder(d.icon).name(d.name).lore(lore).glow(glow).build(), e -> {
                ClickType ct = e.getClick();
                int dir = (ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT) ? -1 : 1;
                Settings.set(id, d.key, Settings.next(d, Settings.get(id, d.key), dir));
                ((Power) plugin).applySettingChange(viewer, d.key);
                refresh();
            });
        }
        set(40, new ItemBuilder(Material.BARRIER).name("&c&lSchließen").lore("&7Einstellungen schließen.").build(),
                e -> viewer.closeInventory());
    }
}
