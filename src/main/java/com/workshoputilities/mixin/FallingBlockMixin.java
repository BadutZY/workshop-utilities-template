package com.workshoputilities.mixin;

import com.workshoputilities.ToolEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anti-Gravity: block bergravitasi (sand, gravel, concrete powder, dll) tidak jadi jatuh bila ada pemain yang
 * memegang shovel ber-Anti-Gravity di dekatnya. Cukup membatalkan tick jatuhnya; block tetap di tempatnya.
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {
	@Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$antiGravity(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
		if (ToolEnchantments.isAntiGravityActive(world, pos)) {
			ci.cancel();
		}
	}
}
