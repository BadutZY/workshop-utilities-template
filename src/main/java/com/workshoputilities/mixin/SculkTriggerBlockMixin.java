package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Silent Step (menginjak langsung): sculk sensor, calibrated sculk sensor, dan sculk shrieker punya logika sendiri
 * saat diinjak (selain lewat getaran), jadi diblok di sini supaya pemakai leggings Silent Step tidak mengaktifkannya.
 */
@Mixin({SculkSensorBlock.class, SculkShriekerBlock.class})
public abstract class SculkTriggerBlockMixin {
	@Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$silentStep(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
		if (entity instanceof LivingEntity living && ModEnchantments.isSilentStepping(living)) {
			ci.cancel();
		}
	}
}
