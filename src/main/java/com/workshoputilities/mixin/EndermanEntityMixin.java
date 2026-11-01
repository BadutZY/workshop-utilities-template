package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ender Blind: pemain yang memakai helmet ber-Ender Blind tidak dianggap "menatap" Enderman,
 * persis seperti carved pumpkin, jadi Enderman tidak marah saat matanya dilihat.
 * (Enderman yang sudah marah ditenangkan oleh {@link com.workshoputilities.EnderBlind}.)
 */
@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin {
	@Inject(method = "isPlayerStaring", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$enderBlind(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (ModEnchantments.hasEnderBlind(player.getEquippedStack(EquipmentSlot.HEAD))) {
			cir.setReturnValue(false);
		}
	}
}
