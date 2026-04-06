package com.aiinventorysort.sorting;

import com.aiinventorysort.AiInventorySort;
import com.aiinventorysort.config.ModConfig;
import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class AiSorterClient implements AiSorter {

    private static final String API_URL = "https://ollama.com/v1/chat/completions";
    private static final String MODEL   = "gpt-oss:20b-cloud"; // Ollama Cloud model
    private static final Gson GSON      = new Gson();

    // Slot layout references
    private static final List<Integer> LEFT_THIRD   = List.of(9,10,11,18,19,20,27,28,29);
    private static final List<Integer> MIDDLE_THIRD = List.of(12,13,14,21,22,23,30,31,32);
    private static final List<Integer> RIGHT_THIRD  = List.of(15,16,17,24,25,26,33,34,35);

    @Override
    public void sortAsync(Consumer<String> onDone) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { onDone.accept("No player found."); return; }

        List<SlotInfo> slots = captureInventory(mc);
        if (slots.isEmpty()) { onDone.accept(null); return; }

        CompletableFuture.runAsync(() -> {
            try {
                int[] desiredPlayerSlots = callApi(slots);
                mc.execute(() -> {
                    applySort(mc, slots, desiredPlayerSlots);
                    onDone.accept(null);
                });
            } catch (Exception e) {
                AiInventorySort.LOGGER.error("AI sort failed", e);
                mc.execute(() -> onDone.accept("Error: " + e.getMessage()));
            }
        });
    }

    @Override
    public void takeSnapshot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        List<String> layout = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            layout.add(stack.isEmpty() ? null : Registries.ITEM.getId(stack.getItem()).toString());
        }
        ModConfig.get().snapshotLayout = layout;
        ModConfig.save();
    }

    private List<SlotInfo> captureInventory(MinecraftClient mc) {
        List<SlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            boolean isEnchanted = stack.get(DataComponentTypes.ENCHANTMENTS) != null
                    && !stack.get(DataComponentTypes.ENCHANTMENTS).isEmpty();

            List<ShulkerSlot> shulkerContents = null;
            if (isShulkerBox(itemId)) shulkerContents = readShulkerContents(stack);

            slots.add(new SlotInfo(i, itemId, stack.getCount(), isEnchanted, shulkerContents));
        }
        return slots;
    }

    private boolean isShulkerBox(String itemId) {
        return itemId.contains("shulker_box");
    }

    private List<ShulkerSlot> readShulkerContents(ItemStack shulker) {
        ContainerComponent container = shulker.get(DataComponentTypes.CONTAINER);
        if (container == null) return Collections.emptyList();

        List<ShulkerSlot> contents = new ArrayList<>();
        container.iterateNonEmpty().forEach(stack -> {
            if (!stack.isEmpty()) {
                contents.add(new ShulkerSlot(Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount()));
            }
        });
        return contents;
    }

    private String buildPrompt(List<SlotInfo> slots) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
You sort a Minecraft inventory into fixed slots.

HOTBAR (0–8, priority order, skip if none):
0 sword (netherite>diamond>iron)
1 mace
2 spear
3 axe
4 pickaxe
5 elytra or chestplate (prefer elytra)
6 shovel
7 best utility item
8 best food

MAIN (9–35):
LEFT  (9,10,11,18,19,20,27,28,29): shulkers/storage
MID   (12,13,14,21,22,23,30,31,32): tools, weapons, armor, blocks, misc
RIGHT (15,16,17,24,25,26,33,34,35): food/consumables

Rules:
- Fill each section in slot order
- Each item gets exactly one unique slot
- Unassigned hotbar items go to MAIN
- Shulkers ALWAYS go LEFT (ignore contents)

Output:
JSON only, same order:
[{"from":X,"to":Y},...]

