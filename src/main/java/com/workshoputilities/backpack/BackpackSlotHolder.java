package com.workshoputilities.backpack;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Diterapkan ke PlayerEntity lewat mixin: pemain punya satu slot khusus Backpack, terpisah dari slot armor dan
 * slot Elytra, sehingga chestplate, elytra, dan backpack bisa dipakai bersamaan.
 */
public interface BackpackSlotHolder {
	/** Isi slot Backpack (bisa kosong). Jangan diubah langsung; pakai {@link #workshop_utilities$setBackpackSlot}. */
	ItemStack workshop_utilities$getBackpackSlot();

	/** Mengganti isi slot (disalin, sehingga otomatis tersinkron ke client dan pemain lain). */
	void workshop_utilities$setBackpackSlot(ItemStack stack);

	/**
	 * Menyinkronkan perubahan di tempat (isi tas berubah) ke client. Setelah dipanggil, objek ItemStack di slot
	 * adalah salinan baru; ambil ulang lewat {@link #workshop_utilities$getBackpackSlot()} bila perlu.
	 */
	void workshop_utilities$syncBackpackSlot();

	static BackpackSlotHolder of(PlayerEntity player) {
		return (BackpackSlotHolder) (Object) player;
	}
}
