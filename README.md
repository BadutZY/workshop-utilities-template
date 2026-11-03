# Workshop Utilities (Fabric 1.21)

A Minecraft 1.21 mod for Fabric that adds four magical workshop tables with their own GUIs, plus custom enchantments.

> The display name is **Workshop Utilities**. The mod ID is `workshop_utilities` (also used for the asset/data folders and language keys). The Java package is `com.workshoputilities`.
>
> **Upgrading from an older build:** earlier builds used the mod ID `uncraftdisenchant`. Blocks, items and enchantments are registered under the mod ID, so worlds created with the old ID will not recognize them under the new one.

## What's inside

### Uncrafting Table
- GUI: 1 input slot on the left, a 3x3 output grid on the right.
- Put an item/block in the input slot and the grid shows the ingredients you get back (the reverse of a crafting table).
- Taking one ingredient consumes the input in the amount of the recipe result (e.g. 4 planks for 1 log), after which all ingredients can be taken. Shift-click on the output moves all ingredients at once.
- If an item has several recipes (e.g. planks from other log types), the "<" and ">" buttons below the input slot choose the recipe. The text between the buttons shows the selected recipe (e.g. 1/3).
- All recipes are read from the RecipeManager, so items/blocks from other mods are supported as long as they use normal shaped or shapeless crafting recipes.
- Skipped for safety: special recipes (firework, map cloning, etc.), recipes that leave a remainder item (e.g. cake with buckets), and recipes that use their own result as an ingredient.
- Tag ingredients (e.g. planks) are resolved to the one most similar to the item name, so oak_stairs gives oak_planks.

### Dis-Enchanting Table
- It is a vanilla grindstone with its own GUI: `DisenchantingTableBlock` extends `GrindstoneBlock`, so it **can be attached to floors, walls and ceilings**, with the exact same shape/outline/placement/rotation, and the block properties (hardness, **stone sounds**, tool requirement, pistons) are copied from `Blocks.GRINDSTONE`. The sound when taking the output is `block.grindstone.use`. The blockstate uses the vanilla grindstone `face` + `facing` variants with a custom model. The upgraded grindstone textures come from `tools/generate_grindstone_textures.py`; preview the model without the game with `tools/preview_block_model.py disenchanting_table out.png`.
- GUI: 2 input slots on the left (top: enchanted item, bottom: book), 2 output slots on the right.
- An enchanted item in the top slot produces the same item without enchantments in the top output.
- If there is a book in the bottom slot, the bottom output contains 1 book holding all of that item's enchantments at once. If the enchantments are **a mix of vanilla + mod**, the result is a **Fused Enchanted Book** (a special item, texture from `tools/generate_fused_book_texture.py`, no recipe and not in any creative tab); if they are all vanilla or all mod, the result is a regular enchanted book.
- Taking the output consumes 1 item and 1 book (if present).
- Survival/Adventure players receive XP levels according to the enchantments. In **Creative** mode no XP is given (the "+N Levels" label is also hidden).

### Upgrade Table
- It is a vanilla anvil with its own GUI: `UpgradeTableBlock` extends `AnvilBlock`, so it **falls when unsupported** (like sand), **hurts entities it lands on** (2 damage per block fallen, max 40, anvil damage source), uses the anvil landing/break sounds and block sounds, and its properties are copied from `Blocks.ANVIL`. Purple-obsidian texture (the old Dis-Enchanting Table texture). There is no chipped/damaged state like the real anvil. GUI: **2 input slots on top** (left: the item/Backpack to upgrade, right: the material), **1 slot below them** (Upgrade Tool), and **1 result slot**. Empty slots show a ghost icon (pickaxe, enchanted book, Upgrade Tool), a small text under the result slot explains what is missing or why the combination is invalid, and a red cross appears on the arrow when it is invalid.
- The result is only a preview: the item, the material and **1 Upgrade Tool** are only consumed when the result is taken (normal click or shift-click). No XP cost.
- **Enchant**: left = any suitable item (tools, armor, or ordinary items for Unlimited), right = a mod-made enchanted book or a **Fused Enchanted Book**. ALL enchantments on the book are applied (vanilla and mod), and other enchantments on the item stay. Enchantments the item already has at the same or a higher level are skipped; enchantments with a lower level are raised to the book's level. Rejected if the item does not support that enchantment, already has it, or it conflicts (exclusive set). Items with Infinite Durability are fully repaired immediately.
- **Upgrade Backpack**: left = Backpack, right = 8 materials for that tier (Small: iron ingot, Normal: gold ingot, Medium: diamond, Big: netherite ingot), bottom = Upgrade Tool. The result is the next-tier Backpack with the same contents, custom name and enchantments.
- **The anvil can no longer apply mod-made enchantments** (empty result, including in Creative). Only via the Upgrade Table. This is handled in `AnvilScreenHandlerMixin` + `ModEnchantments.wouldTransferModEnchantment`. The Anvil recipe Upgrade Enchanted + Book is unchanged.
- **Only the Small Backpack can be crafted and uncrafted.** Normal and above have no crafting recipe and are only reached through the Upgrade Table.
- The rules are in `upgrade/UpgradeTableLogic.java` (used by the server and the GUI), the handler is `screen/UpgradeTableScreenHandler.java`, and the GUI is `client/UpgradeTableScreen.java`. Slot positions are constants in the handler and must match `tools/generate_upgrade_table_textures.py`. To change the number of Backpack upgrade materials, edit `UpgradeTableLogic.BACKPACK_MATERIAL_COUNT`.

