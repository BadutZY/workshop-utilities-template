package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Silent Step (jebakan): tripwire dan pressure plate mengabaikan entity yang {@code canAvoidTraps()} bernilai true
 * (cara vanilla yang sama dipakai oleh Allay dan sejenisnya), jadi pemakai leggings Silent Step tidak memicunya.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "canAvoidTraps", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$silentStep(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity living && ModEnchantments.isSilentStepping(living)) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Fire Resistant: pemakai armor ber-Fire Resistant dianggap kebal api seperti Blaze. Vanilla lalu otomatis
	 * mengabaikan semua damage bertag api (api, lava, magma, fireball), tidak membakar pemain, dan tidak
	 * menampilkan overlay api. Tidak memakai status effect, jadi tidak ada ikon.
	 */
	@Inject(method = "isFireImmune", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$fireResistant(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity living && ModEnchantments.wearsFireResistant(living)) {
			cir.setReturnValue(true);
		}
	}
}
