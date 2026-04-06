package com.aiinventorysort.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the user's API key, inventory snapshot, and hotbar pin rules to
 * config/aiinventorysort.json.  All fields are intentionally public so Gson
 * can (de)serialize them without reflection tricks.
 */
public class ModConfig {

    // ── singleton ──────────────────────────────────────────────────────────
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("aiinventorysort.json");

    public static ModConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    // ── fields ─────────────────────────────────────────────────────────────

    /** Anthropic API key.  Never logged. */
    public String apiKey = "";

    /**
     * Snapshot of the player's preferred inventory layout.
     * Each entry is either an item registry id (e.g. "minecraft:diamond_sword")
     * or null for an empty slot.  Index 0-8 = hotbar, 9-35 = main inventory.
     */
    public List<String> snapshotLayout = new ArrayList<>();

    /**
     * Hard-pinned hotbar slots.  Key = hotbar slot index (0-8),
     * value = item registry id that must always go there.
     */
    public Map<Integer, String> hotbarPins = new HashMap<>();

    /** Whether to show the sort/snapshot buttons inside the inventory screen. */
    public boolean showButtons = true;

    // ── persistence ────────────────────────────────────────────────────────

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
                INSTANCE = GSON.fromJson(r, ModConfig.class);
                if (INSTANCE == null) INSTANCE = new ModConfig();
            } catch (IOException e) {
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, w);
        } catch (IOException e) {
            com.aiinventorysort.AiInventorySort.LOGGER.error("Failed to save config", e);
        }
    }
}