### Upgrade Enchanting Table
- Looks and emits particles like a vanilla enchanting table, but can only be used with at least 20 bookshelves around it (the count is shown in the GUI).
- A floating book above the table turns to face the player, like the vanilla enchanting table.
- GUI: an input slot (Upgrade Book), a list of enchanted book choices (max 3 rows), and an output slot. Click a choice: the Upgrade Book is consumed and the enchanted book appears in the output slot (the output slot must be empty).
- Each Upgrade Book used costs 15 XP levels, **except in Creative mode (free, XP levels are neither checked nor deducted)**. If XP or bookshelves are lacking, a red notice appears in the action bar when choosing, and hovering a choice shows the reason.
- Current choices: **Infinite Durability** (level 1, durable items never lose durability), **Ender Blind**, **Snow Walker** and **Frozen Ward** (see below). If there are more than 3 choices, the list scrolls with the mouse wheel (a scroll indicator is shown on the panel). Hovering a choice in the GUI only shows the book name; the function description and the items it can be applied to are shown in the enchanted book tooltip in the inventory while holding SHIFT.
- **Ender Blind** (level 1, helmet only): Endermen do not get angry when you look at their eyes, and an Enderman that is already angry at the player calms down as soon as the player wears a helmet with Ender Blind. Costs 15 XP levels like the other choices. Texture: `tools/generate_ender_blind_texture.py`.
- **Snow Walker** (level 1, boots only): the player can walk on powder snow without sinking, like wearing leather boots (still sinks when sneaking or falling from a height, as in vanilla). Costs 15 XP levels. Texture: `tools/generate_snow_walker_texture.py`.
- **Frozen Ward** (level 1, all armor, not tools): the wearer cannot freeze, even when sunk in powder snow, as long as any worn armor piece has this enchantment. Costs 15 XP levels. Texture: `tools/generate_frozen_ward_texture.py`.
- **Silent Step** (level 1, leggings only): the wearer is completely undetectable by sculk sensors, calibrated sculk sensors, sculk shriekers and the Warden (steps, jumps, landings and other sounds/vibrations from the wearer are ignored, including when stepping directly on the sensor), and does not trigger tripwires or pressure plates. Costs 15 XP levels like the other choices. Texture: `tools/generate_silent_step_texture.py`.
- **Piglin Protection** (level 1, all armor, not tools): Piglins treat the wearer as if wearing gold armor, so they do not attack as long as any worn armor piece has this enchantment. Like vanilla gold armor, Piglins still get angry if the player opens a chest or mines a gold block, and Piglin Brutes still attack. Costs 15 XP levels like the other choices. Texture: `tools/generate_piglin_protection_texture.py`.
- **Vein Miner** (level 1, pickaxe only): breaking one ore also mines adjacent ores of the same kind (26 directions, max 64 blocks; iron ore and deepslate iron ore count as the same kind). Applies to vanilla ores including nether quartz, nether gold and ancient debris. Vanilla tool rules still apply (durability decreases per block). Texture: `tools/generate_new_book_textures.py`.
- **Thunder Slash** (level 1, sword only): every third successful hit summons lightning on the target (5 damage + fire) plus 4 bonus damage. The lightning is cosmetic for nearby players. Constants in `ThunderSlash.java`.
- **Anti-Gravity** (level 1, shovel only): while this shovel is held, sand, gravel, concrete powder and other gravity blocks within 8 blocks do not fall even if the block below is broken. Logic in `FallingBlockMixin` and `ToolEnchantments.isAntiGravityActive`.
- **Woodcutter** (level 1, axe only): breaking one log fells all logs connected upward/sideways (max 256). Only applies to trees (with natural leaves or an attached wart block), so player-built log structures are safe.
- **More Harvest** (level 1, hoe only): right-clicking a crop (or the farmland under it) harvests all mature crops in a 3x3 area (crops, including nether wart), drops the results, and replants using one seed from the harvest. The hoe loses 1 durability per click.
- **Water Breathing** (level 1, helmet only): the wearer can breathe underwater indefinitely; oxygen does not decrease. Done via the `canBreatheInWater` mixin, with no status effect so there is no icon. The oxygen bubble bar in the HUD is also hidden via `client/mixin/InGameHudMixin`.
- **Swim Fast** (level 1, chestplate only): swims as fast as Dolphin's Grace (water drag 0.96), without a status effect. Done via the `travel` mixin in `LivingEntityMixin`.
- **Fire Resistant** (level 1, all armor, not tools): as long as any worn armor piece has this enchantment, the player is immune to fire and lava (does not burn, takes no fire/lava/magma/fireball damage). Done via the `isFireImmune` mixin in `EntityMixin`; explosions and non-fire damage still apply.
- **Unlimited** (level 1, all items/blocks WITHOUT durability; items with durability are rejected by the anvil): the stack count never decreases on use (blocks placed, food eaten, ender pearls/snowballs/eggs thrown, fireworks, bone meal, arrows shot, etc.). The "no durability" rule lives in `EnchantmentMixin` (it cannot be made via JSON), the decrement cancellation is in `ItemStackMixin` + `UnlimitedScope`; moving/splitting items in the inventory stays normal so there are no duplication loopholes. The usage scope covers: use/useOnBlock/useOnEntity/finishUsing/onStoppedUsing, block interactions (`ServerPlayerInteractionManagerMixin`: composter, flower pot, lectern, jukebox, etc.), entity interactions (`PlayerEntityMixin`: feeding animals, leads, item frames, etc.), and totems (`tryUseTotem` in `LivingEntityMixin`). Items that turn into another item after use (water/lava bucket becoming an empty bucket, stew becoming a bowl) stay as the original item (including the powder snow bucket, which in vanilla replaces the hand with an empty bucket). Empty containers left over from use (bucket, glass bottle, bowl) are not given to the inventory for Unlimited items (`PlayerInventoryMixin`).
- **Night Vision** (level 1, helmet only): the wearer sees clearly in the dark without the Night Vision status effect (no icon/particles, and it neither grants nor depends on that effect). Purely client side: `client/mixin/LightmapTextureManagerMixin` (lightmap) + `client/mixin/GameRendererMixin` (full strength).
- **Lifesteal** (level 1, sword and axe): killing a mob with a direct attack restores a random 1-5 hearts of health (1 heart = 2 health, never above max health). The `MIN_HEARTS`/`MAX_HEARTS` constants are in `Lifesteal.java`. Item tag: `workshop_utilities:lifesteal_items` (swords + axes combined).
- **Hunger Saver** (level 1, leggings only): all exhaustion (sprinting, jumping, swimming, mining, attacking, etc.) is multiplied by 0.5 via `PlayerHungerMixin`. Change `HUNGER_SAVER_FACTOR` in `ModEnchantments` to adjust the strength.
- **Auto Smelt** (level 1, pickaxe only): ore block drops are replaced by the result of the vanilla SMELTING recipe (raw iron becomes iron ingot, raw gold becomes gold ingot, raw copper becomes copper ingot, ancient debris becomes netherite scrap). Ores without a smelting recipe (coal, diamond, redstone, lapis, emerald, quartz) drop normally, Fortune still applies, and Silk Touch still drops the ore block. The hook is in the block drop list (`BlockDropsMixin` + `AutoSmelt.java`), so extra ores from Vein Miner are smelted too.
- **Double Jump** (level 1, boots only): after jumping from the ground, press jump once more in the air (release the key first) for one extra jump (higher than a normal jump: about 3 blocks total if pressed at the peak of the first jump). Resets on touching the ground. No visual effect/sound. Client-side logic in `client/DoubleJump.java` (player movement is decided by the client, so no network packet). Inactive while flying, in water/lava, climbing, or riding.
- Textures of the last 5 books: `tools/generate_arcane_book_textures_2.py`.
- To add a new choice: create the enchantment JSON in `data/workshop_utilities/enchantment/`, create its key in `ModEnchantments`, then add it to `UPGRADE_OPTIONS`, and add the language text plus the tooltip descriptions `tooltip.workshop_utilities.<name>.desc.1`, `.desc.2`, ... and `.applies` (shown while holding SHIFT).

