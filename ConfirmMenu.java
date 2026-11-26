package de.power.bat.gui;

import de.power.Power;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Supplier;

/** Allgemeiner Bestätigungsdialog. */
public class ConfirmMenu extends Menu {

    private final String title;
    private final List<String> info;
    private final Runnable onConfirm;
    private final Supplier<Menu> onCancel;

    public ConfirmMenu(Power plugin, Player viewer, String title, List<String> info,
                       Runnable onConfirm, Supplier<Menu> onCancel) {
        super(plugin, viewer);
        this.title = title;
        this.info = info;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected String title() {
        return title;
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void build() {
        decorate();
        set(11, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("&a&l✔ Bestätigen")
                .lore("&7Klicke, um fortzufahren.")
                .build(), event -> onConfirm.run());
        set(13, new ItemBuilder(Material.PAPER)
                .name("&e&lBist du sicher?")
                .lore(info)
                .build());
        set(15, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&c&l✘ Abbrechen")
                .lore("&7Zurück, ohne etwas zu ändern.")
                .build(), event -> navigate(onCancel));
    }
}
