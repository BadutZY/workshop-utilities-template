package com.workshoputilities.client;

import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.screen.DisenchantingScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DisenchantingScreen extends PanelHandledScreen<DisenchantingScreenHandler> {
	private static final Identifier TEXTURE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/container/disenchanting_table.png");

	/** Slot book = slot index 1 (posisi item 36,48). Ikon hantu = texture item book vanilla dengan opacity dikurangi. */
	private static final int BOOK_SLOT_INDEX = 1;
	private static final int BOOK_SLOT_X = 36;
	private static final int BOOK_SLOT_Y = 48;

	public DisenchantingScreen(DisenchantingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		// Panel samping (backpack / equipment) digambar lebih dulu; texture GUI di bawah menutup sambungannya.
		drawPanels(context);
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
		// Ikon book asli (vanilla) yang dipudarkan, hanya tampil saat slot book kosong dan pas di dalam slot.
		if (this.handler.getSlot(BOOK_SLOT_INDEX).getStack().isEmpty()) {
			GhostSlotIcon.drawVanilla(context, GhostSlotIcon.BOOK, this.x + BOOK_SLOT_X, this.y + BOOK_SLOT_Y);
		}
	}

	/** Menampilkan level XP yang akan didapat (rata kanan di baris judul) selama ada item ter-enchant di slot input. Disembunyikan di Creative. */
	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		if (this.client != null && this.client.player != null && this.client.player.isCreative()) {
			return;
		}
		ItemStack input = this.handler.getSlot(0).getStack();
		int levels = DisenchantingScreenHandler.calculateLevels(input);
		if (levels > 0) {
			Text label = Text.translatable("gui.workshop_utilities.xp_reward", levels);
			int width = this.textRenderer.getWidth(label);
			context.drawText(this.textRenderer, label, this.backgroundWidth - 8 - width, this.titleY, 0x3F9F3F, false);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}
}