package com.workshoputilities;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Daftar enchantment buatan mod ini. Enchantment-nya sendiri didefinisikan lewat datapack:
 * data/workshop_utilities/enchantment/&lt;nama&gt;.json
 */
public final class ModEnchantments {
	/** Semua item yang punya durability tidak akan pernah berkurang durability-nya. */
	public static final RegistryKey<Enchantment> INFINITE_DURABILITY = key("infinite_durability");

	/** Hanya untuk helmet: Enderman tidak marah saat dilihat, dan Enderman yang sudah marah menjadi tenang selama dipakai. */
	public static final RegistryKey<Enchantment> ENDER_BLIND = key("ender_blind");

	/** Hanya untuk boots: pemain bisa berjalan di atas powder snow tanpa tenggelam (seperti leather boots). */
	public static final RegistryKey<Enchantment> SNOW_WALKER = key("snow_walker");

	/** Untuk semua armor (bukan tools): pemakai tidak kedinginan (freeze) walau tenggelam di powder snow. */
	public static final RegistryKey<Enchantment> FROZEN_WARD = key("frozen_ward");

	/** Hanya untuk leggings: pemakai tidak terdeteksi sculk sensor/shrieker/Warden dan tidak memicu tripwire/pressure plate. */
	public static final RegistryKey<Enchantment> SILENT_STEP = key("silent_step");

	/** Untuk semua armor (bukan tools): Piglin menganggap pemakai seperti memakai gold armor, jadi tidak menyerang. */
	public static final RegistryKey<Enchantment> PIGLIN_PROTECTION = key("piglin_protection");

	/** Hanya untuk pickaxe: menghancurkan satu ore ikut menambang ore sejenis yang saling berdekatan. */
	public static final RegistryKey<Enchantment> VEIN_MINER = key("vein_miner");

	/** Hanya untuk sword: setiap serangan ketiga yang berhasil mengenai memanggil petir + bonus damage. */
	public static final RegistryKey<Enchantment> THUNDER_SLASH = key("thunder_slash");

	/** Hanya untuk shovel: sand, gravel, dan block bergravitasi lain di dekat pemain tidak jatuh saat penyangganya dihancurkan. */
	public static final RegistryKey<Enchantment> ANTI_GRAVITY = key("anti_gravity");

	/** Hanya untuk axe: menghancurkan satu log menebang seluruh pohon. */
	public static final RegistryKey<Enchantment> WOODCUTTER = key("woodcutter");

	/** Hanya untuk hoe: klik kanan memanen tanaman matang area 3x3 dan menanam ulang otomatis. */
	public static final RegistryKey<Enchantment> MORE_HARVEST = key("more_harvest");

	/** Hanya untuk helmet: pemakai bisa bernapas di dalam air tanpa batas waktu (tanpa status effect/ikon). */
	public static final RegistryKey<Enchantment> WATER_BREATHING = key("water_breathing");

	/** Hanya untuk chestplate: berenang secepat Dolphin's Grace (tanpa status effect/ikon). */
	public static final RegistryKey<Enchantment> SWIM_FAST = key("swim_fast");

	/** Untuk semua armor: pemakai kebal api dan lava (tanpa status effect/ikon). */
	public static final RegistryKey<Enchantment> FIRE_RESISTANT = key("fire_resistant");

	/** Hanya untuk helmet: pemakai melihat terang di tempat gelap (sisi client, tanpa status effect Night Vision). */
	public static final RegistryKey<Enchantment> NIGHT_VISION = key("night_vision");

	/** Hanya untuk sword dan axe: membunuh makhluk memulihkan health pemain secara acak 1-5 hati. */
	public static final RegistryKey<Enchantment> LIFESTEAL = key("lifesteal");

	/** Hanya untuk leggings: exhaustion (lari, lompat, dll) berkurang sehingga hunger bar awet. */
	public static final RegistryKey<Enchantment> HUNGER_SAVER = key("hunger_saver");

