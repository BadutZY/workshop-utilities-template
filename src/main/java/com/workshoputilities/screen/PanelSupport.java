package com.workshoputilities.screen;

import com.workshoputilities.ModItems;
import com.workshoputilities.backpack.BackpackData;
import com.workshoputilities.backpack.BackpackModuleData;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.elytra.ElytraSlot;
import com.workshoputilities.elytra.ElytraSlotInventory;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.mixin.ScreenHandlerInvoker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logika panel samping yang menempel di GUI block: <b>panel backpack</b> (isi backpack yang sedang dipakai di slot Backpack
 * pemain), <b>panel Tools Holder</b> (7 slot modul Tools Holder milik backpack itu, hanya bila modulnya terpasang), dan
 * <b>panel equipment</b> (4 slot armor, tangan kiri, dan Elytra; hanya untuk block yang fungsinya soal armor / tool / senjata).
 *
 * Kelas ini dipakai dua jalur:
 * <ul>
 * <li>{@link PanelScreenHandler} (Uncrafting, Upgrade, Dis-Enchanting, Upgrade Enchanting Table);</li>
 * <li>block vanilla (Crafting Table, Furnace, Anvil, dst.) lewat mixin, disimpan di {@link PanelHost}.</li>
 * </ul>
 *
 * Semua slot tambahan ditaruh PALING AKHIR (setelah slot inventory pemain) sehingga nomor slot milik block tidak bergeser.
 * Jumlah slot selalu tetap (client dan server tidak pernah berbeda); yang aktif dikirim lewat property. Posisi slot tidak
 * dipakai di server; client yang menempatkannya.
 *
 * Isi backpack dan isi Tools Holder langsung ditulis ke item backpack di slot Backpack setiap ada perubahan, jadi tidak ada
 * data yang hilang walau game ditutup paksa.
 *
 * Panel hanyalah salinan kerja dari data backpack, jadi {@link #refresh()} menyamakannya DUA ARAH setiap tick dan setiap
 * selesai klik slot: perubahan backpack dari luar panel memuat ulang panel (backpack asli selalu menang), dan perubahan
 * panel yang lolos dari listener langsung ditulis ke backpack. Dengan begitu isi panel di semua GUI block (vanilla maupun mod)
 * selalu sama dengan backpack yang sebenarnya.
 */
public final class PanelSupport {
	public static final Logger LOGGER = LoggerFactory.getLogger("workshop_utilities/panel");

	/** Urutan slot equipment (indeks relatif terhadap awal panel equipment). */
	public static final int EQUIP_HEAD = 0;
	public static final int EQUIP_CHEST = 1;
	public static final int EQUIP_LEGS = 2;
	public static final int EQUIP_FEET = 3;
	public static final int EQUIP_OFFHAND = 4;
	public static final int EQUIP_ELYTRA = 5;
	public static final int EQUIP_COUNT = 6;

	/** Jumlah slot panel backpack = slot terbanyak dari semua tier. */
	public static final int MAX_BACKPACK_SLOTS = maxBackpackSlots();
	/** Jumlah slot Tools Holder. */
	public static final int TOOL_SLOTS = BackpackModuleData.TOOL_SLOTS;

	private static final int PROP_BACKPACK = 0;
	private static final int PROP_TOOLS = 1;

	/** Indeks slot offhand di PlayerInventory (36..39 = armor, 40 = tangan kiri). */
	private static final int OFFHAND_INVENTORY_INDEX = 40;
	private static final EquipmentSlot[] ARMOR_ORDER = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private final ScreenHandler handler;
	private final PlayerEntity player;
	private final PlayerInventory playerInventory;
	private final boolean equipment;
	/** Indeks slot pertama inventory pemain di handler (hanya untuk handler vanilla; -1 untuk handler mod). */
	private final int playerStart;

	private int equipStart = -1;
	private int backpackStart = -1;
	private int toolsStart = -1;
	private SimpleInventory panelBackpack;
	private SimpleInventory panelTools;
	/** [0] = jumlah slot backpack yang aktif (0 bila tidak ada), [1] = 1 bila Tools Holder terpasang. Disinkronkan ke client. */
	private final PropertyDelegate props = new ArrayPropertyDelegate(2);
	private boolean bulk = false;
	/** Jumlah slot backpack yang sedang termuat di {@link #panelBackpack} (server saja). */
	private int loaded = 0;
	/**
	 * Server: data kustom (isi + modul) backpack saat terakhir disamakan dengan panel, entah dimuat atau ditulis oleh panel ini.
	 * Bila data backpack yang sebenarnya berbeda dari ini, berarti ada yang mengubahnya dari luar panel dan panel dimuat ulang.
	 */
	private NbtComponent syncedData;
	/** Server: salinan isi panel backpack saat terakhir disamakan; dipakai mendeteksi perubahan yang tidak memicu listener. */
	private final ItemStack[] savedBackpack = emptyStacks(MAX_BACKPACK_SLOTS);
	/** Server: salinan isi panel Tools Holder saat terakhir disamakan. */
	private final ItemStack[] savedTools = emptyStacks(TOOL_SLOTS);
	private boolean reconciling = false;

	public PanelSupport(ScreenHandler handler, PlayerInventory playerInventory, boolean equipment, int playerStart) {
		this.handler = handler;
		this.player = playerInventory.player;
		this.playerInventory = playerInventory;
		this.equipment = equipment;
		this.playerStart = playerStart;
	}

	private static int maxBackpackSlots() {
		int max = 0;
		for (BackpackTier tier : BackpackTier.values()) {
			max = Math.max(max, tier.slots());
		}
		return max;
	}

	private static ItemStack[] emptyStacks(int count) {
		ItemStack[] stacks = new ItemStack[count];
		Arrays.fill(stacks, ItemStack.EMPTY);
		return stacks;
	}

	private static boolean sameData(NbtComponent a, NbtComponent b) {
		return a == b || (a != null && a.equals(b));
	}

	/** Salin {@code count} slot pertama {@code from} ke {@code to} (salinan, karena stack di panel diubah langsung oleh vanilla). */
	private static void snapshot(SimpleInventory from, ItemStack[] to, int count) {
		for (int i = 0; i < count && i < to.length; i++) {
			to[i] = from.getStack(i).copy();
		}
	}

	private static boolean differs(SimpleInventory inventory, ItemStack[] saved, int count) {
		for (int i = 0; i < count && i < saved.length; i++) {
			if (!ItemStack.areEqual(saved[i], inventory.getStack(i))) {
				return true;
			}
		}
		return false;
	}

	private ScreenHandlerInvoker invoker() {
		return (ScreenHandlerInvoker) (Object) this.handler;
	}

	/** True bila {@code handler} adalah salah satu GUI block vanilla yang diberi panel Backpack. */
	public static boolean isVanillaSupported(ScreenHandler handler) {
		return handler instanceof CraftingScreenHandler || handler instanceof AbstractFurnaceScreenHandler
				|| handler instanceof ForgingScreenHandler || handler instanceof GrindstoneScreenHandler
				|| handler instanceof StonecutterScreenHandler || handler instanceof LoomScreenHandler
				|| handler instanceof CartographyTableScreenHandler || handler instanceof EnchantmentScreenHandler
				|| handler instanceof BrewingStandScreenHandler || handler instanceof CrafterScreenHandler
				|| handler instanceof Generic3x3ContainerScreenHandler || handler instanceof HopperScreenHandler
				|| handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler
				|| handler instanceof MerchantScreenHandler || handler instanceof BeaconScreenHandler;
	}

	/** True bila GUI block vanilla ini juga mendapat panel equipment (Anvil, Smithing Table, Grindstone, Enchanting Table). */
	public static boolean vanillaHasEquipment(ScreenHandler handler) {
		return handler instanceof AnvilScreenHandler || handler instanceof SmithingScreenHandler
				|| handler instanceof GrindstoneScreenHandler || handler instanceof EnchantmentScreenHandler;
	}

	/**
	 * Jaring pengaman SISI SERVER: dipanggil dari sendContentUpdates. Injeksi constructor (VanillaHandlerPanelMixin) terbukti
	 * tidak selalu berjalan di handler sisi server (log hanya menunjukkan attach di CLIENT untuk Crafting Table, Furnace, dst.),
	 * sehingga client punya slot panel sementara server tidak: item tidak bisa ditaruh dan isinya tidak pernah terkirim.
	 * Di sini panel dipasang bila belum ada; slot baru langsung tersinkron ke client lewat sinkronisasi slot biasa.
	 */
	public static void attachVanillaIfMissing(ScreenHandler handler) {
		if (!isVanillaSupported(handler) || ((PanelHost) (Object) handler).workshop_utilities$getPanel() != null) {
			return;
		}
		for (Slot slot : handler.slots) {
			if (slot.inventory instanceof PlayerInventory inventory) {
				if (inventory.player.getWorld().isClient) {
					return;
				}
				LOGGER.warn("[panel] server handler {} belum punya panel, dipasang sekarang", handler.getClass().getSimpleName());
				attachVanilla(handler, vanillaHasEquipment(handler));
				return;
			}
		}
	}

	/**
	 * Pasang panel ke handler vanilla: cari slot inventory pemain, tambahkan slot panel, dan simpan di {@link PanelHost}.
	 * Dipanggil dari akhir constructor handler. Tidak berbuat apa-apa bila handler tidak punya slot inventory pemain.
	 */
	public static void attachVanilla(ScreenHandler handler, boolean equipment) {
		PanelHost host = (PanelHost) (Object) handler;
		if (host.workshop_utilities$getPanel() != null) {
			return;
		}
		for (int i = 0; i < handler.slots.size(); i++) {
			Slot slot = handler.slots.get(i);
			if (slot.inventory instanceof PlayerInventory inventory) {
				PanelSupport support = new PanelSupport(handler, inventory, equipment, i);
				support.build();
				host.workshop_utilities$setPanel(support);
				return;
			}
		}
	}

	/** Tambahkan slot panel. Panggil SEKALI di akhir constructor, setelah slot hotbar ditambahkan. */
	public void build() {
		if (this.equipment) {
			this.equipStart = this.handler.slots.size();
			for (int i = 0; i < ARMOR_ORDER.length; i++) {
				invoker().workshop_utilities$addSlot(new ArmorPanelSlot(this.playerInventory, 39 - i, ARMOR_ORDER[i]));
			}
			invoker().workshop_utilities$addSlot(new Slot(this.playerInventory, OFFHAND_INVENTORY_INDEX, 0, 0));
			invoker().workshop_utilities$addSlot(new ElytraSlot(new ElytraSlotInventory(this.player), this.player, 0, 0));
		}

		this.panelBackpack = new SimpleInventory(MAX_BACKPACK_SLOTS);
		this.backpackStart = this.handler.slots.size();
		for (int i = 0; i < MAX_BACKPACK_SLOTS; i++) {
			invoker().workshop_utilities$addSlot(new BackpackPanelSlot(this.panelBackpack, i));
		}
		this.panelTools = new SimpleInventory(TOOL_SLOTS);
		this.toolsStart = this.handler.slots.size();
		for (int i = 0; i < TOOL_SLOTS; i++) {
			invoker().workshop_utilities$addSlot(new ToolPanelSlot(this.panelTools, i));
		}
		invoker().workshop_utilities$addProperties(this.props);

		// Isi dimuat dulu, baru listener pemantau perubahan dipasang (agar proses memuat tidak ikut menyimpan).
		refresh();
		LOGGER.info("[panel] attach {} side={} slots={} equipStart={} backpackStart={} toolsStart={} playerStart={} loaded={} used={}",
				this.handler.getClass().getSimpleName(), this.player.getWorld().isClient ? "CLIENT" : "SERVER", this.handler.slots.size(),
				this.equipStart, this.backpackStart, this.toolsStart, this.playerStart, this.loaded, usedSlots());
		this.panelBackpack.addListener(sender -> {
			if (!this.bulk) {
				saveBackpack();
			}
		});
		this.panelTools.addListener(sender -> {
			if (!this.bulk) {
				saveTools();
			}
		});
	}

	// ------------------------------------------------------------------ info untuk client

	public boolean hasEquipmentPanel() {
		return this.equipStart >= 0;
	}

	/** Indeks slot handler untuk slot equipment {@code which} (EQUIP_*). */
	public int equipmentSlotIndex(int which) {
		return this.equipStart + which;
	}

	/** Jumlah slot backpack yang aktif; 0 berarti pemain tidak memakai backpack. */
	public int slotCount() {
		return this.props.get(PROP_BACKPACK);
	}

	/** Indeks slot handler untuk slot panel backpack ke-{@code i} (0 based). */
	public int slotIndex(int i) {
		return this.backpackStart + i;
	}

	/** True bila backpack yang dipakai punya Tools Holder Module terpasang (slot Tools Holder aktif). */
	public boolean toolsActive() {
		return this.props.get(PROP_TOOLS) == 1;
	}

	/** Indeks slot handler untuk slot Tools Holder ke-{@code i} (0..{@link #TOOL_SLOTS}-1). */
	public int toolSlotIndex(int i) {
		return this.toolsStart + i;
	}

	/** Jumlah slot Tools Holder yang terisi (untuk label "3/7"). */
	public int toolsUsed() {
		int used = 0;
		for (int i = 0; i < TOOL_SLOTS; i++) {
			if (!this.panelTools.getStack(i).isEmpty()) {
				used++;
			}
		}
		return used;
	}

	/** Slot handler untuk indeks {@code index}. */
	public Slot slot(int index) {
		return this.handler.slots.get(index);
	}

	/** Tier backpack yang dipakai berdasarkan jumlah slot aktif, atau null bila tidak ada. */
	public BackpackTier tier() {
		int count = slotCount();
		for (BackpackTier tier : BackpackTier.values()) {
			if (tier.slots() == count) {
				return tier;
			}
		}
		return null;
	}

	/** Jumlah slot backpack yang terisi (untuk label "12/36"). */
	public int usedSlots() {
		int used = 0;
		int count = Math.min(slotCount(), MAX_BACKPACK_SLOTS);
		for (int i = 0; i < count; i++) {
			if (!this.panelBackpack.getStack(i).isEmpty()) {
				used++;
			}
		}
		return used;
	}

	// ------------------------------------------------------------------ sinkronisasi data backpack

	private ItemStack liveBackpack() {
		return BackpackSlotHolder.of(this.player).workshop_utilities$getBackpackSlot();
	}

	/**
	 * Server: samakan panel dengan backpack yang sedang dipakai, DUA ARAH, dan dipanggil tiap tick selama GUI terbuka.
	 * <ul>
	 * <li>Backpack berubah dari luar panel (tier berganti / dilepas, isi atau modul diubah oleh GUI Backpack, command, dsb.):
	 * panel dimuat ulang dari backpack, jadi backpack yang asli selalu menang.</li>
	 * <li>Panel berubah tanpa memicu listener (stack diubah langsung, dsb.): backpack langsung diperbarui.</li>
	 * </ul>
	 * Slot yang berubah otomatis terkirim ke client lewat sinkronisasi slot biasa.
	 */
	public void refresh() {
		if (this.panelBackpack == null || this.player.getWorld().isClient || this.reconciling) {
			return;
		}
		this.reconciling = true;
		try {
			ItemStack live = liveBackpack();
			int want = live.getItem() instanceof BackpackItem item ? item.getTier().slots() : 0;
			NbtComponent data = want > 0 ? live.get(DataComponentTypes.CUSTOM_DATA) : null;
			if (want != this.loaded || !sameData(data, this.syncedData)) {
				reload(live, want, data);
			} else if (want > 0) {
				if (differs(this.panelBackpack, this.savedBackpack, want)) {
					saveBackpack();
				}
				if (toolsActive() && differs(this.panelTools, this.savedTools, TOOL_SLOTS)) {
					saveTools();
				}
			}
		} finally {
			this.reconciling = false;
		}
	}

	/** Server: muat ulang seluruh isi panel (backpack + Tools Holder) dari backpack {@code live}. */
	private void reload(ItemStack live, int want, NbtComponent data) {
		this.bulk = true;
		try {
			for (int i = 0; i < MAX_BACKPACK_SLOTS; i++) {
				this.panelBackpack.setStack(i, ItemStack.EMPTY);
			}
			for (int i = 0; i < TOOL_SLOTS; i++) {
				this.panelTools.setStack(i, ItemStack.EMPTY);
			}
			boolean holder = false;
			if (want > 0) {
				DefaultedList<ItemStack> contents = DefaultedList.ofSize(want, ItemStack.EMPTY);
				BackpackData.load(live, contents, this.player.getRegistryManager());
				for (int i = 0; i < want; i++) {
					this.panelBackpack.setStack(i, contents.get(i));
				}
				BackpackModuleData.State state = BackpackModuleData.load(live, this.player.getRegistryManager());
				holder = state.items.get(BackpackModuleData.sideIndex(1)).isOf(ModItems.TOOLS_HOLDER_MODULE);
				if (holder) {
					for (int i = 0; i < TOOL_SLOTS; i++) {
						this.panelTools.setStack(i, state.items.get(BackpackModuleData.toolIndex(i)));
					}
				}
			}
			this.loaded = want;
			this.props.set(PROP_BACKPACK, want);
			this.props.set(PROP_TOOLS, holder ? 1 : 0);
			this.syncedData = data;
			snapshot(this.panelBackpack, this.savedBackpack, MAX_BACKPACK_SLOTS);
			snapshot(this.panelTools, this.savedTools, TOOL_SLOTS);
		} finally {
			this.bulk = false;
		}
	}

	/** Server: tulis isi panel backpack ke item backpack di slot Backpack pemain. */
	private void saveBackpack() {
		if (this.player.getWorld().isClient || this.loaded <= 0) {
			return;
		}
		BackpackSlotHolder holder = BackpackSlotHolder.of(this.player);
		ItemStack live = holder.workshop_utilities$getBackpackSlot();
		if (!(live.getItem() instanceof BackpackItem item) || item.getTier().slots() != this.loaded) {
			return;
		}
		DefaultedList<ItemStack> contents = DefaultedList.ofSize(this.loaded, ItemStack.EMPTY);
		for (int i = 0; i < this.loaded; i++) {
			contents.set(i, this.panelBackpack.getStack(i));
		}
		BackpackData.save(live, contents, this.player.getRegistryManager());
		this.syncedData = live.get(DataComponentTypes.CUSTOM_DATA);
		snapshot(this.panelBackpack, this.savedBackpack, this.loaded);
		// Kirim isi terbaru ke client (tooltip "terisi x/y") dan pemain lain.
		holder.workshop_utilities$syncBackpackSlot();
	}

	/**
	 * Server: tulis isi Tools Holder ke data modul backpack. Data modul dimuat ulang dulu dan hanya 7 slot tools yang
	 * diganti, jadi modul lain, furnace, dan timernya tidak tersentuh.
	 */
	private void saveTools() {
		if (this.player.getWorld().isClient || this.loaded <= 0 || !toolsActive()) {
			return;
		}
		BackpackSlotHolder holder = BackpackSlotHolder.of(this.player);
		ItemStack live = holder.workshop_utilities$getBackpackSlot();
		if (!(live.getItem() instanceof BackpackItem item) || item.getTier().slots() != this.loaded) {
			return;
		}
		BackpackModuleData.State state = BackpackModuleData.load(live, this.player.getRegistryManager());
		if (!state.items.get(BackpackModuleData.sideIndex(1)).isOf(ModItems.TOOLS_HOLDER_MODULE)) {
			return;
		}
		for (int i = 0; i < TOOL_SLOTS; i++) {
			state.items.set(BackpackModuleData.toolIndex(i), this.panelTools.getStack(i));
		}
		BackpackModuleData.save(live, state, this.player.getRegistryManager());
		this.syncedData = live.get(DataComponentTypes.CUSTOM_DATA);
		snapshot(this.panelTools, this.savedTools, TOOL_SLOTS);
		holder.workshop_utilities$syncBackpackSlot();
	}

	/** Simpan semuanya (dipanggil saat GUI ditutup). */
	public void saveAll() {
		// Rekonsiliasi dulu: bila backpack diubah dari luar, backpack yang menang (panel tidak menimpanya dengan data lama).
		refresh();
		saveBackpack();
		saveTools();
	}

	// ------------------------------------------------------------------ shift-click

	/**
	 * Shift-click pada slot equipment (armor, tangan kiri, Elytra) memindahkannya ke inventory pemain, seperti di inventory
	 * vanilla. Mengembalikan null bila {@code slotIndex} bukan slot equipment (artinya quickMove block berjalan seperti biasa).
	 */
	public ItemStack quickMoveEquipment(PlayerEntity who, int slotIndex, int inventoryStart, int hotbarEnd) {
		if (this.equipStart < 0 || slotIndex < this.equipStart || slotIndex >= this.equipStart + EQUIP_COUNT) {
			return null;
		}
		Slot slot = this.handler.slots.get(slotIndex);
		if (!slot.hasStack() || !slot.canTakeItems(who)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();
		if (!invoker().workshop_utilities$insertItem(stack, inventoryStart, hotbarEnd, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		return original;
	}

	/** True bila {@code index} adalah salah satu slot panel (equipment, backpack, atau Tools Holder). */
	private boolean isPanelSlot(int index) {
		if (this.backpackStart < 0 || index < 0 || index >= this.handler.slots.size()) {
			return false;
		}
		int first = this.equipStart >= 0 ? this.equipStart : this.backpackStart;
		return index >= first;
	}

	/**
	 * Shift-click pada slot panel di handler vanilla. Equipment: kembali ke inventory pemain. Slot backpack / Tools Holder:
	 * item "dianggap" berasal dari slot inventory pemain pertama sehingga quickMove milik block sendiri yang memilih tujuannya
	 * (input furnace, bahan bakar, slot kiri / kanan anvil, dan seterusnya); bila block tidak menerimanya, item pindah ke
	 * hotbar seperti item inventory biasa. Mengembalikan true bila klik ini sudah ditangani di sini.
	 */
	public boolean interceptQuickMove(PlayerEntity who, int slotIndex) {
		if (this.playerStart < 0) {
			return false;
		}
		if (!isPanelSlot(slotIndex)) {
			return needsBoundedQuickMove() && boundedQuickMove(who, slotIndex);
		}
		Slot slot = this.handler.slots.get(slotIndex);
		if (!slot.hasStack() || !slot.canTakeItems(who)) {
			return true;
		}
		if (this.equipStart >= 0 && slotIndex >= this.equipStart && slotIndex < this.equipStart + EQUIP_COUNT) {
			quickMoveEquipment(who, slotIndex, this.playerStart, this.playerStart + 36);
			return true;
		}
		Slot real = this.handler.slots.get(this.playerStart);
		this.handler.slots.set(this.playerStart, slot);
		try {
			ItemStack moved = this.handler.quickMove(who, this.playerStart);
			int guard = 0;
			while (!moved.isEmpty() && guard++ < 512 && slot.hasStack()
					&& ItemStack.areItemsAndComponentsEqual(slot.getStack(), moved)) {
				moved = this.handler.quickMove(who, this.playerStart);
			}
		} finally {
			this.handler.slots.set(this.playerStart, real);
		}
		return true;
	}

	/**
	 * True untuk handler yang quickMove-nya memakai {@code slots.size()} sebagai batas akhir pencarian slot tujuan (Chest, Barrel,
	 * Hopper, Shulker Box). Handler lain (Dispenser, Crafting Table, Furnace, Merchant, Beacon, dst.) memakai angka tetap sehingga aman dari slot panel.
	 */
	private boolean needsBoundedQuickMove() {
		return this.handler instanceof GenericContainerScreenHandler || this.handler instanceof HopperScreenHandler
				|| this.handler instanceof ShulkerBoxScreenHandler;
	}

	/**
	 * Shift-click pada slot BIASA (isi container atau inventory pemain) di Chest / Barrel / Hopper / Shulker Box. Slot panel ditaruh di akhir
	 * daftar slot, dan quickMove vanilla untuk handler ini memindahkan item dari container ke "slot terakhir sampai akhir daftar",
	 * sehingga tanpa penanganan ini item dari chest akan masuk ke panel backpack, bukan ke inventory pemain. Di sini slot panel
	 * disembunyikan sementara selama quickMove berjalan (client dan server melakukan hal yang sama, jadi prediksi client tetap
	 * cocok), lalu dikembalikan. Mengembalikan true bila klik sudah ditangani.
	 */
	private boolean boundedQuickMove(PlayerEntity who, int slotIndex) {
		List<Slot> slots = this.handler.slots;
		int keep = this.playerStart + 36;
		if (slotIndex < 0 || slotIndex >= keep || keep >= slots.size()) {
			return false;
		}
		Slot slot = slots.get(slotIndex);
		if (!slot.canTakeItems(who)) {
			return true;
		}
		List<Slot> hidden = new ArrayList<>();
		for (int i = keep; i < slots.size(); i++) {
			hidden.add(slots.get(i));
		}
		for (int i = slots.size() - 1; i >= keep; i--) {
			slots.remove(i);
		}
		try {
			ItemStack moved = this.handler.quickMove(who, slotIndex);
			int guard = 0;
			while (!moved.isEmpty() && guard++ < 512 && ItemStack.areItemsAndComponentsEqual(slot.getStack(), moved)) {
				moved = this.handler.quickMove(who, slotIndex);
			}
		} finally {
			for (Slot restore : hidden) {
				slots.add(restore);
			}
		}
		refresh();
		return true;
	}

	// ------------------------------------------------------------------ slot

	/** Slot armor seperti inventory vanilla: 1 item, hanya armor yang cocok, dan terkunci oleh Curse of Binding. */
	private static class ArmorPanelSlot extends Slot {
		private final EquipmentSlot equipment;

		ArmorPanelSlot(Inventory inventory, int index, EquipmentSlot equipment) {
			super(inventory, index, 0, 0);
			this.equipment = equipment;
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			Equipment armor = Equipment.fromStack(stack);
			return armor != null && this.equipment == armor.getSlotType();
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			ItemStack stack = this.getStack();
			if (!stack.isEmpty() && !player.isCreative()
					&& EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE)) {
				return false;
			}
			return super.canTakeItems(player);
		}
	}

	/** Slot isi backpack di panel: tas tidak boleh dimasukkan ke dalam tas, dan hanya slot yang aktif yang bisa dipakai. */
	private class BackpackPanelSlot extends Slot {
		private final int panelIndex;

		BackpackPanelSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
			this.panelIndex = index;
		}

		private boolean active() {
			return this.panelIndex < PanelSupport.this.props.get(PROP_BACKPACK);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return active() && !(stack.getItem() instanceof BackpackItem);
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return active();
		}
	}

	/** Slot Tools Holder di panel: hanya tools / senjata / sejenisnya (aturan yang sama dengan GUI Backpack), satu item per slot. */
	private class ToolPanelSlot extends Slot {
		ToolPanelSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return toolsActive() && BackpackScreenHandler.isHoldableTool(stack);
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return toolsActive();
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}
	}
}
