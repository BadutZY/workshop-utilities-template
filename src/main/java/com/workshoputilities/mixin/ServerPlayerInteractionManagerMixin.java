package com.workshoputilities.mixin;

import com.workshoputilities.UnlimitedScope;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlimited: interaksi item dengan block (composter, pot bunga, lectern, jukebox, campfire, respawn anchor,
 * candle di kue, dye di sign, vault, dll) mengurangi stack di Block.onUseWithItem, yaitu sebelum ItemStack.useOnBlock.
 * Seluruh interactBlock dibungkus supaya pengurangan itu ikut dibatalkan untuk item ber-Unlimited.
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	@Inject(method = "interactBlock", at = @At("HEAD"))
	private void workshop_utilities$interactBlockEnter(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
			BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.enter();
	}

	@Inject(method = "interactBlock", at = @At("RETURN"))
	private void workshop_utilities$interactBlockExit(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
			BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.exit();
	}
}
