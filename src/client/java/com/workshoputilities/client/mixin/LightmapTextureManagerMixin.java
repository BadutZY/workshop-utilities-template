package com.workshoputilities.client.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Night Vision: saat menghitung lightmap, pengecekan "pemain punya status effect Night Vision" dibuat bernilai true
 * bila pemain memakai helmet ber-Night Vision. Hanya pengecekan di dalam lightmap yang dialihkan; pemain tidak pernah
 * diberi status effect, jadi tidak ada ikon, partikel, atau efek lain. Kekuatannya diatur di {@link GameRendererMixin}.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
	@Redirect(method = "update",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
	private boolean workshop_utilities$nightVision(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect) {
		boolean real = player.hasStatusEffect(effect);
		if (!real && effect == StatusEffects.NIGHT_VISION
				&& ModEnchantments.hasNightVision(player.getEquippedStack(EquipmentSlot.HEAD))) {
			return true;
		}
		return real;
	}
}
