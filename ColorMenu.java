package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.ClanColor;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class ColorMenu extends ClanMenu {

    public ColorMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Farbe wählen";
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void buildClan() {
        List<Integer> slots = innerSlots();
        ClanColor[] colors = ClanColor.values();
        for (int i = 0; i < colors.length && i < slots.size(); i++) {
            ClanColor color = colors[i];
            boolean selected = color.getCode().equalsIgnoreCase(clan.getColor());
            set(slots.get(i), new ItemBuilder(color.getMaterial())
                    .name(color.getCode() + "&l" + color.getDisplay())
                    .lore("&7Vorschau: " + color.getCode() + clan.getName(), "",
                            selected ? "&a✔ Aktuelle Farbe" : "&eKlicke zum Auswählen")
                    .glow(selected)
                    .build(), event -> {
                plugin.getClanManager().setColor(viewer, color.getCode());
                navigate(() -> new SettingsMenu(plugin, viewer));
            });
        }
        set(31, new ItemBuilder(Material.PAPER)
                .name("&b&lHex-Farben")
                .lore("&7Eigene Farbe per Befehl setzen:", "&f/clan setcolor #RRGGBB", "&7Beispiel: &f/clan setcolor #ff8800")
                .build());
        back(40, () -> new SettingsMenu(plugin, viewer));
    }
}
