package com.workshoputilities.mixin;

import com.workshoputilities.UnlimitedScope;
import com.workshoputilities.elytra.ElytraContext;
import com.workshoputilities.elytra.ElytraSlotData;
import com.workshoputilities.elytra.ElytraSlotHolder;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1) Unlimited: interaksi item dengan entity (memberi makan/menjinakkan hewan, lead, item frame, allay, mooshroom, dll)
 * mengurangi stack di Entity.interact, di luar ItemStack.useOnEntity. Dibungkus di sini agar ikut dibatalkan.
 * 2) Elytra Slot: penyimpanan (data tersinkron + NBT), drop saat mati, dan getEquippedStack(CHEST) yang
 * mempertimbangkan slot Elytra selama pengecekan terbang / render elytra.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements ElytraSlotHolder {
	private int workshop_utilities$lastElytraDamage = -1;

	@Inject(method = "interact", at = @At("HEAD"))
	private void workshop_utilities$interactEnter(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.enter();
	}

	@Inject(method = "interact", at = @At("RETURN"))
	private void workshop_utilities$interactExit(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		UnlimitedScope.exit();
	}

	// ---- Elytra Slot ----

	/** Mendaftarkan data slot Elytra tepat setelah static initializer PlayerEntity selesai (lihat ElytraSlotData). */
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void workshop_utilities$registerElytraData(CallbackInfo ci) {
		ElytraSlotData.register();
	}

	@Inject(method = "initDataTracker", at = @At("TAIL"))
	private void workshop_utilities$initElytraData(DataTracker.Builder builder, CallbackInfo ci) {
		builder.add(ElytraSlotData.ELYTRA, ItemStack.EMPTY);
	}

	@Override
	public ItemStack workshop_utilities$getElytraSlot() {
		return ((PlayerEntity) (Object) this).getDataTracker().get(ElytraSlotData.ELYTRA);
	}

	@Override
	public void workshop_utilities$setElytraSlot(ItemStack stack) {
		ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		workshop_utilities$lastElytraDamage = copy.isEmpty() ? -1 : copy.getDamage();
		((PlayerEntity) (Object) this).getDataTracker().set(ElytraSlotData.ELYTRA, copy);
	}

	@Override
	public void workshop_utilities$syncElytraSlot() {
		ItemStack current = workshop_utilities$getElytraSlot();
		if (current.isEmpty()) {
			// Elytra patah (jumlah jadi 0) tapi objeknya masih tersimpan: kosongkan dan sinkronkan.
			if (current != ItemStack.EMPTY) {
				workshop_utilities$setElytraSlot(ItemStack.EMPTY);
			}
		} else if (current.getDamage() != workshop_utilities$lastElytraDamage) {
			workshop_utilities$setElytraSlot(current);
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void workshop_utilities$writeElytra(NbtCompound nbt, CallbackInfo ci) {
		ItemStack stack = workshop_utilities$getElytraSlot();
		if (!stack.isEmpty()) {
			nbt.put("UncraftElytraSlot", stack.encode(((PlayerEntity) (Object) this).getRegistryManager()));
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void workshop_utilities$readElytra(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("UncraftElytraSlot", NbtElement.COMPOUND_TYPE)) {
			ItemStack.fromNbt(((PlayerEntity) (Object) this).getRegistryManager(), nbt.get("UncraftElytraSlot"))
					.ifPresent(this::workshop_utilities$setElytraSlot);
		}
	}

	/** Mati tanpa keepInventory: elytra di slot khusus ikut jatuh (kecuali terkena Curse of Vanishing). */
	@Inject(method = "dropInventory", at = @At("TAIL"))
	private void workshop_utilities$dropElytra(CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self.getWorld().isClient || self.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
			return;
		}
		ItemStack stack = workshop_utilities$getElytraSlot();
		if (!stack.isEmpty()) {
			if (!EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
				self.dropItem(stack.copy(), true, false);
			}
			workshop_utilities$setElytraSlot(ItemStack.EMPTY);
		}
	}

	/**
	 * Selama pengecekan terbang atau render elytra, slot dada "melihat" Elytra dari slot khusus bila ada.
	 * Terbang: dipilih elytra yang masih layak pakai (dada diutamakan). Render: elytra apa pun yang terpasang.
	 */
	@Inject(method = "getEquippedStack", at = @At("RETURN"), cancellable = true)
	private void workshop_utilities$elytraSlotAsChest(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
		if (slot != EquipmentSlot.CHEST || (!ElytraContext.isGlide() && !ElytraContext.isRender())) {
			return;
		}
		ItemStack extra = workshop_utilities$getElytraSlot();
		if (extra.isEmpty() || !extra.isOf(Items.ELYTRA)) {
			return;
		}
		ItemStack chest = cir.getReturnValue();
		if (ElytraContext.isGlide()) {
			if (!(chest.isOf(Items.ELYTRA) && ElytraItem.isUsable(chest)) && ElytraItem.isUsable(extra)) {
				cir.setReturnValue(extra);
			}
		} else if (!chest.isOf(Items.ELYTRA)) {
			cir.setReturnValue(extra);
		}
	}
}
