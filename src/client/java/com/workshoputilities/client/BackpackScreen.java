package com.workshoputilities.client;

import com.workshoputilities.ModItems;
import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.client.mixin.SlotAccessor;
import com.workshoputilities.screen.BackpackScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Backpack bergaya container vanilla: panel abu-abu 3px, slot 18x18, label "Inventory",
 * dan lima tombol kecil di kanan atas (taruh item serupa, urutkan tas, urutkan inventory, ambil semua, taruh semua).
 *
 * Di kanan GUI ada dock modul (5 slot: Crafting, Furnace, Blast Furnace, Smoker, Uncrafting). Tiap modul terpasang punya
 * tab panah; klik untuk membuka panelnya (grid crafting, furnace, atau uncrafting) di sebelah kanan dock. Tombol kecil di atas dock
 * menyembunyikan / menampilkan seluruh dock; statusnya diingat selama game berjalan.
 *
 * Di kiri GUI ada dock samping (1 slot untuk Bed Module) dengan tombol Sleep di bawahnya: tidur sampai pagi tanpa bed
 * dan tanpa mengubah spawn point. Dock ini ikut disembunyikan / ditampilkan oleh tombol dock di kanan.
 *
 * Layout ramping dan memanjang ke atas: tiap tier punya paling banyak 12 kolom (lihat BackpackTier), inventory pemain
 * berada di tengah di bawah grid tas. Bila tinggi layar tidak cukup untuk semua baris (mis. GUI Scale besar),
 * baris yang tampil dikurangi otomatis dan muncul scrollbar di kanan grid (roda mouse atau seret scrollbar);
 * slot di luar tampilan dipindah keluar layar sehingga tidak bisa diklik, tetapi tetap ikut Shift-click dan tombol.
 */
public class BackpackScreen extends HandledScreen<BackpackScreenHandler> {
	private static final Identifier WIDGETS =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/backpack_widgets.png");

	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int TRACK = 0xFF8B8B8B;
	private static final int TRACK_DARK = 0xFF373737;
	private static final int TEXT = 0x404040;

	private static final int BUTTON_SIZE = 12;
	private static final int BUTTON_GAP = 1;
	private static final String[] BUTTON_KEYS = {"quick_stack", "sort", "sort_inventory", "take_all", "deposit_all"};

	/** Tinggi panel tanpa baris tas (judul + inventory pemain), sama dengan rumus container vanilla. */
	private static final int BASE_HEIGHT = 114;
	private static final int MIN_VISIBLE_ROWS = 3;
	private static final int SCREEN_MARGIN = 12;
	private static final int SCROLLBAR_WIDTH = 8;
	private static final int SCROLLBAR_GUTTER = 12;
	private static final int OFFSCREEN = -10000;

	// ---- dock modul (koordinat relatif panel utama; W = lebar panel utama)
	private static final int TOGGLE_W = 12;
	private static final int TOGGLE_H = 16;
	private static final int TOGGLE_Y = 4;
	private static final int DOCK_Y = 22;
	private static final int DOCK_W = 34;
	private static final int DOCK_PAD = 4;
	private static final int CELL_H = 22;
	private static final int TAB_X = 24;
	private static final int TAB_W = 8;
	private static final int PANEL_W = 84;
	private static final int CRAFT_H = 112;
	private static final int FURNACE_H = 78;
	private static final int UNCRAFT_H = 122;
	private static final int[] MODULE_COLORS = {0xFF8B5A2B, 0xFFE0802C, 0xFFD2562A, 0xFFB5662B, 0xFF3FA7A0};
	private static final Text[] MODULE_TITLES = {Text.translatable("gui.workshop_utilities.backpack.module.title.crafting"),
			Text.translatable("block.minecraft.furnace"), Text.translatable("block.minecraft.blast_furnace"),
			Text.translatable("block.minecraft.smoker"), Text.translatable("gui.workshop_utilities.backpack.module.title.uncrafting")};
	private static final Item[] MODULE_ITEMS = {ModItems.CRAFTING_MODULE, ModItems.FURNACE_MODULE,
			ModItems.BLAST_FURNACE_MODULE, ModItems.SMOKER_MODULE, ModItems.UNCRAFTING_MODULE};

	/** Sprite tombol < > modul Uncrafting diambil dari texture GUI Uncrafting Table (u=176..211, v=0..23), sama persis dengan mejanya. */
	private static final Identifier UNCRAFT_TEXTURE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/container/uncrafting_table.png");
	private static final int UNCRAFT_BUTTON_Y = 36;
	private static final int UNCRAFT_PREV_X = 7;
	private static final int UNCRAFT_NEXT_X = 65;
	private static final int UNCRAFT_COUNTER_X = 21;
	private static final int UNCRAFT_COUNTER_W = 42;

	// ---- dock samping kiri (slot Bed Module + tombol Sleep)
	private static final int LEFT_W = 34;
	private static final int SLEEP_W = 22;
	private static final int SLEEP_H = 16;
	private static final int LEFT_H = DOCK_PAD + CELL_H + 2 + SLEEP_H + 4;
	private static final int BED_COLOR = 0xFF8C96E6;

	/** Status tampilan dock, diingat antar pembukaan GUI. */
	private static boolean dockVisible = true;
	private static int openType = -1;

	private final List<IconButton> iconButtons = new ArrayList<>();
	private final ModuleTab[] moduleTabs = new ModuleTab[BackpackScreenHandler.MODULE_COUNT];
	private DockToggle dockToggle;
	private UncraftArrowButton uncraftPrev;
	private UncraftArrowButton uncraftNext;
	private SleepButton sleepButton;
	private int lastSleepState = -1;
	private int lastMask = -1;

	private final BackpackTier tier;
	private int visibleRows;
	private boolean scrollable;
	private int scrollRow;
	private boolean draggingScrollbar;
	/** Posisi X (relatif panel) tepi kiri slot pertama inventory pemain. */
	private int inventoryLeft;

