package com.workshoputilities.client.compat;

import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.screen.DisenchantingScreenHandler;
import com.workshoputilities.screen.UpgradeEnchantingScreenHandler;
import com.workshoputilities.uncraft.UncraftRecipes;
import com.workshoputilities.upgrade.UpgradeTableLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lapisan data bersama untuk integrasi Roughly Enough Items (REI) dan Just Enough Items (JEI).
 *
 * Class ini SENGAJA tidak memakai class REI/JEI sama sekali, jadi aman dimuat walau kedua mod itu tidak terpasang.
 * Plugin REI ({@code compat/rei}) dan plugin JEI ({@code compat/jei}) hanya menerjemahkan data di sini menjadi tampilan
 * masing-masing, sehingga isi kedua viewer selalu sama dan aturannya selalu mengikuti logika asli mod
 * ({@link UpgradeTableLogic}, {@link UncraftRecipes}, {@link DisenchantingScreenHandler}, dst).
 *
 * Semua data dihitung dari dunia client yang sedang aktif (resep, enchantment, dan tag dari datapack ikut tersinkron),
 * jadi hasilnya ikut berubah bila datapack/mod lain mengubah resep.
 */
public final class ArcaneCompat {
	public static final String MOD_ID = WorkshopUtilitiesMod.MOD_ID;

	/** Jumlah maksimal item contoh yang dibolak-balik di slot "item yang bisa dipasangi" agar tampilan tetap ringan. */
	private static final int MAX_TARGETS = 48;

	/** Item contoh favorit (diamond) supaya contoh pertama yang tampil selalu item yang familiar. */
	private static final Item[] PREFERRED = {
			Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
			Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
	};

	/** Contoh item tanpa durability untuk enchantment Unlimited (aturan aslinya "semua item tanpa durability"). */
	private static final Item[] UNLIMITED_EXAMPLES = {
			Items.COBBLESTONE, Items.ENDER_PEARL, Items.SNOWBALL, Items.BONE_MEAL, Items.FIREWORK_ROCKET,
			Items.GOLDEN_APPLE, Items.ARROW, Items.WATER_BUCKET, Items.TORCH, Items.COOKED_BEEF
	};

	/** Uncrafting Table: item yang di-uncraft (dengan jumlahnya) dan 9 sel grid (tiap sel = daftar kandidat bahan). */
	public record UncraftEntry(ItemStack input, List<List<ItemStack>> grid) {
	}

	/** Dis-Enchanting Table: item ter-enchant, item polos hasilnya, dan book hasilnya (bila ada Book di slot bawah). */
	public record DisenchantEntry(ItemStack tool, ItemStack plain, ItemStack book, int levels, boolean fused) {
	}

	/**
	 * Upgrade Table. {@code targets} dan {@code results} berpasangan satu-satu (indeks sama = pasangan sama).
	 * Untuk upgrade Backpack keduanya berisi satu item.
	 */
	public record UpgradeEntry(List<ItemStack> targets, ItemStack material, ItemStack tool, List<ItemStack> results,
			boolean backpack) {
	}

	/** Upgrade Enchanting Table: Upgrade Book + Upgrade Enchanted menjadi satu enchanted book pilihan. */
	public record UpgradeBookEntry(ItemStack book, ItemStack catalyst, ItemStack result, int levelCost, int bookshelves) {
	}

	/** Halaman penjelasan "bagaimana ini bekerja" untuk satu atau beberapa item. */
	public record InfoEntry(List<ItemStack> subjects, List<Text> lines) {
	}