	/** Hanya untuk pickaxe: hasil tambang ore otomatis menjadi ingot (tanpa furnace). */
	public static final RegistryKey<Enchantment> AUTO_SMELT = key("auto_smelt");

	/** Hanya untuk boots: satu lompatan tambahan di udara setelah melompat dari tanah (logika di sisi client). */
	public static final RegistryKey<Enchantment> DOUBLE_JUMP = key("double_jump");

	/** Pengali exhaustion untuk Hunger Saver (0.5 = konsumsi hunger berkurang separuh). */
	public static final float HUNGER_SAVER_FACTOR = 0.5F;

	/**
	 * Untuk item/block TANPA durability: jumlahnya tidak pernah berkurang saat dipakai (ditaruh, dimakan, dilempar, dll).
	 * Item berdurability tidak bisa diberi enchantment ini; aturannya ada di {@link com.workshoputilities.mixin.EnchantmentMixin}.
	 */
	public static final RegistryKey<Enchantment> UNLIMITED = key("unlimited");

	/** Kunci terjemahan nama enchantment Unlimited; dipakai mixin untuk mengenalinya. */
	public static final String UNLIMITED_TRANSLATION_KEY = "enchantment.workshop_utilities.unlimited";

	/**
	 * Pilihan yang tampil di Upgrade Enchanting Table (urutannya = urutan baris di GUI; lebih dari 3 pilihan bisa di-scroll).
	 * Untuk menambah enchanted book baru: buat JSON enchantment-nya, buat key-nya di atas, lalu tambahkan ke list ini.
	 */
	public static final List<RegistryKey<Enchantment>> UPGRADE_OPTIONS = List.of(INFINITE_DURABILITY, ENDER_BLIND, SNOW_WALKER, FROZEN_WARD, SILENT_STEP, PIGLIN_PROTECTION,
			VEIN_MINER, THUNDER_SLASH, ANTI_GRAVITY, WOODCUTTER, MORE_HARVEST,
			WATER_BREATHING, SWIM_FAST, FIRE_RESISTANT, UNLIMITED,
			NIGHT_VISION, LIFESTEAL, HUNGER_SAVER, AUTO_SMELT, DOUBLE_JUMP);

	private ModEnchantments() {
	}

