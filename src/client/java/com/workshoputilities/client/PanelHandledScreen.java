package com.workshoputilities.client;

import com.workshoputilities.screen.PanelScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Dasar untuk keempat GUI block mod ini (Uncrafting Table, Upgrade Table, Dis-Enchanting Table, Upgrade Enchanting Table)
 * yang punya panel tambahan di sampingnya. Semua tampilan dan penempatan slot panel ada di {@link PanelController}
 * (dipakai juga oleh GUI block vanilla); GUI utama block tidak diubah.
 *
 * Subclass wajib memanggil {@link #drawPanels(DrawContext)} di awal drawBackground (sebelum menggambar texture GUI utama,
 * supaya sambungan panel dengan GUI utama tertutup rapi), dan memanggil super.init() sebelum membuat widget sendiri
 * karena posisi GUI bisa digeser agar panel muat di layar.
 */
public abstract class PanelHandledScreen<T extends PanelScreenHandler> extends HandledScreen<T> {
	private PanelController panel;

	protected PanelHandledScreen(T handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		this.panel = new PanelController(this, this.handler.panelSupport(), true);
		this.panel.init();
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		this.panel.tick();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.panel.consumeReinit() && this.client != null) {
			// Menjalankan init() ulang (lewat resize) supaya widget subclass ikut menyesuaikan posisi GUI yang bergeser.
			this.resize(this.client, this.width, this.height);
		}
		super.render(context, mouseX, mouseY, delta);
	}

	/**
	 * Area di luar GUI utama yang ditempati panel ini, dalam koordinat layar: {x, y, lebar, tinggi}. Dipakai REI / JEI agar
	 * daftar item mereka tidak menimpa panel.
	 */
	public List<int[]> extraAreas() {
		return this.panel.extraAreas();
	}

	/** Klik di panel / tombol bukan klik di luar GUI (kalau tidak, item di kursor akan terbuang). */
	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
		return super.isClickOutsideBounds(mouseX, mouseY, left, top, button) && !this.panel.isOverExtraArea(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.panel.scrolled(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.panel.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.panel.mouseDragged(mouseY)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.panel.mouseReleased(button)) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	/**
	 * Gambar kedua panel. Panggil di AWAL drawBackground subclass, sebelum texture GUI utama: texture utama lalu menutupi
	 * sambungan sehingga panel tampak menempel rapi ke GUI.
	 */
	protected final void drawPanels(DrawContext context) {
		this.panel.draw(context);
	}
}
