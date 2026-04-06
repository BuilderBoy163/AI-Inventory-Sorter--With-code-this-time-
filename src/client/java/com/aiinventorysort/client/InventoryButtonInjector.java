package com.aiinventorysort.client;

import com.aiinventorysort.client.mixin.InventoryScreenMixin;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class InventoryButtonInjector {

    public void injectButton(HandledScreen<?> screen, Runnable onSort) {
        // Use the mixin accessor to get the protected x/y fields from HandledScreen
        InventoryScreenMixin accessor = (InventoryScreenMixin) screen;
        int guiLeft = accessor.getX();
        int guiTop  = accessor.getY();

        Screens.getButtons(screen).add(
            ButtonWidget.builder(Text.literal("AI Sort"), button -> {
                if (onSort != null) onSort.run();
            })
            .dimensions(guiLeft + 10, guiTop + 30, 60, 20)
            .build()
        );
    }
}
