package de.power.gui;

import de.power.Power;
import de.power.bat.gui.Menu;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /news - uebersichtliches News-GUI mit Seiten, Datum und NEU-Markierung. */
public class NewsMenu extends Menu {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final int page;

    public NewsMenu(Power plugin, Player viewer, int page) {
        super(plugin, viewer);
        this.page = Math.max(0, page);
    }

    @Override
    protected String title() { return "&8News & Features"; }

    @Override
    protected int rows() { return 6; }

    private String ago(String date) {
        try {
            long d = ChronoUnit.DAYS.between(LocalDate.parse(date, DATE), LocalDate.now());
            if (d <= 0) return "&aHeute";
            if (d == 1) return "&eGestern";
            return de.power.Lang.SOURCE.equals(de.power.Lang.code(viewer)) ? "&7vor " + d + " Tagen" : "&7" + d + " days ago";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isNew(String date) {
        try {
            return ChronoUnit.DAYS.between(LocalDate.parse(date, DATE), LocalDate.now()) <= 3;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void build() {
        decorate();
        List<String> all = new ArrayList<>(plugin.getData().getStringList("news"));
        Collections.reverse(all);
        List<Integer> slots = innerSlots();
        int per = slots.size();
        int pages = Math.max(1, (all.size() + per - 1) / per);
        int pg = Math.min(page, pages - 1);

        set(4, new ItemBuilder(Material.BOOKSHELF).name("&6&lNews & Features")
                .lore("&7Alle Neuigkeiten des Servers", "&7auf einen Blick.", "", "&7Einträge: &e" + all.size()
                        + " &8| &7Seite: &e" + (pg + 1) + "&8/&e" + pages).build());

        if (all.isEmpty()) {
            set(22, new ItemBuilder(Material.BARRIER).name("&cNoch keine News").lore("&7Schau später nochmal vorbei!").build());
        }
        boolean admin = viewer.hasPermission("power.news.admin");
        for (int i = 0; i < per; i++) {
            int idx = pg * per + i;
            if (idx >= all.size()) break;
            String[] parts = all.get(idx).split("\\|", 2);
            String date = parts.length > 1 ? parts[0] : "";
            String text = parts.length > 1 ? parts[1] : parts[0];
            boolean fresh = isNew(date);
            List<String> lore = new ArrayList<>(plugin.wrap(Power.color(text), 34));
            lore.add("");
            lore.add("&8" + date + (ago(date).isEmpty() ? "" : " &8(" + ago(date) + "&8)"));
            if (admin) lore.add("&8/news remove " + (idx + 1));
            set(slots.get(i), new ItemBuilder(fresh ? Material.NETHER_STAR : Material.BOOK)
                    .name((fresh ? "&e&lNEU &8#" : "&6News &8#") + (idx + 1)).lore(lore).glow(fresh).build());
        }
        if (pg > 0) set(45, new ItemBuilder(Material.ARROW).name("&e« Vorherige Seite").build(),
                e -> new NewsMenu(plugin, viewer, pg - 1).open());
        if (pg < pages - 1) set(53, new ItemBuilder(Material.ARROW).name("&eNächste Seite »").build(),
                e -> new NewsMenu(plugin, viewer, pg + 1).open());
        set(49, new ItemBuilder(Material.BARRIER).name("&c&lSchließen").build(), e -> viewer.closeInventory());
    }
}
