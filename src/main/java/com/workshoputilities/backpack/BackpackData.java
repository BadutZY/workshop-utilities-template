package com.workshoputilities.backpack;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

/** Baca/tulis isi tas ke custom_data milik ItemStack tas (key "BackpackInventory"). */
public final class BackpackData {
	private static final String KEY = "BackpackInventory";
	private static final String ITEMS = "Items";

	private BackpackData() {
	}

	public static void load(ItemStack backpack, DefaultedList<ItemStack> out, RegistryWrapper.WrapperLookup lookup) {
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return;
		}
		NbtCompound root = data.copyNbt();
		if (root.contains(KEY, NbtElement.COMPOUND_TYPE)) {
			Inventories.readNbt(root.getCompound(KEY), out, lookup);
		}
	}

	public static void save(ItemStack backpack, DefaultedList<ItemStack> contents, RegistryWrapper.WrapperLookup lookup) {
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		NbtCompound root = data == null ? new NbtCompound() : data.copyNbt();
		boolean empty = true;
		for (ItemStack stack : contents) {
			if (!stack.isEmpty()) {
				empty = false;
				break;
			}
		}
		if (empty) {
			root.remove(KEY);
		} else {
			NbtCompound inventory = new NbtCompound();
			Inventories.writeNbt(inventory, contents, lookup);
			root.put(KEY, inventory);
		}
		if (root.isEmpty()) {
			backpack.remove(DataComponentTypes.CUSTOM_DATA);
		} else {
			backpack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
		}
	}

	/** Jumlah slot terisi (untuk tooltip), dihitung dari data tersimpan tanpa memuat item. */
	public static int usedSlots(ItemStack backpack) {
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return 0;
		}
		NbtCompound root = data.copyNbt();
		if (!root.contains(KEY, NbtElement.COMPOUND_TYPE)) {
			return 0;
		}
		return root.getCompound(KEY).getList(ITEMS, NbtElement.COMPOUND_TYPE).size();
	}
}
