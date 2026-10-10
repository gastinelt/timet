package com.timet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class TimeConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TimeConfig INSTANCE;

    public boolean enabled = true;
    public long offsetSeconds = 0;

    public static synchronized TimeConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static synchronized void load() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File configFile = configDir.resolve("timet.json").toFile();
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    INSTANCE = GSON.fromJson(reader, TimeConfig.class);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement de config/timet.json", e);
        }

        if (INSTANCE == null) {
            INSTANCE = new TimeConfig();
        }
        save();
    }

    public static synchronized void save() {
        if (INSTANCE == null) return;
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File configFile = configDir.resolve("timet.json").toFile();
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la sauvegarde de config/timet.json", e);
        }
    }
}
