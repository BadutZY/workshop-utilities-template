package com.workshoputilities.screen;

import com.workshoputilities.ModAdvancements;
import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.ModScreenHandlers;
import com.workshoputilities.block.UpgradeEnchantingTableBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * Upgrade Enchanting Table: dua slot input (Upgrade Book di atas, Upgrade Enchanted di bawahnya), daftar pilihan
 * enchanted book, dan slot output.
 * Klik pilihan -> Upgrade Book dan Upgrade Enchanted terpakai dan enchanted book pilihan tersebut muncul di slot output.
 * Hanya berfungsi bila ada minimal {@link #REQUIRED_BOOKSHELVES} bookshelf di sekitar meja dan slot output kosong.
 * Setiap Upgrade Book yang dipakai juga memakan {@link #LEVEL_COST_PER_BOOK} level XP dari pemain (kecuali mode Creative).
 * Bila syarat belum terpenuhi (XP atau bookshelf kurang), pemain mendapat pemberitahuan di action bar.
 */
public class UpgradeEnchantingScreenHandler extends PanelScreenHandler {
	public static final int REQUIRED_BOOKSHELVES = 20;

	/** Biaya LEVEL XP untuk tiap Upgrade Book yang dipakai (15 level). Tidak berlaku di mode Creative. */
	public static final int LEVEL_COST_PER_BOOK = 15;

	/** Posisi slot (koordinat item di dalam GUI). Harus sama dengan konstanta di UpgradeEnchantingScreen dan texture GUI. */
	public static final int INPUT_X = 8;
	/** Dua slot input ditumpuk dan simetris terhadap garis tengah panel/slot output (y = 53): Book di atas, Upgrade Enchanted di bawah. */
	public static final int INPUT_Y = 34;
	public static final int MATERIAL_X = 8;
	public static final int MATERIAL_Y = 56;
	public static final int OUTPUT_X = 152;
	public static final int OUTPUT_Y = 45;
	/** GUI lebih tinggi dari biasa (184) supaya judul, panel, slot, dan inventory tidak berdesakan. */
	public static final int INVENTORY_Y = 102;
	public static final int HOTBAR_Y = 160;

	private static final int INPUT_SLOT = 0;
	private static final int OUTPUT_SLOT = 1;
	/** Slot Upgrade Enchanted (ditambahkan setelah output supaya indeks slot input/output lama tidak berubah). */
	public static final int MATERIAL_SLOT = 2;
	private static final int INV_START = 3;
	private static final int INV_END = 30;
	private static final int HOTBAR_END = 39;

	private final ScreenHandlerContext context;
	private final Inventory input;
	private final Inventory output;
	private final PropertyDelegate properties = new ArrayPropertyDelegate(1);

	public UpgradeEnchantingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public UpgradeEnchantingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ModScreenHandlers.UPGRADE_ENCHANTING, syncId);
		this.context = context;
		this.input = new SimpleInventory(2) {
			@Override
			public void markDirty() {
				super.markDirty();
				UpgradeEnchantingScreenHandler.this.onContentChanged(this);
			}
		};
		this.output = new SimpleInventory(1) {
			@Override
			public void markDirty() {
				super.markDirty();
				UpgradeEnchantingScreenHandler.this.onContentChanged(this);
			}
		};

		this.addSlot(new Slot(this.input, 0, INPUT_X, INPUT_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(ModItems.UPGRADE_BOOK);
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.output, 0, OUTPUT_X, OUTPUT_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}
		});
		this.addSlot(new Slot(this.input, 1, MATERIAL_X, MATERIAL_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(ModItems.UPGRADE_ENCHANTED);
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
		}
		// Panel tambahan di samping GUI (backpack + equipment); ditaruh paling akhir agar nomor slot di atas tidak bergeser.
		this.addPanelSlots(playerInventory, false);
		this.addProperties(this.properties);
		updateBookshelves();
	}

	/** Biaya level XP untuk Upgrade Book yang sedang ada di slot input. Creative selalu gratis (0). */
	public static int levelCostFor(PlayerEntity player, ItemStack stack) {
		if (player.isCreative()) {
			return 0;
		}
		return LEVEL_COST_PER_BOOK * Math.max(1, stack.getCount());
	}

	/** Jumlah bookshelf yang terhubung ke meja (disinkronkan server -> client). */
	public int getBookshelves() {
		return this.properties.get(0);
	}

	private void updateBookshelves() {
		this.context.run((world, pos) -> {
			if (!world.isClient) {
				this.properties.set(0, UpgradeEnchantingTableBlock.countBookshelves(world, pos));
			}
		});
	}

	@Override
	public void sendContentUpdates() {
		// Dihitung ulang tiap tick selama GUI terbuka, jadi menaruh/mengambil bookshelf langsung terasa.
		updateBookshelves();
		super.sendContentUpdates();
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (id < 0 || id >= ModEnchantments.UPGRADE_OPTIONS.size()) {
			return false;
		}
		ItemStack stack = this.input.getStack(0);
		if (!stack.isOf(ModItems.UPGRADE_BOOK) || !this.output.getStack(0).isEmpty()) {
			return false;
		}
		if (!this.input.getStack(1).isOf(ModItems.UPGRADE_ENCHANTED)) {
			if (!player.getWorld().isClient) {
				player.sendMessage(Text.translatable("message.workshop_utilities.need_upgrade_enchanted")
						.formatted(Formatting.RED), true);
			}
			return false;
		}
		updateBookshelves();
		if (getBookshelves() < REQUIRED_BOOKSHELVES) {
			if (!player.getWorld().isClient) {
				player.sendMessage(Text.translatable("message.workshop_utilities.bookshelves_not_enough",
						getBookshelves(), REQUIRED_BOOKSHELVES).formatted(Formatting.RED), true);
			}
			return false;
		}
		// Di Creative biaya = 0: tidak ada pengecekan maupun pengurangan XP. Mode lain tetap membayar level XP.
		int levelCost = levelCostFor(player, stack);
		if (player.experienceLevel < levelCost) {
			if (!player.getWorld().isClient) {
				player.sendMessage(Text.translatable("message.workshop_utilities.xp_not_enough",
						levelCost, player.experienceLevel).formatted(Formatting.RED), true);
			}
			return false;
		}
		if (player.getWorld().isClient) {
			return true;
		}

		RegistryKey<Enchantment> key = ModEnchantments.UPGRADE_OPTIONS.get(id);
		Registry<Enchantment> registry = player.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
		Optional<RegistryEntry.Reference<Enchantment>> entry = registry.getEntry(key);
		if (entry.isEmpty()) {
			return false;
		}

		// Bayar 15 level XP per Upgrade Book (sinkronisasi ke client ditangani vanilla), lalu Upgrade Book dan
		// Upgrade Enchanted terpakai dan hasilnya muncul di slot output. Creative tidak membayar apa pun.
		if (levelCost > 0) {
			player.addExperienceLevels(-levelCost);
			player.sendMessage(Text.translatable("message.workshop_utilities.xp_spent", levelCost), true);
		}
		this.input.setStack(0, ItemStack.EMPTY);
		this.input.setStack(1, ItemStack.EMPTY);
		this.output.setStack(0, ModEnchantments.createBook(entry.get()));
		ModAdvancements.grant(player, "library_visit");
		this.input.markDirty();
		this.output.markDirty();
		this.context.run((world, pos) -> world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
				SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F));
		this.sendContentUpdates();
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		// Slot equipment (armor, tangan kiri, Elytra): shift-click mengembalikan ke inventory seperti vanilla.
		ItemStack equipMoved = this.quickMoveEquipment(player, slotIndex, INV_START, HOTBAR_END);
		if (equipMoved != null) {
			return equipMoved;
		}
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();

		if (slotIndex == INPUT_SLOT || slotIndex == OUTPUT_SLOT || slotIndex == MATERIAL_SLOT) {
			if (!this.insertItem(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.isOf(ModItems.UPGRADE_BOOK)) {
			if (!this.insertItem(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.isOf(ModItems.UPGRADE_ENCHANTED)) {
			if (!this.insertItem(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slotIndex < INV_END) {
			if (!this.insertItem(stack, INV_END, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, INV_START, INV_END, false)) {
			return ItemStack.EMPTY;
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
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, ModBlocks.UPGRADE_ENCHANTING_TABLE);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			this.dropInventory(player, this.input);
			this.dropInventory(player, this.output);
		}
	}
}