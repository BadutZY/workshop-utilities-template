package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import com.workshoputilities.UnlimitedScope;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlimited: selama sebuah item dipakai (lihat {@link UnlimitedScope}), pengurangan jumlah pada stack ber-Unlimited
 * (yang tidak punya durability) dibatalkan. Ini mencakup block yang ditaruh, makanan, ender pearl/snowball/egg,
 * potion lempar, firework, bone meal, dan amunisi (arrow) yang ditembakkan.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "decrement", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$unlimitedDecrement(int amount, CallbackInfo ci) {
		if (amount <= 0 || !UnlimitedScope.isInUse()) {
			return;
		}
		ItemStack self = (ItemStack) (Object) this;
		if (!self.isDamageable() && ModEnchantments.hasUnlimited(self)) {
			UnlimitedScope.markSuppressed();
			ci.cancel();
		}
	}

	@Inject(method = "use", at = @At("HEAD"))
	private void workshop_utilities$useEnter(World world, PlayerEntity user, Hand hand,
			CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
		UnlimitedScope.enter();
	}

	/**
	 * Keluar dari scope. Bila item ber-Unlimited masih ada tetapi use() mengembalikan stack LAIN (mis. water/lava
	 * bucket berubah jadi bucket kosong), stack asli dipertahankan supaya item tidak berubah/hilang.
	 * Satu handler saja karena handler cancellable kedua di titik yang sama bisa terlewat oleh Mixin.
	 */
	@Inject(method = "use", at = @At("RETURN"), cancellable = true)
	private void workshop_utilities$useExit(World world, PlayerEntity user, Hand hand,
			CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
		UnlimitedScope.exit();
		ItemStack self = (ItemStack) (Object) this;
		TypedActionResult<ItemStack> result = cir.getReturnValue();
		if (result != null && result.getValue() != self && !self.isEmpty() && !self.isDamageable()
				&& ModEnchantments.hasUnlimited(self)) {
			cir.setReturnValue(new TypedActionResult<>(result.getResult(), self));
		}
	}

	@Inject(method = "useOnBlock", at = @At("HEAD"))
	private void workshop_utilities$useOnBlockEnter(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.enter();
	}

	/**
	 * Powder snow bucket mengganti isi tangan dengan bucket kosong setelah ditaruh (setStackInHand, bukan decrement).
	 * Untuk item Unlimited, stack aslinya dikembalikan ke tangan.
	 */
	@Inject(method = "useOnBlock", at = @At("RETURN"))
	private void workshop_utilities$useOnBlockExit(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.exit();
		ItemStack self = (ItemStack) (Object) this;
		PlayerEntity player = context.getPlayer();
		if (player != null && !self.isEmpty() && !self.isDamageable() && ModEnchantments.hasUnlimited(self)) {
			Hand hand = context.getHand();
			ItemStack inHand = player.getStackInHand(hand);
			if (inHand != self && UnlimitedScope.isEmptyContainer(inHand)) {
				player.setStackInHand(hand, self);
			}
		}
	}

	@Inject(method = "useOnEntity", at = @At("HEAD"))
	private void workshop_utilities$useOnEntityEnter(PlayerEntity user, LivingEntity entity, Hand hand,
			CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.enter();
	}

	@Inject(method = "useOnEntity", at = @At("RETURN"))
	private void workshop_utilities$useOnEntityExit(PlayerEntity user, LivingEntity entity, Hand hand,
			CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.exit();
	}

	@Inject(method = "finishUsing", at = @At("HEAD"))
	private void workshop_utilities$finishEnter(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
		UnlimitedScope.enter();
	}

	/** Sama seperti use(): sup/stew yang diganti mangkuk setelah dimakan tetap berupa item aslinya bila Unlimited. */
	@Inject(method = "finishUsing", at = @At("RETURN"), cancellable = true)
	private void workshop_utilities$finishExit(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
		UnlimitedScope.exit();
		ItemStack self = (ItemStack) (Object) this;
		ItemStack result = cir.getReturnValue();
		if (result != self && !self.isEmpty() && !self.isDamageable() && ModEnchantments.hasUnlimited(self)) {
			cir.setReturnValue(self);
		}
	}

	@Inject(method = "onStoppedUsing", at = @At("HEAD"))
	private void workshop_utilities$stoppedEnter(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		UnlimitedScope.enter();
	}

	@Inject(method = "onStoppedUsing", at = @At("RETURN"))
	private void workshop_utilities$stoppedExit(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		UnlimitedScope.exit();
	}
}
