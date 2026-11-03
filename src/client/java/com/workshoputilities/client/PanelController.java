package com.workshoputilities.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.client.mixin.HandledScreenAccessor;
import com.workshoputilities.client.mixin.ScreenInvoker;
import com.workshoputilities.client.mixin.SlotAccessor;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.screen.PanelHost;
import com.workshoputilities.screen.PanelSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Seluruh tampilan dan penempatan slot untuk panel samping sebuah GUI block, dipakai oleh {@link PanelHandledScreen}
 * (GUI block mod ini) dan oleh GUI block vanilla (Crafting Table, Furnace, Anvil, dst.; lewat HandledScreenPanelMixin).
 *
 * <ul>
 * <li><b>Panel backpack</b>: dibuka / ditutup lewat tombol kecil di sisi GUI. Isinya slot-slot backpack yang sedang dipakai di
 * slot Backpack pemain, langsung bisa dipakai (drag, klik, Shift-click). Jumlah kolom menyesuaikan lebar layar dan bila tinggi
 * layar tidak cukup muncul scrollbar. Status buka / tutup diingat antar pembukaan GUI. Bila backpack punya Tools Holder
 * Module, 7 slot Tools Holder tampil di panel terpisah tepat di KIRI panel backpack (di semua GUI block utility).</li>
 * <li><b>Panel equipment</b> (kanan, hanya bila handler punya slot equipment): 4 slot armor, tangan kiri, dan Elytra.</li>
 * </ul>
 *
 * Mode {@code shiftAllowed} (GUI block mod ini): GUI boleh digeser supaya panel muat. Mode vanilla: posisi GUI tidak pernah
 * diubah (widget vanilla seperti kolom nama Anvil dihitung dari posisi aslinya), panel menyesuaikan ruang yang tersisa.
 *
 * Tata letak SELALU konsisten di semua GUI: panel backpack (+ Tools Holder) di KIRI, panel equipment (armor, tangan kiri,
 * Elytra) di KANAN. Pada Crafting Table / Furnace / Blast Furnace / Smoker, sisi kiri juga dipakai recipe book; saat recipe
 * book terbuka, panel backpack bergeser ke kiri recipe book (lihat {@link #recipeBookWidth()}) sehingga tetap di kiri.
 */
public final class PanelController {
	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int TRACK = 0xFF8B8B8B;
	private static final int TRACK_DARK = 0xFF373737;
	private static final int TEXT = 0x404040;
	private static final int TAB_HOVER = 0xFFDADAEE;

	private static final int OFFSCREEN = -10000;
	/** Bagian panel yang masuk ke bawah GUI utama, supaya sambungannya tertutup texture GUI. */
	private static final int OVERLAP = 4;
	private static final int EDGE = 4;

	// ---- panel equipment (kanan)
	private static final int EQ_VISIBLE_W = 32;
	private static final int EQ_TOP = 6;
	private static final int EQ_SLOT_DX = 8;
	private static final int EQ_ARMOR_Y = 15;
	private static final int EQ_DIVIDER_Y = 89;
	private static final int EQ_OFFHAND_Y = 93;
	private static final int EQ_ELYTRA_Y = 111;
	private static final int EQ_HEIGHT = 130;

	// ---- panel backpack
	private static final int TAB_SIZE = 20;
	private static final int TAB_OVERLAP = 2;
	private static final int TAB_Y = 6;
	private static final int HEADER_H = 18;
	private static final int FOOTER_H = 7;
	private static final int SIDE_PAD = 7;
	private static final int GUTTER = 12;
	private static final int SCROLLBAR_W = 8;
	private static final int MIN_COLUMNS = 4;
	private static final int MIN_ROWS = 3;
	private static final int EMPTY_W = 112;
	/** Panel Tools Holder: panel terpisah di KIRI panel backpack (rak 2 x 4 = 7 slot + 1 sel penghitung, sama seperti GUI Backpack). */
	private static final int TOOLS_COLS = 2;
	private static final int TOOLS_ROWS = 4;
	private static final int TOOLS_PAD = 6;
	private static final int TOOLS_HEADER_H = 16;
	private static final int TOOLS_W = TOOLS_COLS * 18 + TOOLS_PAD * 2;
	private static final int TOOLS_H = TOOLS_HEADER_H + TOOLS_ROWS * 18 + TOOLS_PAD;

	private static final Identifier BACKPACK_ICON =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/backpack_slot_icon.png");
	private static final Identifier ELYTRA_ICON =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/elytra_slot_icon.png");
	private static final Identifier[] ARMOR_ICONS = {
			Identifier.of("minecraft", "textures/item/empty_armor_slot_helmet.png"),
			Identifier.of("minecraft", "textures/item/empty_armor_slot_chestplate.png"),
			Identifier.of("minecraft", "textures/item/empty_armor_slot_leggings.png"),
			Identifier.of("minecraft", "textures/item/empty_armor_slot_boots.png")};
	private static final Identifier SHIELD_ICON = Identifier.of("minecraft", "textures/item/empty_armor_slot_shield.png");

	/** Status panel backpack, diingat antar pembukaan GUI (dipakai bersama oleh semua GUI). */
	private static boolean backpackOpen = false;

	private final HandledScreen<?> screen;
	private final HandledScreenAccessor access;
	private final PanelSupport support;
	private final boolean shiftAllowed;

	private ToggleTab tab;
	private boolean alive = true;
	private boolean reinitPending;
	private boolean draggingScrollbar;

	// ---- hasil layout
	private int layoutCount;
	private boolean layoutTools;
	private boolean backpackShown;
	private boolean emptyShown;
	private boolean toolsShown;
	private int columns = 9;
	private int totalRows;
	private int visibleRows;
	private boolean scrollable;
	private int scrollRow;
	private int panelW;
	private int panelH;
	/** Posisi panel backpack relatif terhadap GUI utama (kiri atas panel). */
	/** Lebar sisi kiri GUI yang dipakai recipe book vanilla (0 bila tidak ada / tertutup). */
	private int leftBlock;
	private int panelOffsetX;
	/** Posisi panel Tools Holder relatif terhadap GUI utama (kiri atas); selalu tepat di kiri panel backpack. */
	private int toolsOffsetX;
	private int toolsTopRel;
	private int panelTopRel;
	private List<OrderedText> emptyLines = List.of();
	/** Tanda posisi / ukuran terakhir, dipakai mode vanilla untuk mendeteksi GUI yang bergeser (mis. recipe book dibuka). */
	private long lastSignature = Long.MIN_VALUE;

	public PanelController(HandledScreen<?> screen, PanelSupport support, boolean shiftAllowed) {
		this.screen = screen;
		this.access = (HandledScreenAccessor) (Object) screen;
		this.support = support;
		this.shiftAllowed = shiftAllowed;
	}

	/** Support panel milik handler vanilla (null bila handler tidak diberi panel). */
	public static PanelSupport supportOf(ScreenHandler handler) {
		return handler instanceof PanelHost host ? host.workshop_utilities$getPanel() : null;
	}

	/** Area panel (tombol + panel) milik screen vanilla untuk REI / JEI; kosong bila screen tidak diberi panel. */
	public static List<int[]> vanillaAreas(Object screen) {
		if (screen instanceof PanelScreenAccess access && access.workshop_utilities$getPanelController() != null) {
			return access.workshop_utilities$getPanelController().extraAreas();
		}
		return List.of();
	}

	/** Matikan controller lama saat screen di-init ulang (event Fabric yang tersisa jadi tidak berbuat apa-apa). */
	public void dispose() {
		this.alive = false;
	}

	// ------------------------------------------------------------------ ukuran GUI

	private int gx() {
		return this.access.workshop_utilities$getX();
	}

	private int gy() {
		return this.access.workshop_utilities$getY();
	}

	private int bgW() {
		return this.access.workshop_utilities$getBackgroundWidth();
	}

	private MinecraftClient client() {
		return MinecraftClient.getInstance();
	}

	// ------------------------------------------------------------------ layout

	/** Panggil dari init() screen, setelah posisi GUI (x, y) ditetapkan. */
	public void init() {
		this.reinitPending = false;
		computeLayout();
		placeSlots();

		PanelSupport.LOGGER.info("[panel] screen {} count={} open={} shown={} tools={} cols={} rows={} panelW={} gx={} gy={} offX={} toolsOffX={} slots={}",
				this.screen.getClass().getSimpleName(), this.layoutCount, backpackOpen, this.backpackShown, this.toolsShown, this.columns,
				this.visibleRows, this.panelW, gx(), gy(), this.panelOffsetX, this.toolsOffsetX, this.screen.getScreenHandler().slots.size());
		this.tab = new ToggleTab();
		updateTab();
		((ScreenInvoker) (Object) this.screen).workshop_utilities$addDrawableChild(this.tab);
		this.lastSignature = signature();
	}

	/** Jumlah slot backpack yang dipakai untuk layout: dari handler, atau (sebelum sinkron) dari backpack di slot Backpack. */
	private int effectiveCount() {
		int count = this.support.slotCount();
		MinecraftClient client = client();
		if (count == 0 && client != null && client.player != null
				&& BackpackSlotHolder.of(client.player).workshop_utilities$getBackpackSlot().getItem() instanceof BackpackItem item) {
			count = item.getTier().slots();
		}
		return count;
	}

	private static BackpackTier tierFor(int count) {
		for (BackpackTier tier : BackpackTier.values()) {
			if (tier.slots() == count) {
				return tier;
			}
		}
		return null;
	}

	/**
	 * Lebar sisi kiri GUI yang sedang dipakai recipe book (Crafting Table, Furnace, Blast Furnace, Smoker). Recipe book yang
	 * terbuka di layar lebar menggeser GUI ke kanan dan menempati ruang di kirinya; posisinya dihitung sama seperti vanilla
	 * (RecipeBookWidget: kiri = (lebar layar - 147) / 2 - 86). Bernilai 0 bila GUI tidak punya recipe book, recipe book
	 * tertutup, atau layar sempit (recipe book menimpa GUI sehingga tidak ada yang bergeser).
	 */
	private int recipeBookWidth() {
		if (this.screen instanceof RecipeBookProvider provider) {
			RecipeBookWidget book = provider.getRecipeBookWidget();
			if (book != null && book.isOpen() && gx() > (this.screen.width - bgW()) / 2) {
				int bookLeft = (this.screen.width - 147) / 2 - 86;
				return Math.max(0, gx() - bookLeft);
			}
		}
		return 0;
	}

	private long signature() {
		long sig = effectiveCount();
		sig = sig * 31 + (this.support.toolsActive() ? 1 : 0);
		sig = sig * 31 + this.screen.width;
		sig = sig * 31 + this.screen.height;
		sig = sig * 31 + gx();
		sig = sig * 31 + gy();
		sig = sig * 31 + bgW();
		sig = sig * 31 + recipeBookWidth();
		return sig;
	}

	private void computeLayout() {
		MinecraftClient client = client();
		int bgW = bgW();
		this.leftBlock = recipeBookWidth();
		this.layoutCount = effectiveCount();
		this.layoutTools = this.support.toolsActive();
		BackpackTier tier = tierFor(this.layoutCount);
		boolean equipment = this.support.hasEquipmentPanel();
		int rightNeed = equipment ? EQ_VISIBLE_W : 0;

		this.backpackShown = backpackOpen && tier != null;
		this.emptyShown = backpackOpen && tier == null;
		this.toolsShown = this.backpackShown && this.layoutTools;
		this.emptyLines = List.of();
		if (this.backpackShown) {
			int availW;
			int toolsNeed = this.toolsShown ? TOOLS_W - 1 : 0;
			if (this.shiftAllowed) {
				availW = this.screen.width - bgW - rightNeed - TAB_SIZE - EDGE * 2 - toolsNeed;
			} else {
				availW = gx() - this.leftBlock - TAB_SIZE + TAB_OVERLAP - EDGE - toolsNeed;
			}
			int fit = (availW - SIDE_PAD * 2 - GUTTER) / 18;
			int minColumns = this.shiftAllowed ? Math.min(MIN_COLUMNS, tier.columns()) : 1;
			this.columns = MathHelper.clamp(fit, minColumns, tier.columns());
			this.totalRows = (this.layoutCount + this.columns - 1) / this.columns;
			int availH = this.screen.height - EDGE * 2 - HEADER_H - FOOTER_H;
			this.visibleRows = MathHelper.clamp(availH / 18, Math.min(MIN_ROWS, this.totalRows), this.totalRows);
			this.scrollable = this.visibleRows < this.totalRows;
			this.scrollRow = MathHelper.clamp(this.scrollRow, 0, maxScroll());
			this.panelW = SIDE_PAD * 2 + this.columns * 18 + (this.scrollable ? GUTTER : 0);
			this.panelH = HEADER_H + this.visibleRows * 18 + FOOTER_H;
		} else if (this.emptyShown) {
			this.scrollable = false;
			this.emptyLines = client.textRenderer.wrapLines(
					Text.translatable("gui.workshop_utilities.panel.backpack.none"), EMPTY_W - SIDE_PAD * 2);
			this.panelW = EMPTY_W;
			this.panelH = HEADER_H + this.emptyLines.size() * 10 + FOOTER_H;
		} else {
			this.scrollable = false;
			this.panelW = 0;
			this.panelH = 0;
		}

		if (this.shiftAllowed) {
			// Geser GUI utama bila perlu: kiri butuh panel (bila terbuka) + tombol, kanan butuh panel equipment.
			int leftNeed = TAB_SIZE - TAB_OVERLAP + (panelVisible() ? this.panelW : 0) + (this.toolsShown ? TOOLS_W - 1 : 0);
			int minX = leftNeed + EDGE;
			int maxX = this.screen.width - bgW - rightNeed - EDGE;
			this.access.workshop_utilities$setX(Math.max(minX, Math.min(gx(), maxX)));
		}

		this.panelOffsetX = -(this.panelW + this.leftBlock);
		int top = MathHelper.clamp(gy(), EDGE, Math.max(EDGE, this.screen.height - EDGE - this.panelH));
		this.panelTopRel = top - gy();
		this.toolsOffsetX = this.panelOffsetX - TOOLS_W + 1;
		int toolsTop = MathHelper.clamp(gy() + this.panelTopRel, EDGE, Math.max(EDGE, this.screen.height - EDGE - TOOLS_H));
		this.toolsTopRel = toolsTop - gy();
	}

	private int maxScroll() {
		return Math.max(0, this.totalRows - this.visibleRows);
	}

	private boolean panelVisible() {
		return this.backpackShown || this.emptyShown;
	}

	private int tabX() {
		int leftEdge = !panelVisible() ? gx() - this.leftBlock : gx() + (this.toolsShown ? this.toolsOffsetX : this.panelOffsetX);
		return leftEdge - TAB_SIZE + TAB_OVERLAP;
	}

	private int tabY() {
		return (panelVisible() ? gy() + this.panelTopRel : gy()) + TAB_Y;
	}

	private void updateTab() {
		if (this.tab == null) {
			return;
		}
		this.tab.setX(tabX());
		this.tab.setY(tabY());
		this.tab.setTooltip(Tooltip.of(Text.translatable(backpackOpen
				? "gui.workshop_utilities.panel.backpack.close" : "gui.workshop_utilities.panel.backpack.open")));
	}

	private static void place(Slot slot, int x, int y) {
		SlotAccessor accessor = (SlotAccessor) slot;
		accessor.workshop_utilities$setX(x);
		accessor.workshop_utilities$setY(y);
	}

	/** Letakkan slot panel di layar. Slot yang tidak terlihat dipindah keluar layar sehingga tidak bisa diklik. */
	private void placeSlots() {
		int bgW = bgW();
		if (this.support.hasEquipmentPanel()) {
			int ex = bgW + EQ_SLOT_DX;
			for (int i = 0; i < 4; i++) {
				place(this.support.slot(this.support.equipmentSlotIndex(i)), ex, EQ_ARMOR_Y + i * 18);
			}
			place(this.support.slot(this.support.equipmentSlotIndex(PanelSupport.EQUIP_OFFHAND)), ex, EQ_OFFHAND_Y);
			place(this.support.slot(this.support.equipmentSlotIndex(PanelSupport.EQUIP_ELYTRA)), ex, EQ_ELYTRA_Y);
		}
		for (int i = 0; i < PanelSupport.MAX_BACKPACK_SLOTS; i++) {
			Slot slot = this.support.slot(this.support.slotIndex(i));
			int sx = OFFSCREEN;
			int sy = OFFSCREEN;
			if (this.backpackShown && i < this.layoutCount) {
				int visibleRow = i / this.columns - this.scrollRow;
				if (visibleRow >= 0 && visibleRow < this.visibleRows) {
					sx = this.panelOffsetX + SIDE_PAD + 1 + (i % this.columns) * 18;
					sy = this.panelTopRel + HEADER_H + visibleRow * 18;
				}
			}
			place(slot, sx, sy);
		}
		for (int i = 0; i < PanelSupport.TOOL_SLOTS; i++) {
			Slot slot = this.support.slot(this.support.toolSlotIndex(i));
			if (this.toolsShown) {
				place(slot, this.toolsOffsetX + TOOLS_PAD + 1 + (i % TOOLS_COLS) * 18,
						this.toolsTopRel + TOOLS_HEADER_H + 1 + (i / TOOLS_COLS) * 18);
			} else {
				place(slot, OFFSCREEN, OFFSCREEN);
			}
		}
	}

	private void relayout() {
		computeLayout();
		placeSlots();
		updateTab();
		this.lastSignature = signature();
	}

	/** Dipanggil tiap tick oleh screen mod (mode geser): backpack dipakai / dilepas / berganti tier / Tools Holder berubah. */
	public void tick() {
		if (!this.reinitPending && (effectiveCount() != this.layoutCount || this.support.toolsActive() != this.layoutTools)) {
			this.reinitPending = true;
		}
	}

	/** True sekali bila screen mod perlu di-init ulang (posisi GUI bergeser sehingga widget lain ikut dihitung ulang). */
	public boolean consumeReinit() {
		boolean pending = this.reinitPending;
		this.reinitPending = false;
		return pending;
	}

	private void togglePanel() {
		backpackOpen = !backpackOpen;
		this.scrollRow = 0;
		if (this.shiftAllowed) {
			this.reinitPending = true;
		} else {
			relayout();
		}
	}

	// ------------------------------------------------------------------ area (REI / JEI, klik)

	private int frameX() {
		return gx() + this.panelOffsetX;
	}

	/**
	 * Area di luar GUI utama yang ditempati panel ini, dalam koordinat layar: {x, y, lebar, tinggi}. Dipakai REI / JEI agar
	 * daftar item mereka tidak menimpa panel, dan untuk klik di panel.
	 */
	public List<int[]> extraAreas() {
		List<int[]> areas = new ArrayList<>();
		areas.add(new int[] {tabX(), tabY(), TAB_SIZE, TAB_SIZE});
		if (panelVisible()) {
			areas.add(new int[] {frameX(), gy() + this.panelTopRel, this.panelW + OVERLAP, this.panelH});
		}
		if (this.toolsShown) {
			areas.add(new int[] {gx() + this.toolsOffsetX, gy() + this.toolsTopRel, TOOLS_W, TOOLS_H});
		}
		if (this.support.hasEquipmentPanel()) {
			areas.add(new int[] {gx() + bgW(), gy() + EQ_TOP, EQ_VISIBLE_W, EQ_HEIGHT});
		}
		return areas;
	}

	/** True bila titik ada di panel / tombol (klik di sana bukan klik di luar GUI; kalau tidak, item di kursor akan terbuang). */
	public boolean isOverExtraArea(double mouseX, double mouseY) {
		for (int[] area : extraAreas()) {
			if (mouseX >= area[0] && mouseX < area[0] + area[2] && mouseY >= area[1] && mouseY < area[1] + area[3]) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ scroll

	private void setScroll(int row) {
		int clamped = MathHelper.clamp(row, 0, maxScroll());
		if (clamped != this.scrollRow) {
			this.scrollRow = clamped;
			placeSlots();
		}
	}

	private int trackX() {
		return gx() + this.panelOffsetX + SIDE_PAD + this.columns * 18 + 3;
	}

	private int trackY() {
		return gy() + this.panelTopRel + HEADER_H - 1;
	}

	private int trackHeight() {
		return this.visibleRows * 18;
	}

	private int thumbHeight() {
		return Math.max(12, trackHeight() * this.visibleRows / Math.max(1, this.totalRows));
	}

	private int thumbY() {
		int travel = trackHeight() - thumbHeight();
		return trackY() + (maxScroll() == 0 ? 0 : travel * this.scrollRow / maxScroll());
	}

	private boolean overTrack(double mouseX, double mouseY) {
		return this.backpackShown && this.scrollable && mouseX >= trackX() && mouseX < trackX() + SCROLLBAR_W
				&& mouseY >= trackY() && mouseY < trackY() + trackHeight();
	}

	private boolean overBackpackPanel(double mouseX, double mouseY) {
		return this.backpackShown && mouseX >= gx() + this.panelOffsetX && mouseX < gx() + this.panelOffsetX + this.panelW
				&& mouseY >= gy() + this.panelTopRel && mouseY < gy() + this.panelTopRel + this.panelH;
	}

	private void dragTo(double mouseY) {
		int travel = trackHeight() - thumbHeight();
		if (travel <= 0) {
			return;
		}
		double ratio = (mouseY - trackY() - thumbHeight() / 2.0) / travel;
		setScroll((int) Math.round(MathHelper.clamp(ratio, 0.0, 1.0) * maxScroll()));
	}

	/** Roda mouse di atas panel backpack. True bila dipakai (tidak diteruskan ke screen). */
	public boolean scrolled(double mouseX, double mouseY, double verticalAmount) {
		if (this.alive && this.scrollable && verticalAmount != 0 && overBackpackPanel(mouseX, mouseY)) {
			setScroll(this.scrollRow - (int) Math.signum(verticalAmount));
			return true;
		}
		return false;
	}

	/** Klik pada scrollbar. True bila dipakai. */
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.alive && button == 0 && overTrack(mouseX, mouseY)) {
			this.draggingScrollbar = true;
			dragTo(mouseY);
			return true;
		}
		return false;
	}

	public boolean mouseDragged(double mouseY) {
		if (this.alive && this.draggingScrollbar) {
			dragTo(mouseY);
			return true;
		}
		return false;
	}

	public boolean mouseReleased(int button) {
		if (this.alive && button == 0 && this.draggingScrollbar) {
			this.draggingScrollbar = false;
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ gambar

	/**
	 * Gambar semua panel. Panggil di AWAL drawBackground screen, sebelum texture GUI utama: texture utama lalu menutupi
	 * {@value #OVERLAP} piksel sambungan sehingga panel tampak menempel rapi ke GUI.
	 */
	public void draw(DrawContext context) {
		if (!this.alive) {
			return;
		}
		if (!this.shiftAllowed && signature() != this.lastSignature) {
			relayout();
		}
		if (this.support.hasEquipmentPanel()) {
			drawEquipmentPanel(context);
		}
		if (this.toolsShown) {
			drawToolsPanel(context);
		}
		if (this.backpackShown) {
			drawBackpackPanel(context);
		} else if (this.emptyShown) {
			drawEmptyPanel(context);
		}
	}

	private void drawEquipmentPanel(DrawContext context) {
		int bgW = bgW();
		int px = gx() + bgW - OVERLAP;
		int py = gy() + EQ_TOP;
		drawFrame(context, px, py, EQ_VISIBLE_W + OVERLAP, EQ_HEIGHT);

		int sx = gx() + bgW + EQ_SLOT_DX;
		for (int i = 0; i < 4; i++) {
			drawSlotFrame(context, sx, gy() + EQ_ARMOR_Y + i * 18);
			if (!this.support.slot(this.support.equipmentSlotIndex(i)).hasStack()) {
				context.drawTexture(ARMOR_ICONS[i], sx, gy() + EQ_ARMOR_Y + i * 18, 0.0F, 0.0F, 16, 16, 16, 16);
			}
		}
		// Garis pemisah antara armor dan slot tambahan (tangan kiri, Elytra).
		int dx = sx - 1;
		context.fill(dx, gy() + EQ_DIVIDER_Y, dx + 18, gy() + EQ_DIVIDER_Y + 1, SHADOW);
		context.fill(dx, gy() + EQ_DIVIDER_Y + 1, dx + 18, gy() + EQ_DIVIDER_Y + 2, WHITE);

		drawSlotFrame(context, sx, gy() + EQ_OFFHAND_Y);
		if (!this.support.slot(this.support.equipmentSlotIndex(PanelSupport.EQUIP_OFFHAND)).hasStack()) {
			context.drawTexture(SHIELD_ICON, sx, gy() + EQ_OFFHAND_Y, 0.0F, 0.0F, 16, 16, 16, 16);
		}
		drawSlotFrame(context, sx, gy() + EQ_ELYTRA_Y);
		if (!this.support.slot(this.support.equipmentSlotIndex(PanelSupport.EQUIP_ELYTRA)).hasStack()) {
			context.drawTexture(ELYTRA_ICON, sx, gy() + EQ_ELYTRA_Y, 0.0F, 0.0F, 16, 16, 16, 16);
		}
	}

	private void drawBackpackPanel(DrawContext context) {
		MinecraftClient client = client();
		int slotsX = gx() + this.panelOffsetX + SIDE_PAD + 1;
		int py = gy() + this.panelTopRel;
		drawFrame(context, frameX(), py, this.panelW + OVERLAP, this.panelH);

		// Judul (nama backpack, bisa sudah diganti di anvil) dan jumlah slot terisi.
		ItemStack worn = client.player != null
				? BackpackSlotHolder.of(client.player).workshop_utilities$getBackpackSlot() : ItemStack.EMPTY;
		Text name = worn.isEmpty() ? Text.translatable("gui.workshop_utilities.panel.backpack") : worn.getName();
		String used = this.support.usedSlots() + "/" + this.layoutCount;
		int gridRight = slotsX - 1 + this.columns * 18;
		int usedW = client.textRenderer.getWidth(used);
		String title = client.textRenderer.trimToWidth(name.getString(), Math.max(20, this.columns * 18 - usedW - 6));
		context.drawText(client.textRenderer, title, slotsX, py + 6, TEXT, false);
		context.drawText(client.textRenderer, used, gridRight - usedW - 1, py + 6, TEXT, false);

		for (int row = 0; row < this.visibleRows; row++) {
			for (int col = 0; col < this.columns; col++) {
				int index = (row + this.scrollRow) * this.columns + col;
				if (index < this.layoutCount) {
					drawSlotFrame(context, slotsX + col * 18, py + HEADER_H + row * 18);
				}
			}
		}
		if (this.scrollable) {
			drawScrollbar(context);
		}
	}

	/** Panel Tools Holder di kiri panel backpack: judul, rak 2 x 4 (7 slot), dan sel ke-8 berisi jumlah terisi ("3/7"). */
	private void drawToolsPanel(DrawContext context) {
		MinecraftClient client = client();
		int px = gx() + this.toolsOffsetX;
		int py = gy() + this.toolsTopRel;
		drawFrame(context, px, py, TOOLS_W, TOOLS_H);
		String title = client.textRenderer.trimToWidth(
				Text.translatable("gui.workshop_utilities.backpack.tools.title").getString(), TOOLS_W - 12);
		context.drawText(client.textRenderer, title, px + TOOLS_PAD + 1, py + 5, TEXT, false);
		for (int i = 0; i < PanelSupport.TOOL_SLOTS; i++) {
			drawSlotFrame(context, px + TOOLS_PAD + 1 + (i % TOOLS_COLS) * 18, py + TOOLS_HEADER_H + 1 + (i / TOOLS_COLS) * 18);
		}
		// Sel ke-8: kotak cekung gelap berisi "terisi/7", ditulis 75% agar muat di dalam sel 18 px.
		int cx = px + TOOLS_PAD + (TOOLS_COLS - 1) * 18;
		int cy = py + TOOLS_HEADER_H + (TOOLS_ROWS - 1) * 18;
		context.fill(cx, cy, cx + 18, cy + 18, TRACK_DARK);
		context.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF2B2B2B);
		int used = this.support.toolsUsed();
		String label = used + "/" + PanelSupport.TOOL_SLOTS;
		float scale = 0.75F;
		int labelWidth = Math.round(client.textRenderer.getWidth(label) * scale);
		context.getMatrices().push();
		context.getMatrices().translate(cx + 1 + (16 - labelWidth) / 2.0F, cy + 1 + (16 - 8 * scale) / 2.0F + 0.5F, 0.0F);
		context.getMatrices().scale(scale, scale, 1.0F);
		context.drawText(client.textRenderer, label, 0, 0, used > 0 ? 0xFFE0E0E0 : 0xFF8B8B8B, false);
		context.getMatrices().pop();
	}

	private void drawEmptyPanel(DrawContext context) {
		MinecraftClient client = client();
		int px = gx() + this.panelOffsetX;
		int py = gy() + this.panelTopRel;
		drawFrame(context, frameX(), py, this.panelW + OVERLAP, this.panelH);
		context.drawText(client.textRenderer, Text.translatable("gui.workshop_utilities.panel.backpack"),
				px + SIDE_PAD + 1, py + 6, TEXT, false);
		for (int i = 0; i < this.emptyLines.size(); i++) {
			context.drawText(client.textRenderer, this.emptyLines.get(i), px + SIDE_PAD + 1, py + HEADER_H + 2 + i * 10, SHADOW, false);
		}
	}

	private void drawScrollbar(DrawContext context) {
		int x = trackX();
		int y = trackY();
		int h = trackHeight();
		// Alur inset (gelap di kiri-atas, putih di kanan-bawah) seperti slot vanilla.
		context.fill(x, y, x + SCROLLBAR_W, y + h, TRACK_DARK);
		context.fill(x + 1, y + 1, x + SCROLLBAR_W, y + h, WHITE);
		context.fill(x + 1, y + 1, x + SCROLLBAR_W - 1, y + h - 1, TRACK);

		int thumbTop = thumbY();
		int thumbBottom = thumbTop + thumbHeight();
		int tx = x + 1;
		int tr = x + SCROLLBAR_W - 1;
		context.fill(tx, thumbTop, tr, thumbBottom, BLACK);
		context.fill(tx, thumbTop, tr - 1, thumbBottom - 1, PANEL);
		context.fill(tx, thumbTop, tr - 1, thumbTop + 1, WHITE);
		context.fill(tx, thumbTop, tx + 1, thumbBottom - 1, WHITE);
		context.fill(tx + 1, thumbBottom - 2, tr - 1, thumbBottom - 1, SHADOW);
		context.fill(tr - 2, thumbTop + 1, tr - 1, thumbBottom - 1, SHADOW);
	}

	/** Bingkai panel bergaya container vanilla: garis hitam, sisi terang kiri-atas, sisi gelap kanan-bawah. */
	private static void drawFrame(DrawContext context, int x, int y, int w, int h) {
		context.fill(x + 1, y, x + w - 1, y + 1, BLACK);
		context.fill(x + 1, y + h - 1, x + w - 1, y + h, BLACK);
		context.fill(x, y + 1, x + 1, y + h - 1, BLACK);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, BLACK);
		context.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
		context.fill(x + 1, y + 1, x + w - 2, y + 3, WHITE);
		context.fill(x + 1, y + 1, x + 3, y + h - 2, WHITE);
		context.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, SHADOW);
		context.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, SHADOW);
	}

	/** Slot 18x18 bergaya vanilla; (itemX, itemY) = posisi item 16x16, bingkainya digambar mengelilinginya. */
	private static void drawSlotFrame(DrawContext context, int itemX, int itemY) {
		int x = itemX - 1;
		int y = itemY - 1;
		context.fill(x, y, x + 18, y + 18, TRACK_DARK);
		context.fill(x + 1, y + 1, x + 18, y + 18, WHITE);
		context.fill(x + 1, y + 1, x + 17, y + 17, TRACK);
	}

	// ------------------------------------------------------------------ tombol buka / tutup backpack

	/**
	 * Tombol kecil di sisi GUI (atau di sisi panel bila terbuka) untuk membuka / menutup panel backpack. Sisi yang menempel ke
	 * GUI / panel lurus; sisi luarnya membulat (selalu di kiri).
	 */
	private class ToggleTab extends PressableWidget {
		ToggleTab() {
			super(0, 0, TAB_SIZE, TAB_SIZE, Text.translatable("gui.workshop_utilities.panel.backpack"));
		}

		@Override
		public void onPress() {
			togglePanel();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int x = this.getX();
			int y = this.getY();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean open = backpackOpen;
			int body = this.isHovered() ? TAB_HOVER : PANEL;
			int fill = open ? TRACK : body;
			// Sisi kiri membulat; sisi kanan menempel ke panel / GUI utama / recipe book.
			context.fill(x + 1, y, x + w, y + 1, BLACK);
			context.fill(x + 1, y + h - 1, x + w, y + h, BLACK);
			context.fill(x, y + 1, x + 1, y + h - 1, BLACK);
			context.fill(x + 1, y + 1, x + w, y + h - 1, fill);
			if (open) {
				context.fill(x + 1, y + 1, x + w, y + 2, TRACK_DARK);
				context.fill(x + 1, y + 1, x + 2, y + h - 1, TRACK_DARK);
				context.fill(x + 2, y + h - 2, x + w, y + h - 1, WHITE);
			} else {
				context.fill(x + 1, y + 1, x + w, y + 2, WHITE);
				context.fill(x + 1, y + 1, x + 2, y + h - 1, WHITE);
				context.fill(x + 2, y + h - 2, x + w, y + h - 1, SHADOW);
			}
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			context.drawTexture(BACKPACK_ICON, x + 1, y + 2, 0.0F, 0.0F, 16, 16, 16, 16);
			RenderSystem.disableBlend();
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}
}
