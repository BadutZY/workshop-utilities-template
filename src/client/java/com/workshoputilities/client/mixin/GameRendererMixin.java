package com.workshoputilities.client.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Night Vision: kekuatan penglihatan malam. Vanilla membaca durasi dari status effect (yang tidak ada di sini),
 * jadi untuk helmet ber-Night Vision kekuatannya langsung penuh (1.0), tanpa kedip di akhir durasi seperti potion.
 * Bila pemain kebetulan juga punya potion Night Vision asli, perhitungan vanilla tetap dipakai.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
	private static void workshop_utilities$nightVisionStrength(LivingEntity entity, float tickDelta,
			CallbackInfoReturnable<Float> cir) {
		if (entity.getStatusEffect(StatusEffects.NIGHT_VISION) == null
				&& ModEnchantments.hasNightVision(entity.getEquippedStack(EquipmentSlot.HEAD))) {
			cir.setReturnValue(1.0F);
		}
	}
}
