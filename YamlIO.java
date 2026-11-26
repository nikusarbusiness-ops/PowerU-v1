package de.power.bat.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

final class YamlIO {

    private YamlIO() {
    }

    /** Schreibt erst in eine temporäre Datei und ersetzt dann die Zieldatei (verhindert beschädigte Daten). */
    static void save(YamlConfiguration config, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temp = new File(target.getPath() + ".tmp");
        config.save(temp);
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
