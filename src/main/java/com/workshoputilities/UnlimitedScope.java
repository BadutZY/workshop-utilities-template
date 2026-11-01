package com.workshoputilities;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Penanda "sedang memakai item" untuk enchantment Unlimited.
 * Berkurangnya jumlah item hanya dibatalkan ketika terjadi di dalam pemakaian item (use, useOnBlock, finishUsing,
 * onStoppedUsing, useOnEntity). Di luar itu (memindah item di inventory, shift-click, split stack, crafting, dll)
 * jumlah tetap berkurang normal, sehingga tidak ada celah menggandakan item lewat inventory.
 * Pakai ThreadLocal karena thread client dan server berjalan terpisah.
 */
public final class UnlimitedScope {
	private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	/** True bila dalam scope yang sedang berjalan ada pengurangan item Unlimited yang dibatalkan. */
	private static final ThreadLocal<boolean[]> SUPPRESSED = ThreadLocal.withInitial(() -> new boolean[1]);

	private UnlimitedScope() {
	}

	public static void enter() {
		DEPTH.get()[0]++;
	}

	public static void exit() {
		int[] depth = DEPTH.get();
		if (depth[0] > 0) {
			depth[0]--;
		}
		if (depth[0] == 0) {
			SUPPRESSED.get()[0] = false;
		}
	}

	/** Dipanggil di awal tiap tick: jaga-jaga bila ada scope yang tidak sempat keluar (mis. karena exception). */
	public static void reset() {
		DEPTH.get()[0] = 0;
		SUPPRESSED.get()[0] = false;
	}

	/** Dicatat saat pengurangan item Unlimited dibatalkan, supaya sisa wadah kosongnya juga tidak diberikan. */
	public static void markSuppressed() {
		SUPPRESSED.get()[0] = true;
	}

	public static boolean isSuppressed() {
		return SUPPRESSED.get()[0];
	}

	/** Wadah kosong yang biasanya muncul sebagai sisa pemakaian (bucket, botol, mangkuk). */
	public static boolean isEmptyContainer(ItemStack stack) {
		return stack.isOf(Items.BUCKET) || stack.isOf(Items.GLASS_BOTTLE) || stack.isOf(Items.BOWL);
	}

	public static boolean isInUse() {
		return DEPTH.get()[0] > 0;
	}
}
