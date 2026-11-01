package com.workshoputilities.backpack;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Rarity;

/**
 * Lima tier Backpack. Semua fungsinya sama (menyimpan barang), hanya jumlah slot yang berbeda.
 * Hanya Small Backpack yang bisa di-craft. Tier berikutnya hanya lewat Upgrade Table (tas + 8x {@link #upgradeMaterial()}
 * + Upgrade Tool), bukan crafting table; isi tas ikut terbawa.
 *
 * Untuk mengubah ukuran, cukup ganti angka kolom x baris di bawah ini (texture GUI digambar otomatis).
 * GUI sengaja dibuat ramping dan memanjang ke atas (maksimal 12 kolom), bukan melebar ke samping.
 * Jumlah slot tiap tier tidak boleh lebih kecil dari versi sebelumnya (36 / 55 / 78 / 105 / 133),
 * supaya isi tas lama tidak hilang; urutan slot dibaca baris demi baris.
 * Bila layar terlalu pendek untuk semua baris, GUI otomatis memakai scrollbar (lihat BackpackScreen).
 */
public enum BackpackTier {
	SMALL("small_backpack", 9, 4, Rarity.COMMON, Items.IRON_INGOT),        //   9 x  4 =  36 slot
	NORMAL("normal_backpack", 9, 7, Rarity.UNCOMMON, Items.GOLD_INGOT),    //   9 x  7 =  63 slot (> double chest 54)
	MEDIUM("medium_backpack", 10, 8, Rarity.RARE, Items.DIAMOND),          //  10 x  8 =  80 slot
	BIG("big_backpack", 11, 10, Rarity.RARE, Items.NETHERITE_INGOT),       //  11 x 10 = 110 slot
	HUGE("huge_backpack", 12, 12, Rarity.EPIC, null);                      //  12 x 12 = 144 slot

	private final String id;
	private final int columns;
	private final int rows;
	private final Rarity rarity;
	private final Item upgradeMaterial;

	BackpackTier(String id, int columns, int rows, Rarity rarity, Item upgradeMaterial) {
		this.id = id;
		this.columns = columns;
		this.rows = rows;
		this.rarity = rarity;
		this.upgradeMaterial = upgradeMaterial;
	}

	public String id() {
		return this.id;
	}

	public int columns() {
		return this.columns;
	}

	public int rows() {
		return this.rows;
	}

	public int slots() {
		return this.columns * this.rows;
	}

	public Rarity rarity() {
		return this.rarity;
	}

	/** Bahan untuk naik ke tier berikutnya (8 buah mengelilingi tas), null bila sudah tier tertinggi. */
	public Item upgradeMaterial() {
		return this.upgradeMaterial;
	}

	/** Tier berikutnya, atau null bila sudah tertinggi. */
	public BackpackTier next() {
		BackpackTier[] all = values();
		return this.ordinal() + 1 < all.length ? all[this.ordinal() + 1] : null;
	}

	/** Lebar GUI: minimal selebar inventory pemain (9 slot) agar tampilan seperti container vanilla. */
	public int guiWidth() {
		return Math.max(this.columns, 9) * 18 + 14;
	}

	/** Tinggi GUI: rumus sama dengan GenericContainerScreen vanilla (114 + baris * 18). */
	public int guiHeight() {
		return 114 + this.rows * 18;
	}
}