### Elytra Slot
- A dedicated Elytra slot in the survival inventory, right above the offhand slot (below the Backpack slot), and in the Creative inventory (Survival Inventory tab) in the column to the right of the right-hand armor, below the Backpack slot (vanilla slot style, a faded elytra icon when empty). Only accepts an Elytra (max 1).
- The player can wear a Chestplate in the chest slot and an Elytra in this slot at the same time. Press jump in the air to fly as usual; the durability of the Elytra in this slot decreases while flying and is synchronized. If the chest Elytra is still usable, it is used first.
- The Elytra in this slot is shown on the player's back (also to other players) over the chestplate, and the cape is hidden as in vanilla.
- Shift-click: an Elytra from the inventory goes into this slot if it is empty; from this slot it returns to the inventory. Curse of Binding locks it like an armor slot.
- Stored in player data (NBT `UncraftElytraSlot`), carried over on respawn/dimension change, dropped on death (unless keepInventory or Curse of Vanishing). Infinite Durability also repairs the Elytra in this slot.
- Code: package `elytra/`, `PlayerEntityMixin`, `PlayerScreenHandlerMixin`, `ServerPlayNetworkHandlerMixin`, and on the client `InventoryScreenMixin`, `ClientPlayerEntityMixin`, `ElytraFeatureRendererMixin`. Textures: `tools/generate_elytra_slot_textures.py`.
- Not yet implemented: Mending via XP for the Elytra in this slot.
- The slot data (`ElytraSlotData`) is registered at the end of PlayerEntity's static initializer via a `<clinit>` mixin; do not move it to initDataTracker because it will crash (ArrayIndexOutOfBounds) when the first player is created.

