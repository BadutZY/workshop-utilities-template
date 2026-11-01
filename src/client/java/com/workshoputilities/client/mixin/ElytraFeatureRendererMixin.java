package com.workshoputilities.client.mixin;

import com.workshoputilities.elytra.ElytraContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra Slot: sayap elytra dari slot khusus digambar di punggung pemain, di atas armor dada. */
@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin {
	private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";

	@Inject(method = RENDER, at = @At("HEAD"))
	private void workshop_utilities$renderEnter(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress,
			float headYaw, float headPitch, CallbackInfo ci) {
		ElytraContext.enterRender();
	}

	@Inject(method = RENDER, at = @At("RETURN"))
	private void workshop_utilities$renderExit(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress,
			float headYaw, float headPitch, CallbackInfo ci) {
		ElytraContext.exitRender();
	}
}
