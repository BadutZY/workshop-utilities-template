package com.workshoputilities.mixin;

import com.workshoputilities.backpack.BackpackSlotData;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slot Backpack khusus (terpisah dari slot chestplate dan slot Elytra): penyimpanan (data tersinkron + NBT),
 * drop saat mati, dan migrasi tas lama yang dulu dipakai di slot chestplate.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerBackpackSlotMixin implements BackpackSlotHolder {
	private static final String NBT_KEY = "UncraftBackpackSlot";

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void workshop_utilities$registerBackpackData(CallbackInfo ci) {
		BackpackSlotData.register();
	}

	@Inject(method = "initDataTracker", at = @At("TAIL"))
	private void workshop_utilities$initBackpackData(DataTracker.Builder builder, CallbackInfo ci) {
		builder.add(BackpackSlotData.BACKPACK, ItemStack.EMPTY);
	}

	@Override
	public ItemStack workshop_utilities$getBackpackSlot() {
		return ((PlayerEntity) (Object) this).getDataTracker().get(BackpackSlotData.BACKPACK);
	}

	@Override
	public void workshop_utilities$setBackpackSlot(ItemStack stack) {
		// Selalu disalin: objek baru membuat DataTracker menganggap data berubah dan mengirimnya ke client.
		ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		((PlayerEntity) (Object) this).getDataTracker().set(BackpackSlotData.BACKPACK, copy);
	}

	@Override
	public void workshop_utilities$syncBackpackSlot() {
		ItemStack current = workshop_utilities$getBackpackSlot();
		workshop_utilities$setBackpackSlot(current.isEmpty() ? ItemStack.EMPTY : current);
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void workshop_utilities$writeBackpack(NbtCompound nbt, CallbackInfo ci) {
		ItemStack stack = workshop_utilities$getBackpackSlot();
		if (!stack.isEmpty()) {
			nbt.put(NBT_KEY, stack.encode(((PlayerEntity) (Object) this).getRegistryManager()));
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void workshop_utilities$readBackpack(NbtCompound nbt, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (nbt.contains(NBT_KEY, NbtElement.COMPOUND_TYPE)) {
			ItemStack.fromNbt(self.getRegistryManager(), nbt.get(NBT_KEY))
					.ifPresent(this::workshop_utilities$setBackpackSlot);
		}
		// Migrasi: versi lama memakai slot chestplate untuk tas. Pindahkan ke slot Backpack bila masih kosong
		// (bila sudah terisi, tas tetap di slot dada sebagai item biasa dan bisa diambil pemain).
		int chestIndex = EquipmentSlot.CHEST.getEntitySlotId();
		ItemStack chest = self.getInventory().armor.get(chestIndex);
		if (chest.getItem() instanceof BackpackItem && workshop_utilities$getBackpackSlot().isEmpty()) {
			workshop_utilities$setBackpackSlot(chest);
			self.getInventory().armor.set(chestIndex, ItemStack.EMPTY);
		}
	}

	/** Mati tanpa keepInventory: tas di slot khusus ikut jatuh (kecuali terkena Curse of Vanishing). */
	@Inject(method = "dropInventory", at = @At("TAIL"))
	private void workshop_utilities$dropBackpack(CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self.getWorld().isClient || self.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
			return;
		}
		ItemStack stack = workshop_utilities$getBackpackSlot();
		if (!stack.isEmpty()) {
			if (!EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
				self.dropItem(stack.copy(), true, false);
			}
			workshop_utilities$setBackpackSlot(ItemStack.EMPTY);
		}
	}
}