### Backpack: worn on the back, 3D model, slim GUI
- **Dedicated Backpack slot** (separate from the chestplate slot and the Elytra slot): Sneak + right-click the bag, drag it to the Backpack slot in the inventory, or Shift-click. Chestplate, Elytra and Backpack can be worn at the same time. A worn bag is shown in 3D on the player's back (it bends with sneaking, swimming and flying; it automatically shifts backwards when a chestplate or Elytra is worn so it does not clip through). Open the worn bag with the B key (rebindable in Controls).
  - Slot positions: in the survival inventory, the column to the right of the player model (Backpack above Elytra, then offhand). The Creative Survival Inventory tab: the column right of the right-hand armor (Backpack on top, Elytra below). The Creative and Survival slots use the same data (stored in NBT `UncraftBackpackSlot`), drop on death (unless keepInventory or Curse of Vanishing), and carry over on respawn/dimension change.
  - Migration: a bag that is still in the chestplate slot in an old save is moved to the Backpack slot automatically when the player joins (if the Backpack slot is empty).
  - Slot textures: `tools/generate_backpack_slot_textures.py`.
- **Open the worn bag**: press **B** (rebindable in Controls, category Workshop Utilities), without taking the bag off.
- **3D model**: `models/item/<tier>_backpack.json` + `textures/item/backpack_<tier>.png`, generated by `tools/generate_backpack_models.py` (change colors/shapes in the script and run it again). The same model is used in the inventory, hand, ground and on the back. `tools/preview_backpack.py` creates a preview image without opening the game.
- **GUI that grows upward** (max 12 columns): Small 9x4, Normal 9x7, Medium 10x8, Big 11x10, Huge 12x12 (change in `BackpackTier`). If the screen is too short, a scrollbar appears automatically (mouse wheel / drag). The slot count is never smaller than in the old version, so old bag contents are safe (slot positions may shift because the column count changed).
- Code: `BackpackItem`, package `backpack/` (`BackpackSlotHolder`, `BackpackSlotData`, `WornBackpackSlot`, `WornBackpackInventory`), `PlayerBackpackSlotMixin`, `PlayerScreenHandlerMixin`, `BackpackFeatureRenderer`, `ExtraSlotRenderer`, `BackpackScreen`, `network/OpenBackpackPayload`.

