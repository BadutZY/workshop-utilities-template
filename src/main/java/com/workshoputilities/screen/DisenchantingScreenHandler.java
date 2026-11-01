package com.workshoputilities.screen;

import com.workshoputilities.ModAdvancements;
import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModScreenHandlers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Dis-Enchanting Table.
 * Input: slot 0 = item ter-enchant, slot 1 = book (opsional).
 * Output: slot 2 = item tanpa enchantment, slot 3 = enchanted book berisi SEMUA enchantment item tersebut.
 *
 * Pemain mendapat LEVEL XP kembali sesuai enchantment pada item (lihat {@link #calculateLevels}).
 * Level diberikan sekali, saat output pertama kali diambil. Di mode Creative tidak ada level XP yang diberikan.
 *
 * Seperti Uncrafting Table, output awalnya preview. Mengambil salah satu output akan mengonsumsi
 * 1 item (dan 1 book bila ada), sedangkan output lainnya tetap bisa diambil.
 */
public class DisenchantingScreenHandler extends ScreenHandler {
	private static final int OUT_ITEM = 2;
	private static final int OUT_BOOK = 3;
	private static final int INV_START = 4;
	private static final int INV_END = 31;
	private static final int HOTBAR_END = 40;

	/** Pembagi level: total "min power" semua enchantment dibagi nilai ini (dibulatkan ke atas) = level XP yang didapat. */
	private static final double LEVEL_DIVISOR = 5.0;

	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private final Inventory input;
	private final Inventory output = new SimpleInventory(2);

	private boolean committed = false;
	private boolean suppress = false;
	private boolean previewBook = false;
	private long lastSoundTime = -1;

	public DisenchantingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public DisenchantingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ModScreenHandlers.DISENCHANTING, syncId);
		this.context = context;
		this.player = playerInventory.player;
		this.input = new SimpleInventory(2) {
			@Override
			public void markDirty() {
				super.markDirty();
				DisenchantingScreenHandler.this.onContentChanged(this);
			}
		};

		this.addSlot(new Slot(this.input, 0, 36, 22) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return !stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).isEmpty();
			}
		});
		this.addSlot(new Slot(this.input, 1, 36, 48) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(Items.BOOK);
			}
		});
		this.addSlot(new OutputSlot(this.output, 0, 122, 22));
		this.addSlot(new OutputSlot(this.output, 1, 122, 48));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	/**
	 * Menghitung LEVEL XP yang didapat dari membuang semua enchantment pada item.
	 * Tiap enchantment (kecuali curse) dihitung dari min power pada levelnya (nilai yang sama dengan Grindstone),
	 * jadi enchantment yang lebih tinggi/langka memberi level lebih banyak. Totalnya dibagi {@link #LEVEL_DIVISOR}
	 * dan dibulatkan ke atas (contoh: Infinite Durability = 5 level, Sharpness V = 9 level).
	 * Deterministik (tanpa random) sehingga client menampilkan angka yang sama dengan server.
	 */
	public static int calculateLevels(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		ItemEnchantmentsComponent enchantments =
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		int total = 0;
		for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
			RegistryEntry<Enchantment> enchantment = entry.getKey();
			if (enchantment.isIn(EnchantmentTags.CURSE)) {
				continue;
			}
			total += enchantment.value().getMinPower(entry.getIntValue());
		}
		if (total <= 0) {
			return 0;
		}
		return (int) Math.ceil(total / LEVEL_DIVISOR);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (!this.suppress) {
			refresh();
		}
	}

	private boolean outputsEmpty() {
		return this.output.getStack(0).isEmpty() && this.output.getStack(1).isEmpty();
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
		this.output.setStack(0, ItemStack.EMPTY);
		this.output.setStack(1, ItemStack.EMPTY);
		this.previewBook = false;

		ItemStack item = this.input.getStack(0);
		if (item.isEmpty()) {
			return;
		}
		ItemEnchantmentsComponent enchantments =
				item.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		if (enchantments.isEmpty()) {
			return;
		}

		ItemStack clean = item.copyWithCount(1);
		clean.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		this.output.setStack(0, clean);

		ItemStack book = this.input.getStack(1);
		if (!book.isEmpty() && book.isOf(Items.BOOK)) {
			// Satu book menampung semua enchantment sekaligus. Bila campuran vanilla + mod, hasilnya Fused Enchanted Book
			// (item khusus) supaya Upgrade Table memasang semuanya dan enchantment vanilla tidak hilang.
			this.output.setStack(1, ModEnchantments.createDisenchantBook(enchantments));
			this.previewBook = true;
		}
	}

	/** Suara grindstone vanilla saat output diambil. Satu suara per tick agar shift-click (2 output sekaligus) tidak dobel. */
	private void playGrindstoneSound() {
		long now = this.player.getWorld().getTime();
		if (now == this.lastSoundTime) {
			return;
		}
		this.lastSoundTime = now;
		this.context.run((world, pos) -> world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE,
				SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F));
	}

	/** Menambah level XP pemain langsung (disinkronkan ke client oleh vanilla), disertai suara dan pesan action bar. */
	private void giveLevels(int levels) {
		if (levels <= 0 || this.player.isCreative()) {
			return;
		}
		this.player.addExperienceLevels(levels);
		this.player.getWorld().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(),
				SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
		this.player.sendMessage(Text.translatable("message.workshop_utilities.xp_gained", levels), true);
	}

	private void onOutputTaken() {
		if (this.player.getWorld().isClient) {
			return;
		}
		playGrindstoneSound();
		if (!this.committed) {
			this.suppress = true;
			ItemStack item = this.input.getStack(0);
			// Level dihitung dari item sebelum dikurangi, lalu diberikan sekali per proses dis-enchant.
			giveLevels(calculateLevels(item));
			item.decrement(1);
			if (item.isEmpty()) {
				this.input.setStack(0, ItemStack.EMPTY);
			}
			if (this.previewBook) {
				ItemStack book = this.input.getStack(1);
				book.decrement(1);
				if (book.isEmpty()) {
					this.input.setStack(1, ItemStack.EMPTY);
				}
			}
			this.suppress = false;
			this.committed = true;
			this.previewBook = false;
			ModAdvancements.grant(this.player, "disenchant_item");
		}
		if (outputsEmpty()) {
			this.committed = false;
			refresh();
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}

		if (slotIndex == OUT_ITEM || slotIndex == OUT_BOOK) {
			List<Integer> targets = new ArrayList<>();
			for (int i = OUT_ITEM; i <= OUT_BOOK; i++) {
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
		if (slotIndex < 2) {
			if (!this.insertItem(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			boolean moved = false;
			if (this.slots.get(0).canInsert(stack)) {
				moved = this.insertItem(stack, 0, 1, false);
			}
			if (!moved && this.slots.get(1).canInsert(stack)) {
				moved = this.insertItem(stack, 1, 2, false);
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
		return canUse(this.context, player, ModBlocks.DISENCHANTING_TABLE);
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
			DisenchantingScreenHandler.this.onOutputTaken();
			super.onTakeItem(player, stack);
		}
	}
}