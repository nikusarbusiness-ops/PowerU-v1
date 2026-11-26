package de.power.gui;

import de.power.DailyManager;
import de.power.Power;
import de.power.bat.gui.Menu;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /daily - Uebersicht der Streak-Belohnungen. */
public class DailyMenu extends Menu {

    public DailyMenu(Power plugin, Player viewer) { super(plugin, viewer); }

    @Override
    protected String title() { return "&8Daily-Belohnung"; }

    @Override
    protected int rows() { return 6; }

    @Override
    protected void build() {
        decorate();
        DailyManager dm = plugin.daily();
        UUID id = viewer.getUniqueId();
        int cycle = dm.cycle();
        int streak = dm.streak(id);
        boolean claimed = dm.claimedToday(id);
        int claimDay = dm.dayIndex(dm.streakAfterClaim(id));      // Tag, der jetzt abholbar waere
        int doneDay = streak < 1 ? 0 : dm.dayIndex(streak);       // zuletzt abgeholter Tag im Zyklus

        set(4, new ItemBuilder(Material.CLOCK).name("&6&lDeine Login-Streak")
                .lore("&7Streak: &e" + streak + " &7Tag(e)",
                        claimed ? "&aHeute bereits abgeholt." : "&eDeine Belohnung ist bereit!",
                        "", "&7Logge dich jeden Tag ein und hole", "&7deine Belohnung ab.").glow(!claimed).build());

        List<Integer> slots = innerSlots();
        for (int day = 1; day <= cycle && day <= slots.size(); day++) {
            ConfigurationSection r = dm.reward(day);
            String name = r == null ? "&aTag " + day : r.getString("name", "&aTag " + day);
            Material icon = r == null ? Material.PAPER : Material.matchMaterial(r.getString("icon", "PAPER"));
            if (icon == null || !icon.isItem()) icon = Material.PAPER;
            List<String> lore = new ArrayList<>(dm.describe(day));
            lore.add("");
            boolean isDone = claimed ? day <= doneDay : day < claimDay;
            boolean isNow = !claimed && day == claimDay;
            if (isDone) {
                lore.add("&a✔ Abgeholt");
                icon = Material.LIME_STAINED_GLASS_PANE;
            } else if (isNow) {
                lore.add("&eKlicke zum Abholen!");
            } else {
                lore.add("&8Noch gesperrt");
            }
            ItemBuilder b = new ItemBuilder(icon).name(name).lore(lore).glow(isNow).amount(day);
            if (isNow) set(slots.get(day - 1), b.build(), e -> claim());
            else set(slots.get(day - 1), b.build());
        }
        if (!claimed) {
            set(49, new ItemBuilder(Material.EMERALD_BLOCK).name("&a&lBelohnung abholen")
                    .lore("&7Klicke, um Tag &e" + claimDay + " &7abzuholen.").glow(true).build(), e -> claim());
        } else {
            set(49, new ItemBuilder(Material.BARRIER).name("&c&lSchließen").build(), e -> viewer.closeInventory());
        }
    }

    private void claim() {
        plugin.daily().claim(viewer);
        refresh();
    }
}
