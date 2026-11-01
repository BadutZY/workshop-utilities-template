package com.workshoputilities.client.mixin;

import com.workshoputilities.elytra.ElytraContext;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Elytra Slot (client): tombol lompat di udara memeriksa elytra di slot dada sebelum mengirim permintaan mulai
 * terbang ke server. Dibungkus supaya Elytra di slot khusus ikut dipertimbangkan.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "tickMovement", at = @At("HEAD"))
	private void workshop_utilities$glideEnter(CallbackInfo ci) {
		ElytraContext.enterGlide();
	}

	@Inject(method = "tickMovement", at = @At("RETURN"))
	private void workshop_utilities$glideExit(CallbackInfo ci) {
		ElytraContext.exitGlide();
	}
}
