package com.workshoputilities.mixin;

import com.workshoputilities.backpack.WornBackpackInventory;
import com.workshoputilities.backpack.WornBackpackSlot;
import com.workshoputilities.elytra.ElytraSlot;
import com.workshoputilities.elytra.ElytraSlotInventory;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Menambah slot Elytra dan slot Backpack di inventory pemain. Keduanya ditaruh setelah offhand (indeks 46 dan 47)
 * supaya nomor slot vanilla tidak bergeser. Shift-click: Elytra / Backpack dari inventory masuk ke slotnya bila
 * kosong; dari slot khusus kembali ke inventory.
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {
	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void workshop_utilities$addExtraSlots(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
		this.addSlot(new ElytraSlot(new ElytraSlotInventory(owner), owner));
		this.addSlot(new WornBackpackSlot(new WornBackpackInventory(owner)));
	}

	private int workshop_utilities$indexOf(Class<? extends Slot> type) {
		for (int i = this.slots.size() - 1; i >= 0; i--) {
			if (type.isInstance(this.slots.get(i))) {
				return i;
			}
		}
		return -1;
	}

	@Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$quickMoveExtra(PlayerEntity player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (slotIndex < 0 || slotIndex >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return;
		}
		ItemStack stack = slot.getStack();
		if (slot instanceof ElytraSlot || slot instanceof WornBackpackSlot) {
			// Dari slot khusus kembali ke inventory (9..44), dengan fallback ke offhand dihindari.
			ItemStack copy = stack.copy();
			if (!this.insertItem(stack, 9, 45, false)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			slot.setStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
			cir.setReturnValue(copy);
			return;
		}
		if (slotIndex < 9 || slotIndex > 45) {
			return;
		}
		int target = -1;
		if (stack.isOf(Items.ELYTRA)) {
			target = workshop_utilities$indexOf(ElytraSlot.class);
		} else if (stack.getItem() instanceof BackpackItem) {
			target = workshop_utilities$indexOf(WornBackpackSlot.class);
		}
		if (target < 0 || this.slots.get(target).hasStack()) {
			return;
		}
		ItemStack copy = stack.copy();
		if (this.insertItem(stack, target, target + 1, false)) {
			slot.setStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
			cir.setReturnValue(copy);
		}
	}
}
