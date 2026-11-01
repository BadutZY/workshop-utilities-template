package com.workshoputilities.backpack;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

/**
 * Data modul Backpack (Crafting Table, Furnace, Blast Furnace, Smoker) di custom_data milik ItemStack tas
 * (key "BackpackModules"), terpisah dari isi tas ("BackpackInventory").
 *
 * Yang disimpan: 5 slot modul + 1 slot modul samping (Bed Module) + 9 slot furnace (input / bahan bakar / hasil untuk 3 furnace), timer tiap furnace
 * (burnTime, fuelTime, cookTime, cookTimeTotal), XP hasil smelting yang belum diambil, dan waktu dunia saat terakhir
 * disimpan. Waktu itu dipakai untuk menyusul smelting yang berjalan selagi tas ditutup (lihat BackpackScreenHandler).
 */
public final class BackpackModuleData {
	private static final String KEY = "BackpackModules";
	private static final String INV = "Inv";
	private static final String TIMERS = "Timers";
	private static final String XP = "Xp";
	private static final String TIME = "Time";

	/** Jumlah slot modul: Crafting, Furnace, Blast Furnace, Smoker, dan Uncrafting. */
	public static final int MODULES = 5;
	/**
	 * Slot modul versi lama (4 slot). Tas yang disimpan sebelum ada slot Uncrafting menaruh 9 slot furnace tepat setelah
	 * 4 slot ini, jadi urutan itu tidak boleh berubah. Slot modul ke-5 dan seterusnya ditaruh PALING AKHIR.
	 */
	public static final int LEGACY_MODULES = 4;
	public static final int FURNACES = 3;
	public static final int SMELT_SLOTS = FURNACES * 3;
	/** Slot modul samping (di kiri GUI, saat ini hanya Bed Module). Ditaruh PALING AKHIR agar data tas lama tetap utuh. */
	public static final int SIDE_MODULES = 1;
	public static final int TOTAL_ITEMS = MODULES + SMELT_SLOTS + SIDE_MODULES;
	public static final int TIMER_COUNT = FURNACES * 4;

	private BackpackModuleData() {
	}

	/** Indeks di {@link State#items} untuk slot modul ke-{@code cell} (0..MODULES-1). */
	public static int moduleIndex(int cell) {
		return cell < LEGACY_MODULES ? cell : LEGACY_MODULES + SMELT_SLOTS + (cell - LEGACY_MODULES);
	}

	/** Indeks di {@link State#items} untuk slot modul samping ke-{@code cell} (0..SIDE_MODULES-1), setelah semua slot lain. */
	public static int sideIndex(int cell) {
		return MODULES + SMELT_SLOTS + cell;
	}

	/** Indeks di {@link State#items} untuk slot furnace ke-{@code slot} (0..SMELT_SLOTS-1: furnace x input/bahan bakar/hasil). */
	public static int smeltIndex(int slot) {
		return LEGACY_MODULES + slot;
	}

	/** Isi data modul: item (4 modul lama, 9 slot furnace, lalu modul ke-5; lihat {@link #moduleIndex}/{@link #smeltIndex}), timer, XP tertunda, dan waktu simpan (-1 = belum ada). */
	public static final class State {
		public final DefaultedList<ItemStack> items = DefaultedList.ofSize(TOTAL_ITEMS, ItemStack.EMPTY);
		public final int[] timers = new int[TIMER_COUNT];
		public final float[] xp = new float[FURNACES];
		public long time = -1L;
	}

	public static State load(ItemStack backpack, RegistryWrapper.WrapperLookup lookup) {
		State state = new State();
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return state;
		}
		NbtCompound root = data.copyNbt();
		if (!root.contains(KEY, NbtElement.COMPOUND_TYPE)) {
			return state;
		}
		NbtCompound tag = root.getCompound(KEY);
		if (tag.contains(INV, NbtElement.COMPOUND_TYPE)) {
			Inventories.readNbt(tag.getCompound(INV), state.items, lookup);
		}
		int[] timers = tag.getIntArray(TIMERS);
		for (int i = 0; i < Math.min(timers.length, state.timers.length); i++) {
			state.timers[i] = timers[i];
		}
		int[] xp = tag.getIntArray(XP);
		for (int i = 0; i < Math.min(xp.length, state.xp.length); i++) {
			state.xp[i] = Float.intBitsToFloat(xp[i]);
		}
		if (tag.contains(TIME, NbtElement.LONG_TYPE)) {
			state.time = tag.getLong(TIME);
		}
		return state;
	}

	public static void save(ItemStack backpack, State state, RegistryWrapper.WrapperLookup lookup) {
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		NbtCompound root = data == null ? new NbtCompound() : data.copyNbt();
		boolean empty = true;
		for (ItemStack stack : state.items) {
			if (!stack.isEmpty()) {
				empty = false;
				break;
			}
		}
		if (empty) {
			root.remove(KEY);
		} else {
			NbtCompound tag = new NbtCompound();
			NbtCompound inventory = new NbtCompound();
			Inventories.writeNbt(inventory, state.items, lookup);
			tag.put(INV, inventory);
			tag.putIntArray(TIMERS, state.timers.clone());
			int[] xp = new int[state.xp.length];
			for (int i = 0; i < xp.length; i++) {
				xp[i] = Float.floatToIntBits(state.xp[i]);
			}
			tag.putIntArray(XP, xp);
			tag.putLong(TIME, state.time);
			root.put(KEY, tag);
		}
		if (root.isEmpty()) {
			backpack.remove(DataComponentTypes.CUSTOM_DATA);
		} else {
			backpack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
		}
	}

	/** True bila ada item di slot modul / furnace (tas seperti ini tidak boleh di-uncraft karena isinya akan hilang). */
	public static boolean hasContents(ItemStack backpack) {
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return false;
		}
		NbtCompound root = data.copyNbt();
		if (!root.contains(KEY, NbtElement.COMPOUND_TYPE)) {
			return false;
		}
		NbtCompound tag = root.getCompound(KEY);
		return tag.contains(INV, NbtElement.COMPOUND_TYPE)
				&& tag.getCompound(INV).getList("Items", NbtElement.COMPOUND_TYPE).size() > 0;
	}
}