Inventory:
""");
        for (SlotInfo s : slots) {
            sb.append("slot ").append(s.slot()).append(": ").append(s.itemId()).append(" x").append(s.count());
            if (s.enchanted()) sb.append(" [enchanted]");
            if (s.shulkerContents() != null && !s.shulkerContents().isEmpty()) {
                sb.append(" [shulker contents: ");
                List<String> parts = new ArrayList<>();
                for (ShulkerSlot ss : s.shulkerContents()) parts.add(ss.itemId() + " x" + ss.count());
                sb.append(String.join(", ", parts)).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private int[] callApi(List<SlotInfo> slots) throws Exception {
        String apiKey = ModConfig.get().apiKey;
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("No API key set. Put your Ollama API key in config.");
        apiKey = apiKey.trim();

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", buildPrompt(slots));
        messages.add(userMsg);
        requestBody.add("messages", messages);

        byte[] body = GSON.toJson(requestBody).getBytes(StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(300_000);

        try (OutputStream os = conn.getOutputStream()) { os.write(body); }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status < 200 || status >= 300)
            throw new IOException("Ollama API error " + status + ": " + response);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        String text = json.getAsJsonArray("choices")
                         .get(0).getAsJsonObject()
                         .get("message").getAsJsonObject()
                         .get("content").getAsString().trim();

        // REMOVE MARKDOWN WRAPPERS
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json\\s*", "")
               .replaceFirst("^```\\s*", "")
               .replaceAll("```$", "")
               .trim();
        }

        AiInventorySort.LOGGER.info("Ollama sort response: " + text);

        JsonArray arr = JsonParser.parseString(text).getAsJsonArray();
        Map<Integer, Integer> fromTo = new LinkedHashMap<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            fromTo.put(obj.get("from").getAsInt(), obj.get("to").getAsInt());
        }

        int[] result = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            int from = slots.get(i).slot();
            if (!fromTo.containsKey(from))
                throw new IOException("Ollama didn't assign a target for slot " + from);
            result[i] = fromTo.get(from);
        }
        return result;
    }

    private void applySort(MinecraftClient mc, List<SlotInfo> original, int[] desiredPlayerSlots) {
        if (mc.player == null || mc.interactionManager == null || mc.player.currentScreenHandler == null) return;
        if (desiredPlayerSlots.length != original.size()) {
            AiInventorySort.LOGGER.error("Ollama returned wrong number of slots: expected " + original.size() + " got " + desiredPlayerSlots.length);
            return;
        }

        boolean inventoryOpen = mc.player.currentScreenHandler.slots.size() > 36;
        int screenSlotCount = mc.player.currentScreenHandler.slots.size();
        int syncId = mc.player.currentScreenHandler.syncId;

        AiInventorySort.LOGGER.info("=== applySort DEBUG ===");
        AiInventorySort.LOGGER.info("Inventory open: " + inventoryOpen);
        AiInventorySort.LOGGER.info("Screen slot count: " + screenSlotCount);
        AiInventorySort.LOGGER.info("SyncId: " + syncId);
        AiInventorySort.LOGGER.info("Screen handler class: " + mc.player.currentScreenHandler.getClass().getName());

        Map<Integer, Integer> targetFor = new LinkedHashMap<>();
        for (int i = 0; i < original.size(); i++) {
            int from = original.get(i).slot();
            int to   = desiredPlayerSlots[i];
            if (!inventoryOpen && from >= 9) continue;
            if (!inventoryOpen && to >= 9) continue;
            if (from != to) targetFor.put(from, to);
        }

        AiInventorySort.LOGGER.info("Moves to make: " + targetFor.size());
        for (Map.Entry<Integer, Integer> entry : targetFor.entrySet()) {
            int fromScreen = playerSlotToScreenSlot(entry.getKey());
            int toScreen   = playerSlotToScreenSlot(entry.getValue());
            AiInventorySort.LOGGER.info("  playerSlot " + entry.getKey() + " -> " + entry.getValue()
                    + "  (screenSlot " + fromScreen + " -> " + toScreen + ")");
        }

        Set<Integer> done = new HashSet<>();
        for (int start : new ArrayList<>(targetFor.keySet())) {
            if (done.contains(start)) continue;
            List<Integer> chain = new ArrayList<>();
            Set<Integer> seen = new LinkedHashSet<>();
            int cur = start;
            while (!done.contains(cur) && targetFor.containsKey(cur) && seen.add(cur)) { chain.add(cur); cur = targetFor.get(cur); }
            boolean isCycle = chain.contains(cur);
            done.addAll(chain);
            if (chain.isEmpty()) continue;

            AiInventorySort.LOGGER.info("Processing chain: " + chain + " isCycle=" + isCycle);

            if (isCycle) {
                int firstScreen = playerSlotToScreenSlot(chain.get(0));
                AiInventorySort.LOGGER.info("  [cycle] picking up from screenSlot " + firstScreen);
                mc.interactionManager.clickSlot(syncId, firstScreen, 0, SlotActionType.PICKUP, mc.player);

                for (int i = 1; i < chain.size(); i++) {
                    int targetScreen = playerSlotToScreenSlot(targetFor.get(chain.get(i - 1)));
                    AiInventorySort.LOGGER.info("  [cycle] clicking screenSlot " + targetScreen);
                    mc.interactionManager.clickSlot(syncId, targetScreen, 0, SlotActionType.PICKUP, mc.player);
                }

                int lastScreen = playerSlotToScreenSlot(targetFor.get(chain.get(chain.size() - 1)));
                AiInventorySort.LOGGER.info("  [cycle] final click screenSlot " + lastScreen);
                mc.interactionManager.clickSlot(syncId, lastScreen, 0, SlotActionType.PICKUP, mc.player);
            } else {
                int firstScreen = playerSlotToScreenSlot(chain.get(0));
                AiInventorySort.LOGGER.info("  [chain] picking up from screenSlot " + firstScreen);
                mc.interactionManager.clickSlot(syncId, firstScreen, 0, SlotActionType.PICKUP, mc.player);

                for (int from : chain) {
                    int targetScreen = playerSlotToScreenSlot(targetFor.get(from));
                    AiInventorySort.LOGGER.info("  [chain] clicking screenSlot " + targetScreen);
                    mc.interactionManager.clickSlot(syncId, targetScreen, 0, SlotActionType.PICKUP, mc.player);
                }

                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    AiInventorySort.LOGGER.info("  [chain] cursor not empty, returning to screenSlot " + firstScreen);
                    mc.interactionManager.clickSlot(syncId, firstScreen, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
        AiInventorySort.LOGGER.info("AI sort applied " + targetFor.size() + " moves. Inventory open? " + inventoryOpen);
        AiInventorySort.LOGGER.info("=== applySort DEBUG END ===");
    }

    private int playerSlotToScreenSlot(int playerSlot) {
        return playerSlot < 9 ? playerSlot + 36 : playerSlot;
    }

    private record SlotInfo(int slot, String itemId, int count, boolean enchanted, List<ShulkerSlot> shulkerContents) {}
    private record ShulkerSlot(String itemId, int count) {}
}