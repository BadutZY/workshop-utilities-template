package com.workshoputilities.screen;

import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.ModScreenHandlers;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.upgrade.UpgradeTableLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Upgrade Table: 3 slot input (kiri atas = item / Backpack, kanan atas = enchanted book mod atau bahan Backpack,
 * bawah = Upgrade Tool) dan 1 slot output. Aturan upgrade ada di {@link UpgradeTableLogic}.
 *
 * Output hanyalah preview: input baru dipakai saat output diambil (klik biasa maupun shift-click).
 * Perilakunya mengikuti Anvil: hanya perubahan slot input yang memicu penghitungan ulang hasil.
 */
public class UpgradeTableScreenHandler extends ScreenHandler {
	/** Posisi slot (koordinat item di dalam GUI). Harus sama dengan texture GUI (tools/generate_upgrade_table_textures.py). */
	public static final int TARGET_X = 30;
	public static final int TARGET_Y = 22;
	public static final int MATERIAL_X = 74;
	public static final int MATERIAL_Y = 22;
	public static final int TOOL_X = 52;
	public static final int TOOL_Y = 50;
	public static final int OUTPUT_X = 138;
	public static final int OUTPUT_Y = 36;

	public static final int TARGET_SLOT = 0;
	public static final int MATERIAL_SLOT = 1;
	public static final int TOOL_SLOT = 2;
	private static final int OUTPUT_SLOT = 3;
	private static final int INV_START = 4;
	private static final int INV_END = 31;
	private static final int HOTBAR_END = 40;

	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private final Inventory input;
	/** Output sengaja SimpleInventory biasa (tanpa markDirty khusus) supaya mengambil hasil tidak memicu hitung ulang di tengah klik. */
	private final Inventory output = new SimpleInventory(1);

	public UpgradeTableScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public UpgradeTableScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ModScreenHandlers.UPGRADE_TABLE, syncId);
		this.context = context;
		this.player = playerInventory.player;
		this.input = new SimpleInventory(3) {
			@Override
			public void markDirty() {
				super.markDirty();
				UpgradeTableScreenHandler.this.onContentChanged(this);
			}
		};

		this.addSlot(new Slot(this.input, TARGET_SLOT, TARGET_X, TARGET_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return !stack.isOf(Items.ENCHANTED_BOOK) && !stack.isOf(ModItems.FUSED_ENCHANTED_BOOK)
						&& !stack.isOf(ModItems.UPGRADE_TOOL);
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.input, MATERIAL_SLOT, MATERIAL_X, MATERIAL_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isMaterial(stack);
			}
		});
		this.addSlot(new Slot(this.input, TOOL_SLOT, TOOL_X, TOOL_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(ModItems.UPGRADE_TOOL);
			}
		});
		this.addSlot(new OutputSlot(this.output, 0, OUTPUT_X, OUTPUT_Y));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	/** Enchanted book buatan mod atau bahan upgrade Backpack (iron/gold ingot, diamond, netherite ingot). */
	private static boolean isMaterial(ItemStack stack) {
		if (!ModEnchantments.storedModEnchantments(stack).isEmpty()) {
			return true;
		}
		for (BackpackTier tier : BackpackTier.values()) {
			if (tier.upgradeMaterial() != null && stack.isOf(tier.upgradeMaterial())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (inventory == this.input) {
			updateResult();
		}
	}

	private void updateResult() {
		if (this.player.getWorld().isClient) {
			return;
		}
		UpgradeTableLogic.Result result = UpgradeTableLogic.evaluate(
				this.input.getStack(TARGET_SLOT), this.input.getStack(MATERIAL_SLOT), this.input.getStack(TOOL_SLOT));
		this.output.setStack(0, result.status() == UpgradeTableLogic.Status.OK ? result.output().copy() : ItemStack.EMPTY);
		this.sendContentUpdates();
	}

	/** Dipanggil saat pemain mengambil output: item, bahan, dan 1 Upgrade Tool terpakai. */
	private void onOutputTaken() {
		if (this.player.getWorld().isClient) {
			return;
		}
		UpgradeTableLogic.Result result = UpgradeTableLogic.evaluate(
				this.input.getStack(TARGET_SLOT), this.input.getStack(MATERIAL_SLOT), this.input.getStack(TOOL_SLOT));
		if (result.status() != UpgradeTableLogic.Status.OK) {
			return;
		}
		boolean enchanting = result.materialCost() == 1
				&& !ModEnchantments.storedModEnchantments(this.input.getStack(MATERIAL_SLOT)).isEmpty();
		this.input.getStack(TARGET_SLOT).decrement(1);
		this.input.getStack(MATERIAL_SLOT).decrement(result.materialCost());
		this.input.getStack(TOOL_SLOT).decrement(1);
		for (int i = 0; i < this.input.size(); i++) {
			if (this.input.getStack(i).isEmpty()) {
				this.input.setStack(i, ItemStack.EMPTY);
			}
		}
		// Memicu hitung ulang hasil (bisa ada upgrade berikutnya bila bahan masih cukup).
		this.input.markDirty();
		this.context.run((world, pos) -> {
			world.playSound(null, pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0F,
					world.random.nextFloat() * 0.1F + 0.9F);
			if (enchanting) {
				world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.6F,
						world.random.nextFloat() * 0.1F + 1.1F);
			}
		});
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();

		if (slotIndex == OUTPUT_SLOT) {
			if (!this.insertItem(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (slotIndex < OUTPUT_SLOT) {
			if (!this.insertItem(stack, INV_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			boolean moved = false;
			if (this.slots.get(TOOL_SLOT).canInsert(stack)) {
				moved = this.insertItem(stack, TOOL_SLOT, TOOL_SLOT + 1, false);
			} else if (this.slots.get(MATERIAL_SLOT).canInsert(stack)) {
				moved = this.insertItem(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false);
			}
			if (!moved && this.slots.get(TARGET_SLOT).canInsert(stack)) {
				moved = this.insertItem(stack, TARGET_SLOT, TARGET_SLOT + 1, false);
			}
			if (!moved) {
				if (slotIndex < INV_END) {
					if (!this.insertItem(stack, INV_END, HOTBAR_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.insertItem(stack, INV_START, INV_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		}

		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTakeItem(player, stack);
		return original;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return !(slot instanceof OutputSlot);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, ModBlocks.UPGRADE_TABLE);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			// Output hanya preview, jadi hanya input yang dikembalikan.
			this.dropInventory(player, this.input);
		}
	}

	private class OutputSlot extends Slot {
		OutputSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false;
		}

		@Override
		public void onTakeItem(PlayerEntity player, ItemStack stack) {
			UpgradeTableScreenHandler.this.onOutputTaken();
			super.onTakeItem(player, stack);
		}
	}
}
