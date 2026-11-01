package com.workshoputilities;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Auto Smelt (pickaxe): drop dari block ore diganti hasil lebur resep SMELTING vanilla (raw iron -> iron ingot,
 * raw gold -> gold ingot, raw copper -> copper ingot, ancient debris -> netherite scrap, dst). Drop yang tidak punya
 * resep lebur (coal, diamond, redstone, lapis, emerald, quartz) tetap seperti biasa, dan jumlah drop (Fortune)
 * ikut terbawa. Drop berupa block ore itu sendiri (hasil Silk Touch) tidak dilebur, supaya Silk Touch tetap berfungsi.
 *
 * Hook-nya ada di {@link com.workshoputilities.mixin.BlockDropsMixin}, yaitu pada daftar drop sebuah block, sehingga
 * semua cara menambang ikut tercakup, termasuk ore tambahan dari Vein Miner (yang memakai tryBreakBlock).
 */
public final class AutoSmelt {
	private AutoSmelt() {
	}

	public static List<ItemStack> apply(ServerWorld world, BlockState state, ItemStack tool, List<ItemStack> drops) {
		if (drops.isEmpty() || !ModEnchantments.hasAutoSmelt(tool) || !ToolEnchantments.isOre(state)) {
			return drops;
		}
		Item ownItem = state.getBlock().asItem();
		boolean debris = state.isOf(Blocks.ANCIENT_DEBRIS);
		List<ItemStack> result = new ArrayList<>(drops.size());
		for (ItemStack drop : drops) {
			// Drop berupa block ore itu sendiri = Silk Touch; biarkan. Ancient debris selalu menjatuhkan dirinya sendiri.
			if (drop.isEmpty() || (drop.isOf(ownItem) && !debris)) {
				result.add(drop);
				continue;
			}
			result.add(smelt(world, drop));
		}
		return result;
	}

	private static ItemStack smelt(ServerWorld world, ItemStack drop) {
		Optional<RecipeEntry<SmeltingRecipe>> match =
				world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(drop), world);
		if (match.isEmpty()) {
			return drop;
		}
		ItemStack output = match.get().value().getResult(world.getRegistryManager());
		if (output.isEmpty()) {
			return drop;
		}
		ItemStack smelted = output.copy();
		smelted.setCount(Math.min(smelted.getMaxCount(), output.getCount() * drop.getCount()));
		return smelted;
	}
}
