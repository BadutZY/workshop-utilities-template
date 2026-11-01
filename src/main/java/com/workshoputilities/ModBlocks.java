package com.workshoputilities;

import com.workshoputilities.block.DisenchantingTableBlock;
import com.workshoputilities.block.UncraftingTableBlock;
import com.workshoputilities.block.UpgradeEnchantingTableBlock;
import com.workshoputilities.block.UpgradeTableBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	public static final Block UNCRAFTING_TABLE = register("uncrafting_table",
			new UncraftingTableBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)));

	/**
	 * Grindstone vanilla: semua sifat (kekerasan, suara batu saat ditaruh/dipecah/dipijak, butuh tool, piston) disalin
	 * dari Blocks.GRINDSTONE, jadi sama persis. Bisa menempel di lantai, dinding, dan langit-langit.
	 */
	public static final Block DISENCHANTING_TABLE = register("disenchanting_table",
			new DisenchantingTableBlock(AbstractBlock.Settings.copy(Blocks.GRINDSTONE)));

	/**
	 * Anvil vanilla: sifat disalin dari Blocks.ANVIL (suara anvil, kekerasan 5, ketahanan ledakan 1200), hanya warna peta
	 * yang berbeda. Jatuh bila tanpa alas dan melukai makhluk yang tertimpa seperti anvil.
	 */
	public static final Block UPGRADE_TABLE = register("upgrade_table",
			new UpgradeTableBlock(AbstractBlock.Settings.copy(Blocks.ANVIL).mapColor(MapColor.PURPLE)));

	/** Sifat block mengikuti enchanting table vanilla (kekerasan, cahaya level 7), hanya warna peta yang berbeda. */
	public static final Block UPGRADE_ENCHANTING_TABLE = register("upgrade_enchanting_table",
			new UpgradeEnchantingTableBlock(AbstractBlock.Settings.copy(Blocks.ENCHANTING_TABLE)
					.mapColor(MapColor.BLUE)));

	private ModBlocks() {
	}

	private static Block register(String name, Block block) {
		Identifier id = Identifier.of(WorkshopUtilitiesMod.MOD_ID, name);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
		return Registry.register(Registries.BLOCK, id, block);
	}

	/** Dipanggil agar class ini dimuat dan block terdaftar. Tab creative ada di {@link ModItemGroups}. */
	public static void init() {
	}
}
