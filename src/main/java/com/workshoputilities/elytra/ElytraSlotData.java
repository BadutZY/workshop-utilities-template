package com.workshoputilities.elytra;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Data yang ikut tersinkron otomatis (server ke client pemain itu sendiri dan semua pemain di sekitarnya),
 * sehingga elytra di slot khusus ikut tampil di model pemain dan dipakai untuk terbang di kedua sisi.
 * Didaftarkan di akhir static initializer PlayerEntity (lihat PlayerEntityMixin): setelah data vanilla selesai
 * terdaftar, tetapi SEBELUM pemain mana pun dibuat. Bila didaftarkan lebih lambat (mis. saat pemain pertama dibuat),
 * array DataTracker.Builder sudah terlanjur dibuat dengan ukuran lama dan terjadi ArrayIndexOutOfBoundsException.
 */
public final class ElytraSlotData {
	public static TrackedData<ItemStack> ELYTRA;

	public static void register() {
		ELYTRA = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	}

	private ElytraSlotData() {
	}
}
