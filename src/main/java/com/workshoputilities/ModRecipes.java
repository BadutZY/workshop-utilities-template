package com.workshoputilities;

import com.workshoputilities.recipe.BackpackUpgradeRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRecipes {
	public static final RecipeSerializer<BackpackUpgradeRecipe> BACKPACK_UPGRADE = Registry.register(
			Registries.RECIPE_SERIALIZER, Identifier.of(WorkshopUtilitiesMod.MOD_ID, "backpack_upgrade"),
			new SpecialRecipeSerializer<>(BackpackUpgradeRecipe::new));

	private ModRecipes() {
	}

	/** Dipanggil agar class ini dimuat dan serializer resep terdaftar. */
	public static void init() {
	}
}
