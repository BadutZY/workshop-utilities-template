package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hunger Saver: semua exhaustion yang masuk lewat {@code PlayerEntity.addExhaustion} (lari, lompat, berenang,
 * menambang, menyerang, dll) dikalikan {@link ModEnchantments#HUNGER_SAVER_FACTOR} selama pemain memakai
 * leggings ber-Hunger Saver, sehingga hunger bar dan saturation berkurang lebih lambat.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerHungerMixin {
	@ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
	private float workshop_utilities$hungerSaver(float exhaustion) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (exhaustion > 0.0F && ModEnchantments.hasHungerSaver(self.getEquippedStack(EquipmentSlot.LEGS))) {
			return exhaustion * ModEnchantments.HUNGER_SAVER_FACTOR;
		}
		return exhaustion;
	}
}
