package com.workshoputilities.screen;

import com.workshoputilities.backpack.BackpackTier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

/**
 * Dasar untuk GUI block mod ini yang punya panel tambahan di samping GUI utamanya:
 * <ul>
 * <li><b>Panel backpack</b> (Uncrafting Table, Upgrade Table, Dis-Enchanting Table, Upgrade Enchanting Table): isi
 * backpack yang sedang dipakai di slot Backpack pemain, bisa dibuka / ditutup lewat tombol kecil di kiri atas GUI.
 * Bila backpack itu punya Tools Holder Module, 7 slot Tools Holder ikut tampil di panel yang sama.</li>
 * <li><b>Panel equipment</b> (hanya Upgrade Table dan Dis-Enchanting Table): 4 slot armor,
 * slot tangan kiri, dan slot Elytra milik pemain, selalu tampil.</li>
 * </ul>
 * Seluruh logikanya ada di {@link PanelSupport} (dipakai juga oleh GUI block vanilla); kelas ini hanya meneruskannya.
 * Semua slot tambahan ditaruh PALING AKHIR (setelah slot inventory pemain), sehingga nomor slot milik block tidak bergeser.
 */
public abstract class PanelScreenHandler extends ScreenHandler {
	public static final int EQUIP_HEAD = PanelSupport.EQUIP_HEAD;
	public static final int EQUIP_CHEST = PanelSupport.EQUIP_CHEST;
	public static final int EQUIP_LEGS = PanelSupport.EQUIP_LEGS;
	public static final int EQUIP_FEET = PanelSupport.EQUIP_FEET;
	public static final int EQUIP_OFFHAND = PanelSupport.EQUIP_OFFHAND;
	public static final int EQUIP_ELYTRA = PanelSupport.EQUIP_ELYTRA;
	public static final int EQUIP_COUNT = PanelSupport.EQUIP_COUNT;

	/** Jumlah slot panel backpack di handler = slot terbanyak dari semua tier. */
	public static final int MAX_BACKPACK_SLOTS = PanelSupport.MAX_BACKPACK_SLOTS;

	private PanelSupport panel;

	protected PanelScreenHandler(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	/**
	 * Tambahkan slot panel. Panggil SEKALI di akhir constructor, setelah slot hotbar ditambahkan.
	 *
	 * @param equipment true untuk block yang punya panel equipment
	 */
	protected final void addPanelSlots(PlayerInventory playerInventory, boolean equipment) {
		this.panel = new PanelSupport(this, playerInventory, equipment, -1);
		this.panel.build();
	}

	/** Logika panel (untuk client: tombol, layout, dan penempatan slot). */
	public final PanelSupport panelSupport() {
		return this.panel;
	}

	// ------------------------------------------------------------------ info untuk client

	public final boolean hasEquipmentPanel() {
		return this.panel != null && this.panel.hasEquipmentPanel();
	}

	/** Indeks slot handler untuk slot equipment {@code which} (EQUIP_*). */
	public final int equipmentSlotIndex(int which) {
		return this.panel.equipmentSlotIndex(which);
	}

	/** Jumlah slot backpack yang aktif; 0 berarti pemain tidak memakai backpack. */
	public final int panelSlotCount() {
		return this.panel == null ? 0 : this.panel.slotCount();
	}

	/** Indeks slot handler untuk slot panel backpack ke-{@code i} (0 based). */
	public final int panelSlotIndex(int i) {
		return this.panel.slotIndex(i);
	}

	/** Tier backpack yang dipakai berdasarkan jumlah slot aktif, atau null bila tidak ada. */
	public final BackpackTier panelTier() {
		return this.panel == null ? null : this.panel.tier();
	}

	/** Jumlah slot backpack yang terisi (untuk label "12/36"). */
	public final int panelUsedSlots() {
		return this.panel == null ? 0 : this.panel.usedSlots();
	}

	// ------------------------------------------------------------------ siklus hidup

	@Override
	public void sendContentUpdates() {
		if (this.panel != null) {
			this.panel.refresh();
		}
		super.sendContentUpdates();
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (this.panel != null) {
			this.panel.saveAll();
		}
	}

	// ------------------------------------------------------------------ shift-click

	/**
	 * Shift-click pada slot equipment (armor, tangan kiri, Elytra) memindahkannya ke inventory pemain, seperti di inventory
	 * vanilla. Mengembalikan null bila {@code slotIndex} bukan slot equipment (artinya quickMove block berjalan seperti biasa).
	 */
	protected final ItemStack quickMoveEquipment(PlayerEntity player, int slotIndex, int inventoryStart, int hotbarEnd) {
		return this.panel == null ? null : this.panel.quickMoveEquipment(player, slotIndex, inventoryStart, hotbarEnd);
	}
}
