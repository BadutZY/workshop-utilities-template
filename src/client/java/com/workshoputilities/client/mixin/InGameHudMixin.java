package com.workshoputilities.client.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Water Breathing: sembunyikan bar gelembung oksigen di HUD selama helmet ber-Water Breathing dipakai.
 * HUD vanilla menggambar bubble bila pemain terendam air atau oksigen kurang dari penuh; kita membuat cek "terendam"
 * di HUD saja menjadi false (oksigen sendiri sudah selalu penuh karena Water Breathing). Tidak mengubah fisika/renang,
 * karena hanya panggilan di dalam InGameHud yang dialihkan.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Redirect(method = "renderStatusBars",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
	private boolean workshop_utilities$hideAirBubbles(PlayerEntity player, TagKey<Fluid> fluid) {
		if (ModEnchantments.hasWaterBreathing(player.getEquippedStack(EquipmentSlot.HEAD))) {
			return false;
		}
		return player.isSubmergedIn(fluid);
	}

	/**
	 * HUD juga menggambar bubble bila oksigen kurang dari penuh (mis. baru naik dari menyelam tanpa helmet).
	 * Selama helmet Water Breathing dipakai, HUD diberi nilai oksigen penuh sehingga bubble tidak pernah tampil,
	 * dalam keadaan apa pun.
	 */
	@Redirect(method = "renderStatusBars",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/player/PlayerEntity;getAir()I"))
	private int workshop_utilities$fullAirForHud(PlayerEntity player) {
		if (ModEnchantments.hasWaterBreathing(player.getEquippedStack(EquipmentSlot.HEAD))) {
			return player.getMaxAir();
		}
		return player.getAir();
	}
}
