package com.workshoputilities.client.compat.rei;

import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModItems;
import com.workshoputilities.client.BackpackScreen;
import com.workshoputilities.client.UncraftingScreen;
import com.workshoputilities.client.DisenchantingScreen;
import com.workshoputilities.client.UpgradeEnchantingScreen;
import com.workshoputilities.client.UpgradeTableScreen;
import com.workshoputilities.client.compat.ArcaneCompat;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integrasi Roughly Enough Items (opsional). Didaftarkan lewat entrypoint {@code rei_client} di fabric.mod.json,
 * jadi class ini hanya dimuat bila REI terpasang. Semua data berasal dari {@link ArcaneCompat}.
 *
 * Yang ditambahkan ke REI:
 * <ul>
 * <li>Kategori Uncrafting, Dis-Enchanting, Upgrade Table (Enchant dan Backpack), Upgrade Enchanting Table, dan halaman Guide;</li>
 * <li>Block/item workstation (tekan R/U pada block untuk melihat kegunaannya);</li>
 * <li>Area klik di GUI (arahkan kursor ke area panah lalu klik untuk membuka resep kategori tersebut).</li>
 * </ul>
 */
public class ArcaneReiPlugin implements REIClientPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger("workshop_utilities/rei");

	public static final CategoryIdentifier<ArcaneDisplay> UNCRAFTING = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "uncrafting");
	public static final CategoryIdentifier<ArcaneDisplay> DISENCHANTING = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "disenchanting");
	public static final CategoryIdentifier<ArcaneDisplay> UPGRADE_ENCHANT = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "upgrade_enchant");
	public static final CategoryIdentifier<ArcaneDisplay> UPGRADE_BACKPACK = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "upgrade_backpack");
	public static final CategoryIdentifier<ArcaneDisplay> UPGRADE_BOOK = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "upgrade_book");
	public static final CategoryIdentifier<ArcaneDisplay> INFO = CategoryIdentifier.of(ArcaneCompat.MOD_ID, "info");

	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.add(new ReiCategories.Uncrafting(ModBlocks.UNCRAFTING_TABLE));
		registry.add(new ReiCategories.Disenchanting(ModBlocks.DISENCHANTING_TABLE));
		registry.add(new ReiCategories.Upgrade(UPGRADE_ENCHANT, "category.workshop_utilities.upgrade_enchant", ModBlocks.UPGRADE_TABLE));
		registry.add(new ReiCategories.Upgrade(UPGRADE_BACKPACK, "category.workshop_utilities.upgrade_backpack", ModBlocks.UPGRADE_TABLE));
		registry.add(new ReiCategories.UpgradeBook(ModBlocks.UPGRADE_ENCHANTING_TABLE));
		registry.add(new ReiCategories.Info(ModItems.UPGRADE_BOOK));

		// Block yang ditekan R/U akan menampilkan kategori ini (workstation).
		registry.addWorkstations(UNCRAFTING, EntryStacks.of(ModBlocks.UNCRAFTING_TABLE), EntryStacks.of(ModItems.UNCRAFTING_MODULE));
		registry.addWorkstations(DISENCHANTING, EntryStacks.of(ModBlocks.DISENCHANTING_TABLE));
		registry.addWorkstations(UPGRADE_ENCHANT, EntryStacks.of(ModBlocks.UPGRADE_TABLE));
		registry.addWorkstations(UPGRADE_BACKPACK, EntryStacks.of(ModBlocks.UPGRADE_TABLE));
		registry.addWorkstations(UPGRADE_BOOK, EntryStacks.of(ModBlocks.UPGRADE_ENCHANTING_TABLE));
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		try {
			for (ArcaneCompat.UncraftEntry entry : ArcaneCompat.uncraftEntries()) {
				List<EntryIngredient> inputs = List.of(EntryIngredients.of(entry.input()));
				List<EntryIngredient> outputs = new ArrayList<>();
				for (List<ItemStack> cell : entry.grid()) {
					if (!cell.isEmpty()) {
						outputs.add(EntryIngredients.ofItemStacks(cell));
					}
				}
				registry.add(new ArcaneDisplay(UNCRAFTING, inputs, outputs, entry));
			}

			for (ArcaneCompat.DisenchantEntry entry : ArcaneCompat.disenchantEntries()) {
				List<EntryIngredient> inputs = List.of(EntryIngredients.of(entry.tool()));
				List<EntryIngredient> outputs = List.of(EntryIngredients.of(entry.plain()), EntryIngredients.of(entry.book()));
				registry.add(new ArcaneDisplay(DISENCHANTING, inputs, outputs, entry));
			}

			for (ArcaneCompat.UpgradeEntry entry : ArcaneCompat.enchantUpgradeEntries()) {
				registry.add(upgradeDisplay(UPGRADE_ENCHANT, entry));
			}
			for (ArcaneCompat.UpgradeEntry entry : ArcaneCompat.backpackUpgradeEntries()) {
				registry.add(upgradeDisplay(UPGRADE_BACKPACK, entry));
			}

			for (ArcaneCompat.UpgradeBookEntry entry : ArcaneCompat.upgradeBookEntries()) {
				List<EntryIngredient> inputs = List.of(EntryIngredients.of(entry.book()), EntryIngredients.of(entry.catalyst()));
				List<EntryIngredient> outputs = List.of(EntryIngredients.of(entry.result()));
				registry.add(new ArcaneDisplay(UPGRADE_BOOK, inputs, outputs, entry));
			}

			for (ArcaneCompat.InfoEntry entry : ArcaneCompat.infoEntries()) {
				EntryIngredient subjects = EntryIngredients.ofItemStacks(entry.subjects());
				for (ReiCategories.InfoPage page : ReiCategories.paginate(entry)) {
					registry.add(new ArcaneDisplay(INFO, List.of(subjects), List.of(subjects), page));
				}
			}
		} catch (RuntimeException e) {
			// Kegagalan plugin kita tidak boleh sampai merusak REI atau game.
			LOGGER.error("Gagal mendaftarkan display Workshop Utilities ke REI", e);
		}
	}

	private static ArcaneDisplay upgradeDisplay(CategoryIdentifier<ArcaneDisplay> category, ArcaneCompat.UpgradeEntry entry) {
		List<EntryIngredient> inputs = List.of(
				EntryIngredients.ofItemStacks(entry.targets()),
				EntryIngredients.of(entry.material()),
				EntryIngredients.of(entry.tool()));
		List<EntryIngredient> outputs = List.of(EntryIngredients.ofItemStacks(entry.results()));
		return new ArcaneDisplay(category, inputs, outputs, entry);
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		// Koordinat relatif terhadap pojok kiri atas GUI. Area panah dipilih agar tidak menimpa slot, tombol, atau pilihan enchant.
		registry.registerContainerClickArea(new Rectangle(48, 34, 44, 20), UncraftingScreen.class, UNCRAFTING);
		registry.registerContainerClickArea(new Rectangle(62, 30, 52, 18), DisenchantingScreen.class, DISENCHANTING);
		registry.registerContainerClickArea(new Rectangle(101, 37, 22, 15), UpgradeTableScreen.class, UPGRADE_ENCHANT, UPGRADE_BACKPACK);
		registry.registerContainerClickArea(new Rectangle(140, 45, 10, 16), UpgradeEnchantingScreen.class, UPGRADE_BOOK);

		// Dock modul Backpack ada di luar panel utama, jadi daftarkan sebagai exclusion zone agar daftar item REI tidak menimpanya.
		registry.exclusionZones().register(BackpackScreen.class, screen -> {
			List<Rectangle> zones = new ArrayList<>();
			for (int[] area : ((BackpackScreen) screen).extraAreas()) {
				zones.add(new Rectangle(area[0], area[1], area[2], area[3]));
			}
			return zones;
		});
	}
}
