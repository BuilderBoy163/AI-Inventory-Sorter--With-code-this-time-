package com.aiinventorysort.client.gui;

import com.aiinventorysort.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Simple in-game config screen.
 * Open it from the Mods menu (ModMenu integration) or via the /aiinvsort config command.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget apiKeyField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("AI Inventory Sort — Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        // API key input
        apiKeyField = new TextFieldWidget(textRenderer, centerX - 150, centerY - 20, 300, 20,
                Text.literal("Anthropic API key"));
        apiKeyField.setMaxLength(200);
        apiKeyField.setText(ModConfig.get().apiKey);
        apiKeyField.setPlaceholder(Text.literal("sk-ant-..."));
        addDrawableChild(apiKeyField);

        // Save button
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> {
            ModConfig.get().apiKey = apiKeyField.getText().trim();
            ModConfig.save();
            client.setScreen(parent);
        }).dimensions(centerX - 80, centerY + 10, 75, 20).build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                btn -> client.setScreen(parent))
                .dimensions(centerX + 5, centerY + 10, 75, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Anthropic API key:"), width / 2 - 150, height / 2 - 35, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
