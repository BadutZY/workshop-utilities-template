package com.workshoputilities.elytra;

/**
 * Penanda "sedang mengecek terbang/render elytra". Selama penanda aktif, PlayerEntity.getEquippedStack(CHEST)
 * juga mempertimbangkan Elytra di slot khusus (lihat PlayerEntityMixin). Dengan cara ini seluruh logika vanilla
 * (cek elytra layak pakai, damage durability, cek di client dan server, render) dipakai apa adanya,
 * tanpa menyalin kode vanilla. ThreadLocal karena thread client dan server terpisah.
 */
public final class ElytraContext {
	private static final ThreadLocal<int[]> GLIDE = ThreadLocal.withInitial(() -> new int[1]);
	private static final ThreadLocal<int[]> RENDER = ThreadLocal.withInitial(() -> new int[1]);

	private ElytraContext() {
	}

	public static void enterGlide() {
		GLIDE.get()[0]++;
	}

	public static void exitGlide() {
		int[] d = GLIDE.get();
		if (d[0] > 0) {
			d[0]--;
		}
	}

	public static void enterRender() {
		RENDER.get()[0]++;
	}

	public static void exitRender() {
		int[] d = RENDER.get();
		if (d[0] > 0) {
			d[0]--;
		}
	}

	public static boolean isGlide() {
		return GLIDE.get()[0] > 0;
	}

	public static boolean isRender() {
		return RENDER.get()[0] > 0;
	}

	/** Jaga-jaga bila ada scope yang tidak sempat keluar (mis. karena exception). */
	public static void reset() {
		GLIDE.get()[0] = 0;
		RENDER.get()[0] = 0;
	}
}
