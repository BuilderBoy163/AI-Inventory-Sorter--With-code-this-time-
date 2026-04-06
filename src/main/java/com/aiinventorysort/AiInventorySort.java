package com.aiinventorysort;

import com.aiinventorysort.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiInventorySort implements ModInitializer {

    public static final String MOD_ID = "aiinventorysort";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Log exact config path so we can verify file placement
        LOGGER.info("AI Inventory Sort config path: "
                + FabricLoader.getInstance().getConfigDir().resolve("aiinventorysort.json"));

        ModConfig.load();

        LOGGER.info("AI Inventory Sort initialized. API key set: "
                + (!ModConfig.get().apiKey.isBlank()));
    }
}