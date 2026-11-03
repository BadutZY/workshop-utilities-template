package com.workshoputilities.screen;

import com.workshoputilities.ModItems;
import com.workshoputilities.backpack.BackpackData;
import com.workshoputilities.backpack.BackpackModuleData;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.backpack.FloorSleep;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.uncraft.UncraftRecipes;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BrushItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Isi Backpack. Slot tas ada di indeks 0..N-1, lalu 27 slot inventory pemain, lalu 9 slot hotbar.
 *
 * Setelah slot hotbar ada slot tambahan untuk modul tas: 5 slot modul (Crafting, Furnace, Blast Furnace, Smoker, Uncrafting;
 * item modul khusus mod ini, tiap jenis hanya boleh satu), 9 slot grid + 1 slot hasil crafting, 3 furnace (input, bahan bakar,
 * hasil), dan modul Uncrafting (1 slot input + 9 slot hasil, perilakunya sama dengan Uncrafting Table). Slot tambahan
 * hanya aktif bila modulnya terpasang; posisinya diatur oleh BackpackScreen.
 *
 * Slot modul samping (kiri GUI) ada paling akhir setelah slot hasil Uncrafting: slot 0 untuk Bed Module (tombol Sleep,
 * {@link #BUTTON_SLEEP}, melewati malam tanpa bed dan tanpa mengubah spawn point) dan slot 1 untuk Tools Holder Module,
 * disusul {@link BackpackModuleData#TOOL_SLOTS} slot isi Tools Holder (tools, senjata, dan sejenisnya) yang bisa
 * ditampilkan / disembunyikan lewat tombol {@link #BUTTON_TOOLS_TOGGLE}.
 *
 * Isi tas disimpan langsung ke ItemStack tas (custom_data) setiap ada perubahan, jadi tidak ada data yang hilang
 * walau game ditutup paksa. Slot tempat tas yang sedang dibuka dikunci (tidak bisa diambil / dipindah) dan
 * tas tidak bisa dimasukkan ke dalam tas.
 */
public class BackpackScreenHandler extends ScreenHandler {
	/** Nilai {@code lockedSlot} bila tas dibuka dari slot Backpack khusus (tidak ada slot hotbar yang perlu dikunci). */
	public static final int WORN_SLOT = -2;

	public static final int BUTTON_QUICK_STACK = 0;
	public static final int BUTTON_SORT = 1;
	public static final int BUTTON_SORT_INVENTORY = 2;
	public static final int BUTTON_TAKE_ALL = 3;
	public static final int BUTTON_DEPOSIT_ALL = 4;
	/** Tombol pilihan resep modul Uncrafting (sama dengan tombol Uncrafting Table). */
	public static final int BUTTON_UNCRAFT_PREVIOUS = 5;
	public static final int BUTTON_UNCRAFT_NEXT = 6;
	/** Lompat 10 pilihan sekaligus (dipakai saat Shift ditekan). */
	public static final int BUTTON_UNCRAFT_PREVIOUS_10 = 7;
	public static final int BUTTON_UNCRAFT_NEXT_10 = 8;
	/** Tombol tidur modul Bed: lewati malam sampai pagi tanpa bed, spawn point tidak berubah. */
	public static final int BUTTON_SLEEP = 9;
	/** Tombol modul Tools Holder: tampilkan / sembunyikan 7 slot tools. Statusnya disimpan di tas. */
	public static final int BUTTON_TOOLS_TOGGLE = 10;

	public static final int MODULE_COUNT = BackpackModuleData.MODULES;
	public static final int TYPE_CRAFTING = 0;
	public static final int TYPE_FURNACE = 1;
	public static final int TYPE_BLAST_FURNACE = 2;
	public static final int TYPE_SMOKER = 3;
	public static final int TYPE_UNCRAFTING = 4;
	private static final int FURNACES = BackpackModuleData.FURNACES;
	private static final int PROPS = 4;
	private static final int SAVE_INTERVAL = 20;
	private static final long MAX_CATCH_UP = 2_000_000L;

	private final BackpackTier tier;
	private final int backpackSlots;
	private final Inventory backpack;
	private final PlayerEntity player;
	private final ItemStack backpackStack;
	/** True bila tas yang dibuka berada di slot Backpack khusus pemain (bukan di inventory biasa). */
	private final boolean worn;
	/** [0] = indeks slot inventory pemain yang dikunci (tas yang sedang dibuka), -1 bila tidak ada. */
	private final PropertyDelegate properties;
	private boolean bulk = false;

	// ---- modul tas
	/** Isi 4 slot modul. */
	private final SimpleInventory modules;
	/** Slot modul samping di kiri GUI: [0] Bed Module, [1] Tools Holder Module. */
	private final SimpleInventory sideModules;
	/** Isi Tools Holder Module: 7 slot untuk tools, senjata, dan sejenisnya. */
	private final SimpleInventory tools;
	/** [0] = 1 bila slot tools disembunyikan, 0 bila ditampilkan (bawaan). Disinkronkan ke client dan disimpan di tas. */
	private final PropertyDelegate toolsProps = new ArrayPropertyDelegate(1);
	/** 3 furnace x (input, bahan bakar, hasil). */
	private final SimpleInventory smelt;
	private final CraftingInventory craftGrid = new CraftingInventory(this, 3, 3);
	private final CraftingResultInventory craftResult = new CraftingResultInventory();
	/** Per furnace: [0] burnTime, [1] fuelTime, [2] cookTime, [3] cookTimeTotal (disinkronkan ke client). */
	private final PropertyDelegate smeltProps = new ArrayPropertyDelegate(FURNACES * PROPS);

	// ---- modul Uncrafting (logikanya sama dengan UncraftingScreenHandler; tidak disimpan ke tas, isinya dikembalikan saat GUI ditutup)
	/** 1 slot input; setiap perubahan memicu penghitungan ulang preview. */
	private final SimpleInventory uncraftInput = new SimpleInventory(1) {
		@Override
		public void markDirty() {
			super.markDirty();
			BackpackScreenHandler.this.onContentChanged(this);
		}
	};
	/** 9 slot hasil (grid 3x3). */
	private final SimpleInventory uncraftOutput = new SimpleInventory(9);
	/** [0] = indeks resep terpilih, [1] = jumlah pilihan resep (disinkronkan ke client). */
	private final PropertyDelegate uncraftProps = new ArrayPropertyDelegate(2);
	private List<UncraftRecipes.Option> uncraftOptions = List.of();
	private int uncraftSelected = 0;
	private boolean uncraftCommitted = false;
	private boolean uncraftSuppress = false;
	private Item uncraftLastItem = null;
	private final float[] smeltXp = new float[FURNACES];
	private long lastSmeltTime = -1L;
	private boolean simulating = false;

	/** Sisi client: isi tas dikirim server lewat sinkronisasi slot biasa. */
	public BackpackScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, BackpackTier tier) {
		this(type, syncId, playerInventory, tier, ItemStack.EMPTY, -1);
	}

	/** Sisi server: membuka tas {@code stack}; {@code lockedSlot} = indeks inventory pemain tempat tas itu berada. */
	public BackpackScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, BackpackTier tier,
								 ItemStack stack, int lockedSlot) {
		super(type, syncId);
		this.tier = tier;
		this.backpackSlots = tier.slots();
		this.player = playerInventory.player;
		this.backpackStack = stack;
		this.worn = lockedSlot == WORN_SLOT;
		this.properties = new ArrayPropertyDelegate(1);
		this.properties.set(0, lockedSlot);

		SimpleInventory inventory = new SimpleInventory(this.backpackSlots);
		if (!stack.isEmpty()) {
			DefaultedList<ItemStack> loaded = DefaultedList.ofSize(this.backpackSlots, ItemStack.EMPTY);
			BackpackData.load(stack, loaded, this.player.getRegistryManager());
			for (int i = 0; i < loaded.size(); i++) {
				inventory.setStack(i, loaded.get(i));
			}
			inventory.addListener(sender -> {
				if (!this.bulk) {
					saveToStack();
				}
			});
		}
		this.backpack = inventory;

		this.modules = new SimpleInventory(MODULE_COUNT);
		this.sideModules = new SimpleInventory(BackpackModuleData.SIDE_MODULES);
		this.tools = new SimpleInventory(BackpackModuleData.TOOL_SLOTS);
		this.smelt = new SimpleInventory(FURNACES * 3) {
			@Override
			public void setStack(int slot, ItemStack stack) {
				ItemStack old = this.getStack(slot);
				boolean same = !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(old, stack);
				super.setStack(slot, stack);
				// Seperti furnace vanilla: mengganti item input mengulang progress masak dari nol.
				if (slot % 3 == 0 && !same && !BackpackScreenHandler.this.player.getWorld().isClient) {
					BackpackScreenHandler.this.smeltProps.set(slot / 3 * PROPS + 2, 0);
				}
			}
		};
		if (!stack.isEmpty()) {
			BackpackModuleData.State state = BackpackModuleData.load(stack, this.player.getRegistryManager());
			for (int i = 0; i < MODULE_COUNT; i++) {
				this.modules.setStack(i, state.items.get(BackpackModuleData.moduleIndex(i)));
			}
			for (int i = 0; i < BackpackModuleData.SIDE_MODULES; i++) {
				this.sideModules.setStack(i, state.items.get(BackpackModuleData.sideIndex(i)));
			}
			for (int i = 0; i < BackpackModuleData.TOOL_SLOTS; i++) {
				this.tools.setStack(i, state.items.get(BackpackModuleData.toolIndex(i)));
			}
			this.toolsProps.set(0, state.toolsShown ? 0 : 1);
			for (int i = 0; i < FURNACES * 3; i++) {
				this.smelt.setStack(i, state.items.get(BackpackModuleData.smeltIndex(i)));
			}
			for (int i = 0; i < state.timers.length; i++) {
				this.smeltProps.set(i, state.timers[i]);
			}
			System.arraycopy(state.xp, 0, this.smeltXp, 0, FURNACES);
			this.lastSmeltTime = state.time;
			this.modules.addListener(sender -> {
				if (!this.bulk && !this.simulating) {
					saveToStack();
				}
			});
			this.sideModules.addListener(sender -> {
				if (!this.bulk && !this.simulating) {
					saveToStack();
				}
			});
			this.tools.addListener(sender -> {
				if (!this.bulk && !this.simulating) {
					saveToStack();
				}
			});
			this.smelt.addListener(sender -> {
				if (!this.bulk && !this.simulating) {
					saveToStack();
				}
			});
		}

		int width = tier.guiWidth();
		int playerX = (width - 162) / 2 + 1;
		int playerY = tier.rows() * 18 + 31;

		for (int row = 0; row < tier.rows(); row++) {
			for (int col = 0; col < tier.columns(); col++) {
				this.addSlot(new BackpackSlot(this.backpack, col + row * tier.columns(), 8 + col * 18, 18 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new PlayerSlot(playerInventory, col + row * 9 + 9, playerX + col * 18, playerY + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new PlayerSlot(playerInventory, col, playerX + col * 18, playerY + 58));
		}
		// Slot modul, grid + hasil crafting, dan furnace. Urutan ini dipakai oleh moduleSlot() dst.
		for (int i = 0; i < MODULE_COUNT; i++) {
			this.addSlot(new ModuleSlot(this.modules, i));
		}
		for (int i = 0; i < 9; i++) {
			this.addSlot(new CraftGridSlot(this.craftGrid, i));
		}
		this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftGrid, this.craftResult, 0, 0, 0));
		for (int f = 0; f < FURNACES; f++) {
			for (int role = 0; role < 3; role++) {
				this.addSlot(new SmeltSlot(this.smelt, f * 3 + role, f, role));
			}
		}
		this.addSlot(new UncraftInputSlot(this.uncraftInput, 0));
		for (int i = 0; i < 9; i++) {
			this.addSlot(new UncraftOutputSlot(this.uncraftOutput, i));
		}
		// Slot modul samping (kiri GUI): paling akhir agar indeks slot lain tidak bergeser. Lihat bedSlot() dan toolsHolderSlot().
		this.addSlot(new SideModuleSlot(this.sideModules, 0, ModItems.BED_MODULE));
		this.addSlot(new SideModuleSlot(this.sideModules, 1, ModItems.TOOLS_HOLDER_MODULE));
		for (int i = 0; i < BackpackModuleData.TOOL_SLOTS; i++) {
			this.addSlot(new ToolSlot(this.tools, i));
		}
		this.addProperties(this.properties);
		this.addProperties(this.smeltProps);
		this.addProperties(this.uncraftProps);
		this.addProperties(this.toolsProps);

		// Susul smelting yang berjalan selagi tas ditutup (server saja).
		tickSmelting();
	}

	// ------------------------------------------------------------------ modul: indeks & info (dipakai client)

	/** Jenis modul untuk item ini (TYPE_*), atau -1 bila bukan modul. */
	public static int moduleTypeOf(ItemStack stack) {
		if (stack.isOf(ModItems.CRAFTING_MODULE)) {
			return TYPE_CRAFTING;
		}
		if (stack.isOf(ModItems.FURNACE_MODULE)) {
			return TYPE_FURNACE;
		}
		if (stack.isOf(ModItems.BLAST_FURNACE_MODULE)) {
			return TYPE_BLAST_FURNACE;
		}
		if (stack.isOf(ModItems.SMOKER_MODULE)) {
			return TYPE_SMOKER;
		}
		if (stack.isOf(ModItems.UNCRAFTING_MODULE)) {
			return TYPE_UNCRAFTING;
		}
		return -1;
	}

	/** Indeks slot handler untuk slot modul ke-{@code cell} (0..4). */
	public int moduleSlot(int cell) {
		return hotbarEnd() + cell;
	}

	public int craftGridSlot(int index) {
		return hotbarEnd() + MODULE_COUNT + index;
	}

	public int craftResultSlot() {
		return hotbarEnd() + MODULE_COUNT + 9;
	}

	/** {@code furnace}: 0 Furnace, 1 Blast Furnace, 2 Smoker. {@code role}: 0 input, 1 bahan bakar, 2 hasil. */
	public int smeltSlot(int furnace, int role) {
		return hotbarEnd() + MODULE_COUNT + 10 + furnace * 3 + role;
	}

	/** Slot input modul Uncrafting. */
	public int uncraftInputSlot() {
		return hotbarEnd() + MODULE_COUNT + 10 + FURNACES * 3;
	}

	/** Slot hasil modul Uncrafting ke-{@code index} (0..8, grid 3x3 baris demi baris). */
	public int uncraftOutputSlot(int index) {
		return uncraftInputSlot() + 1 + index;
	}

	/** Slot modul samping di kiri GUI (Bed Module). */
	public int bedSlot() {
		return uncraftOutputSlot(8) + 1;
	}

	/** True bila Bed Module terpasang di slot modul samping. */
	public boolean hasBedModule() {
		return this.sideModules.getStack(0).isOf(ModItems.BED_MODULE);
	}

	/** Slot modul samping untuk Tools Holder Module (di bawah slot Bed Module). */
	public int toolsHolderSlot() {
		return bedSlot() + 1;
	}

	/** Slot isi Tools Holder ke-{@code index} (0..{@link BackpackModuleData#TOOL_SLOTS}-1). */
	public int toolSlot(int index) {
		return toolsHolderSlot() + 1 + index;
	}

	/** True bila indeks slot handler {@code slotIndex} adalah salah satu slot isi Tools Holder. */
	public boolean isToolSlot(int slotIndex) {
		return slotIndex >= toolSlot(0) && slotIndex < toolSlot(BackpackModuleData.TOOL_SLOTS);
	}

	/** True bila Tools Holder Module terpasang di slot modul samping. */
	public boolean hasToolsHolder() {
		return this.sideModules.getStack(1).isOf(ModItems.TOOLS_HOLDER_MODULE);
	}

	/** True bila slot isi Tools Holder sedang ditampilkan (pilihan pemain; hanya berarti bila modulnya terpasang). */
	public boolean toolsShown() {
		return this.toolsProps.get(0) == 0;
	}

	/** Jumlah slot Tools Holder yang terisi (untuk label "3/7"). */
	public int toolsUsed() {
		int used = 0;
		for (int i = 0; i < BackpackModuleData.TOOL_SLOTS; i++) {
			if (!this.tools.getStack(i).isEmpty()) {
				used++;
			}
		}
		return used;
	}

	/** Tools Holder tidak boleh dilepas selama slot isinya masih berisi item. */
	public boolean toolsBusy() {
		return toolsUsed() > 0;
	}

	/** Item yang boleh masuk slot Tools Holder: alat (pickaxe, axe, shovel, hoe), pedang, busur / crossbow, trident, mace, perisai, pancing, dan alat sejenis. */
	public static boolean isHoldableTool(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof ToolItem || item instanceof RangedWeaponItem || item instanceof TridentItem
				|| item instanceof MaceItem || item instanceof ShieldItem || item instanceof FishingRodItem
				|| item instanceof ShearsItem || item instanceof FlintAndSteelItem || item instanceof BrushItem;
	}

	private void toggleTools() {
		if (hasToolsHolder()) {
			this.toolsProps.set(0, toolsShown() ? 1 : 0);
		}
	}

	/** Indeks resep Uncrafting yang sedang dipilih (0 based). */
	public int getUncraftSelected() {
		return this.uncraftProps.get(0);
	}

	/** Jumlah pilihan resep untuk item di slot input Uncrafting (0 bila tidak ada). */
	public int getUncraftTotal() {
		return this.uncraftProps.get(1);
	}

	/** Jenis modul yang terpasang di slot modul ke-{@code cell}, atau -1 bila kosong. */
	public int moduleTypeAt(int cell) {
		return moduleTypeOf(this.modules.getStack(cell));
	}

	public boolean hasModule(int type) {
		for (int i = 0; i < MODULE_COUNT; i++) {
			if (moduleTypeOf(this.modules.getStack(i)) == type) {
				return true;
			}
		}
		return false;
	}

	/** Modul tidak boleh dilepas selama slot-slot di panelnya masih berisi item. */
	private boolean moduleBusy(int type) {
		if (type == TYPE_UNCRAFTING) {
			return !this.uncraftInput.getStack(0).isEmpty() || !uncraftOutputsEmpty();
		}
		if (type == TYPE_CRAFTING) {
			for (int i = 0; i < 9; i++) {
				if (!this.craftGrid.getStack(i).isEmpty()) {
					return true;
				}
			}
			return false;
		}
		int furnace = type - 1;
		for (int role = 0; role < 3; role++) {
			if (!this.smelt.getStack(furnace * 3 + role).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean isBurning(int furnace) {
		return this.smeltProps.get(furnace * PROPS) > 0;
	}

	/** Sisa api 0..1 (untuk ikon api), sama dengan furnace vanilla. */
	public float burnFraction(int furnace) {
		int total = this.smeltProps.get(furnace * PROPS + 1);
		if (total == 0) {
			total = 200;
		}
		return MathHelper.clamp(this.smeltProps.get(furnace * PROPS) / (float) total, 0.0F, 1.0F);
	}

	/** Progress masak 0..1 (untuk panah). */
	public float cookFraction(int furnace) {
		int cook = this.smeltProps.get(furnace * PROPS + 2);
		int total = this.smeltProps.get(furnace * PROPS + 3);
		return total != 0 && cook != 0 ? MathHelper.clamp(cook / (float) total, 0.0F, 1.0F) : 0.0F;
	}

	public BackpackTier getTier() {
		return this.tier;
	}

	public int getBackpackSlots() {
		return this.backpackSlots;
	}

	/** Jumlah slot tas yang terisi (untuk label "12/36"). */
	public int getUsedSlots() {
		int used = 0;
		for (int i = 0; i < this.backpackSlots; i++) {
			if (this.slots.get(i).hasStack()) {
				used++;
			}
		}
		return used;
	}

	/**
	 * Stack tas yang sedang diedit. Untuk tas di slot Backpack, objeknya diambil ulang dari pemain setiap kali karena
	 * sinkronisasi ke client mengganti objek ItemStack di slot dengan salinan baru.
	 */
	private ItemStack liveStack() {
		return this.worn ? BackpackSlotHolder.of(this.player).workshop_utilities$getBackpackSlot() : this.backpackStack;
	}

	private void saveToStack() {
		ItemStack target = liveStack();
		if (this.player.getWorld().isClient || this.backpackStack.isEmpty() || target.isEmpty()
				|| !(target.getItem() instanceof BackpackItem)) {
			return;
		}
		DefaultedList<ItemStack> contents = DefaultedList.ofSize(this.backpackSlots, ItemStack.EMPTY);
		for (int i = 0; i < this.backpackSlots; i++) {
			contents.set(i, this.backpack.getStack(i));
		}
		BackpackData.save(target, contents, this.player.getRegistryManager());
		saveModules(target);
		if (this.worn) {
			// Kirim isi terbaru ke client (tooltip "terisi x/y") dan pemain lain.
			BackpackSlotHolder.of(this.player).workshop_utilities$syncBackpackSlot();
		}
	}

	/** Tulis slot modul, isi furnace, timer, dan XP ke stack tas (tanpa sinkronisasi ke pemain lain). */
	private void saveModules(ItemStack target) {
		BackpackModuleData.State state = new BackpackModuleData.State();
		for (int i = 0; i < MODULE_COUNT; i++) {
			state.items.set(BackpackModuleData.moduleIndex(i), this.modules.getStack(i));
		}
		for (int i = 0; i < BackpackModuleData.SIDE_MODULES; i++) {
			state.items.set(BackpackModuleData.sideIndex(i), this.sideModules.getStack(i));
		}
		for (int i = 0; i < BackpackModuleData.TOOL_SLOTS; i++) {
			state.items.set(BackpackModuleData.toolIndex(i), this.tools.getStack(i));
		}
		state.toolsShown = toolsShown();
		for (int i = 0; i < FURNACES * 3; i++) {
			state.items.set(BackpackModuleData.smeltIndex(i), this.smelt.getStack(i));
		}
		for (int i = 0; i < state.timers.length; i++) {
			state.timers[i] = this.smeltProps.get(i);
		}
		System.arraycopy(this.smeltXp, 0, state.xp, 0, FURNACES);
		state.time = this.lastSmeltTime;
		BackpackModuleData.save(target, state, this.player.getRegistryManager());
	}

	// ------------------------------------------------------------------ tombol

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (player.getWorld().isClient) {
			return false;
		}
		this.bulk = true;
		try {
			switch (id) {
				case BUTTON_QUICK_STACK -> quickStack(player);
				case BUTTON_SORT -> sortRange(player, 0, this.backpackSlots);
				case BUTTON_SORT_INVENTORY -> sortRange(player, this.backpackSlots, this.backpackSlots + 27);
				case BUTTON_TAKE_ALL -> takeAll(player);
				case BUTTON_DEPOSIT_ALL -> depositAll(player);
				case BUTTON_UNCRAFT_PREVIOUS, BUTTON_UNCRAFT_NEXT, BUTTON_UNCRAFT_PREVIOUS_10, BUTTON_UNCRAFT_NEXT_10 -> {
					if (!uncraftNavigate(id)) {
						return false;
					}
				}
				case BUTTON_SLEEP -> sleepThroughNight(player);
				case BUTTON_TOOLS_TOGGLE -> toggleTools();
				default -> {
					return false;
				}
			}
		} finally {
			this.bulk = false;
			saveToStack();
		}
		return true;
	}

	// ------------------------------------------------------------------ modul Bed

	/** Waktu dunia (tick dalam hari) tempat bed vanilla boleh dipakai: malam hari, atau saat badai petir. Dipakai server dan client. */
	public static boolean isSleepTime(World world) {
		long time = world.getTimeOfDay() % 24000L;
		return (time >= 12542L && time < 23460L) || world.isThundering();
	}

	/**
	 * Tidur tanpa bed: aturannya mengikuti bed vanilla (hanya Overworld, hanya malam / badai, tidak boleh ada monster
	 * yang mengincar dalam radius 8x5x8), lalu pemain tidur di bed sementara di lantai seperti tidur biasa (lihat FloorSleep).
	 * Spawn point TIDAK diubah sama sekali karena tidak ada blok bed yang dipakai.
	 */
	private void sleepThroughNight(PlayerEntity player) {
		if (!(player.getWorld() instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)
				|| !hasBedModule()) {
			return;
		}
		if (serverWorld.getRegistryKey() != World.OVERWORLD) {
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.wrong_dimension"), true);
			return;
		}
		if (!isSleepTime(serverWorld)) {
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.not_night"), true);
			return;
		}
		if (!serverWorld.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.no_cycle"), true);
			return;
		}
		Vec3d pos = player.getPos();
		Box area = new Box(pos.x - 8.0, pos.y - 5.0, pos.z - 8.0, pos.x + 8.0, pos.y + 5.0, pos.z + 8.0);
		List<HostileEntity> monsters = serverWorld.getEntitiesByClass(HostileEntity.class, area,
				mob -> mob.isAlive() && (mob.getTarget() == player
						|| !(mob instanceof AbstractPiglinEntity || mob instanceof ZombifiedPiglinEntity
						|| mob instanceof EndermanEntity)));
		if (!monsters.isEmpty()) {
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.monsters"), true);
			return;
		}

		if (FloorSleep.isSleeping(serverPlayer)) {
			return;
		}
		// Pemain tidur di bed sementara (hilang saat bangun) dengan alur waktu seperti bed biasa. Lihat FloorSleep.
		long next = serverWorld.getTimeOfDay() + 24000L;
		FloorSleep.start(serverPlayer, next - next % 24000L);
	}

	private int inventoryStart() {
		return this.backpackSlots;
	}

	private int inventoryEnd() {
		return this.backpackSlots + 27;
	}

	private int hotbarEnd() {
		return this.backpackSlots + 36;
	}

	/** Pindahkan item dari inventory (tanpa hotbar) yang jenisnya sudah ada di dalam tas. */
	private void quickStack(PlayerEntity player) {
		List<Item> present = new ArrayList<>();
		for (int i = 0; i < this.backpackSlots; i++) {
			ItemStack stack = this.slots.get(i).getStack();
			if (!stack.isEmpty() && !present.contains(stack.getItem())) {
				present.add(stack.getItem());
			}
		}
		for (int i = inventoryStart(); i < inventoryEnd(); i++) {
			Slot slot = this.slots.get(i);
			ItemStack stack = slot.getStack();
			if (stack.isEmpty() || !slot.canTakeItems(player) || !present.contains(stack.getItem())) {
				continue;
			}
			moveToBackpack(slot, stack);
		}
	}

	/** Pindahkan semua item inventory (tanpa hotbar) ke tas. */
	private void depositAll(PlayerEntity player) {
		for (int i = inventoryStart(); i < inventoryEnd(); i++) {
			Slot slot = this.slots.get(i);
			ItemStack stack = slot.getStack();
			if (stack.isEmpty() || !slot.canTakeItems(player)) {
				continue;
			}
			moveToBackpack(slot, stack);
		}
	}

	private void moveToBackpack(Slot from, ItemStack stack) {
		this.insertItem(stack, 0, this.backpackSlots, false);
		if (stack.isEmpty()) {
			from.setStack(ItemStack.EMPTY);
		} else {
			from.markDirty();
		}
	}

	/** Pindahkan semua isi tas ke inventory pemain (selama masih ada tempat). */
	private void takeAll(PlayerEntity player) {
		for (int i = 0; i < this.backpackSlots; i++) {
			Slot slot = this.slots.get(i);
			ItemStack stack = slot.getStack();
			if (stack.isEmpty()) {
				continue;
			}
			this.insertItem(stack, inventoryStart(), hotbarEnd(), true);
			if (stack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}
	}

	/** Gabungkan tumpukan sejenis lalu urutkan berdasarkan urutan registrasi item. Slot terkunci dilewati. */
	private void sortRange(PlayerEntity player, int from, int to) {
		List<Slot> targets = new ArrayList<>();
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = from; i < to; i++) {
			Slot slot = this.slots.get(i);
			if (!slot.canTakeItems(player)) {
				continue;
			}
			targets.add(slot);
			ItemStack stack = slot.getStack();
			if (!stack.isEmpty()) {
				stacks.add(stack.copy());
			}
		}
		List<ItemStack> sorted = mergeAndSort(stacks);
		for (int i = 0; i < targets.size(); i++) {
			targets.get(i).setStack(i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
		}
	}

	private static List<ItemStack> mergeAndSort(List<ItemStack> input) {
		List<ItemStack> out = new ArrayList<>();
		for (ItemStack source : input) {
			ItemStack rest = source.copy();
			for (ItemStack existing : out) {
				if (rest.isEmpty()) {
					break;
				}
				if (existing.getCount() < existing.getMaxCount() && ItemStack.areItemsAndComponentsEqual(existing, rest)) {
					int move = Math.min(rest.getCount(), existing.getMaxCount() - existing.getCount());
					existing.increment(move);
					rest.decrement(move);
				}
			}
			if (!rest.isEmpty()) {
				out.add(rest);
			}
		}
		out.sort(Comparator.comparingInt((ItemStack s) -> Registries.ITEM.getRawId(s.getItem()))
				.thenComparingInt(s -> -s.getCount()));
		return out;
	}

	// ------------------------------------------------------------------ handler dasar

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack() || !slot.canTakeItems(player)) {
			return ItemStack.EMPTY;
		}
		if (slotIndex >= hotbarEnd()) {
			return quickMoveExtra(player, slot, slotIndex);
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();
		if (slotIndex < this.backpackSlots) {
			if (!this.insertItem(stack, this.backpackSlots, hotbarEnd(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, 0, this.backpackSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		return original;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		if (this.backpackStack.isEmpty()) {
			return true;
		}
		if (this.worn) {
			// Tutup otomatis bila tas di slot Backpack dilepas atau diganti tas dengan tier lain.
			ItemStack live = liveStack();
			return live.getItem() instanceof BackpackItem item && item.getTier() == this.tier;
		}
		// Tutup otomatis bila tas sudah tidak ada di inventory (dibuang, hilang, dll).
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			if (inventory.getStack(i) == this.backpackStack) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			// Seperti crafting table vanilla: isi grid dikembalikan ke pemain saat GUI ditutup.
			this.dropInventory(player, this.craftGrid);
			// Seperti Uncrafting Table: input dikembalikan, dan hasil yang sudah dikonsumsi (committed) tidak boleh hilang.
			this.dropInventory(player, this.uncraftInput);
			if (this.uncraftCommitted) {
				this.dropInventory(player, this.uncraftOutput);
			}
			tickSmelting();
		}
		saveToStack();
	}

	// ------------------------------------------------------------------ crafting

	@Override
	public void onContentChanged(Inventory inventory) {
		if (inventory == this.craftGrid) {
			updateCraftResult();
		}
		if (inventory == this.uncraftInput && !this.uncraftSuppress) {
			refreshUncraft();
		}
		super.onContentChanged(inventory);
	}

	/** Sama dengan CraftingScreenHandler.updateResult vanilla, hanya aktif bila modul Crafting Table terpasang. */
	private void updateCraftResult() {
		World world = this.player.getWorld();
		if (world.isClient) {
			return;
		}
		ItemStack result = ItemStack.EMPTY;
		if (hasModule(TYPE_CRAFTING)) {
			List<ItemStack> stacks = new ArrayList<>(9);
			for (int i = 0; i < 9; i++) {
				stacks.add(this.craftGrid.getStack(i));
			}
			CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, stacks);
			Optional<RecipeEntry<CraftingRecipe>> match =
					world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, world);
			if (match.isPresent()) {
				result = match.get().value().craft(input, world.getRegistryManager());
			}
		}
		this.craftResult.setStack(0, result);
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		if (slot.inventory == this.craftResult || slot instanceof UncraftOutputSlot
				|| (slot instanceof SmeltSlot smeltSlot && smeltSlot.role == 2)
				|| (slot instanceof ToolSlot && !(hasToolsHolder() && toolsShown()))) {
			return false;
		}
		return super.canInsertIntoSlot(stack, slot);
	}

	private ItemStack quickMoveExtra(PlayerEntity player, Slot slot, int slotIndex) {
		if (slotIndex >= uncraftOutputSlot(0) && slotIndex <= uncraftOutputSlot(8)) {
			return quickMoveUncraftOutputs(player);
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();
		int smeltBase = smeltSlot(0, 0);
		boolean result = slotIndex == craftResultSlot();
		boolean smeltOutput = slotIndex >= smeltBase && slotIndex < smeltBase + FURNACES * 3 && (slotIndex - smeltBase) % 3 == 2;
		if (!this.insertItem(stack, inventoryStart(), hotbarEnd(), result || smeltOutput)) {
			return ItemStack.EMPTY;
		}
		if (result) {
			slot.onQuickTransfer(stack, original);
		}
		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTakeItem(player, stack);
		if (result) {
			player.dropItem(stack, false);
		}
		return original;
	}

	// ------------------------------------------------------------------ uncrafting (modul Uncrafting)

	private boolean uncraftOutputsEmpty() {
		for (int i = 0; i < this.uncraftOutput.size(); i++) {
			if (!this.uncraftOutput.getStack(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** Hitung ulang pilihan resep dan preview hasil dari item di slot input (server saja). */
	private void refreshUncraft() {
		if (this.player.getWorld().isClient) {
			return;
		}
		if (this.uncraftCommitted) {
			if (!uncraftOutputsEmpty()) {
				return;
			}
			this.uncraftCommitted = false;
		}
		ItemStack in = this.uncraftInput.getStack(0);
		this.uncraftOptions = hasModule(TYPE_UNCRAFTING)
				? UncraftRecipes.find(this.player.getWorld(), in) : List.of();
		Item item = in.isEmpty() ? null : in.getItem();
		if (item != this.uncraftLastItem) {
			this.uncraftSelected = 0;
			this.uncraftLastItem = item;
		}
		if (this.uncraftSelected >= this.uncraftOptions.size()) {
			this.uncraftSelected = 0;
		}
		showUncraft();
	}

	private void showUncraft() {
		for (int i = 0; i < this.uncraftOutput.size(); i++) {
			this.uncraftOutput.setStack(i, ItemStack.EMPTY);
		}
		if (!this.uncraftOptions.isEmpty()) {
			List<ItemStack> grid = this.uncraftOptions.get(this.uncraftSelected).grid();
			for (int i = 0; i < 9; i++) {
				this.uncraftOutput.setStack(i, grid.get(i).copy());
			}
		}
		this.uncraftProps.set(0, this.uncraftSelected);
		this.uncraftProps.set(1, this.uncraftOptions.size());
	}

	/** Tombol < > (dan lompat 10 pilihan). False bila tidak berlaku (modul tidak ada, sudah diambil, atau pilihan hanya satu). */
	private boolean uncraftNavigate(int id) {
		int n = this.uncraftOptions.size();
		if (!hasModule(TYPE_UNCRAFTING) || this.uncraftCommitted || n <= 1) {
			return false;
		}
		if (id == BUTTON_UNCRAFT_PREVIOUS) {
			this.uncraftSelected = (this.uncraftSelected - 1 + n) % n;
		} else if (id == BUTTON_UNCRAFT_NEXT) {
			this.uncraftSelected = (this.uncraftSelected + 1) % n;
		} else if (id == BUTTON_UNCRAFT_PREVIOUS_10) {
			this.uncraftSelected = Math.floorMod(this.uncraftSelected - 10, n);
		} else if (id == BUTTON_UNCRAFT_NEXT_10) {
			this.uncraftSelected = (this.uncraftSelected + 10) % n;
		} else {
			return false;
		}
		showUncraft();
		return true;
	}

	/**
	 * Pemain mengambil salah satu hasil. Pengambilan pertama mengonsumsi input (sejumlah hasil resep) dan mengubah
	 * seluruh grid menjadi item nyata; setelah grid kosong, preview baru muncul lagi bila input masih cukup.
	 */
	private void onUncraftOutputTaken() {
		if (this.player.getWorld().isClient) {
			return;
		}
		if (!this.uncraftCommitted) {
			if (this.uncraftOptions.isEmpty()) {
				return;
			}
			UncraftRecipes.Option option = this.uncraftOptions.get(this.uncraftSelected);
			this.uncraftSuppress = true;
			ItemStack in = this.uncraftInput.getStack(0);
			in.decrement(option.resultCount());
			if (in.isEmpty()) {
				this.uncraftInput.setStack(0, ItemStack.EMPTY);
			}
			this.uncraftSuppress = false;
			this.uncraftCommitted = true;
		}
		if (uncraftOutputsEmpty()) {
			this.uncraftCommitted = false;
			refreshUncraft();
		} else {
			this.uncraftProps.set(1, 0);
		}
	}

	/** Shift-click pada hasil Uncrafting: pindahkan semua bahan sekaligus ke inventory pemain. */
	private ItemStack quickMoveUncraftOutputs(PlayerEntity player) {
		List<Integer> targets = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			if (this.slots.get(uncraftOutputSlot(i)).hasStack()) {
				targets.add(uncraftOutputSlot(i));
			}
		}
		ItemStack moved = ItemStack.EMPTY;
		for (int index : targets) {
			Slot s = this.slots.get(index);
			ItemStack st = s.getStack();
			if (st.isEmpty()) {
				continue;
			}
			ItemStack before = st.copy();
			if (!this.insertItem(st, inventoryStart(), hotbarEnd(), true)) {
				continue;
			}
			if (st.isEmpty()) {
				s.setStack(ItemStack.EMPTY);
			} else {
				s.markDirty();
			}
			s.onTakeItem(player, before);
			if (moved.isEmpty()) {
				moved = before;
			}
		}
		return moved;
	}

	// ------------------------------------------------------------------ smelting

	/** Dipanggil server tiap tick selagi GUI terbuka. */
	@Override
	public void sendContentUpdates() {
		tickSmelting();
		super.sendContentUpdates();
	}

	/**
	 * Majukan ketiga furnace sebanyak tick yang berlalu sejak terakhir dihitung. Saat GUI terbuka itu 1 tick; saat
	 * tas baru dibuka, selisihnya adalah waktu tas ditutup, sehingga hasilnya sama dengan furnace yang terus berjalan.
	 */
	private void tickSmelting() {
		World world = this.player.getWorld();
		if (world.isClient || this.backpackStack.isEmpty() || this.simulating) {
			return;
		}
		long now = world.getTime();
		if (this.lastSmeltTime < 0 || this.lastSmeltTime > now) {
			this.lastSmeltTime = now;
		}
		long elapsed = now - this.lastSmeltTime;
		if (elapsed <= 0) {
			return;
		}
		this.lastSmeltTime = now;
		boolean itemsChanged = false;
		this.simulating = true;
		try {
			for (int f = 0; f < FURNACES; f++) {
				if (hasModule(TYPE_FURNACE + f)) {
					itemsChanged |= simulateFurnace(world, f, Math.min(elapsed, MAX_CATCH_UP));
				}
			}
		} finally {
			this.simulating = false;
		}
		if (itemsChanged) {
			saveToStack();
		} else if (now % SAVE_INTERVAL == 0 || elapsed > 1) {
			ItemStack target = liveStack();
			if (!target.isEmpty() && target.getItem() instanceof BackpackItem) {
				saveModules(target);
			}
		}
	}

	private static int fuelTime(ItemStack fuel) {
		if (fuel.isEmpty()) {
			return 0;
		}
		return AbstractFurnaceBlockEntity.createFuelTimeMap().getOrDefault(fuel.getItem(), 0);
	}

	private static boolean isFuel(ItemStack stack) {
		return fuelTime(stack) > 0;
	}

	private static AbstractCookingRecipe findRecipe(World world, int furnace, ItemStack input) {
		SingleStackRecipeInput single = new SingleStackRecipeInput(input);
		RecipeManager manager = world.getRecipeManager();
		if (furnace == 0) {
			return manager.getFirstMatch(RecipeType.SMELTING, single, world)
					.map(entry -> (AbstractCookingRecipe) entry.value()).orElse(null);
		}
		if (furnace == 1) {
			return manager.getFirstMatch(RecipeType.BLASTING, single, world)
					.map(entry -> (AbstractCookingRecipe) entry.value()).orElse(null);
		}
		return manager.getFirstMatch(RecipeType.SMOKING, single, world)
				.map(entry -> (AbstractCookingRecipe) entry.value()).orElse(null);
	}

	private boolean canAccept(World world, AbstractCookingRecipe recipe, int outIndex) {
		if (recipe == null) {
			return false;
		}
		ItemStack result = recipe.getResult(world.getRegistryManager());
		if (result.isEmpty()) {
			return false;
		}
		ItemStack output = this.smelt.getStack(outIndex);
		if (output.isEmpty()) {
			return true;
		}
		if (!ItemStack.areItemsAndComponentsEqual(output, result)) {
			return false;
		}
		if (output.getCount() < 64 && output.getCount() < output.getMaxCount()) {
			return true;
		}
		return output.getCount() < result.getMaxCount();
	}

	/** Langkah furnace vanilla (AbstractFurnaceBlockEntity.tick) yang diulang {@code ticks} kali. True bila item berubah. */
	private boolean simulateFurnace(World world, int f, long ticks) {
		int in = f * 3;
		int fuelIndex = in + 1;
		int out = in + 2;
		int p = f * PROPS;
		boolean itemsChanged = false;
		Map<Item, Optional<AbstractCookingRecipe>> cache = new HashMap<>();
		for (long t = 0; t < ticks; t++) {
			if (this.smeltProps.get(p) > 0) {
				this.smeltProps.set(p, this.smeltProps.get(p) - 1);
			}
			ItemStack input = this.smelt.getStack(in);
			ItemStack fuel = this.smelt.getStack(fuelIndex);
			boolean hasInput = !input.isEmpty();
			boolean hasFuel = !fuel.isEmpty();
			if (this.smeltProps.get(p) > 0 || (hasFuel && hasInput)) {
				AbstractCookingRecipe recipe = hasInput ? cachedRecipe(world, f, input, cache) : null;
				boolean accept = canAccept(world, recipe, out);
				if (this.smeltProps.get(p) <= 0 && accept) {
					int time = fuelTime(fuel);
					this.smeltProps.set(p, time);
					this.smeltProps.set(p + 1, time);
					if (time > 0) {
						itemsChanged = true;
						if (hasFuel) {
							Item item = fuel.getItem();
							fuel.decrement(1);
							if (fuel.isEmpty()) {
								this.smelt.setStack(fuelIndex,
										item == Items.LAVA_BUCKET ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
							}
						}
					}
				}
				if (this.smeltProps.get(p) > 0 && accept) {
					if (this.smeltProps.get(p + 2) == 0) {
						this.smeltProps.set(p + 3, recipe.getCookingTime());
					}
					int cook = this.smeltProps.get(p + 2) + 1;
					this.smeltProps.set(p + 2, cook);
					if (cook >= this.smeltProps.get(p + 3)) {
						this.smeltProps.set(p + 2, 0);
						this.smeltProps.set(p + 3, recipe.getCookingTime());
						craftOne(world, recipe, f);
						this.smeltXp[f] += recipe.getExperience();
						itemsChanged = true;
					}
				} else {
					this.smeltProps.set(p + 2, 0);
				}
			} else if (this.smeltProps.get(p + 2) > 0) {
				this.smeltProps.set(p + 2, MathHelper.clamp(this.smeltProps.get(p + 2) - 2, 0, this.smeltProps.get(p + 3)));
			}
			// Berhenti lebih awal bila furnace tidak mungkin berubah lagi (hemat saat menyusul waktu yang lama).
			if (this.smeltProps.get(p) <= 0 && this.smeltProps.get(p + 2) <= 0) {
				ItemStack input2 = this.smelt.getStack(in);
				ItemStack fuel2 = this.smelt.getStack(fuelIndex);
				boolean could = !input2.isEmpty() && fuelTime(fuel2) > 0
						&& canAccept(world, cachedRecipe(world, f, input2, cache), out);
				if (!could) {
					break;
				}
			}
		}
		return itemsChanged;
	}

	private static AbstractCookingRecipe cachedRecipe(World world, int f, ItemStack input,
													  Map<Item, Optional<AbstractCookingRecipe>> cache) {
		return cache.computeIfAbsent(input.getItem(), item -> Optional.ofNullable(findRecipe(world, f, input))).orElse(null);
	}

	private void craftOne(World world, AbstractCookingRecipe recipe, int f) {
		int in = f * 3;
		int fuelIndex = in + 1;
		int out = in + 2;
		ItemStack input = this.smelt.getStack(in);
		ItemStack fuel = this.smelt.getStack(fuelIndex);
		ItemStack result = recipe.getResult(world.getRegistryManager());
		ItemStack output = this.smelt.getStack(out);
		if (output.isEmpty()) {
			this.smelt.setStack(out, result.copy());
		} else if (ItemStack.areItemsAndComponentsEqual(output, result)) {
			output.increment(result.getCount());
		}
		if (input.isOf(Items.WET_SPONGE) && !fuel.isEmpty() && fuel.isOf(Items.BUCKET)) {
			this.smelt.setStack(fuelIndex, new ItemStack(Items.WATER_BUCKET));
		}
		input.decrement(1);
		if (input.isEmpty()) {
			this.smelt.setStack(in, ItemStack.EMPTY);
		}
	}

	/** XP hasil smelting dibayarkan saat pemain mengambil hasil, sebagai orb XP seperti furnace vanilla. */
	private void giveSmeltXp(PlayerEntity player, int furnace) {
		if (player.getWorld().isClient || !(player.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		float amount = this.smeltXp[furnace];
		if (amount <= 0.0F) {
			return;
		}
		this.smeltXp[furnace] = 0.0F;
		int whole = MathHelper.floor(amount);
		float fraction = amount - whole;
		if (fraction > 0.0F && Math.random() < fraction) {
			whole++;
		}
		if (whole > 0) {
			ExperienceOrbEntity.spawn(serverWorld, player.getPos(), whole);
		}
	}

	// ------------------------------------------------------------------ slot

	/** Slot modul: hanya Crafting / Furnace / Blast Furnace / Smoker / Uncrafting Module, satu per jenis, dan tidak bisa diambil selagi panelnya berisi. */
	private class ModuleSlot extends Slot {
		ModuleSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			int type = moduleTypeOf(stack);
			if (type < 0) {
				return false;
			}
			for (int i = 0; i < MODULE_COUNT; i++) {
				if (i != this.getIndex() && moduleTypeOf(BackpackScreenHandler.this.modules.getStack(i)) == type) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			int type = moduleTypeOf(this.getStack());
			return type < 0 || !moduleBusy(type);
		}
	}

	/**
	 * Slot modul samping (kiri GUI): satu modul tertentu (Bed Module atau Tools Holder Module), satu buah.
	 * Bed Module boleh diambil kapan saja; Tools Holder Module tidak bisa dilepas selama slot tools-nya masih berisi.
	 */
	private class SideModuleSlot extends Slot {
		private final Item accepts;

		SideModuleSlot(Inventory inventory, int index, Item accepts) {
			super(inventory, index, 0, 0);
			this.accepts = accepts;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return stack.isOf(this.accepts);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return this.accepts != ModItems.TOOLS_HOLDER_MODULE || !toolsBusy();
		}
	}

	/** Slot isi Tools Holder: hanya tools / senjata / sejenisnya, satu item per slot, dan hanya aktif bila modulnya terpasang. */
	private class ToolSlot extends Slot {
		ToolSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return hasToolsHolder() && isHoldableTool(stack);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}
	}

	/** Slot input modul Uncrafting: menerima item apa pun (seperti Uncrafting Table), asal modulnya terpasang. */
	private class UncraftInputSlot extends Slot {
		UncraftInputSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return hasModule(TYPE_UNCRAFTING);
		}
	}

	/** Slot hasil modul Uncrafting: hanya bisa diambil; pengambilan memicu {@link #onUncraftOutputTaken()}. */
	private class UncraftOutputSlot extends Slot {
		UncraftOutputSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false;
		}

		@Override
		public void onTakeItem(PlayerEntity player, ItemStack stack) {
			onUncraftOutputTaken();
			super.onTakeItem(player, stack);
		}
	}

	private class CraftGridSlot extends Slot {
		CraftGridSlot(Inventory inventory, int index) {
			super(inventory, index, 0, 0);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return hasModule(TYPE_CRAFTING);
		}
	}

	/** Slot furnace: role 0 input, 1 bahan bakar, 2 hasil. Hanya aktif bila furnace-nya terpasang. */
	private class SmeltSlot extends Slot {
		private final int furnace;
		final int role;

		SmeltSlot(Inventory inventory, int index, int furnace, int role) {
			super(inventory, index, 0, 0);
			this.furnace = furnace;
			this.role = role;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			if (!hasModule(TYPE_FURNACE + this.furnace)) {
				return false;
			}
			return switch (this.role) {
				case 0 -> true;
				case 1 -> isFuel(stack) || stack.isOf(Items.BUCKET);
				default -> false;
			};
		}

		@Override
		public int getMaxItemCount(ItemStack stack) {
			return this.role == 1 && stack.isOf(Items.BUCKET) ? 1 : super.getMaxItemCount(stack);
		}

		@Override
		public void onTakeItem(PlayerEntity player, ItemStack stack) {
			if (this.role == 2) {
				giveSmeltXp(player, this.furnace);
			}
			super.onTakeItem(player, stack);
		}
	}

	private static class BackpackSlot extends Slot {
		BackpackSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return !(stack.getItem() instanceof BackpackItem);
		}
	}

	/** Slot inventory pemain; slot tempat tas yang sedang dibuka tidak bisa diambil atau ditimpa. */
	private class PlayerSlot extends Slot {
		PlayerSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		private boolean locked() {
			return this.getIndex() == BackpackScreenHandler.this.properties.get(0);
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return !locked();
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return !locked();
		}
	}
}
