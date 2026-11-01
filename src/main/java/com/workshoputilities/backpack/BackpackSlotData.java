package com.workshoputilities.backpack;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Data tersinkron slot Backpack (server ke client sendiri dan semua pemain di sekitar), sehingga tas tampil di
 * punggung pemain lain. Didaftarkan di akhir static initializer PlayerEntity (lihat PlayerBackpackSlotMixin),
 * alasannya sama dengan ElytraSlotData: harus sebelum pemain mana pun dibuat.
 */
public final class BackpackSlotData {
	public static TrackedData<ItemStack> BACKPACK;

	public static void register() {
		BACKPACK = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	}

	private BackpackSlotData() {
	}
}
