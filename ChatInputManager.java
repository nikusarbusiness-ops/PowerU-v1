package de.power.bat.utils;

import de.power.Power;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Erlaubt es, in GUIs Texteingaben über den Chat abzufragen. */
public class ChatInputManager {

    private static final long TIMEOUT_TICKS = 20L * 60L;

    private record Prompt(Consumer<String> onInput, Runnable onCancel, BukkitTask timeout) {
    }

    private final Power plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatInputManager(Power plugin) {
        this.plugin = plugin;
    }

    public boolean has(UUID id) {
        return prompts.containsKey(id);
    }

    public void request(Player player, String promptKey, Consumer<String> onInput, Runnable onCancel) {
        UUID id = player.getUniqueId();
        clear(id);
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Prompt prompt = prompts.remove(id);
            if (prompt != null) {
                Player online = Bukkit.getPlayer(id);
                if (online != null) {
                    plugin.getMessages().send(online, "gui.prompt-timeout");
                }
                if (prompt.onCancel() != null) {
                    prompt.onCancel().run();
                }
            }
        }, TIMEOUT_TICKS);
        prompts.put(id, new Prompt(onInput, onCancel, timeout));
        player.closeInventory();
        plugin.getMessages().send(player, promptKey);
        plugin.getMessages().send(player, "gui.prompt-cancel-hint");
    }

    /** Wird synchron im Hauptthread aufgerufen. */
    public void handle(Player player, String message) {
        Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        prompt.timeout().cancel();
        String trimmed = message.trim();
        if (trimmed.equalsIgnoreCase("cancel") || trimmed.equalsIgnoreCase("abbrechen")) {
            plugin.getMessages().send(player, "gui.prompt-cancelled");
            if (prompt.onCancel() != null) {
                prompt.onCancel().run();
            }
            return;
        }
        prompt.onInput().accept(trimmed);
    }

    public void clear(UUID id) {
        Prompt prompt = prompts.remove(id);
        if (prompt != null) {
            prompt.timeout().cancel();
        }
    }

    public void clearAll() {
        for (UUID id : prompts.keySet()) {
            clear(id);
        }
    }
}
