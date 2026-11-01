package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Piglin Protection: pengecekan "memakai gold armor" milik Piglin juga bernilai true bila salah satu armor yang dipakai
 * ber-Piglin Protection, jadi Piglin tidak menyerang pemakainya. Tag item piglin_safe_armor tidak bisa dipakai untuk ini
 * karena tag berlaku per jenis item, bukan per enchantment. Perilaku vanilla lain tetap sama: Piglin tetap marah bila
 * pemain membuka chest atau menambang gold block, dan Piglin Brute tidak peduli pada gold armor.
 */
@Mixin(PiglinBrain.class)
public abstract class PiglinBrainMixin {
	@Inject(method = "wearsGoldArmor", at = @At("HEAD"), cancellable = true)
	private static void workshop_utilities$piglinProtection(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ModEnchantments.wearsPiglinProtection(entity)) {
			cir.setReturnValue(true);
		}
	}
}
