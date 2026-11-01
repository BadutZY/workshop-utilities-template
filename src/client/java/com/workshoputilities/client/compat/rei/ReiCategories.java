package com.workshoputilities.client.compat.rei;

import com.workshoputilities.client.compat.ArcaneCompat;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Semua kategori REI milik Workshop Utilities. */
final class ReiCategories {
	/** Tinggi semua kategori (REI memakai ini untuk menyusun halaman recipe). */
	private static final int HEIGHT = 72;
	/** Isi (54 tinggi) dipusatkan secara vertikal: 9 di atas dan 9 di bawah. */
	private static final int TOP = 9;

	// Halaman Guide: ukuran tetap, teks sudah dibungkus dan dibagi per halaman sebelum didaftarkan, jadi tidak mungkin keluar kotak.
	static final int INFO_WIDTH = 180;
	static final int INFO_HEIGHT = 150;
	private static final int INFO_PAD = 10;
	private static final int INFO_TEXT_TOP = 32;
	private static final int INFO_LINE = 10;
	private static final int INFO_GAP = 4;
	/** Tinggi area teks: dari INFO_TEXT_TOP sampai 24 di atas tepi bawah (sisa untuk penunjuk halaman). */
	private static final int INFO_BUDGET = INFO_HEIGHT - INFO_TEXT_TOP - 24;

	/** Satu halaman Guide. Elemen null di {@code lines} = jarak antar paragraf. */
	record InfoPage(List<ItemStack> subjects, List<Text> lines, int page, int pages) {
	}

	/** Membungkus teks Guide sesuai lebar kotak lalu membaginya menjadi halaman-halaman yang pas. */
	static List<InfoPage> paginate(ArcaneCompat.InfoEntry entry) {
		TextHandler handler = MinecraftClient.getInstance().textRenderer.getTextHandler();
		int width = INFO_WIDTH - INFO_PAD * 2;
		List<List<Text>> pages = new ArrayList<>();
		List<Text> current = new ArrayList<>();
		int used = 0;
		for (Text paragraph : entry.lines()) {
			for (StringVisitable part : handler.wrapLines(paragraph, width, Style.EMPTY)) {
				if (used + INFO_LINE > INFO_BUDGET) {
					pages.add(current);
					current = new ArrayList<>();
					used = 0;
				}
				current.add(Text.literal(part.getString()));
				used += INFO_LINE;
			}
			if (used + INFO_GAP + INFO_LINE <= INFO_BUDGET) {
				current.add(null);
				used += INFO_GAP;
			}
		}
		pages.add(current);
		List<InfoPage> result = new ArrayList<>();
		for (int i = 0; i < pages.size(); i++) {
			List<Text> lines = pages.get(i);
			while (!lines.isEmpty() && lines.get(lines.size() - 1) == null) {
				lines.remove(lines.size() - 1);
			}
			if (!lines.isEmpty()) {
				result.add(new InfoPage(entry.subjects(), lines, 0, 0));
			}
		}
		List<InfoPage> numbered = new ArrayList<>();
		for (int i = 0; i < result.size(); i++) {
			InfoPage page = result.get(i);
			numbered.add(new InfoPage(page.subjects(), page.lines(), i + 1, result.size()));
		}
		return numbered;
	}

	private ReiCategories() {
	}

	private static Widget label(int centerX, int y, Text text) {
		return Widgets.createLabel(new Point(centerX, y), text).noShadow().color(0xFF404040, 0xFFBBBBBB);
	}

	/** Dasar semua kategori: id, judul, dan ikon (item block/item workstation-nya). */
	abstract static class Base implements DisplayCategory<ArcaneDisplay> {
		private final CategoryIdentifier<ArcaneDisplay> id;
		private final String titleKey;
		private final ItemConvertible icon;

		Base(CategoryIdentifier<ArcaneDisplay> id, String titleKey, ItemConvertible icon) {
			this.id = id;
			this.titleKey = titleKey;
			this.icon = icon;
		}

