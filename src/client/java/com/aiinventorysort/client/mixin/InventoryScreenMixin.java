package com.aiinventorysort.client.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected x/y position fields of {@link net.minecraft.client.gui.screen.ingame.HandledScreen}
 * so {@link com.aiinventorysort.client.InventoryButtonInjector} can place buttons correctly.
 */
@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public interface InventoryScreenMixin {

    @Accessor("x")
    int getX();

    @Accessor("y")
    int getY();
}
