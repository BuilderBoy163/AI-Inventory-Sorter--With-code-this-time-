package com.aiinventorysort.client;

import com.aiinventorysort.AiInventorySort;
import com.aiinventorysort.client.mixin.InventoryScreenMixin;
import com.aiinventorysort.sorting.AiSorterClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class AiInventorySortClient implements ClientModInitializer {

    private final AiSorterClient sorter = new AiSorterClient();

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?> handledScreen) {
                injectSorterButton(handledScreen);
            }
        });
    }

    private void injectSorterButton(HandledScreen<?> screen) {
        InventoryScreenMixin accessor = (InventoryScreenMixin) screen;
        int guiLeft = accessor.getX();
        int guiTop  = accessor.getY();

        // Standard inventory grid: 9 slots wide, each slot 18px, starting at x+8
        // Top-right slot left edge = guiLeft + 8 + (8 * 18) = guiLeft + 152
        // Button is 10px wide, so place at guiLeft + 152 + (18/2) - 5 = guiLeft + 158
        // Place it just above the top row: guiTop + 84 - 12 = guiTop + 72
        Screens.getButtons(screen).add(
            ButtonWidget.builder(Text.literal("S"), button -> {
                button.active = false;
                sorter.sortAsync(result -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.execute(() -> {
                        button.active = true;
                        if (result != null) {
                            AiInventorySort.LOGGER.warn("AI Sort error: " + result);
                        } else {
                            AiInventorySort.LOGGER.info("Inventory sorted by AI!");
                        }
                    });
                });
            })
            .dimensions(guiLeft + 158, guiTop + 52, 10, 10)
            .build()
        );
    }
}