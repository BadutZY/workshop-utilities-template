package com.workshoputilities.screen;

import com.workshoputilities.ModAdvancements;
import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModScreenHandlers;
import com.workshoputilities.uncraft.UncraftRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Uncrafting Table: 1 slot input di kiri, 9 slot output (grid 3x3) di kanan.
 *
 * Grid output awalnya hanya "preview" bahan hasil uncraft. Saat pemain mengambil salah satu item,
 * input dikonsumsi (sejumlah hasil resep) dan seluruh isi grid menjadi item nyata yang bisa diambil.
 * Setelah grid kosong, preview baru muncul lagi bila input masih cukup.
 */
public class UncraftingScreenHandler extends PanelScreenHandler {
	private static final int OUT_START = 1;
	private static final int OUT_END = 10;
	private static final int INV_START = 10;
	private static final int INV_END = 37;
	private static final int HOTBAR_END = 46;

	public static final int BUTTON_PREVIOUS = 0;
	public static final int BUTTON_NEXT = 1;
	/** Lompat 10 pilihan sekaligus (dipakai saat Shift ditekan) agar daftar panjang mudah dinavigasi. */
	public static final int BUTTON_PREVIOUS_10 = 2;
	public static final int BUTTON_NEXT_10 = 3;

	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private final Inventory input;
	private final Inventory output = new SimpleInventory(9);
	private final PropertyDelegate properties = new ArrayPropertyDelegate(2);

	private List<UncraftRecipes.Option> options = List.of();
	private int selected = 0;
	private boolean committed = false;
	private boolean suppress = false;
	private Item lastItem = null;

	public UncraftingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public UncraftingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ModScreenHandlers.UNCRAFTING, syncId);
		this.context = context;
		this.player = playerInventory.player;
		this.input = new SimpleInventory(1) {
			@Override
			public void markDirty() {
				super.markDirty();
				UncraftingScreenHandler.this.onContentChanged(this);
			}
		};

		this.addSlot(new Slot(this.input, 0, 28, 36));
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				this.addSlot(new OutputSlot(this.output, col + row * 3, 94 + col * 18, 18 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
		// Panel tambahan di samping GUI (backpack); ditaruh paling akhir agar nomor slot di atas tidak bergeser.
		this.addPanelSlots(playerInventory, false);
		this.addProperties(this.properties);
	}

	/** Indeks resep yang sedang dipilih (0 based). */
	public int getSelected() {
		return this.properties.get(0);
	}

	/** Jumlah pilihan resep yang tersedia untuk item di slot input. */
	public int getTotal() {
		return this.properties.get(1);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (!this.suppress) {
			refresh();
		}
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		int n = this.options.size();
		if (this.committed || n <= 1) {
			return false;
		}
		if (id == BUTTON_PREVIOUS) {
			this.selected = (this.selected - 1 + n) % n;
		} else if (id == BUTTON_NEXT) {
			this.selected = (this.selected + 1) % n;
		} else if (id == BUTTON_PREVIOUS_10) {
			this.selected = Math.floorMod(this.selected - 10, n);
		} else if (id == BUTTON_NEXT_10) {
			this.selected = (this.selected + 10) % n;
		} else {
			return false;
		}
		show();
		return true;
	}

	private boolean outputsEmpty() {
		for (int i = 0; i < this.output.size(); i++) {
			if (!this.output.getStack(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private void refresh() {
		if (this.player.getWorld().isClient) {
			return;
		}
		if (this.committed) {
			if (!outputsEmpty()) {
				return;
			}
			this.committed = false;
		}
		ItemStack in = this.input.getStack(0);
		this.options = UncraftRecipes.find(this.player.getWorld(), in);
		Item item = in.isEmpty() ? null : in.getItem();
		if (item != this.lastItem) {
			this.selected = 0;
			this.lastItem = item;
		}
		if (this.selected >= this.options.size()) {
			this.selected = 0;
		}
		show();
	}

	private void show() {
		for (int i = 0; i < this.output.size(); i++) {
			this.output.setStack(i, ItemStack.EMPTY);
		}
		if (!this.options.isEmpty()) {
			List<ItemStack> grid = this.options.get(this.selected).grid();
			for (int i = 0; i < 9; i++) {
				this.output.setStack(i, grid.get(i).copy());
			}
		}
		this.properties.set(0, this.selected);
		this.properties.set(1, this.options.size());
	}

	private void onOutputTaken() {
		if (this.player.getWorld().isClient) {
			return;
		}
		if (!this.committed) {
			if (this.options.isEmpty()) {
				return;
			}
			UncraftRecipes.Option option = this.options.get(this.selected);
			this.suppress = true;
			ItemStack in = this.input.getStack(0);
			in.decrement(option.resultCount());
			if (in.isEmpty()) {
				this.input.setStack(0, ItemStack.EMPTY);
			}
			this.suppress = false;
			this.committed = true;
			ModAdvancements.grant(this.player, "uncraft_item");
		}
		if (outputsEmpty()) {
			this.committed = false;
			refresh();
		} else {
			this.properties.set(1, 0);
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}

		if (slotIndex >= OUT_START && slotIndex < OUT_END) {
			// Shift-click pada output: pindahkan semua bahan sekaligus ke inventory pemain.
			List<Integer> targets = new ArrayList<>();
			for (int i = OUT_START; i < OUT_END; i++) {
				if (this.slots.get(i).hasStack()) {
					targets.add(i);
				}
			}
			ItemStack moved = ItemStack.EMPTY;
			for (int i : targets) {
				Slot s = this.slots.get(i);
				ItemStack st = s.getStack();
				if (st.isEmpty()) {
					continue;
				}
				ItemStack before = st.copy();
				if (!this.insertItem(st, INV_START, HOTBAR_END, true)) {
					continue;
				}
				if (st.isEmpty()) {
					s.setStack(ItemStack.EMPTY);
				} else {
					s.markDirty();
				}
				s.onTakeItem(player, before);
				if (moved.isEmpty()) {
					moved = before;
				}
			}
			return moved;
		}

		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();
		if (slotIndex == 0) {
			if (!this.insertItem(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, 0, 1, false)) {
			if (slotIndex < INV_END) {
				if (!this.insertItem(stack, INV_END, HOTBAR_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, INV_START, INV_END, false)) {
				return ItemStack.EMPTY;
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
		// Cegah double-click "kumpulkan semua" mengambil preview tanpa lewat proses uncraft.
		return !(slot instanceof OutputSlot);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, ModBlocks.UNCRAFTING_TABLE);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			this.dropInventory(player, this.input);
			if (this.committed) {
				this.dropInventory(player, this.output);
			}
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
			UncraftingScreenHandler.this.onOutputTaken();
			super.onTakeItem(player, stack);
		}
	}
}