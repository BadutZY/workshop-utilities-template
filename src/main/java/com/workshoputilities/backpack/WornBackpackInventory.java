package com.workshoputilities.backpack;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** Inventory satu slot yang membungkus slot Backpack milik pemain, dipakai oleh {@link WornBackpackSlot}. */
public class WornBackpackInventory implements Inventory {
	private final PlayerEntity player;

	public WornBackpackInventory(PlayerEntity player) {
		this.player = player;
	}

	private BackpackSlotHolder holder() {
		return BackpackSlotHolder.of(player);
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return holder().workshop_utilities$getBackpackSlot().isEmpty();
	}

	@Override
	public ItemStack getStack(int slot) {
		return holder().workshop_utilities$getBackpackSlot();
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack current = holder().workshop_utilities$getBackpackSlot();
		if (current.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = current.split(amount);
		holder().workshop_utilities$setBackpackSlot(current.isEmpty() ? ItemStack.EMPTY : current);
		return taken;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack current = holder().workshop_utilities$getBackpackSlot();
		holder().workshop_utilities$setBackpackSlot(ItemStack.EMPTY);
		return current;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		holder().workshop_utilities$setBackpackSlot(stack);
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	@Override
	public void markDirty() {
		// Perubahan isi tas disinkronkan oleh BackpackScreenHandler; slot ini tidak perlu berbuat apa-apa.
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		holder().workshop_utilities$setBackpackSlot(ItemStack.EMPTY);
	}
}
