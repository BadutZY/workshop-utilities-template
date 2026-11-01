package com.workshoputilities.backpack;

import com.workshoputilities.item.BackpackItem;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Slot Backpack di inventory pemain: hanya menerima Backpack, maksimal 1, dan terkunci oleh Curse of Binding. */
public class WornBackpackSlot extends Slot {
	/** Posisi item di inventory survival (koordinat relatif panel): kolom kanan model pemain, di atas slot Elytra. */
	public static final int X = 77;
	public static final int Y = 26;

	/** Posisi di tab Survival Inventory pada layar Creative: kolom kanan, tepat di atas slot Elytra. */
	public static final int CREATIVE_X = 127;
	public static final int CREATIVE_Y = 11;

	public WornBackpackSlot(WornBackpackInventory inventory) {
		super(inventory, 0, X, Y);
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return stack.getItem() instanceof BackpackItem;
	}

	@Override
	public int getMaxItemCount() {
		return 1;
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack stack = getStack();
		if (!stack.isEmpty() && !player.isCreative()
				&& EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE)) {
			return false;
		}
		return super.canTakeItems(player);
	}
}
