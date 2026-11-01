package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Snow Walker: boots ber-Snow Walker memperlakukan pemain seperti memakai leather boots, jadi bisa berjalan di atas
 * powder snow tanpa tenggelam. Method ini dipakai oleh collision powder snow di client maupun server.
 * Perilaku vanilla lain tetap berlaku (misalnya tenggelam bila sneak atau jatuh dari ketinggian).
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
	private static void workshop_utilities$snowWalker(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof LivingEntity living && ModEnchantments.hasSnowWalker(living.getEquippedStack(EquipmentSlot.FEET))) {
			cir.setReturnValue(true);
		}
	}
}
