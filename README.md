# AI Inventory Sort — Fabric Mod

Adds two buttons to your Minecraft inventory screen:

- **AI Sort** — sends your current inventory to Claude AI, which reorganises it by survival priority (or matches your saved snapshot layout)
- **Snapshot** — saves your current inventory arrangement as a template the AI will replicate on future sorts

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | ≥ 0.16.5 |
| Fabric API | ≥ 0.103.0+1.21.1 |
| Java | 21 |

---

## Building

```bash
./gradlew build
```

The compiled `.jar` will be in `build/libs/aiinventorysort-1.0.0.jar`.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and drop it in your `mods/` folder.
3. Drop `aiinventorysort-1.0.0.jar` into your `mods/` folder.
4. Launch the game.

---

## Setting your API key

The mod needs an [Anthropic API key](https://console.anthropic.com/) to call Claude.

**Option A — Config file (recommended):**

Edit `.minecraft/config/aiinventorysort.json` and set:

```json
{
  "apiKey": "sk-ant-YOUR-KEY-HERE",
  "snapshotLayout": [],
  "hotbarPins": {},
  "showButtons": true
}
```

**Option B — In-game config screen:**

Open the Mods screen (requires ModMenu), find *AI Inventory Sort*, and click *Configure*.

> ⚠️ Your API key is stored in plaintext on disk. Do not share your config file.

---

## How the sorting works

1. When you press **AI Sort**, the mod collects every item in your 36 inventory slots.
2. It builds a prompt describing those items plus any snapshot / pin constraints.
3. It sends the prompt to `claude-sonnet-4-20250514` via the Anthropic API.
4. Claude returns a JSON array of 36 slot assignments.
5. The mod rearranges your inventory on-the-fly.

### Default sort order (no snapshot)

```
Hotbar  →  weapon · pickaxe · axe · food · shield · bow · torch · ender pearls
Main    →  combat gear · tools · food · resources · building blocks · misc
```

### Snapshot system

Press **Snapshot** with your preferred layout active.  The mod saves the arrangement to `config/aiinventorysort.json`.  On the next **AI Sort**, Claude will try to match that layout as closely as possible given the items currently in your inventory.

### Hotbar pins

Add entries to `hotbarPins` in the config to hard-lock specific items to specific hotbar slots:

```json
"hotbarPins": {
  "0": "minecraft:diamond_sword",
  "1": "minecraft:diamond_pickaxe",
  "4": "minecraft:cooked_beef"
}
```

---

## Project structure

```
src/
  main/java/com/aiinventorysort/
    AiInventorySort.java          # Mod entrypoint (server + client)
    config/ModConfig.java         # JSON config (API key, snapshot, pins)
    sorting/AiSorter.java         # API call + inventory application logic

  client/java/com/aiinventorysort/client/
    AiInventorySortClient.java    # Client entrypoint
    InventoryButtonInjector.java  # Adds Sort + Snapshot buttons to inventory
    mixin/InventoryScreenMixin.java
    gui/ConfigScreen.java         # In-game API key entry screen
```

---

## Privacy

- Your inventory contents are sent to Anthropic's API to perform the sort.
- No data is stored by Anthropic beyond what their standard API data retention policies cover.
- Your API key is stored locally and never transmitted anywhere other than `api.anthropic.com`.

---

## License

MIT
