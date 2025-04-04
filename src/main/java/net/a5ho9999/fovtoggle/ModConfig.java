package net.a5ho9999.fovtoggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", FOVToggleMod.MOD_ID + ".json");

    private int fovValue = 50;

    public int getFovValue() {
        return fovValue;
    }

    public void setFovValue(int fovValue) {
        this.fovValue = fovValue;
    }

    public static ModConfig load() {
        ModConfig config = new ModConfig();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            File configFile = CONFIG_PATH.toFile();
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    config = GSON.fromJson(reader, ModConfig.class);
                    FOVToggleMod.LOGGER.info("Config loaded successfully");
                }
            } else {
                config.save();
                FOVToggleMod.LOGGER.info("Created default config");
            }
        } catch (IOException e) {
            FOVToggleMod.LOGGER.error("Failed to load config", e);
        }

        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
                FOVToggleMod.LOGGER.info("Config saved successfully");
            }
        } catch (IOException e) {
            FOVToggleMod.LOGGER.error("Failed to save config", e);
        }
    }
}
