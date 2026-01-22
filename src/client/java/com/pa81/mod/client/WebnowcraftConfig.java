package com.pa81.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WebnowcraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "webnowcraft.json");

    // 設定値のデフォルト
    public int xOffset = -10; // 画面端からのオフセット
    public int yOffset = 10;
    public float scale = 1.0f;
    public boolean anchorLeft = false; // 左上基準にするかどうか

    private static WebnowcraftConfig instance;

    public static WebnowcraftConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, WebnowcraftConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (instance == null) {
            instance = new WebnowcraftConfig();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}