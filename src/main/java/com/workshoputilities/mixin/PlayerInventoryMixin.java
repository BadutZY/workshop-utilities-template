package com.workshoputilities.mixin;

import com.workshoputilities.UnlimitedScope;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlimited: bucket air/lava/susu, powder snow, potion, honey, dll yang dipakai/ditaruh tidak boleh memunculkan
 * wadah kosong (bucket, glass bottle, bowl) di inventory. Vanilla memberikannya lewat insertStack setelah
 * pengurangan item; bila pengurangan itu dibatalkan oleh Unlimited, wadah kosongnya juga dibuang.
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
	@Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$skipEmptyContainer(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (UnlimitedScope.isInUse() && UnlimitedScope.isSuppressed() && UnlimitedScope.isEmptyContainer(stack)) {
			cir.setReturnValue(true);
		}
	}
}