	public BackpackScreen(BackpackScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.tier = handler.getTier();
		this.visibleRows = this.tier.rows();
		computeLayout(Integer.MAX_VALUE);
	}

	/** Hitung ukuran panel dari tinggi layar yang tersedia. Dipanggil setiap init() (juga saat jendela diubah ukurannya). */
	private void computeLayout(int screenHeight) {
		int fit = screenHeight == Integer.MAX_VALUE ? this.tier.rows()
				: (screenHeight - SCREEN_MARGIN - BASE_HEIGHT) / 18;
		this.visibleRows = MathHelper.clamp(fit, Math.min(MIN_VISIBLE_ROWS, this.tier.rows()), this.tier.rows());
		this.scrollable = this.visibleRows < this.tier.rows();
		this.scrollRow = MathHelper.clamp(this.scrollRow, 0, maxScroll());

		int gridWidth = Math.max(this.tier.columns() * 18, 162);
		this.backgroundWidth = 14 + gridWidth + (this.scrollable ? SCROLLBAR_GUTTER : 0);
		this.backgroundHeight = BASE_HEIGHT + this.visibleRows * 18;
		this.inventoryLeft = 7 + (gridWidth - 162) / 2;
		this.playerInventoryTitleX = this.inventoryLeft + 1;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	private int maxScroll() {
		return Math.max(0, this.tier.rows() - this.visibleRows);
	}

	@Override
	protected void init() {
		computeLayout(this.height);
		super.init();
		this.iconButtons.clear();
		for (int i = 0; i < BUTTON_KEYS.length; i++) {
			int id = i;
			IconButton button = new IconButton(0, 0, i, () -> click(id));
			button.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.backpack." + BUTTON_KEYS[i])));
			this.iconButtons.add(button);
			this.addDrawableChild(button);
		}
		this.dockToggle = new DockToggle();
		this.addDrawableChild(this.dockToggle);
		this.uncraftPrev = new UncraftArrowButton(true, () -> click(hasShiftDown()
				? BackpackScreenHandler.BUTTON_UNCRAFT_PREVIOUS_10 : BackpackScreenHandler.BUTTON_UNCRAFT_PREVIOUS));
		this.uncraftPrev.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.prev_recipe")));
		this.addDrawableChild(this.uncraftPrev);
		this.uncraftNext = new UncraftArrowButton(false, () -> click(hasShiftDown()
				? BackpackScreenHandler.BUTTON_UNCRAFT_NEXT_10 : BackpackScreenHandler.BUTTON_UNCRAFT_NEXT));
		this.uncraftNext.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.next_recipe")));
		this.addDrawableChild(this.uncraftNext);
		this.sleepButton = new SleepButton(() -> click(BackpackScreenHandler.BUTTON_SLEEP));
		this.addDrawableChild(this.sleepButton);
		for (int i = 0; i < this.moduleTabs.length; i++) {
			this.moduleTabs[i] = new ModuleTab(i);
			this.addDrawableChild(this.moduleTabs[i]);
		}
		relayout();
	}

	/** Lebar tambahan di kanan panel utama (tombol, dock, dan panel modul yang terbuka). */
	private int extrasWidth() {
		if (!dockVisible) {
			return TOGGLE_W;
		}
		return DOCK_W + (activeType() >= 0 ? PANEL_W - 1 : 0);
	}

	/**
	 * Area di luar panel utama yang ikut ditempati GUI ini (tombol, dock modul, panel modul yang terbuka, dan dock Bed Module di kiri),
	 * dalam koordinat layar: tiap elemen = {x, y, lebar, tinggi}. Dipakai REI/JEI agar daftar item mereka tidak menimpa area ini.
	 */
	public List<int[]> extraAreas() {
		List<int[]> areas = new ArrayList<>();
		if (!dockVisible) {
			areas.add(new int[] {this.x + this.backgroundWidth, this.y + TOGGLE_Y, TOGGLE_W, TOGGLE_H});
			return areas;
		}
		int dockHeight = DOCK_PAD + BackpackScreenHandler.MODULE_COUNT * CELL_H;
		int bottom = DOCK_Y + dockHeight;
		int active = activeType();
		if (active >= 0) {
			bottom = Math.max(bottom, DOCK_Y + panelHeight(active));
		}
		areas.add(new int[] {this.x + this.backgroundWidth, this.y + TOGGLE_Y, extrasWidth(), bottom - TOGGLE_Y});
		areas.add(new int[] {this.x - LEFT_W, this.y + DOCK_Y, LEFT_W, LEFT_H});
		return areas;
	}

	/** Jenis modul yang panelnya sedang terbuka, atau -1. */
	private int activeType() {
		return dockVisible && openType >= 0 && this.handler.hasModule(openType) ? openType : -1;
	}

	private int installedMask() {
		int mask = 0;
		for (int i = 0; i < BackpackScreenHandler.MODULE_COUNT; i++) {
			mask = mask * 8 + (this.handler.moduleTypeAt(i) + 1);
		}
		return mask * 2 + (this.handler.hasBedModule() ? 1 : 0);
	}

	/**
	 * Hitung ulang posisi semuanya (panel utama digeser ke kiri bila dock + panel tidak muat di layar), posisi
	 * tombol, visibilitas tab, dan posisi slot. Tidak mengubah daftar widget sehingga aman dipanggil dari klik tombol.
	 */
	private void relayout() {
		int centered = (this.width - this.backgroundWidth) / 2;
		int overflow = centered + this.backgroundWidth + extrasWidth() - this.width;
		// Dock samping kiri butuh LEFT_W di sebelah kiri panel utama selama dock ditampilkan.
		this.x = Math.max(dockVisible ? LEFT_W : 0, overflow > 0 ? centered - overflow : centered);

		int total = BUTTON_KEYS.length * BUTTON_SIZE + (BUTTON_KEYS.length - 1) * BUTTON_GAP;
		int startX = this.x + this.backgroundWidth - 7 - total;
		for (int i = 0; i < this.iconButtons.size(); i++) {
			IconButton button = this.iconButtons.get(i);
			button.setX(startX + i * (BUTTON_SIZE + BUTTON_GAP));
			button.setY(this.y + 4);
		}

		this.lastMask = installedMask();
		this.dockToggle.setX(this.x + this.backgroundWidth);
		this.dockToggle.setY(this.y + TOGGLE_Y);
		this.dockToggle.setTooltip(Tooltip.of(Text.translatable(dockVisible
				? "gui.workshop_utilities.backpack.modules.hide" : "gui.workshop_utilities.backpack.modules.show")));
		int active = activeType();
		for (int i = 0; i < this.moduleTabs.length; i++) {
			ModuleTab tab = this.moduleTabs[i];
			int type = this.handler.moduleTypeAt(i);
			tab.visible = dockVisible && type >= 0;
			tab.setX(this.x + this.backgroundWidth + TAB_X);
			tab.setY(this.y + cellTop(i));
			tab.setTooltip(Tooltip.of(Text.translatable(type >= 0 && type == active
					? "gui.workshop_utilities.backpack.module.close" : "gui.workshop_utilities.backpack.module.open")));
		}
		boolean uncraft = active == BackpackScreenHandler.TYPE_UNCRAFTING;
		this.uncraftPrev.visible = uncraft;
		this.uncraftNext.visible = uncraft;
		this.uncraftPrev.setX(this.x + panelLeft() + UNCRAFT_PREV_X);
		this.uncraftPrev.setY(this.y + DOCK_Y + UNCRAFT_BUTTON_Y);
		this.uncraftNext.setX(this.x + panelLeft() + UNCRAFT_NEXT_X);
		this.uncraftNext.setY(this.y + DOCK_Y + UNCRAFT_BUTTON_Y);
		this.sleepButton.setX(this.x - LEFT_W + 5);
		this.sleepButton.setY(this.y + DOCK_Y + DOCK_PAD + CELL_H + 2);
		updateSleepButton();
		layoutSlots();
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		// Modul dipasang / dilepas (juga dari sinkronisasi server): sesuaikan tab, panel, dan posisi slot.
		if (installedMask() != this.lastMask) {
			relayout();
		}
		if (sleepState() != this.lastSleepState) {
			updateSleepButton();
		}
	}

	/** 0 = tidak ada Bed Module, 1 = ada tetapi belum waktunya tidur, 2 = siap dipakai. */
	private int sleepState() {
		if (!this.handler.hasBedModule()) {
			return 0;
		}
		return this.client != null && this.client.world != null
				&& BackpackScreenHandler.isSleepTime(this.client.world) ? 2 : 1;
	}

	/** Perbarui status aktif dan tooltip tombol Sleep (hanya bila statusnya berubah). */
	private void updateSleepButton() {
		int state = sleepState();
		this.lastSleepState = state;
		this.sleepButton.visible = dockVisible;
		this.sleepButton.active = state == 2;
		Text hint = switch (state) {
			case 0 -> Text.translatable("gui.workshop_utilities.backpack.sleep.need_module");
			case 1 -> Text.translatable("gui.workshop_utilities.backpack.sleep.need_night");
			default -> Text.translatable("gui.workshop_utilities.backpack.sleep.hint");
		};
		this.sleepButton.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.backpack.sleep")
				.append(Text.literal("\n")).append(hint)));
	}

	/**
	 * Letakkan semua slot sesuai layout client: baris tas yang terlihat di grid, sisanya di luar layar,
	 * lalu inventory pemain dan hotbar di bawah grid. Posisi slot di server tidak dipakai, jadi aman diubah di sini.
	 */
	private void layoutSlots() {
		int columns = this.tier.columns();
		int backpackSlots = this.handler.getBackpackSlots();
		for (int i = 0; i < backpackSlots; i++) {
			int visibleRow = i / columns - this.scrollRow;
			boolean shown = visibleRow >= 0 && visibleRow < this.visibleRows;
			place(this.handler.slots.get(i),
					shown ? 8 + (i % columns) * 18 : OFFSCREEN,
					shown ? 18 + visibleRow * 18 : OFFSCREEN);
		}
		int playerX = this.inventoryLeft + 1;
		int playerY = this.visibleRows * 18 + 31;
		for (int i = 0; i < 27; i++) {
			place(this.handler.slots.get(backpackSlots + i), playerX + (i % 9) * 18, playerY + (i / 9) * 18);
		}
		for (int i = 0; i < 9; i++) {
			place(this.handler.slots.get(backpackSlots + 27 + i), playerX + i * 18, playerY + 58);
		}
		layoutModuleSlots();
	}

	// ------------------------------------------------------------------ modul: geometri (relatif panel utama)

	private int cellTop(int cell) {
		return DOCK_Y + DOCK_PAD + cell * CELL_H;
	}

	private int panelLeft() {
		return this.backgroundWidth + DOCK_W - 1;
	}

	private int panelHeight(int type) {
		if (type == BackpackScreenHandler.TYPE_UNCRAFTING) {
			return UNCRAFT_H;
		}
		return type == BackpackScreenHandler.TYPE_CRAFTING ? CRAFT_H : FURNACE_H;
	}

	private void layoutModuleSlots() {
		for (int i = 0; i < BackpackScreenHandler.MODULE_COUNT; i++) {
			place(this.handler.slots.get(this.handler.moduleSlot(i)),
					dockVisible ? this.backgroundWidth + 5 : OFFSCREEN,
					dockVisible ? cellTop(i) + 1 : OFFSCREEN);
		}
		place(this.handler.slots.get(this.handler.bedSlot()),
				dockVisible ? -LEFT_W + 7 : OFFSCREEN, dockVisible ? cellTop(0) + 1 : OFFSCREEN);
		int active = activeType();
		int px = panelLeft();
		int py = DOCK_Y;
		for (int i = 0; i < 9; i++) {
			boolean shown = active == BackpackScreenHandler.TYPE_CRAFTING;
			place(this.handler.slots.get(this.handler.craftGridSlot(i)),
					shown ? px + 15 + (i % 3) * 18 : OFFSCREEN, shown ? py + 17 + (i / 3) * 18 : OFFSCREEN);
		}
		boolean craft = active == BackpackScreenHandler.TYPE_CRAFTING;
		place(this.handler.slots.get(this.handler.craftResultSlot()),
				craft ? px + 34 : OFFSCREEN, craft ? py + 88 : OFFSCREEN);
		for (int f = 0; f < 3; f++) {
			boolean shown = active == BackpackScreenHandler.TYPE_FURNACE + f;
			place(this.handler.slots.get(this.handler.smeltSlot(f, 0)),
					shown ? px + 10 : OFFSCREEN, shown ? py + 17 : OFFSCREEN);
			place(this.handler.slots.get(this.handler.smeltSlot(f, 1)),
					shown ? px + 10 : OFFSCREEN, shown ? py + 53 : OFFSCREEN);
			place(this.handler.slots.get(this.handler.smeltSlot(f, 2)),
					shown ? px + 62 : OFFSCREEN, shown ? py + 35 : OFFSCREEN);
		}
		// Modul Uncrafting: 1 slot input di atas, grid hasil 3x3 di bawah (vertikal agar muat selebar panel modul lain).
		boolean uncraft = active == BackpackScreenHandler.TYPE_UNCRAFTING;
		place(this.handler.slots.get(this.handler.uncraftInputSlot()),
				uncraft ? px + 34 : OFFSCREEN, uncraft ? py + 16 : OFFSCREEN);
		for (int i = 0; i < 9; i++) {
			place(this.handler.slots.get(this.handler.uncraftOutputSlot(i)),
					uncraft ? px + 15 + (i % 3) * 18 : OFFSCREEN, uncraft ? py + 63 + (i / 3) * 18 : OFFSCREEN);
		}
	}

	/** Klik di dock / panel modul bukan klik di luar GUI (kalau tidak, item di kursor akan terbuang). */
	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
		if (!super.isClickOutsideBounds(mouseX, mouseY, left, top, button)) {
			return false;
		}
		int rx = (int) mouseX - this.x;
		int ry = (int) mouseY - this.y;
		if (rx >= this.backgroundWidth && rx < this.backgroundWidth + TOGGLE_W
				&& ry >= TOGGLE_Y && ry < TOGGLE_Y + TOGGLE_H) {
			return false;
		}
		if (!dockVisible) {
			return true;
		}
		if (rx >= -LEFT_W && rx < 0 && ry >= DOCK_Y && ry < DOCK_Y + LEFT_H) {
			return false;
		}
		int dockHeight = DOCK_PAD + BackpackScreenHandler.MODULE_COUNT * CELL_H;
		if (rx >= this.backgroundWidth && rx < this.backgroundWidth + DOCK_W && ry >= DOCK_Y && ry < DOCK_Y + dockHeight) {
			return false;
		}
		int active = activeType();
		return !(active >= 0 && rx >= panelLeft() && rx < panelLeft() + PANEL_W
				&& ry >= DOCK_Y && ry < DOCK_Y + panelHeight(active));
	}

	private static void place(Slot slot, int x, int y) {
		SlotAccessor accessor = (SlotAccessor) slot;
		accessor.workshop_utilities$setX(x);
		accessor.workshop_utilities$setY(y);
	}

	private void click(int id) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, id);
		}
	}

	// ------------------------------------------------------------------ scroll

	private void setScroll(int row) {
		int clamped = MathHelper.clamp(row, 0, maxScroll());
		if (clamped != this.scrollRow) {
			this.scrollRow = clamped;
			layoutSlots();
		}
	}

	private int trackX() {
		return this.x + 7 + this.tier.columns() * 18 + 3;
	}

	private int trackY() {
		return this.y + 17;
	}

	private int trackHeight() {
		return this.visibleRows * 18;
	}

	private int thumbHeight() {
		return Math.max(12, this.trackHeight() * this.visibleRows / this.tier.rows());
	}

	private int thumbY() {
		int travel = this.trackHeight() - thumbHeight();
		return trackY() + (maxScroll() == 0 ? 0 : travel * this.scrollRow / maxScroll());
	}

	private boolean overTrack(double mouseX, double mouseY) {
		return this.scrollable && mouseX >= trackX() && mouseX < trackX() + SCROLLBAR_WIDTH
				&& mouseY >= trackY() && mouseY < trackY() + trackHeight();
	}

	private void dragTo(double mouseY) {
		int travel = this.trackHeight() - thumbHeight();
		if (travel <= 0) {
			return;
		}
		double ratio = (mouseY - trackY() - thumbHeight() / 2.0) / travel;
		setScroll((int) Math.round(MathHelper.clamp(ratio, 0.0, 1.0) * maxScroll()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.scrollable && verticalAmount != 0) {
			setScroll(this.scrollRow - (int) Math.signum(verticalAmount));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overTrack(mouseX, mouseY)) {
			this.draggingScrollbar = true;
			dragTo(mouseY);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.draggingScrollbar) {
			dragTo(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && this.draggingScrollbar) {
			this.draggingScrollbar = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	// ------------------------------------------------------------------ gambar

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		// Judul dipotong agar tidak menabrak tombol bila tas diberi nama panjang di anvil.
		int buttonsWidth = BUTTON_KEYS.length * (BUTTON_SIZE + BUTTON_GAP) + 4;
		int maxWidth = this.backgroundWidth - this.titleX - 7 - buttonsWidth;
		String name = this.textRenderer.trimToWidth(this.title.getString(), Math.max(20, maxWidth));
		context.drawText(this.textRenderer, name, this.titleX, this.titleY, TEXT, false);

		context.drawText(this.textRenderer, this.playerInventoryTitle,
				this.playerInventoryTitleX, this.playerInventoryTitleY, TEXT, false);

		String used = this.handler.getUsedSlots() + "/" + this.handler.getBackpackSlots();
		int usedX = this.playerInventoryTitleX + 162 - this.textRenderer.getWidth(used);
		context.drawText(this.textRenderer, used, usedX, this.playerInventoryTitleY, TEXT, false);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		drawPanel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);

		// Slot tas (hanya baris yang terlihat)
		for (int row = 0; row < this.visibleRows; row++) {
			for (int col = 0; col < this.tier.columns(); col++) {
				context.drawTexture(WIDGETS, this.x + 7 + col * 18, this.y + 17 + row * 18, 0, 24, 18, 18);
			}
		}
		// Slot inventory pemain + hotbar (terpusat di bawah tas)
		int slotX = this.x + this.inventoryLeft;
		int slotY = this.y + this.visibleRows * 18 + 30;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				context.drawTexture(WIDGETS, slotX + col * 18, slotY + row * 18, 0, 24, 18, 18);
			}
		}
		for (int col = 0; col < 9; col++) {
			context.drawTexture(WIDGETS, slotX + col * 18, slotY + 58, 0, 24, 18, 18);
		}
		if (this.scrollable) {
			drawScrollbar(context);
		}
		drawModules(context);
	}

	private void drawScrollbar(DrawContext context) {
		int x = trackX();
		int y = trackY();
		int h = trackHeight();
		// Alur inset (gelap di kiri-atas, putih di kanan-bawah) seperti slot vanilla.
		context.fill(x, y, x + SCROLLBAR_WIDTH, y + h, TRACK_DARK);
		context.fill(x + 1, y + 1, x + SCROLLBAR_WIDTH, y + h, WHITE);
		context.fill(x + 1, y + 1, x + SCROLLBAR_WIDTH - 1, y + h - 1, TRACK);

		int thumbTop = thumbY();
		int thumbBottom = thumbTop + thumbHeight();
		int tx = x + 1;
		int tr = x + SCROLLBAR_WIDTH - 1;
		context.fill(tx, thumbTop, tr, thumbBottom, BLACK);
		context.fill(tx, thumbTop, tr - 1, thumbBottom - 1, PANEL);
		context.fill(tx, thumbTop, tr - 1, thumbTop + 1, WHITE);
		context.fill(tx, thumbTop, tx + 1, thumbBottom - 1, WHITE);
		context.fill(tx + 1, thumbBottom - 2, tr - 1, thumbBottom - 1, SHADOW);
		context.fill(tr - 2, thumbTop + 1, tr - 1, thumbBottom - 1, SHADOW);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Tombol resep Uncrafting hanya aktif bila ada lebih dari satu pilihan resep.
		boolean choose = this.handler.getUncraftTotal() > 1;
		if (this.uncraftPrev != null) {
			this.uncraftPrev.active = choose;
		}
		if (this.uncraftNext != null) {
			this.uncraftNext.active = choose;
		}
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	/** Tooltip khusus slot modul: petunjuk saat kosong, dan alasan modul belum bisa dilepas bila panelnya masih berisi. */
	@Override
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (dockVisible && this.focusedSlot != null && this.handler.getCursorStack().isEmpty()) {
			int index = this.handler.slots.indexOf(this.focusedSlot);
			if (index == this.handler.bedSlot() && !this.focusedSlot.hasStack()) {
				List<Text> tip = new ArrayList<>();
				tip.add(Text.translatable("gui.workshop_utilities.backpack.side.slot"));
				tip.add(Text.translatable("gui.workshop_utilities.backpack.side.accepts", ModItems.BED_MODULE.getName())
						.formatted(net.minecraft.util.Formatting.GRAY));
				context.drawTooltip(this.textRenderer, tip, x, y);
				return;
			}
			for (int i = 0; i < BackpackScreenHandler.MODULE_COUNT; i++) {
				if (index != this.handler.moduleSlot(i)) {
					continue;
				}
				List<Text> tip = new ArrayList<>();
				if (this.focusedSlot.hasStack()) {
					tip.add(this.focusedSlot.getStack().getName());
					if (!this.focusedSlot.canTakeItems(this.client.player)) {
						tip.add(Text.translatable("gui.workshop_utilities.backpack.module.busy")
								.formatted(net.minecraft.util.Formatting.RED));
					}
				} else {
					tip.add(Text.translatable("gui.workshop_utilities.backpack.module.slot"));
					tip.add(Text.translatable("gui.workshop_utilities.backpack.module.accepts_a",
							ModItems.CRAFTING_MODULE.getName(), ModItems.FURNACE_MODULE.getName())
							.formatted(net.minecraft.util.Formatting.GRAY));
					tip.add(Text.translatable("gui.workshop_utilities.backpack.module.accepts_b",
							ModItems.BLAST_FURNACE_MODULE.getName(), ModItems.SMOKER_MODULE.getName())
							.formatted(net.minecraft.util.Formatting.GRAY));
					tip.add(Text.translatable("gui.workshop_utilities.backpack.module.accepts_c",
							ModItems.UNCRAFTING_MODULE.getName()).formatted(net.minecraft.util.Formatting.GRAY));
				}
				context.drawTooltip(this.textRenderer, tip, x, y);
				return;
			}
		}
		super.drawMouseoverTooltip(context, x, y);
	}

	// ------------------------------------------------------------------ modul: gambar

	private static final int GRAY_ARROW = 0xFF8B8B8B;

	/** Bingkai slot 18x18 untuk slot dengan posisi item (ix, iy) relatif panel utama. */
	private void slotFrame(DrawContext context, int ix, int iy) {
		context.drawTexture(WIDGETS, this.x + ix - 1, this.y + iy - 1, 0, 24, 18, 18);
	}

	/**
	 * Tab yang menempel di tepi kanan panel utama (seperti tab Creative): menutupi garis tepi panel utama pada
	 * rentang tab supaya terlihat menyatu. (rx, ry) relatif panel utama, rx harus sama dengan lebar panel utama.
	 */
	private void drawAttached(DrawContext context, int rx, int ry, int w, int h) {
		int x0 = this.x + rx;
		int y0 = this.y + ry;
		context.fill(x0 - 3, y0, x0 + w, y0 + h, PANEL);
		context.fill(x0, y0 - 1, x0 + w, y0, BLACK);
		context.fill(x0, y0 + h, x0 + w, y0 + h + 1, BLACK);
		context.fill(x0 + w - 1, y0, x0 + w, y0 + h, BLACK);
		context.fill(x0, y0, x0 + w - 3, y0 + 2, WHITE);
		context.fill(x0, y0 + h - 2, x0 + w - 1, y0 + h, SHADOW);
		context.fill(x0 + w - 3, y0 + 2, x0 + w - 1, y0 + h, SHADOW);
	}

	/** Tab di tepi KIRI panel utama (cermin drawAttached): slot Bed Module dan tombol Sleep. (rx, ry) relatif panel utama. */
	private void drawAttachedLeft(DrawContext context, int ry, int w, int h) {
		int left = this.x - w;
		int y0 = this.y + ry;
		context.fill(left, y0, this.x + 3, y0 + h, PANEL);
		context.fill(left, y0 - 1, this.x, y0, BLACK);
		context.fill(left, y0 + h, this.x, y0 + h + 1, BLACK);
		context.fill(left, y0, left + 1, y0 + h, BLACK);
		context.fill(left + 1, y0, this.x + 3, y0 + 2, WHITE);
		context.fill(left + 1, y0, left + 3, y0 + h - 2, WHITE);
		context.fill(left + 3, y0 + h - 2, this.x + 1, y0 + h, SHADOW);
	}

	private void drawLeftDock(DrawContext context) {
		drawAttachedLeft(context, DOCK_Y, LEFT_W, LEFT_H);
		int ix = -LEFT_W + 7;
		int iy = cellTop(0) + 1;
		slotFrame(context, ix, iy);
		if (this.handler.hasBedModule()) {
			outline(context, this.x + ix - 1, this.y + iy - 1, 18, 18, BED_COLOR);
		} else {
			drawDashed(context, this.x + ix + 1, this.y + iy + 1, 14, 14, 0xFF555555);
		}
	}

	private void drawModules(DrawContext context) {
		drawAttached(context, this.backgroundWidth, TOGGLE_Y, TOGGLE_W, TOGGLE_H);
		if (!dockVisible) {
			return;
		}
		drawLeftDock(context);
		int dockHeight = DOCK_PAD + BackpackScreenHandler.MODULE_COUNT * CELL_H;
		drawAttached(context, this.backgroundWidth, DOCK_Y, DOCK_W, dockHeight);
		for (int i = 0; i < BackpackScreenHandler.MODULE_COUNT; i++) {
			int ix = this.backgroundWidth + 5;
			int iy = cellTop(i) + 1;
			slotFrame(context, ix, iy);
			int type = this.handler.moduleTypeAt(i);
			if (type < 0) {
				drawDashed(context, this.x + ix + 1, this.y + iy + 1, 14, 14, 0xFF555555);
			} else {
				outline(context, this.x + ix - 1, this.y + iy - 1, 18, 18, MODULE_COLORS[type]);
			}
		}
		int active = activeType();
		if (active < 0) {
			return;
		}
		int px = this.x + panelLeft();
		int py = this.y + DOCK_Y;
		drawPanel(context, px, py, PANEL_W, panelHeight(active));
		String title = this.textRenderer.trimToWidth(MODULE_TITLES[active].getString(), PANEL_W - 12);
		context.drawText(this.textRenderer, title, px + 7, py + 5, TEXT, false);
		if (active == BackpackScreenHandler.TYPE_CRAFTING) {
			for (int i = 0; i < 9; i++) {
				slotFrame(context, panelLeft() + 15 + (i % 3) * 18, DOCK_Y + 17 + (i / 3) * 18);
			}
			drawDownArrow(context, px + 37, py + 75);
			slotFrame(context, panelLeft() + 34, DOCK_Y + 88);
		} else if (active == BackpackScreenHandler.TYPE_UNCRAFTING) {
			slotFrame(context, panelLeft() + 34, DOCK_Y + 16);
			// Kotak penghitung pilihan resep (n/total), sama dengan Uncrafting Table.
			int bx = px + UNCRAFT_COUNTER_X;
			int by = py + UNCRAFT_BUTTON_Y;
			context.fill(bx, by, bx + UNCRAFT_COUNTER_W, by + 12, WHITE);
			context.fill(bx, by, bx + UNCRAFT_COUNTER_W - 1, by + 11, TRACK_DARK);
			context.fill(bx + 1, by + 1, bx + UNCRAFT_COUNTER_W - 1, by + 11, 0xFF1E1E1E);
			int total = this.handler.getUncraftTotal();
			String label = total > 0 ? (this.handler.getUncraftSelected() + 1) + "/" + total : "-";
			int lw = this.textRenderer.getWidth(label);
			context.drawText(this.textRenderer, label, bx + (UNCRAFT_COUNTER_W - lw) / 2, by + 2, 0xE0E0E0, false);
			drawDownArrow(context, px + 37, py + 50);
			for (int i = 0; i < 9; i++) {
				slotFrame(context, panelLeft() + 15 + (i % 3) * 18, DOCK_Y + 63 + (i / 3) * 18);
			}
		} else {
			int f = active - BackpackScreenHandler.TYPE_FURNACE;
			slotFrame(context, panelLeft() + 10, DOCK_Y + 17);
			slotFrame(context, panelLeft() + 10, DOCK_Y + 53);
			slotFrame(context, panelLeft() + 62, DOCK_Y + 35);
			drawFlame(context, px + 11, py + 36, this.handler.burnFraction(f));
			drawProgressArrow(context, px + 32, py + 35, this.handler.cookFraction(f));
		}
	}

	private static void outline(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y + 1, x + 1, y + h - 1, color);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** Garis tepi putus-putus (2 piksel menyala, 2 mati) untuk slot modul yang masih kosong. */
	private static void drawDashed(DrawContext context, int x, int y, int w, int h, int color) {
		for (int i = 0; i < w; i += 4) {
			int end = Math.min(i + 2, w);
			context.fill(x + i, y, x + end, y + 1, color);
			context.fill(x + i, y + h - 1, x + end, y + h, color);
		}
		for (int i = 0; i < h; i += 4) {
			int end = Math.min(i + 2, h);
			context.fill(x, y + i, x + 1, y + end, color);
			context.fill(x + w - 1, y + i, x + w, y + end, color);
		}
	}

	private static final String[] FLAME = {
			"......oo......",
			".....oyyo.....",
			".....oyyo.....",
			"....oyyyyo.o..",
			"...oyyrryyoo..",
			"..oyyrrrryyo..",
			"..oyrrrrrryo..",
			".oyyrrrrrryyo.",
			".oyrrrrwwrryo.",
			".oyrrrwwwwryo.",
			".oyrrwwwwwryo.",
			".oyyrwwwwryyo.",
			"..oyyrrrryyo..",
			"...ooyyyyoo..."};

	/** Ikon api 14x14 buatan sendiri (tanpa texture): siluet abu-abu, bagian menyala naik-turun mengikuti sisa bahan bakar. */
	private static void drawFlame(DrawContext context, int x, int y, float burn) {
		int lit = burn <= 0.0F ? 0 : Math.min(14, 1 + (int) (13 * burn));
		for (int row = 0; row < FLAME.length; row++) {
			boolean on = row >= 14 - lit;
			for (int col = 0; col < FLAME[row].length(); col++) {
				char c = FLAME[row].charAt(col);
				if (c == '.') {
					continue;
				}
				int color = 0xFF7A7A7A;
				if (on) {
					color = switch (c) {
						case 'o' -> 0xFF6B2A00;
						case 'y' -> 0xFFFFC832;
						case 'r' -> 0xFFE8681C;
						default -> 0xFFFFF0A0;
					};
				}
				context.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	/** Panah progress 24x16 seperti furnace vanilla; bagian yang terisi memutih dari kiri. */
	private static void drawProgressArrow(DrawContext context, int x, int y, float progress) {
		int filled = (int) (24 * progress);
		for (int col = 0; col < 24; col++) {
			int top;
			int bottom;
			if (col < 16) {
				top = 5;
				bottom = 11;
			} else {
				int c = col - 16;
				top = c;
				bottom = 16 - c;
			}
			context.fill(x + col, y + top, x + col + 1, y + bottom, col < filled ? WHITE : GRAY_ARROW);
		}
	}

	private static void drawDownArrow(DrawContext context, int x, int y) {
		context.fill(x + 4, y, x + 7, y + 4, GRAY_ARROW);
		for (int r = 0; r < 6; r++) {
			context.fill(x + r, y + 4 + r, x + 11 - r, y + 5 + r, GRAY_ARROW);
		}
	}

	/** Chevron kecil 3 piksel; {@code right} = menghadap kanan. */
	private static void drawChevron(DrawContext context, int x, int y, boolean right, int color) {
		for (int c = 0; c < 3; c++) {
			int half = 2 - c;
			int cx = right ? x + c : x + 2 - c;
			context.fill(cx, y + 2 - half, cx + 1, y + 3 + half, color);
		}
	}

	private static void drawInset(DrawContext context, int x, int y, int w, int h, int fill) {
		context.fill(x, y, x + w, y + h, WHITE);
		context.fill(x, y, x + w - 1, y + h - 1, TRACK_DARK);
		context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
	}

	/** Tombol tab kecil di sebelah slot modul: panah buka/tutup panel, bar hijau = progress masak, titik oranye = menyala. */
	private class ModuleTab extends PressableWidget {
		private final int cell;

		ModuleTab(int cell) {
			super(0, 0, TAB_W, 18, Text.translatable("gui.workshop_utilities.backpack.module.open"));
			this.cell = cell;
		}

		@Override
		public void onPress() {
			int type = BackpackScreen.this.handler.moduleTypeAt(this.cell);
			if (type < 0) {
				return;
			}
			openType = openType == type ? -1 : type;
			relayout();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int type = BackpackScreen.this.handler.moduleTypeAt(this.cell);
			if (type < 0) {
				return;
			}
			int x = this.getX();
			int y = this.getY();
			boolean open = activeType() == type;
			drawInset(context, x, y, TAB_W, 18, this.isHovered() ? 0xFFA5A5A5 : TRACK);
			drawChevron(context, x + 1, y + 6, !open, 0xFF373737);
			if (type >= BackpackScreenHandler.TYPE_FURNACE && type <= BackpackScreenHandler.TYPE_SMOKER) {
				int f = type - BackpackScreenHandler.TYPE_FURNACE;
				context.fill(x + 5, y + 4, x + 7, y + 15, 0xFF404040);
				int bar = Math.round(11 * BackpackScreen.this.handler.cookFraction(f));
				context.fill(x + 5, y + 15 - bar, x + 7, y + 15, 0xFF3FBF3F);
				if (BackpackScreen.this.handler.isBurning(f)) {
					context.fill(x + 5, y + 1, x + 7, y + 3, 0xFFFF8A1E);
				}
			}
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}

	/** Tombol panah 12x12 modul Uncrafting; sprite diambil dari texture GUI Uncrafting Table. */
	private static class UncraftArrowButton extends PressableWidget {
		private final boolean left;
		private final Runnable action;

		UncraftArrowButton(boolean left, Runnable action) {
			super(0, 0, 12, 12, Text.literal(left ? "<" : ">"));
			this.left = left;
			this.action = action;
		}

		@Override
		public void onPress() {
			this.action.run();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int u = !this.active ? 200 : (this.isHovered() ? 188 : 176);
			int v = this.left ? 0 : 12;
			context.drawTexture(UNCRAFT_TEXTURE, this.getX(), this.getY(), u, v, 12, 12);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}

	/** Tombol Sleep di bawah slot Bed Module; ikon tempat tidur digambar langsung (tanpa texture), abu-abu bila tidak aktif. */
	private class SleepButton extends PressableWidget {
		private final Runnable action;

		SleepButton(Runnable action) {
			super(0, 0, SLEEP_W, SLEEP_H, Text.translatable("gui.workshop_utilities.backpack.sleep"));
			this.action = action;
		}

		@Override
		public void onPress() {
			this.action.run();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int x = this.getX();
			int y = this.getY();
			boolean on = this.active;
			int base = !on ? 0xFF707070 : (this.isHovered() ? 0xFFA6A6C8 : 0xFF8B8B8B);
			context.fill(x, y, x + SLEEP_W, y + SLEEP_H, BLACK);
			context.fill(x + 1, y + 1, x + SLEEP_W - 1, y + SLEEP_H - 1, base);
			context.fill(x + 1, y + 1, x + SLEEP_W - 1, y + 2, on ? WHITE : 0xFF909090);
			context.fill(x + 1, y + 1, x + 2, y + SLEEP_H - 1, on ? WHITE : 0xFF909090);
			context.fill(x + 2, y + SLEEP_H - 2, x + SLEEP_W - 1, y + SLEEP_H - 1, TRACK_DARK);
			context.fill(x + SLEEP_W - 2, y + 2, x + SLEEP_W - 1, y + SLEEP_H - 1, TRACK_DARK);
			drawBed(context, x + 4, y + 3, on);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}

	/** Ikon tempat tidur 14x10: papan kepala, bantal putih, selimut merah, kaki kayu. */
	private static void drawBed(DrawContext context, int x, int y, boolean on) {
		int wood = on ? 0xFF6B4423 : 0xFF4A4A4A;
		int pillow = on ? 0xFFF4F4F4 : 0xFFB8B8B8;
		int blanket = on ? 0xFFC02828 : 0xFF808080;
		int fold = on ? 0xFF8E1C1C : 0xFF666666;
		context.fill(x, y, x + 2, y + 10, wood);
		context.fill(x + 12, y + 4, x + 14, y + 10, wood);
		context.fill(x + 2, y + 3, x + 5, y + 6, pillow);
		context.fill(x + 5, y + 3, x + 12, y + 6, blanket);
		context.fill(x + 5, y + 5, x + 12, y + 6, fold);
		context.fill(x + 2, y + 6, x + 12, y + 8, wood);
	}

	/** Tab kecil di tepi kanan panel utama untuk menyembunyikan / menampilkan dock modul. */
	private class DockToggle extends PressableWidget {
		DockToggle() {
			super(0, 0, TOGGLE_W, TOGGLE_H, Text.translatable("gui.workshop_utilities.backpack.modules.hide"));
		}

		@Override
		public void onPress() {
			dockVisible = !dockVisible;
			relayout();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int cx = this.getX() + 4;
			int cy = this.getY() + 5;
			if (this.isHovered()) {
				context.fill(this.getX(), this.getY(), this.getX() + TOGGLE_W - 1, this.getY() + TOGGLE_H,
						0x40FFFFFF);
			}
			drawChevron(context, cx, cy, !dockVisible, 0xFF373737);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}

	/** Panel container vanilla: garis hitam 1px (sudut dipotong), highlight putih & bayangan abu-abu 2px, isi #C6C6C6. */
	private static void drawPanel(DrawContext context, int x, int y, int w, int h) {
		context.fill(x + 1, y, x + w, y + 1, BLACK);
		context.fill(x, y + 1, x + 1, y + h, BLACK);
		context.fill(x + 1, y + h - 1, x + w - 1, y + h, BLACK);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, BLACK);
		context.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
		context.fill(x + 1, y + 1, x + w - 3, y + 3, WHITE);
		context.fill(x + 1, y + 1, x + 3, y + h - 3, WHITE);
		context.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
		context.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
	}

	/** Tombol ikon 12x12; sprite normal di v=0, saat disorot di v=12 pada atlas widget. */
	private static class IconButton extends PressableWidget {
		private final int icon;
		private final Runnable action;

		IconButton(int x, int y, int icon, Runnable action) {
			super(x, y, BUTTON_SIZE, BUTTON_SIZE, Text.translatable("gui.workshop_utilities.backpack." + BUTTON_KEYS[icon]));
			this.icon = icon;
			this.action = action;
		}

		@Override
		public void onPress() {
			this.action.run();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			context.drawTexture(WIDGETS, this.getX(), this.getY(), this.icon * BUTTON_SIZE,
					this.isHovered() ? BUTTON_SIZE : 0, BUTTON_SIZE, BUTTON_SIZE);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}
}
