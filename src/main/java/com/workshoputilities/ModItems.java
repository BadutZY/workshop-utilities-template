package com.workshoputilities;

import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.item.LoreItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
	/** Bahan crafting Upgrade Table / Upgrade Tool, dan wajib dipakai bersama Upgrade Book di Upgrade Enchanting Table. */
	public static final Item UPGRADE_ENCHANTED = register("upgrade_enchanted",
			new LoreItem(new Item.Settings().rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.upgrade_enchanted.tooltip"));

	/** Book biasa (texture sederhana, tanpa glint), dipakai di Upgrade Enchanting Table. */
	public static final Item UPGRADE_BOOK = register("upgrade_book",
			new LoreItem(new Item.Settings().rarity(Rarity.RARE),
					"item.workshop_utilities.upgrade_book.tooltip"));

	/**
	 * Enchanted book campuran: menyimpan enchantment vanilla DAN buatan mod sekaligus. Hanya didapat dari
	 * Dis-Enchanting Table (tidak ada resep dan tidak ada di creative tab), dan hanya bisa dipasang lewat Upgrade Table.
	 */
	public static final Item FUSED_ENCHANTED_BOOK = register("fused_enchanted_book",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
					.component(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT),
					"item.workshop_utilities.fused_enchanted_book.tooltip", true));

	/** Bahan habis pakai: 1 buah dipakai tiap upgrade di Upgrade Table (enchant maupun upgrade Backpack). */
	public static final Item UPGRADE_TOOL = register("upgrade_tool",
			new LoreItem(new Item.Settings().rarity(Rarity.RARE),
					"item.workshop_utilities.upgrade_tool.tooltip"));

	/** Modul Backpack (dipasang di slot modul dock Backpack): Crafting, Furnace, Blast Furnace, Smoker, Uncrafting. */
	public static final Item CRAFTING_MODULE = register("crafting_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.crafting_module.tooltip"));
	public static final Item FURNACE_MODULE = register("furnace_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.furnace_module.tooltip"));
	public static final Item BLAST_FURNACE_MODULE = register("blast_furnace_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.blast_furnace_module.tooltip"));
	public static final Item SMOKER_MODULE = register("smoker_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.smoker_module.tooltip"));
	/** Modul Uncrafting: fitur Uncrafting Table (input + pilihan resep + grid 3x3) di dalam Backpack. */
	public static final Item UNCRAFTING_MODULE = register("uncrafting_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.uncrafting_module.tooltip"));

	/** Modul Bed: dipasang di slot modul samping (kiri GUI Backpack); tombol Sleep melewati malam tanpa bed dan tanpa mengubah spawn point. */
	public static final Item BED_MODULE = register("bed_module",
			new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON),
					"item.workshop_utilities.bed_module.tooltip"));

	/** Lima tier Backpack (Small, Normal, Medium, Big, Huge); lihat {@link BackpackTier}. */
	private static final Map<BackpackTier, Item> BACKPACKS = new EnumMap<>(BackpackTier.class);

	static {
		for (BackpackTier tier : BackpackTier.values()) {
			Item.Settings settings = new Item.Settings().maxCount(1).rarity(tier.rarity());
			if (tier == BackpackTier.HUGE) {
				settings.fireproof();
			}
			BACKPACKS.put(tier, register(tier.id(), new BackpackItem(settings, tier)));
		}
	}

	private ModItems() {
	}

	public static Item backpack(BackpackTier tier) {
		return BACKPACKS.get(tier);
	}

	/** Tier dari item tas, atau null bila bukan Backpack. */
	public static BackpackTier tierOf(Item item) {
		for (Map.Entry<BackpackTier, Item> entry : BACKPACKS.entrySet()) {
			if (entry.getValue() == item) {
				return entry.getKey();
			}
		}
		return null;
	}

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(WorkshopUtilitiesMod.MOD_ID, name), item);
	}

	/** Dipanggil agar class ini dimuat dan item terdaftar. */
	public static void init() {
	}
}