	private static RegistryKey<Enchantment> key(String name) {
		return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(WorkshopUtilitiesMod.MOD_ID, name));
	}

	/** Membuat enchanted book yang menyimpan satu enchantment level 1. */
	public static ItemStack createBook(RegistryEntry<Enchantment> enchantment) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
		builder.add(enchantment, 1);
		book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
		return book;
	}

	/**
	 * True bila kumpulan enchantment ini berisi enchantment mod DAN enchantment vanilla (campuran).
	 * Dipakai Dis-Enchanting Table untuk memutuskan apakah hasilnya Fused Enchanted Book.
	 */
	public static boolean isMixed(ItemEnchantmentsComponent enchantments) {
		boolean mod = false;
		boolean vanilla = false;
		for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
			if (isModEnchantment(entry)) {
				mod = true;
			} else {
				vanilla = true;
			}
		}
		return mod && vanilla;
	}

	/**
	 * Membuat book hasil Dis-Enchanting Table. Campuran vanilla + mod menjadi {@link ModItems#FUSED_ENCHANTED_BOOK};
	 * selain itu tetap enchanted book biasa (semua vanilla, atau semua buatan mod).
	 */
	public static ItemStack createDisenchantBook(ItemEnchantmentsComponent enchantments) {
		ItemStack book = new ItemStack(isMixed(enchantments) ? ModItems.FUSED_ENCHANTED_BOOK : Items.ENCHANTED_BOOK);
		book.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments);
		return book;
	}

	/** True bila item ini ter-enchant Infinite Durability (enchantment di item, bukan yang tersimpan di dalam book). */
	public static boolean hasInfiniteDurability(ItemStack stack) {
		return hasEnchantment(stack, INFINITE_DURABILITY);
	}

	/** True bila item ini (helmet) ter-enchant Ender Blind. */
	public static boolean hasEnderBlind(ItemStack stack) {
		return hasEnchantment(stack, ENDER_BLIND);
	}

	/** True bila item ini (boots) ter-enchant Snow Walker. */
	public static boolean hasSnowWalker(ItemStack stack) {
		return hasEnchantment(stack, SNOW_WALKER);
	}

	/** True bila item ini (armor) ter-enchant Frozen Ward. */
	public static boolean hasFrozenWard(ItemStack stack) {
		return hasEnchantment(stack, FROZEN_WARD);
	}

	/** True bila item ini (leggings) ter-enchant Silent Step. */
	public static boolean hasSilentStep(ItemStack stack) {
		return hasEnchantment(stack, SILENT_STEP);
	}

	/** True bila item ini (armor) ter-enchant Piglin Protection. */
	public static boolean hasPiglinProtection(ItemStack stack) {
		return hasEnchantment(stack, PIGLIN_PROTECTION);
	}

	/** True bila item ini (pickaxe) ter-enchant Vein Miner. */
	public static boolean hasVeinMiner(ItemStack stack) {
		return hasEnchantment(stack, VEIN_MINER);
	}

	/** True bila item ini (sword) ter-enchant Thunder Slash. */
	public static boolean hasThunderSlash(ItemStack stack) {
		return hasEnchantment(stack, THUNDER_SLASH);
	}

	/** True bila item ini (shovel) ter-enchant Anti-Gravity. */
	public static boolean hasAntiGravity(ItemStack stack) {
		return hasEnchantment(stack, ANTI_GRAVITY);
	}

	/** True bila item ini (axe) ter-enchant Woodcutter. */
	public static boolean hasWoodcutter(ItemStack stack) {
		return hasEnchantment(stack, WOODCUTTER);
	}

	/** True bila item ini (hoe) ter-enchant More Harvest. */
	public static boolean hasMoreHarvest(ItemStack stack) {
		return hasEnchantment(stack, MORE_HARVEST);
	}

	/** True bila item ini ter-enchant Unlimited. */
	public static boolean hasUnlimited(ItemStack stack) {
		return hasEnchantment(stack, UNLIMITED);
	}

	/** True bila item ini (helmet) ter-enchant Water Breathing. */
	public static boolean hasWaterBreathing(ItemStack stack) {
		return hasEnchantment(stack, WATER_BREATHING);
	}

	/** True bila item ini (chestplate) ter-enchant Swim Fast. */
	public static boolean hasSwimFast(ItemStack stack) {
		return hasEnchantment(stack, SWIM_FAST);
	}

	/** True bila item ini (armor) ter-enchant Fire Resistant. */
	public static boolean hasFireResistant(ItemStack stack) {
		return hasEnchantment(stack, FIRE_RESISTANT);
	}

	/** True bila item ini (helmet) ter-enchant Night Vision. */
	public static boolean hasNightVision(ItemStack stack) {
		return hasEnchantment(stack, NIGHT_VISION);
	}

	/** True bila item ini (sword/axe) ter-enchant Lifesteal. */
	public static boolean hasLifesteal(ItemStack stack) {
		return hasEnchantment(stack, LIFESTEAL);
	}

	/** True bila item ini (leggings) ter-enchant Hunger Saver. */
	public static boolean hasHungerSaver(ItemStack stack) {
		return hasEnchantment(stack, HUNGER_SAVER);
	}

	/** True bila item ini (pickaxe) ter-enchant Auto Smelt. */
	public static boolean hasAutoSmelt(ItemStack stack) {
		return hasEnchantment(stack, AUTO_SMELT);
	}

	/** True bila item ini (boots) ter-enchant Double Jump. */
	public static boolean hasDoubleJump(ItemStack stack) {
		return hasEnchantment(stack, DOUBLE_JUMP);
	}

	/** True bila salah satu armor yang dipakai (helmet, chestplate, leggings, atau boots) ber-Fire Resistant. */
	public static boolean wearsFireResistant(LivingEntity entity) {
		for (EquipmentSlot slot : new EquipmentSlot[] {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (hasFireResistant(entity.getEquippedStack(slot))) {
				return true;
			}
		}
		return false;
	}

	/** True bila salah satu armor (helmet, chestplate, leggings, atau boots) yang dipakai ber-Piglin Protection. */
	public static boolean wearsPiglinProtection(LivingEntity entity) {
		for (EquipmentSlot slot : new EquipmentSlot[] {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (hasPiglinProtection(entity.getEquippedStack(slot))) {
				return true;
			}
		}
		return false;
	}

	/** True bila makhluk ini sedang memakai leggings ber-Silent Step (gerakan dan suaranya tidak terdeteksi). */
	public static boolean isSilentStepping(LivingEntity entity) {
		return hasSilentStep(entity.getEquippedStack(EquipmentSlot.LEGS));
	}

	/**
	 * Enchantment buatan mod ini yang tersimpan di dalam enchanted book (kosong bila bukan book atau tidak ada).
	 * Dipakai Upgrade Table untuk mengenali book mana yang boleh dipasang.
	 */
	public static List<RegistryEntry<Enchantment>> storedModEnchantments(ItemStack stack) {
		List<RegistryEntry<Enchantment>> result = new ArrayList<>();
		if (stack.isEmpty()) {
			return result;
		}
		ItemEnchantmentsComponent stored =
				stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		for (RegistryEntry<Enchantment> entry : stored.getEnchantments()) {
			if (isModEnchantment(entry)) {
				result.add(entry);
			}
		}
		return result;
	}

	/**
	 * True bila item di slot kanan anvil membawa enchantment buatan mod ini (tersimpan di book maupun terpasang di item)
	 * yang belum dimiliki item di slot kiri. Enchantment buatan mod hanya boleh dipasang lewat Upgrade Table,
	 * jadi anvil mengosongkan hasilnya ("X"), termasuk di mode Creative.
	 */
	public static boolean wouldTransferModEnchantment(ItemStack left, ItemStack right) {
		if (right.isEmpty()) {
			return false;
		}
		for (ItemEnchantmentsComponent component : new ItemEnchantmentsComponent[] {
				right.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT),
				right.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)}) {
			for (RegistryEntry<Enchantment> entry : component.getEnchantments()) {
				if (isModEnchantment(entry) && !holdsEnchantment(left, entry)) {
					return true;
				}
			}
		}
		return false;
	}

	/** True bila item ini sudah memiliki enchantment tersebut (terpasang di item atau tersimpan di book). */
	public static boolean holdsEnchantment(ItemStack stack, RegistryEntry<Enchantment> entry) {
		if (stack.isEmpty() || entry.getKey().isEmpty()) {
			return false;
		}
		RegistryKey<Enchantment> key = entry.getKey().get();
		for (ItemEnchantmentsComponent component : new ItemEnchantmentsComponent[] {
				stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT),
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)}) {
			for (RegistryEntry<Enchantment> held : component.getEnchantments()) {
				if (held.matchesKey(key)) {
					return true;
				}
			}
		}
		return false;
	}

	/** True bila enchantment ini buatan mod (terdaftar di {@link #UPGRADE_OPTIONS}). */
	public static boolean isModEnchantment(RegistryEntry<Enchantment> entry) {
		for (RegistryKey<Enchantment> key : UPGRADE_OPTIONS) {
			if (entry.matchesKey(key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> key) {
		if (stack.isEmpty()) {
			return false;
		}
		ItemEnchantmentsComponent enchantments =
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
			if (entry.matchesKey(key)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Bila item ber-Infinite Durability dan sudah rusak sebagian/hampir habis, durability-nya dikembalikan penuh
	 * (seolah tidak pernah terkena damage). Mengembalikan true jika ada yang diperbaiki.
	 */
	public static boolean restoreIfInfinite(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageable() || stack.getDamage() <= 0) {
			return false;
		}
		if (!hasInfiniteDurability(stack)) {
			return false;
		}
		stack.setDamage(0);
		return true;
	}
}