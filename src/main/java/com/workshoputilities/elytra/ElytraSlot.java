package com.workshoputilities.elytra;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

/** Slot Elytra di inventory pemain: hanya menerima Elytra, maksimal 1, dan terkunci oleh Curse of Binding. */
public class ElytraSlot extends Slot {
	/** Posisi item di layar inventory (koordinat relatif panel): tepat di atas slot offhand. */
	public static final int X = 77;
	public static final int Y = 44;

	/** Posisi di tab Survival Inventory pada layar Creative: di kanan kolom armor kanan, tepat di bawah slot Backpack. */
	public static final int CREATIVE_X = 127;
	public static final int CREATIVE_Y = 29;

	private final PlayerEntity owner;

	public ElytraSlot(ElytraSlotInventory inventory, PlayerEntity owner) {
		super(inventory, 0, X, Y);
		this.owner = owner;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return stack.isOf(Items.ELYTRA);
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
