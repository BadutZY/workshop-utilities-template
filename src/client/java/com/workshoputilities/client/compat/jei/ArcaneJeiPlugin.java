package com.workshoputilities.client.compat.jei;

import com.workshoputilities.ModBlocks;
import com.workshoputilities.ModItems;
import com.workshoputilities.client.BackpackScreen;
import com.workshoputilities.client.DisenchantingScreen;
import com.workshoputilities.client.UncraftingScreen;
import com.workshoputilities.client.UpgradeEnchantingScreen;
import com.workshoputilities.client.UpgradeTableScreen;
import com.workshoputilities.client.compat.ArcaneCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integrasi Just Enough Items (opsional). Didaftarkan lewat entrypoint {@code jei_mod_plugin} di fabric.mod.json,
 * jadi class ini hanya dimuat bila JEI terpasang. Semua data berasal dari {@link ArcaneCompat}.
 *
 * Yang ditambahkan ke JEI:
 * <ul>
 * <li>Kategori Uncrafting, Dis-Enchanting, Upgrade Table (Enchant dan Backpack), dan Upgrade Enchanting Table;</li>
 * <li>Block catalyst (tekan R/U pada block untuk melihat kegunaannya);</li>
 * <li>Halaman penjelasan (tab "i") di tiap block, item, backpack, modul, dan enchanted book mod;</li>
 * <li>Area klik di GUI (arahkan kursor ke area panah lalu klik untuk membuka resep kategori tersebut).</li>
 * </ul>
 */
public class ArcaneJeiPlugin implements IModPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger("workshop_utilities/jei");

	static final RecipeType<ArcaneCompat.UncraftEntry> UNCRAFTING =
			RecipeType.create(ArcaneCompat.MOD_ID, "uncrafting", ArcaneCompat.UncraftEntry.class);
	static final RecipeType<ArcaneCompat.DisenchantEntry> DISENCHANTING =
			RecipeType.create(ArcaneCompat.MOD_ID, "disenchanting", ArcaneCompat.DisenchantEntry.class);
	static final RecipeType<ArcaneCompat.UpgradeEntry> UPGRADE_ENCHANT =
			RecipeType.create(ArcaneCompat.MOD_ID, "upgrade_enchant", ArcaneCompat.UpgradeEntry.class);
	static final RecipeType<ArcaneCompat.UpgradeEntry> UPGRADE_BACKPACK =
			RecipeType.create(ArcaneCompat.MOD_ID, "upgrade_backpack", ArcaneCompat.UpgradeEntry.class);
	static final RecipeType<ArcaneCompat.UpgradeBookEntry> UPGRADE_BOOK =
			RecipeType.create(ArcaneCompat.MOD_ID, "upgrade_book", ArcaneCompat.UpgradeBookEntry.class);

	@Override
	public Identifier getPluginUid() {
		return ArcaneCompat.id("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(
				new JeiCategories.Uncrafting(gui, ModBlocks.UNCRAFTING_TABLE),
				new JeiCategories.Disenchanting(gui, ModBlocks.DISENCHANTING_TABLE),
				new JeiCategories.Upgrade(UPGRADE_ENCHANT, "category.workshop_utilities.upgrade_enchant", gui, ModBlocks.UPGRADE_TABLE),
				new JeiCategories.Upgrade(UPGRADE_BACKPACK, "category.workshop_utilities.upgrade_backpack", gui, ModBlocks.UPGRADE_TABLE),
				new JeiCategories.UpgradeBook(gui, ModBlocks.UPGRADE_ENCHANTING_TABLE));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		try {
			registration.addRecipes(UNCRAFTING, ArcaneCompat.uncraftEntries());
			registration.addRecipes(DISENCHANTING, ArcaneCompat.disenchantEntries());
			registration.addRecipes(UPGRADE_ENCHANT, ArcaneCompat.enchantUpgradeEntries());
			registration.addRecipes(UPGRADE_BACKPACK, ArcaneCompat.backpackUpgradeEntries());
			registration.addRecipes(UPGRADE_BOOK, ArcaneCompat.upgradeBookEntries());

			// Halaman penjelasan JEI ("i"): tiap item diberi teksnya sendiri (JEI membungkus teks dan membuat halaman otomatis).
			for (ArcaneCompat.InfoEntry entry : ArcaneCompat.infoEntries()) {
				Text[] lines = entry.lines().toArray(new Text[0]);
				for (ItemStack subject : entry.subjects()) {
					registration.addIngredientInfo(subject, VanillaTypes.ITEM_STACK, lines);
				}
			}
		} catch (RuntimeException e) {
			// Kegagalan plugin kita tidak boleh sampai merusak JEI atau game.
			LOGGER.error("Gagal mendaftarkan recipe Workshop Utilities ke JEI", e);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.UNCRAFTING_TABLE), UNCRAFTING);
		registration.addRecipeCatalyst(new ItemStack(ModItems.UNCRAFTING_MODULE), UNCRAFTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.DISENCHANTING_TABLE), DISENCHANTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.UPGRADE_TABLE), UPGRADE_ENCHANT, UPGRADE_BACKPACK);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.UPGRADE_ENCHANTING_TABLE), UPGRADE_BOOK);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		// Koordinat relatif terhadap pojok kiri atas GUI. Area panah dipilih agar tidak menimpa slot, tombol, atau pilihan enchant.
		registration.addRecipeClickArea(UncraftingScreen.class, 48, 34, 44, 20, UNCRAFTING);
		registration.addRecipeClickArea(DisenchantingScreen.class, 62, 30, 52, 18, DISENCHANTING);
		registration.addRecipeClickArea(UpgradeTableScreen.class, 101, 37, 22, 15, UPGRADE_ENCHANT, UPGRADE_BACKPACK);
		registration.addRecipeClickArea(UpgradeEnchantingScreen.class, 140, 45, 10, 16, UPGRADE_BOOK);

		// Dock modul Backpack ada di luar panel utama; beri tahu JEI agar daftar itemnya tidak menimpa area ini.
		registration.addGuiContainerHandler(BackpackScreen.class, new IGuiContainerHandler<BackpackScreen>() {
			public List<Rect2i> getGuiExtraAreas(BackpackScreen screen) {
				List<Rect2i> areas = new ArrayList<>();
				for (int[] area : screen.extraAreas()) {
					areas.add(new Rect2i(area[0], area[1], area[2], area[3]));
				}
				return areas;
			}
		});
	}
}
