package com.workshoputilities.uncraft;

import com.workshoputilities.backpack.BackpackData;
import com.workshoputilities.backpack.BackpackModuleData;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mencari semua resep crafting (vanilla maupun dari mod lain) yang menghasilkan item tertentu,
 * lalu mengubahnya menjadi susunan bahan 3x3 (kebalikan dari crafting).
 *
 * Semua resep dibaca langsung dari RecipeManager, jadi resep mod lain otomatis ikut terbaca
 * selama mod tersebut mendaftarkan resepnya sebagai crafting shaped/shapeless biasa.
 */
public final class UncraftRecipes {
	/** Satu pilihan uncraft: jumlah item yang dikonsumsi dan 9 isi grid output. */
	public record Option(int resultCount, List<ItemStack> grid) {
	}

	/** Batas jumlah pilihan per item agar label "n/total" di GUI tetap muat (maks 3 digit). */
	private static final int MAX_OPTIONS = 999;

	private UncraftRecipes() {
	}

	public static List<Option> find(World world, ItemStack input) {
		if (input.isEmpty()) {
			return List.of();
		}
		// Tas yang masih berisi barang tidak boleh di-uncraft (isinya akan hilang); kosongkan dulu.
		if (input.getItem() instanceof BackpackItem
				&& (BackpackData.usedSlots(input) > 0 || BackpackModuleData.hasContents(input))) {
			return List.of();
		}

		List<RecipeEntry<CraftingRecipe>> entries =
				new ArrayList<>(world.getRecipeManager().listAllOfType(RecipeType.CRAFTING));
		entries.sort(Comparator.comparing((RecipeEntry<CraftingRecipe> e) -> e.id().toString()));

		Map<String, Option> unique = new LinkedHashMap<>();
		for (RecipeEntry<CraftingRecipe> entry : entries) {
			CraftingRecipe recipe = entry.value();
			// Resep "special" (firework, map clone, dye armor, dll) tidak punya bahan tetap, jadi dilewati.
			if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) {
				continue;
			}
			ItemStack result = recipe.getResult(world.getRegistryManager());
			if (result.isEmpty() || result.getItem() != input.getItem()) {
				continue;
			}
			if (input.getCount() < result.getCount()) {
				continue;
			}
			for (Option option : build(recipe, result, input.getItem())) {
				if (unique.size() >= MAX_OPTIONS) {
					break;
				}
				unique.putIfAbsent(key(option), option);
			}
		}
		return new ArrayList<>(unique.values());
	}

	/**
	 * Satu baris daftar resep uncraft untuk REI/JEI: item yang di-uncraft (lengkap dengan jumlahnya) dan isi grid 3x3
	 * (urutan baris demi baris). Tiap sel berisi semua kandidat bahan (kosong bila sel itu kosong), sehingga bahan
	 * berupa tag (planks, wool, dll) tampil bergantian di REI/JEI.
	 */
	public record Preview(ItemStack input, List<List<ItemStack>> grid) {
	}

	/**
	 * Daftar semua resep uncraft yang tersedia di dunia ini, satu entri per resep crafting. Aturan penyaringannya sama dengan
	 * {@link #find}: hanya resep shaped/shapeless maksimal 3x3, resep special dan resep yang menyisakan item dilewati.
	 * Hanya dipakai untuk tampilan (REI/JEI); logika uncrafting di GUI tidak berubah.
	 */
	public static List<Preview> listPreviews(World world) {
		List<RecipeEntry<CraftingRecipe>> entries =
				new ArrayList<>(world.getRecipeManager().listAllOfType(RecipeType.CRAFTING));
		entries.sort(Comparator.comparing((RecipeEntry<CraftingRecipe> e) -> e.id().toString()));

		Map<String, Preview> unique = new LinkedHashMap<>();
		for (RecipeEntry<CraftingRecipe> entry : entries) {
			CraftingRecipe recipe = entry.value();
			if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) {
				continue;
			}
			ItemStack result = recipe.getResult(world.getRegistryManager());
			if (result.isEmpty()) {
				continue;
			}
			Preview preview = buildPreview(recipe, result);
			if (preview != null) {
				unique.putIfAbsent(previewKey(preview), preview);
			}
		}
		return new ArrayList<>(unique.values());
	}

	private static Preview buildPreview(CraftingRecipe recipe, ItemStack result) {
		try {
			List<Ingredient> ingredients = recipe.getIngredients();
			boolean shaped = recipe instanceof ShapedRecipe;
			int width;
			int height;
			if (shaped) {
				ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
				width = shapedRecipe.getWidth();
				height = shapedRecipe.getHeight();
				if (width > 3 || height > 3 || ingredients.size() != width * height) {
					return null;
				}
			} else {
				width = ingredients.size();
				height = 1;
				if (width == 0 || width > 9) {
					return null;
				}
			}

			Item target = result.getItem();
			Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
			List<String> slotKeys = new ArrayList<>();
			for (Ingredient ingredient : ingredients) {
				if (ingredient.isEmpty()) {
					slotKeys.add(null);
					continue;
				}
				List<ItemStack> candidates = candidates(ingredient, target);
				if (candidates.isEmpty()) {
					return null;
				}
				String groupKey = groupKey(candidates);
				groups.putIfAbsent(groupKey, candidates);
				slotKeys.add(groupKey);
			}

			// Resep yang menyisakan item (mis. ember dari kue) dilewati, sama seperti di GUI Uncrafting Table.
			List<ItemStack> chosen = new ArrayList<>();
			for (String slotKey : slotKeys) {
				chosen.add(slotKey == null ? ItemStack.EMPTY : groups.get(slotKey).get(0).copy());
			}
			CraftingRecipeInput recipeInput = CraftingRecipeInput.create(width, height, chosen);
			for (ItemStack remainder : recipe.getRemainder(recipeInput)) {
				if (!remainder.isEmpty()) {
					return null;
				}
			}

			List<List<ItemStack>> grid = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				grid.add(List.of());
			}
			for (int i = 0; i < slotKeys.size(); i++) {
				String slotKey = slotKeys.get(i);
				if (slotKey == null) {
					continue;
				}
				int row = shaped ? i / width : i / 3;
				int col = shaped ? i % width : i % 3;
				List<ItemStack> cell = new ArrayList<>();
				for (ItemStack candidate : groups.get(slotKey)) {
					cell.add(candidate.copy());
				}
				grid.set(row * 3 + col, cell);
			}
			return new Preview(new ItemStack(target, result.getCount()), grid);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static String previewKey(Preview preview) {
		StringBuilder builder = new StringBuilder(Registries.ITEM.getId(preview.input().getItem()).toString())
				.append('x').append(preview.input().getCount());
		for (List<ItemStack> cell : preview.grid()) {
			builder.append('|').append(cell.isEmpty() ? "_" : groupKey(cell));
		}
		return builder.toString();
	}

	/**
	 * Membuat SEMUA kombinasi bahan dari satu resep. Bahan berupa tag/ingredient dengan banyak pilihan
	 * (planks, log, wool, batu, batubara, dll) diperluas ke setiap item anggotanya, sehingga misalnya
	 * crafting table bisa di-uncraft menjadi planks jenis apa pun. Slot yang memakai kelompok bahan
	 * yang sama diisi dengan item yang sama; kelompok bahan yang berbeda dikombinasikan (hasil kali).
	 */
	private static List<Option> build(CraftingRecipe recipe, ItemStack result, Item target) {
		try {
			List<Ingredient> ingredients = recipe.getIngredients();
			boolean shaped = recipe instanceof ShapedRecipe;
			int width;
			int height;
			if (shaped) {
				ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
				width = shapedRecipe.getWidth();
				height = shapedRecipe.getHeight();
				if (width > 3 || height > 3 || ingredients.size() != width * height) {
					return List.of();
				}
			} else {
				width = ingredients.size();
				height = 1;
				if (width == 0 || width > 9) {
					return List.of();
				}
			}

			// Kelompokkan bahan: tiap kelompok = himpunan item kandidat yang sama.
			Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
			List<String> slotKeys = new ArrayList<>();
			for (Ingredient ingredient : ingredients) {
				if (ingredient.isEmpty()) {
					slotKeys.add(null);
					continue;
				}
				List<ItemStack> candidates = candidates(ingredient, target);
				// Resep yang hanya bisa memakai hasilnya sendiri sebagai bahan dilewati (mencegah loop).
				if (candidates.isEmpty()) {
					return List.of();
				}
				String groupKey = groupKey(candidates);
				groups.putIfAbsent(groupKey, candidates);
				slotKeys.add(groupKey);
			}

			// Hasil kali semua kelompok bahan (kandidat terbaik lebih dulu).
			List<Map<String, ItemStack>> combos = new ArrayList<>();
			combos.add(new HashMap<>());
			for (Map.Entry<String, List<ItemStack>> group : groups.entrySet()) {
				List<Map<String, ItemStack>> next = new ArrayList<>();
				outer:
				for (Map<String, ItemStack> combo : combos) {
					for (ItemStack candidate : group.getValue()) {
						Map<String, ItemStack> copy = new HashMap<>(combo);
						copy.put(group.getKey(), candidate);
						next.add(copy);
						if (next.size() >= MAX_OPTIONS) {
							break outer;
						}
					}
				}
				combos = next;
			}

			List<Option> options = new ArrayList<>();
			for (Map<String, ItemStack> combo : combos) {
				List<ItemStack> chosen = new ArrayList<>();
				for (String slotKey : slotKeys) {
					chosen.add(slotKey == null ? ItemStack.EMPTY : combo.get(slotKey).copy());
				}

				// Resep yang menyisakan item (mis. ember dari kue) dilewati agar tidak bisa dipakai menduplikasi item.
				CraftingRecipeInput recipeInput = CraftingRecipeInput.create(width, height, chosen);
				boolean hasRemainder = false;
				for (ItemStack remainder : recipe.getRemainder(recipeInput)) {
					if (!remainder.isEmpty()) {
						hasRemainder = true;
						break;
					}
				}
				if (hasRemainder) {
					continue;
				}

				List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
				for (int i = 0; i < chosen.size(); i++) {
					int row = shaped ? i / width : i / 3;
					int col = shaped ? i % width : i % 3;
					grid.set(row * 3 + col, chosen.get(i));
				}
				options.add(new Option(result.getCount(), grid));
			}
			return options;
		} catch (RuntimeException e) {
			return List.of();
		}
	}

	/**
	 * Semua item yang cocok dengan sebuah bahan (tanpa duplikat dan tanpa item target itu sendiri).
	 * Item yang namanya paling mirip dengan item yang di-uncraft diletakkan paling depan, sehingga
	 * oak_stairs tetap menampilkan oak_planks lebih dulu, tetapi semua jenis kayu lain tetap tersedia.
	 */
	private static List<ItemStack> candidates(Ingredient ingredient, Item target) {
		Set<Item> seen = new HashSet<>();
		List<ItemStack> list = new ArrayList<>();
		for (ItemStack stack : ingredient.getMatchingStacks()) {
			if (stack.isEmpty() || stack.getItem() == target || !seen.add(stack.getItem())) {
				continue;
			}
			list.add(stack.copyWithCount(1));
		}
		String targetPath = Registries.ITEM.getId(target).getPath();
		// List.sort bersifat stabil: yang skornya sama tetap berurutan seperti di registry (oak lebih dulu).
		list.sort(Comparator.comparingInt((ItemStack st) -> -score(Registries.ITEM.getId(st.getItem()).getPath(), targetPath)));
		return list;
	}

	private static String groupKey(List<ItemStack> candidates) {
		return candidates.stream()
				.map(s -> Registries.ITEM.getId(s.getItem()).toString())
				.sorted()
				.collect(Collectors.joining("|"));
	}

	/** Skor kemiripan berdasarkan jumlah kata (dipisah underscore) yang sama, mis. oak_planks vs oak_stairs = 1. */
	private static int score(String candidatePath, String targetPath) {
		Set<String> targetWords = new HashSet<>(List.of(targetPath.split("_")));
		int score = 0;
		for (String word : candidatePath.split("_")) {
			if (targetWords.contains(word)) {
				score++;
			}
		}
		return score;
	}

	private static String key(Option option) {
		return option.grid().stream()
				.map(s -> s.isEmpty() ? "_" : Registries.ITEM.getId(s.getItem()).toString())
				.collect(Collectors.joining(",")) + "x" + option.resultCount();
	}
}