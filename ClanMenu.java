package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.ClanPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Menü, das einen Clan voraussetzt. Existiert der Clan nicht mehr, wird das Hauptmenü geöffnet. */
public abstract class ClanMenu extends Menu {

    protected Clan clan;

    protected ClanMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    protected Clan currentClan() {
        return plugin.getClanManager().getClan(viewer.getUniqueId());
    }

    @Override
    protected final void build() {
        clan = currentClan();
        decorate();
        if (clan == null) {
            Bukkit.getScheduler().runTask(plugin, () -> MainMenu.create(plugin, viewer).open());
            return;
        }
        buildClan();
    }

    protected abstract void buildClan();

    protected boolean can(ClanPermission permission) {
        return clan != null && plugin.getClanManager().hasPermission(clan, viewer.getUniqueId(), permission);
    }
}
