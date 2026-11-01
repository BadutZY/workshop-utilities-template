package com.workshoputilities.mixin;

import com.workshoputilities.item.BackpackItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Upgrade Backpack hanya lewat Upgrade Table (Backpack + 8 bahan + Upgrade Tool), bukan di crafting table.
 * Resep JSON upgrade (normal/medium/big/huge_backpack.json) tetap ada supaya Uncrafting Table masih bisa membaliknya
 * (tas tier atas -> tas tier bawah + 8 bahan), tetapi crafting table menolaknya: resep dengan hasil Backpack
 * tidak cocok bila di grid ada Backpack. Resep Small Backpack (tanpa Backpack di grid) tidak terpengaruh.
 */
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$noBackpackUpgradeInCraftingGrid(CraftingRecipeInput input, World world,
			CallbackInfoReturnable<Boolean> cir) {
		ShapedRecipe self = (ShapedRecipe) (Object) this;
		if (!(self.getResult(world.getRegistryManager()).getItem() instanceof BackpackItem)) {
			return;
		}
		for (int i = 0; i < input.getSize(); i++) {
			ItemStack stack = input.getStackInSlot(i);
			if (stack.getItem() instanceof BackpackItem) {
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
