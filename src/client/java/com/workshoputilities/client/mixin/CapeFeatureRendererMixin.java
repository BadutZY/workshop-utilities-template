package com.workshoputilities.client.mixin;

import com.workshoputilities.elytra.ElytraContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra Slot: seperti vanilla, cape disembunyikan saat pemain punya elytra (di slot khusus juga). Hanya kosmetik. */
@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
	private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V";

	@Inject(method = RENDER, at = @At("HEAD"), require = 0)
	private void workshop_utilities$renderEnter(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
			float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
		ElytraContext.enterRender();
	}

	@Inject(method = RENDER, at = @At("RETURN"), require = 0)
	private void workshop_utilities$renderExit(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
			float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
		ElytraContext.exitRender();
	}
}