	private ArcaneCompat() {
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	private static World world() {
		return MinecraftClient.getInstance().world;
	}

	private static Optional<RegistryEntry.Reference<Enchantment>> lookup(World world, RegistryKey<Enchantment> key) {
		return world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Uncrafting Table
	// ------------------------------------------------------------------------------------------------------------

	public static List<UncraftEntry> uncraftEntries() {
		World world = world();
		List<UncraftEntry> list = new ArrayList<>();
		if (world == null) {
			return list;
		}
		for (UncraftRecipes.Preview preview : UncraftRecipes.listPreviews(world)) {
			list.add(new UncraftEntry(preview.input(), preview.grid()));
		}
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Dis-Enchanting Table
	// ------------------------------------------------------------------------------------------------------------

	public static List<DisenchantEntry> disenchantEntries() {
		World world = world();
		List<DisenchantEntry> list = new ArrayList<>();
		if (world == null) {
			return list;
		}

		// 1) Satu contoh per enchantment buatan mod: item contoh ter-enchant -> item polos + enchanted book berisi enchantment itu.
		for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
			Optional<RegistryEntry.Reference<Enchantment>> entry = lookup(world, key);
			if (entry.isEmpty()) {
				continue;
			}
			List<ItemStack> targets = targetsFor(entry.get());
			if (targets.isEmpty()) {
				continue;
			}
			ItemStack plain = targets.get(0).copyWithCount(1);
			ItemStack tool = enchanted(plain, entry.get(), 1);
			list.add(new DisenchantEntry(tool, plain, ModEnchantments.createBook(entry.get()),
					DisenchantingScreenHandler.calculateLevels(tool), false));
		}

		// 2) Contoh vanilla (Sharpness V) dan contoh campuran vanilla + mod -> Fused Enchanted Book.
		Optional<RegistryEntry.Reference<Enchantment>> sharpness =
				lookup(world, RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("minecraft", "sharpness")));
		if (sharpness.isPresent()) {
			ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
			ItemEnchantmentsComponent.Builder vanilla = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
			vanilla.add(sharpness.get(), 5);
			ItemEnchantmentsComponent vanillaOnly = vanilla.build();
			ItemStack tool = plain.copy();
			tool.set(DataComponentTypes.ENCHANTMENTS, vanillaOnly);
			list.add(new DisenchantEntry(tool, plain, ModEnchantments.createDisenchantBook(vanillaOnly),
					DisenchantingScreenHandler.calculateLevels(tool), false));

			Optional<RegistryEntry.Reference<Enchantment>> lifesteal = lookup(world, ModEnchantments.LIFESTEAL);
			if (lifesteal.isPresent()) {
				ItemEnchantmentsComponent.Builder mixed = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
				mixed.add(sharpness.get(), 5);
				mixed.add(lifesteal.get(), 1);
				ItemEnchantmentsComponent mixedComponent = mixed.build();
				ItemStack mixedTool = plain.copy();
				mixedTool.set(DataComponentTypes.ENCHANTMENTS, mixedComponent);
				list.add(new DisenchantEntry(mixedTool, plain, ModEnchantments.createDisenchantBook(mixedComponent),
						DisenchantingScreenHandler.calculateLevels(mixedTool), true));
			}
		}
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Upgrade Table
	// ------------------------------------------------------------------------------------------------------------

	/** Upgrade Table mode Enchant: item yang cocok + book buatan mod + Upgrade Tool -> item ter-enchant. */
	public static List<UpgradeEntry> enchantUpgradeEntries() {
		World world = world();
		List<UpgradeEntry> list = new ArrayList<>();
		if (world == null) {
			return list;
		}
		for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
			Optional<RegistryEntry.Reference<Enchantment>> entry = lookup(world, key);
			if (entry.isEmpty()) {
				continue;
			}
			List<ItemStack> targets = targetsFor(entry.get());
			if (targets.isEmpty()) {
				continue;
			}
			List<ItemStack> results = new ArrayList<>();
			for (ItemStack target : targets) {
				ItemStack result = enchanted(target, entry.get(), 1);
				// Sama seperti Upgrade Table asli: item ber-Infinite Durability langsung penuh durability-nya.
				ModEnchantments.restoreIfInfinite(result);
				results.add(result);
			}
			list.add(new UpgradeEntry(targets, ModEnchantments.createBook(entry.get()),
					new ItemStack(ModItems.UPGRADE_TOOL), results, false));
		}
		return list;
	}

	/** Upgrade Table mode Backpack: tas + 8 bahan tier + Upgrade Tool -> tas tier berikutnya. */
	public static List<UpgradeEntry> backpackUpgradeEntries() {
		List<UpgradeEntry> list = new ArrayList<>();
		for (BackpackTier tier : BackpackTier.values()) {
			BackpackTier next = tier.next();
			if (next == null || tier.upgradeMaterial() == null) {
				continue;
			}
			list.add(new UpgradeEntry(
					List.of(new ItemStack(ModItems.backpack(tier))),
					new ItemStack(tier.upgradeMaterial(), UpgradeTableLogic.BACKPACK_MATERIAL_COUNT),
					new ItemStack(ModItems.UPGRADE_TOOL),
					List.of(new ItemStack(ModItems.backpack(next))),
					true));
		}
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Upgrade Enchanting Table
	// ------------------------------------------------------------------------------------------------------------

	public static List<UpgradeBookEntry> upgradeBookEntries() {
		World world = world();
		List<UpgradeBookEntry> list = new ArrayList<>();
		if (world == null) {
			return list;
		}
		for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
			Optional<RegistryEntry.Reference<Enchantment>> entry = lookup(world, key);
			if (entry.isEmpty()) {
				continue;
			}
			list.add(new UpgradeBookEntry(new ItemStack(ModItems.UPGRADE_BOOK), new ItemStack(ModItems.UPGRADE_ENCHANTED),
					ModEnchantments.createBook(entry.get()), UpgradeEnchantingScreenHandler.LEVEL_COST_PER_BOOK,
					UpgradeEnchantingScreenHandler.REQUIRED_BOOKSHELVES));
		}
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Halaman penjelasan (info)
	// ------------------------------------------------------------------------------------------------------------

	public static List<InfoEntry> infoEntries() {
		List<InfoEntry> list = new ArrayList<>();

		// Block
		list.add(info(List.of(new ItemStack(ModBlocks.UNCRAFTING_TABLE)), "uncrafting_table", 5));
		list.add(info(List.of(new ItemStack(ModBlocks.DISENCHANTING_TABLE)), "disenchanting_table", 4));
		list.add(info(List.of(new ItemStack(ModBlocks.UPGRADE_TABLE)), "upgrade_table", 4));
		list.add(info(List.of(new ItemStack(ModBlocks.UPGRADE_ENCHANTING_TABLE)), "upgrade_enchanting_table", 4));

		// Bahan dan item khusus
		list.add(info(List.of(new ItemStack(ModItems.UPGRADE_ENCHANTED)), "upgrade_enchanted", 1));
		list.add(info(List.of(new ItemStack(ModItems.UPGRADE_BOOK)), "upgrade_book", 1));
		list.add(info(List.of(new ItemStack(ModItems.UPGRADE_TOOL)), "upgrade_tool", 1));
		list.add(info(List.of(new ItemStack(ModItems.FUSED_ENCHANTED_BOOK)), "fused_enchanted_book", 1));

		// Backpack (semua tier berbagi satu halaman)
		List<ItemStack> backpacks = new ArrayList<>();
		for (BackpackTier tier : BackpackTier.values()) {
			backpacks.add(new ItemStack(ModItems.backpack(tier)));
		}
		list.add(info(backpacks, "backpack", 4));

		// Modul Backpack
		list.add(info(List.of(new ItemStack(ModItems.CRAFTING_MODULE)), "crafting_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.FURNACE_MODULE)), "furnace_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.BLAST_FURNACE_MODULE)), "blast_furnace_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.SMOKER_MODULE)), "smoker_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.UNCRAFTING_MODULE)), "uncrafting_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.BED_MODULE)), "bed_module", 1));
		list.add(info(List.of(new ItemStack(ModItems.TOOLS_HOLDER_MODULE)), "tools_holder_module", 1));

		// Satu halaman per enchanted book buatan mod. Teksnya memakai terjemahan tooltip yang sudah ada
		// (tooltip.workshop_utilities.<nama>.desc.1 / .desc.2 / .applies), jadi tidak ada teks ganda yang harus dirawat.
		World world = world();
		if (world != null) {
			for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
				Optional<RegistryEntry.Reference<Enchantment>> entry = lookup(world, key);
				if (entry.isEmpty()) {
					continue;
				}
				String name = key.getValue().getPath();
				List<Text> lines = new ArrayList<>();
				lines.add(Text.translatable("tooltip." + MOD_ID + "." + name + ".desc.1"));
				lines.add(Text.translatable("tooltip." + MOD_ID + "." + name + ".desc.2"));
				lines.add(Text.translatable("tooltip." + MOD_ID + "." + name + ".applies"));
				lines.add(Text.translatable("compat." + MOD_ID + ".book_how"));
				list.add(new InfoEntry(List.of(ModEnchantments.createBook(entry.get())), lines));
			}
		}
		return list;
	}

	private static InfoEntry info(List<ItemStack> subjects, String name, int lineCount) {
		List<Text> lines = new ArrayList<>();
		for (int i = 1; i <= lineCount; i++) {
			lines.add(Text.translatable("compat." + MOD_ID + ".info." + name + "." + i));
		}
		return new InfoEntry(subjects, lines);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Pembantu
	// ------------------------------------------------------------------------------------------------------------

	/** Salinan item yang diberi enchantment ini pada level tertentu (tanpa mengubah item aslinya). */
	private static ItemStack enchanted(ItemStack base, RegistryEntry<Enchantment> entry, int level) {
		ItemStack copy = base.copy();
		ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
		builder.add(entry, level);
		copy.set(DataComponentTypes.ENCHANTMENTS, builder.build());
		return copy;
	}

	/**
	 * Item yang bisa dipasangi enchantment ini, memakai aturan enchantment itu sendiri ({@code isSupportedItem}),
	 * jadi ikut benar bila tag item diubah datapack atau mod lain menambah tool/armor baru.
	 */
	private static List<ItemStack> targetsFor(RegistryEntry<Enchantment> entry) {
		List<ItemStack> targets = new ArrayList<>();
		if (entry.matchesKey(ModEnchantments.UNLIMITED)) {
			for (Item item : UNLIMITED_EXAMPLES) {
				ItemStack stack = new ItemStack(item);
				if (entry.value().isSupportedItem(stack)) {
					targets.add(stack);
				}
			}
			return targets;
		}
		for (Item item : Registries.ITEM) {
			if (item == Items.AIR) {
				continue;
			}
			ItemStack stack = new ItemStack(item);
			if (entry.value().isSupportedItem(stack)) {
				targets.add(stack);
			}
		}
		// List.sort stabil: item favorit di depan, sisanya tetap berurutan seperti di registry.
		targets.sort(Comparator.comparingInt((ItemStack stack) -> rank(stack.getItem())));
		if (targets.size() > MAX_TARGETS) {
			return new ArrayList<>(targets.subList(0, MAX_TARGETS));
		}
		return targets;
	}

	private static int rank(Item item) {
		for (int i = 0; i < PREFERRED.length; i++) {
			if (PREFERRED[i] == item) {
				return i;
			}
		}
		return PREFERRED.length;
	}
}
