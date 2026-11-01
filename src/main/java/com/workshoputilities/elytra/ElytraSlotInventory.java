package com.workshoputilities.elytra;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** Inventory satu slot yang membungkus slot Elytra milik pemain, dipakai oleh {@link ElytraSlot}. */
public class ElytraSlotInventory implements Inventory {
	private final PlayerEntity player;

	public ElytraSlotInventory(PlayerEntity player) {
		this.player = player;
	}

	private ElytraSlotHolder holder() {
		return ElytraSlotHolder.of(player);
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return holder().workshop_utilities$getElytraSlot().isEmpty();
	}

	@Override
	public ItemStack getStack(int slot) {
		return holder().workshop_utilities$getElytraSlot();
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack current = holder().workshop_utilities$getElytraSlot();
		if (current.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = current.split(amount);
		holder().workshop_utilities$setElytraSlot(current.isEmpty() ? ItemStack.EMPTY : current);
		return taken;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack current = holder().workshop_utilities$getElytraSlot();
		holder().workshop_utilities$setElytraSlot(ItemStack.EMPTY);
		return current;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		holder().workshop_utilities$setElytraSlot(stack);
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	@Override
	public void markDirty() {
		holder().workshop_utilities$syncElytraSlot();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		holder().workshop_utilities$setElytraSlot(ItemStack.EMPTY);
	}
}
