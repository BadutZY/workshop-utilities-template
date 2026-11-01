package com.workshoputilities.recipe;

import com.workshoputilities.ModItems;
import com.workshoputilities.ModRecipes;
import com.workshoputilities.backpack.BackpackTier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Upgrade tas: tas di tengah grid 3x3, dikelilingi 8 bahan upgrade (lihat {@link BackpackTier#upgradeMaterial()}).
 * Hasilnya tas tier berikutnya dengan isi dan nama kustom yang sama.
 */
public class BackpackUpgradeRecipe extends SpecialCraftingRecipe {
	public BackpackUpgradeRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() != 3 || input.getHeight() != 3) {
			return false;
		}
		BackpackTier tier = ModItems.tierOf(input.getStackInSlot(4).getItem());
		if (tier == null || tier.next() == null) {
			return false;
		}
		Item material = tier.upgradeMaterial();
		for (int i = 0; i < 9; i++) {
			if (i != 4 && !input.getStackInSlot(i).isOf(material)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
		ItemStack source = input.getStackInSlot(4);
		BackpackTier tier = ModItems.tierOf(source.getItem());
		if (tier == null || tier.next() == null) {
			return ItemStack.EMPTY;
		}
		ItemStack result = new ItemStack(ModItems.backpack(tier.next()));
		NbtComponent data = source.get(DataComponentTypes.CUSTOM_DATA);
		if (data != null) {
			result.set(DataComponentTypes.CUSTOM_DATA, data);
		}
		Text name = source.get(DataComponentTypes.CUSTOM_NAME);
		if (name != null) {
			result.set(DataComponentTypes.CUSTOM_NAME, name);
		}
		return result;
	}

	@Override
	public boolean fits(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.BACKPACK_UPGRADE;
	}
}
