package com.workshoputilities.client;

import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.screen.UpgradeEnchantingScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Layout (GUI 176x184, texture 256x256):
 *   slot input atas Upgrade Book (8,34) dan slot input bawah Upgrade Enchanted (8,56) -> panel pilihan (38,21) 100x64
 *   -> slot output (152,45)
 *   panel berada tepat di tengah antara input dan output (tengah GUI = x 88, tengah slot = y 53); kedua slot input
 *   ditumpuk simetris terhadap garis y = 53 yang sama dengan tengah slot output dan panah
 *   baris pilihan 94x19 (maksimal 3), sprite di texture: aktif (0,186), nonaktif (96,186), hover (0,206)
 *   indikator bookshelf kecil 44x12 di kanan, sebaris dengan tulisan "Inventory" (125,88)
 *   ikon hantu slot kosong (sama seperti Dis-Enchanting Table): Upgrade Book dan Upgrade Enchanted di slot input,
 *   enchanted book vanilla (tanpa glint) di output, semuanya dengan opacity dikurangi
 *   Teks nama selalu muat di dalam baris (otomatis dibungkus / diperkecil).
 *   Bila pilihan lebih banyak dari {@link #MAX_ROWS}, daftar bisa di-scroll dengan roda mouse (ada penunjuk scroll di tepi kanan panel).
 */
public class UpgradeEnchantingScreen extends PanelHandledScreen<UpgradeEnchantingScreenHandler> {
	private static final Identifier TEXTURE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/container/upgrade_enchanting_table.png");

	private static final ItemStack BOOKSHELF_ICON = new ItemStack(Items.BOOKSHELF);
	private static final ItemStack FALLBACK_OPTION_ICON = new ItemStack(Items.ENCHANTED_BOOK);

	/** Ikon tiap pilihan = enchanted book yang benar-benar berisi enchantment tersebut (texture khusus per enchantment). */
	private final Map<RegistryKey<Enchantment>, ItemStack> optionIcons = new HashMap<>();

	private static final int GUI_HEIGHT = 184;

	/** Panel pilihan (digambar di texture). */
	private static final int PANEL_X = 38;
	private static final int PANEL_Y = 21;
	private static final int PANEL_W = 100;
	private static final int PANEL_H = 64;

	/** Baris pilihan: maksimal 3 baris terlihat, tiap baris 94x19 dan selalu berada di tengah panel. */
	private static final int ROW_W = 94;
	private static final int ROW_H = 19;
	private static final int ROW_GAP = 1;
	private static final int MAX_ROWS = 3;
	private static final int ROW_X = PANEL_X + (PANEL_W - ROW_W) / 2;
	private static final int U_ENABLED = 0;
	private static final int V_ENABLED = 186;
	private static final int U_DISABLED = 96;
	private static final int V_DISABLED = 186;
	private static final int U_HOVER = 0;
	private static final int V_HOVER = 206;

	/** Ikon pilihan 16x16 di kiri baris, teks nama di kanan pemisah. */
	private static final int ICON_OFFSET_X = 3;
	private static final int ICON_OFFSET_Y = 1;
	private static final int TEXT_OFFSET_X = 27;
	private static final int TEXT_MAX_W = ROW_W - TEXT_OFFSET_X - 4;

	/** Indikator jumlah bookshelf: kotak kecil di kanan, sebaris dengan tulisan "Inventory". */
	private static final int COUNTER_X = 125;
	private static final int COUNTER_Y = 88;
	private static final int COUNTER_W = 44;
	private static final int COUNTER_H = 12;

	/** Indeks pilihan pertama yang terlihat (dipakai bila pilihan lebih banyak dari MAX_ROWS). */
	private int scroll = 0;

	public UpgradeEnchantingScreen(UpgradeEnchantingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundHeight = GUI_HEIGHT;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	private boolean hasBook() {
		return this.handler.getSlot(0).getStack().isOf(ModItems.UPGRADE_BOOK);
	}

	/** Slot Upgrade Enchanted (di bawah Upgrade Book) harus terisi supaya enchanted book bisa dibuat. */
	private boolean hasMaterial() {
		return this.handler.getSlot(UpgradeEnchantingScreenHandler.MATERIAL_SLOT).getStack().isOf(ModItems.UPGRADE_ENCHANTED);
	}

	private boolean outputFree() {
		return this.handler.getSlot(1).getStack().isEmpty();
	}

	private boolean enoughShelves() {
		return this.handler.getBookshelves() >= UpgradeEnchantingScreenHandler.REQUIRED_BOOKSHELVES;
	}

	/** Level XP yang dibutuhkan (0 di Creative). */
	private int xpCost() {
		if (this.client == null || this.client.player == null) {
			return UpgradeEnchantingScreenHandler.LEVEL_COST_PER_BOOK;
		}
		return UpgradeEnchantingScreenHandler.levelCostFor(this.client.player, this.handler.getSlot(0).getStack());
	}

	/** Butuh minimal LEVEL_COST_PER_BOOK level XP. Di Creative selalu cukup. */
	private boolean enoughXp() {
		if (this.client == null || this.client.player == null) {
			return false;
		}
		return this.client.player.experienceLevel >= xpCost();
	}

	/** Book dan Upgrade Enchanted ada serta slot output kosong: klik pilihan tetap dikirim ke server agar syarat yang kurang diberitahukan. */
	private boolean canAttempt() {
		return hasBook() && hasMaterial() && outputFree();
	}

	private boolean isUsable() {
		return hasBook() && hasMaterial() && enoughShelves() && enoughXp() && outputFree();
	}

	private int visibleRows() {
		return Math.min(ModEnchantments.UPGRADE_OPTIONS.size(), MAX_ROWS);
	}

	private int maxScroll() {
		return Math.max(0, ModEnchantments.UPGRADE_OPTIONS.size() - MAX_ROWS);
	}

	/** Posisi Y (relatif GUI) baris terlihat ke-i; seluruh baris dipusatkan secara vertikal di dalam panel. */
	private int rowTop(int visibleIndex) {
		int n = visibleRows();
		int contentH = n * ROW_H + Math.max(0, n - 1) * ROW_GAP;
		int top = PANEL_Y + 1 + ((PANEL_H - 2) - contentH) / 2;
		return top + visibleIndex * (ROW_H + ROW_GAP);
	}

	/** Indeks pilihan (di UPGRADE_OPTIONS) di bawah mouse, atau -1. */
	private int rowAt(double mouseX, double mouseY) {
		int rx = (int) Math.floor(mouseX) - this.x - ROW_X;
		int ry = (int) Math.floor(mouseY) - this.y;
		if (rx < 0 || rx >= ROW_W) {
			return -1;
		}
		for (int i = 0; i < visibleRows(); i++) {
			int top = rowTop(i);
			if (ry >= top && ry < top + ROW_H) {
				return this.scroll + i;
			}
		}
		return -1;
	}

	private boolean overPanel(double mouseX, double mouseY) {
		int rx = (int) Math.floor(mouseX) - this.x - PANEL_X;
		int ry = (int) Math.floor(mouseY) - this.y - PANEL_Y;
		return rx >= 0 && rx < PANEL_W && ry >= 0 && ry < PANEL_H;
	}

	private boolean overCounter(int mouseX, int mouseY) {
		int rx = mouseX - this.x - COUNTER_X;
		int ry = mouseY - this.y - COUNTER_Y;
		return rx >= 0 && rx < COUNTER_W && ry >= 0 && ry < COUNTER_H;
	}

	private ItemStack optionIcon(RegistryKey<Enchantment> key) {
		ItemStack cached = this.optionIcons.get(key);
		if (cached != null) {
			return cached;
		}
		ItemStack icon = FALLBACK_OPTION_ICON;
		if (this.client != null && this.client.world != null) {
			Optional<RegistryEntry.Reference<Enchantment>> entry =
					this.client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key);
			if (entry.isPresent()) {
				icon = ModEnchantments.createBook(entry.get());
			}
		}
		this.optionIcons.put(key, icon);
		return icon;
	}

	private static String optionKey(RegistryKey<Enchantment> key) {
		Identifier id = key.getValue();
		return id.getNamespace() + "." + id.getPath();
	}

	private static Text optionName(RegistryKey<Enchantment> key) {
		return Text.translatable("enchantment." + optionKey(key));
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		// Panel samping (backpack / equipment) digambar lebih dulu; texture GUI di bawah menutup sambungannya.
		drawPanels(context);
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

		// Ikon hantu pada slot kosong: Upgrade Book dan Upgrade Enchanted (input) serta enchanted book vanilla tanpa glint (output), dipudarkan.
		if (this.handler.getSlot(0).getStack().isEmpty()) {
			GhostSlotIcon.drawVanilla(context, GhostSlotIcon.UPGRADE_BOOK, this.x + UpgradeEnchantingScreenHandler.INPUT_X,
					this.y + UpgradeEnchantingScreenHandler.INPUT_Y);
		}
		if (this.handler.getSlot(UpgradeEnchantingScreenHandler.MATERIAL_SLOT).getStack().isEmpty()) {
			GhostSlotIcon.drawVanilla(context, GhostSlotIcon.UPGRADE_ENCHANTED,
					this.x + UpgradeEnchantingScreenHandler.MATERIAL_X, this.y + UpgradeEnchantingScreenHandler.MATERIAL_Y);
		}
		if (this.handler.getSlot(1).getStack().isEmpty()) {
			GhostSlotIcon.drawVanilla(context, GhostSlotIcon.ENCHANTED_BOOK,
					this.x + UpgradeEnchantingScreenHandler.OUTPUT_X, this.y + UpgradeEnchantingScreenHandler.OUTPUT_Y);
		}

		// Indikator bookshelf: ikon bookshelf kecil + "x/20" (hijau bila cukup, merah bila kurang).
		int shelves = this.handler.getBookshelves();
		context.getMatrices().push();
		context.getMatrices().translate(this.x + COUNTER_X + 3, this.y + COUNTER_Y + 2, 0.0F);
		context.getMatrices().scale(0.5F, 0.5F, 1.0F);
		context.drawItem(BOOKSHELF_ICON, 0, 0);
		context.getMatrices().pop();
		String label = shelves + "/" + UpgradeEnchantingScreenHandler.REQUIRED_BOOKSHELVES;
		context.drawText(this.textRenderer, label, this.x + COUNTER_X + 13, this.y + COUNTER_Y + 2,
				enoughShelves() ? 0x55FF55 : 0xFF5555, false);

		// Daftar pilihan enchanted book (besar, di tengah panel).
		boolean usable = isUsable();
		List<RegistryKey<Enchantment>> options = ModEnchantments.UPGRADE_OPTIONS;
		int hoverIndex = rowAt(mouseX, mouseY);
		for (int i = 0; i < visibleRows(); i++) {
			int index = this.scroll + i;
			RegistryKey<Enchantment> key = options.get(index);
			int rx = this.x + ROW_X;
			int ry = this.y + rowTop(i);
			boolean hover = usable && hoverIndex == index;
			int u = !usable ? U_DISABLED : (hover ? U_HOVER : U_ENABLED);
			int v = !usable ? V_DISABLED : (hover ? V_HOVER : V_ENABLED);
			context.drawTexture(TEXTURE, rx, ry, u, v, ROW_W, ROW_H);

			context.drawItem(optionIcon(key), rx + ICON_OFFSET_X, ry + ICON_OFFSET_Y);

			int color = !usable ? 0x7A7A7A : (hover ? 0xFFFFA0 : 0xE8E8E8);
			drawFittedName(context, optionName(key), rx + TEXT_OFFSET_X, ry, color);
		}
		drawScrollbar(context);
	}

	/** Penunjuk scroll tipis di tepi kanan panel; hanya tampil bila pilihan lebih banyak dari baris yang terlihat. */
	private void drawScrollbar(DrawContext context) {
		int max = maxScroll();
		if (max <= 0) {
			return;
		}
		int trackX = this.x + PANEL_X + PANEL_W - 4;
		int trackTop = this.y + PANEL_Y + 3;
		int trackH = PANEL_H - 6;
		int total = ModEnchantments.UPGRADE_OPTIONS.size();
		int thumbH = Math.max(6, trackH * MAX_ROWS / total);
		int thumbTop = trackTop + (trackH - thumbH) * this.scroll / max;
		context.fill(trackX, trackTop, trackX + 2, trackTop + trackH, 0xFF0C0E20);
		context.fill(trackX, thumbTop, trackX + 2, thumbTop + thumbH, 0xFFB4C0F0);
	}

	/**
	 * Menggambar nama pilihan agar selalu muat di dalam baris: dibungkus jadi maksimal 2 baris pada ukuran normal,
	 * bila tidak muat teks otomatis diperkecil (0.8x, lalu 0.65x dengan maksimal 3 baris).
	 */
	private void drawFittedName(DrawContext context, Text name, int textX, int rowY, int color) {
		float[] scales = {1.0F, 0.8F, 0.65F};
		float scale = scales[scales.length - 1];
		List<OrderedText> lines = this.textRenderer.wrapLines(name, Math.round(TEXT_MAX_W / scale));
		for (float candidate : scales) {
			List<OrderedText> wrapped = this.textRenderer.wrapLines(name, Math.round(TEXT_MAX_W / candidate));
			if ((wrapped.size() * 9 - 1) * candidate <= ROW_H - 2) {
				scale = candidate;
				lines = wrapped;
				break;
			}
		}
		int n = 0;
		while (n < lines.size() && ((n + 1) * 9 - 1) * scale <= ROW_H - 2) {
			n++;
		}
		n = Math.max(1, n);
		float height = (n * 9 - 1) * scale;
		float top = rowY + (ROW_H - height) / 2.0F;
		context.getMatrices().push();
		context.getMatrices().translate(textX, top, 0.0F);
		context.getMatrices().scale(scale, scale, 1.0F);
		for (int j = 0; j < n; j++) {
			context.drawText(this.textRenderer, lines.get(j), 0, j * 9, color, true);
		}
		context.getMatrices().pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && canAttempt()) {
			int row = rowAt(mouseX, mouseY);
			if (row >= 0 && this.client != null && this.client.interactionManager != null) {
				this.client.interactionManager.clickButton(this.handler.syncId, row);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int max = maxScroll();
		if (max > 0 && overPanel(mouseX, mouseY)) {
			int next = this.scroll - (int) Math.signum(verticalAmount);
			this.scroll = Math.max(0, Math.min(max, next));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);

		int shelves = this.handler.getBookshelves();
		int required = UpgradeEnchantingScreenHandler.REQUIRED_BOOKSHELVES;
		int row = rowAt(mouseX, mouseY);
		if (row >= 0) {
			RegistryKey<Enchantment> key = ModEnchantments.UPGRADE_OPTIONS.get(row);
			// Hover menampilkan nama book; tahan SHIFT untuk deskripsi fungsi dan item yang bisa dipasangi
			// (teks yang sama dengan tooltip item di inventory).
			// Bila syarat belum terpenuhi, alasannya (XP / bookshelf kurang) ditampilkan dengan warna merah.
			List<Text> tip = new ArrayList<>();
			tip.add(optionName(key).copy().formatted(Formatting.AQUA));
			WorkshopUtilitiesClient.addBookTooltip(tip, key.getValue().getPath());
			if (hasBook() && !hasMaterial()) {
				tip.add(Text.translatable("message.workshop_utilities.need_upgrade_enchanted").formatted(Formatting.RED));
			}
			if (hasBook()) {
				if (!enoughXp()) {
					int have = this.client != null && this.client.player != null ? this.client.player.experienceLevel : 0;
					tip.add(Text.translatable("message.workshop_utilities.xp_not_enough", xpCost(), have)
							.formatted(Formatting.RED));
				}
				if (!enoughShelves()) {
					tip.add(Text.translatable("message.workshop_utilities.bookshelves_not_enough", shelves, required)
							.formatted(Formatting.RED));
				}
			}
			context.drawTooltip(this.textRenderer, tip, mouseX, mouseY);
		} else if (overCounter(mouseX, mouseY)) {
			List<Text> tip = new ArrayList<>();
			tip.add(Text.translatable("gui.workshop_utilities.bookshelves", shelves, required)
					.formatted(enoughShelves() ? Formatting.GREEN : Formatting.RED));
			tip.add(Text.translatable("gui.workshop_utilities.bookshelves.hint").formatted(Formatting.GRAY));
			context.drawTooltip(this.textRenderer, tip, mouseX, mouseY);
		}
	}
}