### Side panels in the table GUIs (Backpack panel + Equipment panel)
- **Backpack panel (all 4 tables: Uncrafting, Upgrade Table, Dis-Enchanting, Upgrade Enchanting)**: a small button sits at the top left of the GUI (it moves to the top left of the panel while the panel is open). Click it to open or close a panel on the left that shows the contents of the Backpack you are wearing in the Backpack slot, ready to use: drag items in and out, click, and Shift-click (an item from the panel goes to the table's input slots first, otherwise to your inventory). Backpacks cannot be put inside the panel, and the panel is saved straight into the worn Backpack on every change. The open or closed state is remembered. If no Backpack is worn, the panel explains how to wear one.
- **Consistent layout everywhere**: the Backpack panel (with the Tools Holder section at its bottom when the module is installed) is ALWAYS on the left, and the Equipment panel (armor, offhand, Elytra) is ALWAYS on the right, on every utility block, mod and vanilla (Crafting Table, Furnace, Blast Furnace, Smoker, Anvil, Smithing Table, Grindstone, Stonecutter, Loom, Cartography Table, Enchanting Table, Brewing Stand, Crafter, Dispenser, Dropper, Hopper / Minecart Hopper, Chest / Trapped Chest / Double Chest / Minecart Chest, Barrel, **Shulker Box, Villager / Wandering Trader trading screen and Beacon**). On Crafting Table / Furnace / Blast Furnace / Smoker the vanilla recipe book also lives on the left: while it is open, the Backpack panel and its button move to the left of the recipe book (`PanelController.recipeBookWidth()`), so the panel never ends up on the right.
- **Tools Holder panel**: when the worn Backpack has a Tools Holder Module, a separate Tools Holder panel (title "Tools", 2 x 4 rack with 7 slots and a `3/7` counter cell, same look as the rack in the Backpack GUI) appears directly to the LEFT of the Backpack panel, on every utility block (mod and vanilla). It opens and closes together with the Backpack panel.
- The number of columns adapts to the window width and a scrollbar (mouse wheel or drag) appears when the window is too short for all rows. The table GUI is shifted a little when the window is too narrow for the panel.
- **Shulker Box, Villager trading and Beacon**: same Backpack + Tools Holder panel on the left (no equipment panel), added through the same `VanillaHandlerPanelMixin` (`ShulkerBoxScreenHandler`, `MerchantScreenHandler`, `BeaconScreenHandler`) and drawn by `VanillaScreenBackgroundMixin` (`ShulkerBoxScreen`, `MerchantScreen`, `BeaconScreen`). Shulker Box uses the bounded Shift-click like Chest (its vanilla `quickMove` searches up to `slots.size()`); Merchant and Beacon use fixed slot ranges, so no extra handling is needed. From the panel, Shift-click on a single item in the Beacon GUI fills the payment slot (vanilla rule: only a stack of 1). The panel is also registered as an exclusion zone in REI and JEI for these three screens.
- **Storage blocks (Dispenser, Dropper, Hopper, Chest, Barrel)**: same Backpack + Tools Holder panel on the left (no equipment panel), added by `VanillaHandlerPanelMixin` (`Generic3x3ContainerScreenHandler`, `HopperScreenHandler`, `GenericContainerScreenHandler`). Shift-click from the backpack panel moves the item into the container; Shift-click from the container or your inventory never lands in the panel: vanilla `quickMove` of Chest / Barrel / Hopper searches up to `slots.size()`, so `PanelSupport.boundedQuickMove()` hides the panel slots while it runs (client and server alike, so the client prediction matches). The chest handler is shared with the Ender Chest and Chest Boats (the client cannot tell them apart from a Chest because it only gets an empty container), so they get the panel too.
- **Equipment panel (Upgrade Table, Dis-Enchanting Table, Upgrade Enchanting Table only)**: always shown on the right of the GUI, with the 4 armor slots, the offhand slot and the Elytra slot. Items can be dragged in, out and used on the table directly. Armor slots follow vanilla rules (correct armor type only, max 1, locked by Curse of Binding). Shift-click on an equipped item returns it to your inventory. The Uncrafting Table has no equipment panel.
- **Backpack panel sync (two-way, every tick)**: the panel in vanilla and mod GUIs is only a working copy of the worn Backpack's data, so `PanelSupport.refresh()` reconciles it with the real Backpack on every server tick and after every slot click. If the real Backpack changed (contents, modules, tier, removed), the panel is reloaded from it (the real Backpack always wins); if the panel changed without firing the slot listener, the Backpack is updated at once. Closing the GUI reconciles first, so a stale panel can never overwrite newer Backpack data. This is why every utility block (vanilla and mod) always shows the same contents as the real Backpack.
- **Server-side safety attach**: the constructor mixin does not always run on the server-side vanilla handler (the log showed `side=CLIENT` only for Crafting Table, Furnace, etc.), which left the client with panel slots the server did not know (items could not be placed, contents never synced). `PanelSupport.attachVanillaIfMissing()` now attaches the panel on the server from `sendContentUpdates` when it is missing and logs a `[panel] server handler ... belum punya panel` warning.
- Code: `screen/PanelScreenHandler` (shared base of the 4 handlers; extra slots are always added last so the table slot numbers do not change; the number of Backpack panel slots is always the Huge tier size and the active count is synced as a property), `client/PanelHandledScreen` (shared base of the 4 screens: drawing, slot placement, scrolling, toggle button), and the extra `ElytraSlot` constructor. Panels are drawn in code, so there are no new textures.

### Creative tabs
- **Workshop Utilities**: the table blocks (Uncrafting, Dis-Enchanting, Upgrade Table, Upgrade Enchanting) and the five Backpacks.
- **Workshop Books**: Upgrade Enchanted, Upgrade Book, Upgrade Tool, and all mod-made enchanted books (Infinite Durability, Ender Blind, Snow Walker, Frozen Ward, Silent Step, Piglin Protection, Vein Miner, Thunder Slash, Anti-Gravity, Woodcutter, More Harvest, Water Breathing, Swim Fast, Fire Resistant, Unlimited).

### Items
- **Upgrade Enchanted**: a material. Combine it with a Book in the Anvil (cost 1 XP level) to make an Upgrade Book.
- **Upgrade Tool**: a small consumable hammer (stacks to 64). 1 is consumed per upgrade in the Upgrade Table. Texture: `tools/generate_upgrade_table_textures.py`.
- **Upgrade Book**: a book (base texture from the vanilla book) colored blue with gold accents and an animated sparkle, plus the enchanted glint, used in the Upgrade Enchanting Table.

### Backpack (5 tiers)
Right-click to open. The contents are stored on the bag item itself (they travel with it, and it can be renamed in an anvil). All tiers work the same, only the slot count differs:

| Tier | Slots (columns x rows) | Upgrade material to the next tier (8 pieces, at the Upgrade Table) |
|---|---|---|
| Small Backpack | 36 (9x4) | 8 iron ingot |
| Normal Backpack | 63 (9x7) | 8 gold ingot |
| Medium Backpack | 80 (10x8) | 8 diamond |
| Big Backpack | 110 (11x10) | 8 netherite ingot |
| Huge Backpack | 144 (12x12) | - (highest tier) |

- Small Backpack recipe: leather x7 + string (top middle) + chest (middle): `LSL / LCL / LLL`.
- **Upgrade via the Upgrade Table**: Backpack in the top-left slot, 8 materials in the top-right slot, Upgrade Tool in the bottom slot (see the Upgrade Table section). The contents and custom name carry over. The recipe JSONs `normal_backpack.json`, `medium_backpack.json`, `big_backpack.json`, `huge_backpack.json` (pattern `MMM / MBM / MMM`) are only used by the Uncrafting Table to reverse a higher-tier bag into a lower-tier bag + 8 materials; the crafting table rejects them. If the materials change, also change `BackpackTier.upgradeMaterial()` and the JSON.
- A bag that still contains items cannot be uncrafted (empty it first), so its contents are never lost.
- The GUI is vanilla-container styled with 5 buttons at the top right: put similar items, sort the bag, sort the inventory, take all, put all.
- A bag cannot be put inside a bag, and the slot of the bag being opened is locked so it cannot be lost/duplicated.
- Change tier sizes in `backpack/BackpackTier.java` (column and row numbers). Textures: `python tools/generate_backpack_textures.py`.

## Block crafting recipes
- Uncrafting Table: a crafting table in the middle and 4 iron ingots on the top, bottom, left and right.
- Dis-Enchanting Table: a book on top, a grindstone in the middle, lapis lazuli on the left, right and below the grindstone.
- Upgrade Enchanting Table: an Upgrade Book at the top middle, diamonds on the left and right of the middle row, obsidian in the middle of the middle row and the entire bottom row.
- Upgrade Table: diamonds on the top left and right, Upgrade Enchanted at the top middle, an anvil in the middle, iron ingots across the bottom row.
- Upgrade Tool: iron ingots across the top row and on the left and right of the middle, Upgrade Enchanted in the middle, a stick at the bottom (hammer shape).
- Upgrade Enchanted: a diamond in the middle, lapis lazuli above, below, left and right of the diamond, amethyst shards in the four corners.
- Upgrade Book: Upgrade Enchanted (left slot) + Book (right slot) in the Anvil.

## Building
1. Install JDK 21.
2. Run `./gradlew build` (Windows: `gradlew.bat build`) from the project folder.
3. The jar is in `build/libs/`. Copy the main jar `workshop-utilities-1.0.0.jar` (not the `-sources` one) to the mods folder together with Fabric API 0.102.0+1.21.
4. To try it directly: `./gradlew runClient`.

Fabric Loader, Loom, mappings and Fabric API versions are in `gradle.properties`. Adjust them according to https://fabricmc.net/develop if needed.

## Project structure
- `src/main/java/com/workshoputilities/` : blocks, items, screen handlers, mixins (Anvil etc.), uncrafting logic, the enchantment list.
- `src/client/java/com/workshoputilities/client/` : GUI screens and client-only code.
- `src/main/resources/assets/workshop_utilities/` : textures, models, blockstates, language files.
- `src/main/resources/data/workshop_utilities/` : recipes, loot tables, advancements, enchantments, tags.
- `tools/generate_textures.py` : texture generator for the Uncrafting/Dis-Enchanting blocks and GUIs (requires Pillow).
- `tools/generate_upgrade_textures.py` : texture generator for the Upgrade features. Requires the vanilla `book.png` and `enchanted_book.png` in `tools/source/` (block textures are also made if the 3 vanilla enchanting table textures are there).
- `tools/generate_grindstone_textures.py` : block textures for the Dis-Enchanting Table (grindstone).
- `tools/generate_upgrade_table_textures.py` : GUI textures for the Upgrade Table and the Upgrade Tool item. The Upgrade Table block textures (`upgrade_table.png`, `upgrade_table_top.png`) are the old Dis-Enchanting textures and are not generated.
- `tools/preview_block_model.py` : isometric preview of block models without opening the game.
- `tools/generate_icon.py` : generator for the mod icon (requires Pillow and numpy). Produces `assets/workshop_utilities/icon.png` (128x128) and `tools/icon_512.png` (for Modrinth/CurseForge). The icon is a mirror-symmetric emblem: the Upgrade Enchanting Table on a rune platform with a floating Upgrade Book.

## Notes
- Damaged items (reduced durability) can still be uncrafted and give full ingredients. Change `UncraftRecipes.find` to restrict this.
- If an item is still in a slot when the GUI is closed, it is returned to the player.

## Anvil
Mod-made enchanted books can only be combined with suitable items. If the left item does not support the enchantment on the book (e.g. Vein Miner on a sword), the anvil output is empty ("X"), including in Creative. Logic in `AnvilScreenHandlerMixin` and `ModEnchantments.hasUnsupportedModEnchantment`.

### Backpack module: Uncrafting Module
- The Backpack now has **5 module slots** (Crafting, Furnace, Blast Furnace, Smoker, **Uncrafting**), only one of each kind allowed. The 5th slot is stored last in the module data (`BackpackModuleData.moduleIndex/smeltIndex`), so the contents of old Backpacks (modules and furnaces) stay intact.
- The **Uncrafting Module** (`uncrafting_module`, recipe: iron ingot, amethyst shard, redstone, Uncrafting Table) brings the Uncrafting Table feature into the Backpack with exactly the same rules: 1 input slot, recipe selection with the < > buttons (Shift = jump 10), a 3x3 result grid as a preview, taking one result consumes the input in the amount of the recipe result and then all ingredients can be taken, Shift-click on a result moves all ingredients, and all recipes are read from the RecipeManager via the same `UncraftRecipes` as the table (a Backpack that still contains items cannot be uncrafted).
- The Uncrafting module panel is opened with the arrow tab in the dock. The module cannot be removed while its input slot or result grid still has contents. When the GUI is closed, the input is returned to the player and results that were already consumed are not lost (like the Uncrafting Table). The panel contents are not stored in the bag.
- Logic in `screen/BackpackScreenHandler.java` ("uncrafting" section), GUI in `client/BackpackScreen.java`, the item texture is made by `tools/generate_module_textures.py`. Buttons use the ids `BUTTON_UNCRAFT_*` (5..8).

### Backpack module: Bed Module (left side slot)
- On the LEFT of the Backpack GUI there is a **side module slot** (separate from the module dock on the right) that only accepts the **Bed Module**. This slot is hidden/shown along with the dock button on the right. Its data is stored last in `BackpackModuleData` (`SIDE_MODULES`, `sideIndex`), so old Backpack contents stay intact.
- The **Bed Module** (`bed_module`, recipe: iron ingot, phantom membrane, redstone, any bed) enables the **Sleep** button (bed icon) right below its slot. The button is only active at night or during a thunderstorm.
- Pressing Sleep skips the night until morning without placing a bed, and the **spawn point does not change** (it stays at the last bed the player slept in, because no bed block is used). Weather is also cleared if the `doWeatherCycle` gamerule is on, and the Phantom (insomnia) counter is reset like sleeping in a bed.
- The rules follow the vanilla bed: only in the Overworld, only at night / during storms, refused if there are monsters hunting nearby within 8x5x8 (Piglins, Zombified Piglins and not-yet-angry Endermen do not count), and refused if the `doDaylightCycle` gamerule is off. The refusal reason is shown in the action bar.
- Multiplayer note: world time jumps straight to morning for all players in that dimension (like a sleeping bag), without waiting for a percentage of players to sleep.
- Logic in `screen/BackpackScreenHandler.java` ("Bed module" section, `BUTTON_SLEEP` button id 9, `SideModuleSlot` slot), GUI in `client/BackpackScreen.java` (left dock and `SleepButton`), the item texture is made by `tools/generate_module_textures.py`.
- The Uncrafting Module texture is now derived from the Crafting Module (teal frame, cyan gem in the middle) via `make_uncrafting()` in the same script.



### Backpack module: Tools Holder Module (left side slot, below the Bed Module)
- Directly **below the Bed Module dock** on the left of the Backpack GUI there is a second **side module slot** that only accepts the **Tools Holder Module** (`tools_holder_module`, recipe: 4 iron ingots, stick, 2 redstone, chest, leather). It is hidden/shown together with the dock button on the right.
- When installed it adds **7 slots** for tools, weapons and similar items (pickaxe, axe, shovel, hoe, sword, bow, crossbow, trident, mace, shield, fishing rod, shears, flint and steel, brush). One item per slot.
- A **show / hide button** (mini pickaxe with an arrow) sits right under the module slot. When shown, the slots open as a tidy **rack to the left of the dock** (2 columns, 7 slots, and a counter cell such as `3/7`); when hidden the rack closes. The choice is saved in the Backpack, so it is remembered. The button is grey and disabled until the module is installed.
- The Tools Holder Module cannot be removed while any of its 7 slots still holds an item (the tooltip says so). Hidden slots keep their items. Shift-click on a tool slot moves the item to the player inventory.
- Data is stored after the side module slots in `BackpackModuleData` (`TOOL_SLOTS`, `toolIndex`, `toolsShown`), so old Backpacks stay intact. Logic in `screen/BackpackScreenHandler.java` (`BUTTON_TOOLS_TOGGLE` button id 10, `SideModuleSlot` and `ToolSlot` slots, `isHoldableTool`), GUI in `client/BackpackScreen.java` (`drawToolsDock`, `drawToolsRack`, `ToolsButton`), item texture made by `tools/generate_module_textures.py`.

## REI and JEI integration (optional)

The mod integrates automatically with **Roughly Enough Items (REI)** and **Just Enough Items (JEI)** if either is installed. Neither is **required**: they are only used at compile time (`modCompileOnly`), are not bundled in the jar, and are only listed as `suggests` in `fabric.mod.json`.

What appears in REI/JEI:
- The categories **Uncrafting** (one entry per crafting recipe, tag ingredients cycle), **Dis-Enchanting** (an example per mod enchantment, vanilla examples, and mixed examples that produce a Fused Enchanted Book), **Upgrade Table: Enchant** (suitable items are computed from the enchantment's own rules), **Upgrade Table: Backpack**, and **Upgrade Enchanting Table** (one per enchanted book, with the 20 bookshelf and 15 XP level requirements).
- Each block is a workstation: press R/U on a block to see what it is used for.
- "How this works" info pages for all blocks, material items, Backpacks, modules, and every mod enchanted book (JEI: the "i" tab; REI: the Workshop Guide category). The text is in the language keys `compat.workshop_utilities.info.*`.
- Clickable areas in the GUI (near the arrow) that directly open that category's recipes.

Code: `client/compat/ArcaneCompat.java` (shared data, no REI/JEI classes), `client/compat/rei/`, `client/compat/jei/`. Versions are in `gradle.properties` (`rei_version`, `jei_version`, `jei_mc_version`). To try it live in `runClient`: `./gradlew runClient -PtestRei=true` or `-PtestJei=true`.
