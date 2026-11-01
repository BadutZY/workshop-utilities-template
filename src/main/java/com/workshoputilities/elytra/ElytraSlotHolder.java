package com.workshoputilities.elytra;

import net.minecraft.item.ItemStack;

/** Diterapkan ke PlayerEntity lewat mixin: pemain punya satu slot khusus Elytra di samping slot armor. */
public interface ElytraSlotHolder {
	/** Isi slot Elytra (bisa kosong). Jangan diubah langsung; pakai {@link #workshop_utilities$setElytraSlot}. */
	ItemStack workshop_utilities$getElytraSlot();

	/** Mengganti isi slot Elytra (disalin, sehingga otomatis tersinkron ke client dan pemain lain). */
	void workshop_utilities$setElytraSlot(ItemStack stack);

	/** Menyinkronkan perubahan di tempat (mis. durability berkurang karena terbang) ke client. */
	void workshop_utilities$syncElytraSlot();

	static ElytraSlotHolder of(net.minecraft.entity.player.PlayerEntity player) {
		return (ElytraSlotHolder) (Object) player;
	}
}
