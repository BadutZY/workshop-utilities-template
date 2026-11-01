package com.workshoputilities.client.compat.jei;

import com.workshoputilities.client.compat.ArcaneCompat;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Semua kategori JEI milik Workshop Utilities. Latar slot dan panah digambar sendiri (hanya memakai API gambar Minecraft),
 * sehingga tampilannya tidak bergantung pada texture internal JEI yang bisa berubah antar versi.
 */
final class JeiCategories {
	/** Lebar semua kategori; isi dipusatkan di dalamnya. */
	static final int WIDTH = 140;

	private static final int SLOT_DARK = 0xFF373737;
	private static final int SLOT_LIGHT = 0xFFFFFFFF;
	private static final int SLOT_FILL = 0xFF8B8B8B;
	private static final int ARROW = 0xFF555555;

	private JeiCategories() {
	}

	/** Latar slot vanilla 18x18; (x, y) adalah posisi item 16x16 di dalam slot. */
	static void drawSlot(DrawContext context, int x, int y) {
		int left = x - 1;
		int top = y - 1;
		context.fill(left, top, left + 18, top + 18, SLOT_LIGHT);
		context.fill(left, top, left + 17, top + 17, SLOT_DARK);
		context.fill(left + 1, top + 1, left + 17, top + 17, SLOT_FILL);
	}

	/** Panah kanan 18x8 dengan ujung segitiga. */
	static void drawArrow(DrawContext context, int x, int y) {
		context.fill(x, y + 3, x + 14, y + 5, ARROW);
		for (int i = 0; i < 4; i++) {
			context.fill(x + 14 + i, y + i, x + 15 + i, y + 8 - i, ARROW);
		}
	}


	private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
		return builder.addSlot(role, x, y);
	}

	/** Dasar semua kategori: tipe, judul, ikon, dan ukuran. */
	abstract static class Base<T> implements IRecipeCategory<T> {
		private final RecipeType<T> type;
		private final Text title;
		private final IDrawable icon;
		private final int height;

		Base(RecipeType<T> type, String titleKey, IGuiHelper gui, ItemConvertible icon, int height) {
			this.type = type;
			this.title = Text.translatable(titleKey);
			this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(icon));
			this.height = height;
		}

		public RecipeType<T> getRecipeType() {
			return this.type;
		}

		public Text getTitle() {
			return this.title;
		}

		public IDrawable getIcon() {
			return this.icon;
		}

		public int getWidth() {
			return WIDTH;
		}

		public int getHeight() {
			return this.height;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Uncrafting Table: item di kiri, grid 3x3 bahan di kanan. */
	static final class Uncrafting extends Base<ArcaneCompat.UncraftEntry> {
		Uncrafting(IGuiHelper gui, ItemConvertible icon) {
			super(ArcaneJeiPlugin.UNCRAFTING, "category.workshop_utilities.uncrafting", gui, icon, 62);
		}

		public void setRecipe(IRecipeLayoutBuilder builder, ArcaneCompat.UncraftEntry entry, IFocusGroup focuses) {
			slot(builder, RecipeIngredientRole.INPUT, 12, 22).addItemStack(entry.input());
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					List<ItemStack> cell = entry.grid().get(row * 3 + col);
					if (!cell.isEmpty()) {
						slot(builder, RecipeIngredientRole.OUTPUT, 76 + col * 18, 4 + row * 18).addItemStacks(cell);
					}
				}
			}
		}

		public void draw(ArcaneCompat.UncraftEntry entry, IRecipeSlotsView slots, DrawContext context, double mouseX, double mouseY) {
			drawSlot(context, 12, 22);
			drawArrow(context, 43, 26);
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					drawSlot(context, 76 + col * 18, 4 + row * 18);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Dis-Enchanting Table: item ter-enchant (+ Book) menjadi item polos (+ enchanted book). */
	static final class Disenchanting extends Base<ArcaneCompat.DisenchantEntry> {
		Disenchanting(IGuiHelper gui, ItemConvertible icon) {
			super(ArcaneJeiPlugin.DISENCHANTING, "category.workshop_utilities.disenchanting", gui, icon, 62);
		}

		public void setRecipe(IRecipeLayoutBuilder builder, ArcaneCompat.DisenchantEntry entry, IFocusGroup focuses) {
			slot(builder, RecipeIngredientRole.INPUT, 20, 8).addItemStack(entry.tool());
			slot(builder, RecipeIngredientRole.INPUT, 20, 36).addItemStack(new ItemStack(Items.BOOK));
			slot(builder, RecipeIngredientRole.OUTPUT, 104, 8).addItemStack(entry.plain());
			slot(builder, RecipeIngredientRole.OUTPUT, 104, 36).addItemStack(entry.book());
		}

		public void draw(ArcaneCompat.DisenchantEntry entry, IRecipeSlotsView slots, DrawContext context, double mouseX, double mouseY) {
			drawSlot(context, 20, 8);
			drawSlot(context, 20, 36);
			drawArrow(context, 61, 26);
			drawSlot(context, 104, 8);
			drawSlot(context, 104, 36);
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Upgrade Table (mode Enchant dan Backpack): item + bahan + Upgrade Tool menjadi hasil. */
	static final class Upgrade extends Base<ArcaneCompat.UpgradeEntry> {
		Upgrade(RecipeType<ArcaneCompat.UpgradeEntry> type, String titleKey, IGuiHelper gui, ItemConvertible icon) {
			super(type, titleKey, gui, icon, 62);
		}

		public void setRecipe(IRecipeLayoutBuilder builder, ArcaneCompat.UpgradeEntry entry, IFocusGroup focuses) {
			slot(builder, RecipeIngredientRole.INPUT, 14, 9).addItemStacks(entry.targets());
			slot(builder, RecipeIngredientRole.INPUT, 36, 9).addItemStack(entry.material());
			slot(builder, RecipeIngredientRole.INPUT, 25, 35).addItemStack(entry.tool());
			slot(builder, RecipeIngredientRole.OUTPUT, 110, 22).addItemStacks(entry.results());
		}

		public void draw(ArcaneCompat.UpgradeEntry entry, IRecipeSlotsView slots, DrawContext context, double mouseX, double mouseY) {
			drawSlot(context, 14, 9);
			drawSlot(context, 36, 9);
			drawSlot(context, 25, 35);
			drawArrow(context, 72, 26);
			drawSlot(context, 110, 22);
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Upgrade Enchanting Table: Upgrade Book + Upgrade Enchanted menjadi enchanted book pilihan. */
	static final class UpgradeBook extends Base<ArcaneCompat.UpgradeBookEntry> {
		UpgradeBook(IGuiHelper gui, ItemConvertible icon) {
			super(ArcaneJeiPlugin.UPGRADE_BOOK, "category.workshop_utilities.upgrade_book", gui, icon, 62);
		}

		public void setRecipe(IRecipeLayoutBuilder builder, ArcaneCompat.UpgradeBookEntry entry, IFocusGroup focuses) {
			slot(builder, RecipeIngredientRole.INPUT, 30, 9).addItemStack(entry.book());
			slot(builder, RecipeIngredientRole.INPUT, 30, 35).addItemStack(entry.catalyst());
			slot(builder, RecipeIngredientRole.OUTPUT, 94, 22).addItemStack(entry.result());
		}

		public void draw(ArcaneCompat.UpgradeBookEntry entry, IRecipeSlotsView slots, DrawContext context, double mouseX, double mouseY) {
			drawSlot(context, 30, 9);
			drawSlot(context, 30, 35);
			drawArrow(context, 61, 26);
			drawSlot(context, 94, 22);
		}
	}
}