		@Override
		public CategoryIdentifier<? extends ArcaneDisplay> getCategoryIdentifier() {
			return this.id;
		}

		@Override
		public Text getTitle() {
			return Text.translatable(this.titleKey);
		}

		@Override
		public Renderer getIcon() {
			return EntryStacks.of(this.icon);
		}

		public int getDisplayHeight() {
			return HEIGHT;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Uncrafting Table: item di kiri, grid 3x3 bahan di kanan. */
	static final class Uncrafting extends Base {
		Uncrafting(ItemConvertible icon) {
			super(ArcaneReiPlugin.UNCRAFTING, "category.workshop_utilities.uncrafting", icon);
		}

		@Override
		public List<Widget> setupDisplay(ArcaneDisplay display, Rectangle bounds) {
			ArcaneCompat.UncraftEntry entry = (ArcaneCompat.UncraftEntry) display.data();
			int x = bounds.getCenterX() - 57;
			int y = bounds.getY() + TOP;

			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			widgets.add(Widgets.createSlot(new Point(x, y + 18)).entries(EntryIngredients.of(entry.input())).markInput());
			widgets.add(Widgets.createArrow(new Point(x + 28, y + 19)));
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					List<ItemStack> cell = entry.grid().get(row * 3 + col);
					var slot = Widgets.createSlot(new Point(x + 60 + col * 18, y + row * 18)).markOutput();
					if (!cell.isEmpty()) {
						slot.entries(EntryIngredients.ofItemStacks(cell));
					}
					widgets.add(slot);
				}
			}
			return widgets;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Dis-Enchanting Table: item ter-enchant (+ Book) menjadi item polos (+ enchanted book). */
	static final class Disenchanting extends Base {
		Disenchanting(ItemConvertible icon) {
			super(ArcaneReiPlugin.DISENCHANTING, "category.workshop_utilities.disenchanting", icon);
		}

		@Override
		public List<Widget> setupDisplay(ArcaneDisplay display, Rectangle bounds) {
			ArcaneCompat.DisenchantEntry entry = (ArcaneCompat.DisenchantEntry) display.data();
			int x = bounds.getCenterX() - 53;
			int y = bounds.getY() + TOP;

			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			widgets.add(Widgets.createSlot(new Point(x, y + 5)).entries(EntryIngredients.of(entry.tool())).markInput());
			widgets.add(Widgets.createSlot(new Point(x, y + 33)).entries(EntryIngredients.of(new ItemStack(Items.BOOK))).markInput());
			widgets.add(Widgets.createArrow(new Point(x + 41, y + 19)));
			widgets.add(Widgets.createResultSlotBackground(new Point(x + 79, y + 5)));
			widgets.add(Widgets.createSlot(new Point(x + 79, y + 5)).entries(EntryIngredients.of(entry.plain()))
					.disableBackground().markOutput());
			widgets.add(Widgets.createResultSlotBackground(new Point(x + 79, y + 33)));
			widgets.add(Widgets.createSlot(new Point(x + 79, y + 33)).entries(EntryIngredients.of(entry.book()))
					.disableBackground().markOutput());
			return widgets;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Upgrade Table (mode Enchant dan mode Backpack memakai tampilan yang sama): item + bahan + Upgrade Tool menjadi hasil. */
	static final class Upgrade extends Base {
		Upgrade(CategoryIdentifier<ArcaneDisplay> id, String titleKey, ItemConvertible icon) {
			super(id, titleKey, icon);
		}

		@Override
		public List<Widget> setupDisplay(ArcaneDisplay display, Rectangle bounds) {
			ArcaneCompat.UpgradeEntry entry = (ArcaneCompat.UpgradeEntry) display.data();
			int x = bounds.getCenterX() - 57;
			int y = bounds.getY() + TOP;

			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			widgets.add(Widgets.createSlot(new Point(x, y + 6)).entries(EntryIngredients.ofItemStacks(entry.targets())).markInput());
			widgets.add(Widgets.createSlot(new Point(x + 22, y + 6)).entries(EntryIngredients.of(entry.material())).markInput());
			widgets.add(Widgets.createSlot(new Point(x + 11, y + 32)).entries(EntryIngredients.of(entry.tool())).markInput());
			widgets.add(Widgets.createArrow(new Point(x + 54, y + 19)));
			widgets.add(Widgets.createResultSlotBackground(new Point(x + 88, y + 19)));
			widgets.add(Widgets.createSlot(new Point(x + 88, y + 19)).entries(EntryIngredients.ofItemStacks(entry.results()))
					.disableBackground().markOutput());

			return widgets;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Upgrade Enchanting Table: Upgrade Book + Upgrade Enchanted menjadi enchanted book pilihan. */
	static final class UpgradeBook extends Base {
		UpgradeBook(ItemConvertible icon) {
			super(ArcaneReiPlugin.UPGRADE_BOOK, "category.workshop_utilities.upgrade_book", icon);
		}

		@Override
		public List<Widget> setupDisplay(ArcaneDisplay display, Rectangle bounds) {
			ArcaneCompat.UpgradeBookEntry entry = (ArcaneCompat.UpgradeBookEntry) display.data();
			int x = bounds.getCenterX() - 46;
			int y = bounds.getY() + TOP;

			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			widgets.add(Widgets.createSlot(new Point(x, y + 5)).entries(EntryIngredients.of(entry.book())).markInput());
			widgets.add(Widgets.createSlot(new Point(x, y + 33)).entries(EntryIngredients.of(entry.catalyst())).markInput());
			widgets.add(Widgets.createArrow(new Point(x + 30, y + 19)));
			widgets.add(Widgets.createResultSlotBackground(new Point(x + 66, y + 19)));
			widgets.add(Widgets.createSlot(new Point(x + 66, y + 19)).entries(EntryIngredients.of(entry.result()))
					.disableBackground().markOutput());
			return widgets;
		}
	}

	// ------------------------------------------------------------------------------------------------------------

	/** Halaman Guide: ikon item di tengah atas, teks di bawahnya rata kiri dengan margin kiri-kanan sama. */
	static final class Info extends Base {
		Info(ItemConvertible icon) {
			super(ArcaneReiPlugin.INFO, "category.workshop_utilities.info", icon);
		}

		@Override
		public int getDisplayHeight() {
			return INFO_HEIGHT;
		}

		public int getDisplayWidth(ArcaneDisplay display) {
			return INFO_WIDTH;
		}

		@Override
		public List<Widget> setupDisplay(ArcaneDisplay display, Rectangle bounds) {
			InfoPage page = (InfoPage) display.data();
			int left = bounds.getCenterX() - (INFO_WIDTH - INFO_PAD * 2) / 2;

			List<Widget> widgets = new ArrayList<>();
			widgets.add(Widgets.createRecipeBase(bounds));
			widgets.add(Widgets.createSlot(new Point(bounds.getCenterX() - 8, bounds.getY() + 10))
					.entries(EntryIngredients.ofItemStacks(page.subjects())).markInput());

			int y = bounds.getY() + INFO_TEXT_TOP;
			for (Text line : page.lines()) {
				if (line == null) {
					y += INFO_GAP;
					continue;
				}
				widgets.add(Widgets.createLabel(new Point(left, y), line).leftAligned().noShadow().color(0xFF404040, 0xFFBBBBBB));
				y += INFO_LINE;
			}
			if (page.pages() > 1) {
				widgets.add(label(bounds.getCenterX(), bounds.getY() + INFO_HEIGHT - 18,
						Text.literal(page.page() + "/" + page.pages())));
			}
			return widgets;
		}
	}
}
