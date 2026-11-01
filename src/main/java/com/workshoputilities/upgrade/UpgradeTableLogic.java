package com.workshoputilities.upgrade;

import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Aturan Upgrade Table. Fungsi murni (tanpa efek samping) sehingga server dan GUI client memakai hasil yang sama.
 *
 * Susunan slot: kiri atas = item yang diupgrade, kanan atas = bahan, bawah = {@link ModItems#UPGRADE_TOOL}.
 *
 * <ul>
 * <li><b>Enchant</b>: bahan berupa enchanted book buatan mod atau Fused Enchanted Book. Item mendapat SEMUA enchantment di book (vanilla juga) dan tetap
 *     mempertahankan enchantment lain. Enchantment buatan mod tidak bisa dipasang lewat Anvil.</li>
 * <li><b>Backpack</b>: item berupa Backpack dan bahan berupa {@value #BACKPACK_MATERIAL_COUNT} bahan tier tersebut
 *     (lihat {@link BackpackTier#upgradeMaterial()}). Hasilnya Backpack tier berikutnya dengan isi dan nama yang sama.</li>
 * </ul>
 * Setiap upgrade memakai 1 Upgrade Tool.
 */
public final class UpgradeTableLogic {
	/** Jumlah bahan (ingot/diamond) untuk naik satu tier Backpack. */
	public static final int BACKPACK_MATERIAL_COUNT = 8;

	private UpgradeTableLogic() {
	}

	public enum Status {
		/** Slot item kosong. */
		IDLE(false),
		/** Item sudah ada tetapi bahan belum dimasukkan. */
		NEED_MATERIAL(false),
		/** Bahan kurang atau salah jenis. */
		WRONG_MATERIAL(true),
		NEED_TOOL(false),
		MAX_TIER(true),
		/** Enchantment tidak cocok untuk item ini. */
		UNSUPPORTED(true),
		ALREADY_HAS(true),
		CONFLICT(true),
		OK(false);

		private final boolean error;

		Status(boolean error) {
			this.error = error;
		}

		/** True bila kombinasi ini tidak valid (GUI menampilkan tanda silang merah di panah). */
		public boolean isError() {
			return this.error;
		}
	}

	/**
	 * @param output       hasil upgrade (kosong bila status bukan OK)
	 * @param materialCost jumlah bahan di slot kanan yang terpakai
	 * @param message      keterangan untuk pemain (selalu terisi)
	 */
	public record Result(Status status, ItemStack output, int materialCost, Text message) {
		private static Result of(Status status, Text message) {
			return new Result(status, ItemStack.EMPTY, 0, message);
		}
	}

	public static Result evaluate(ItemStack target, ItemStack material, ItemStack tool) {
		if (target.isEmpty()) {
			return Result.of(Status.IDLE, Text.translatable("gui.workshop_utilities.upgrade_table.idle"));
		}
		List<RegistryEntry<Enchantment>> books = ModEnchantments.storedModEnchantments(material);
		if (!books.isEmpty()) {
			return evaluateEnchant(target, material, tool);
		}
		if (target.getItem() instanceof BackpackItem backpack) {
			return evaluateBackpack(backpack.getTier(), target, material, tool);
		}
		if (material.isEmpty()) {
			return Result.of(Status.NEED_MATERIAL, Text.translatable("gui.workshop_utilities.upgrade_table.need_book"));
		}
		return Result.of(Status.WRONG_MATERIAL, Text.translatable("gui.workshop_utilities.upgrade_table.wrong_material"));
	}

	private static Result evaluateEnchant(ItemStack target, ItemStack book, ItemStack tool) {
		ItemEnchantmentsComponent existing =
				target.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		// SEMUA enchantment di dalam book dipasang (vanilla maupun buatan mod), bukan hanya yang buatan mod.
		// Ini yang membuat Fused Enchanted Book (dan book campuran lama) tidak lagi menghilangkan enchantment vanilla.
		ItemEnchantmentsComponent stored =
				book.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

		ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(existing);
		boolean changed = false;
		Text firstDuplicate = null;
		for (RegistryEntry<Enchantment> entry : stored.getEnchantments()) {
			Text name = entry.value().description();
			if (!entry.value().isSupportedItem(target)) {
				return Result.of(Status.UNSUPPORTED, Text.translatable("gui.workshop_utilities.upgrade_table.unsupported", name));
			}
			for (RegistryEntry<Enchantment> held : existing.getEnchantments()) {
				if (held.value() == entry.value()) {
					continue;
				}
				if (held.value().exclusiveSet().contains(entry) || entry.value().exclusiveSet().contains(held)) {
					return Result.of(Status.CONFLICT, Text.translatable("gui.workshop_utilities.upgrade_table.conflict", name));
				}
			}
			int bookLevel = Math.max(1, stored.getLevel(entry));
			int heldLevel = existing.getLevel(entry);
			if (heldLevel >= bookLevel) {
				// Sudah punya (level sama atau lebih tinggi): dilewati, enchantment lain di book tetap dipasang.
				if (firstDuplicate == null) {
					firstDuplicate = entry.value().description();
				}
				continue;
			}
			builder.set(entry, bookLevel);
			changed = true;
		}
		if (!changed) {
			Text name = firstDuplicate != null ? firstDuplicate : Text.empty();
			return Result.of(Status.ALREADY_HAS, Text.translatable("gui.workshop_utilities.upgrade_table.already_has", name));
		}
		if (!tool.isOf(ModItems.UPGRADE_TOOL)) {
			return Result.of(Status.NEED_TOOL, Text.translatable("gui.workshop_utilities.upgrade_table.need_tool"));
		}

		ItemStack output = target.copyWithCount(1);
		output.set(DataComponentTypes.ENCHANTMENTS, builder.build());
		// Seperti anvil: item ber-Infinite Durability langsung penuh durability-nya.
		ModEnchantments.restoreIfInfinite(output);
		return new Result(Status.OK, output, 1, Text.translatable("gui.workshop_utilities.upgrade_table.ready"));
	}

	private static Result evaluateBackpack(BackpackTier tier, ItemStack backpack, ItemStack material, ItemStack tool) {
		BackpackTier next = tier.next();
		if (next == null) {
			return Result.of(Status.MAX_TIER, Text.translatable("gui.workshop_utilities.upgrade_table.max_tier"));
		}
		Item required = tier.upgradeMaterial();
		Text needs = Text.translatable("gui.workshop_utilities.upgrade_table.need_material",
				BACKPACK_MATERIAL_COUNT, required.getName());
		if (material.isEmpty() || (material.isOf(required) && material.getCount() < BACKPACK_MATERIAL_COUNT)) {
			return Result.of(Status.NEED_MATERIAL, needs);
		}
		if (!material.isOf(required)) {
			return Result.of(Status.WRONG_MATERIAL, needs);
		}
		if (!tool.isOf(ModItems.UPGRADE_TOOL)) {
			return Result.of(Status.NEED_TOOL, Text.translatable("gui.workshop_utilities.upgrade_table.need_tool"));
		}

		ItemStack output = new ItemStack(ModItems.backpack(next));
		// Isi tas (data kustom), nama kustom, dan enchantment ikut pindah ke tas baru.
		NbtComponent data = backpack.get(DataComponentTypes.CUSTOM_DATA);
		if (data != null) {
			output.set(DataComponentTypes.CUSTOM_DATA, data);
		}
		Text name = backpack.get(DataComponentTypes.CUSTOM_NAME);
		if (name != null) {
			output.set(DataComponentTypes.CUSTOM_NAME, name);
		}
		ItemEnchantmentsComponent enchantments = backpack.get(DataComponentTypes.ENCHANTMENTS);
		if (enchantments != null) {
			output.set(DataComponentTypes.ENCHANTMENTS, enchantments);
		}
		return new Result(Status.OK, output, BACKPACK_MATERIAL_COUNT,
				Text.translatable("gui.workshop_utilities.upgrade_table.ready"));
	}
}
