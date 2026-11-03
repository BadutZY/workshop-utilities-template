package com.workshoputilities.client;

import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.screen.UpgradeTableScreenHandler;
import com.workshoputilities.upgrade.UpgradeTableLogic;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * GUI Upgrade Table: dua slot kosong polos di atas (item + bahan), satu slot Upgrade Tool (ikon outline) di bawahnya, panah, dan satu slot hasil.
 * Texture digambar oleh tools/generate_upgrade_table_textures.py (posisi slot sama dengan UpgradeTableScreenHandler).
 * Keterangan status (apa yang kurang / kenapa tidak valid) dihitung dari isi slot lewat {@link UpgradeTableLogic}.
 */
public class UpgradeTableScreen extends PanelHandledScreen<UpgradeTableScreenHandler> {
	private static final Identifier TEXTURE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/container/upgrade_table.png");

	/** Panah (22x15) dan tanda silang merah yang menutupinya (sprite di sisi kanan texture). */
	private static final int ARROW_X = 101;
	private static final int ARROW_Y = 37;
	private static final int ARROW_W = 22;
	private static final int ARROW_H = 15;
	private static final int CROSS_U = 176;
	private static final int CROSS_V = 0;

	/** Area teks status di bawah slot hasil. Teks diperkecil 0.75x agar muat 3 baris. */
	private static final int STATUS_X = 98;
	private static final int STATUS_Y = 62;
	private static final float STATUS_SCALE = 0.75F;
	private static final int STATUS_WRAP = 100;
	private static final int STATUS_MAX_LINES = 3;

	private static final int COLOR_ERROR = 0xB03030;
	private static final int COLOR_HINT = 0x505050;
	private static final int COLOR_OK = 0x3F9F3F;

	public UpgradeTableScreen(UpgradeTableScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	private UpgradeTableLogic.Result evaluate() {
		return UpgradeTableLogic.evaluate(
				this.handler.getSlot(UpgradeTableScreenHandler.TARGET_SLOT).getStack(),
				this.handler.getSlot(UpgradeTableScreenHandler.MATERIAL_SLOT).getStack(),
				this.handler.getSlot(UpgradeTableScreenHandler.TOOL_SLOT).getStack());
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		// Panel samping (backpack / equipment) digambar lebih dulu; texture GUI di bawah menutup sambungannya.
		drawPanels(context);
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

		// Slot item dan bahan dibiarkan kosong polos (dipakai untuk tool/enchanted book maupun Backpack).
		// Hanya slot Upgrade Tool yang diberi petunjuk: outline gelap saja, seperti ikon slot armor kosong vanilla.
		if (this.handler.getSlot(UpgradeTableScreenHandler.TOOL_SLOT).getStack().isEmpty()) {
			GhostSlotIcon.drawOutline(context, GhostSlotIcon.UPGRADE_TOOL_OUTLINE,
					this.x + UpgradeTableScreenHandler.TOOL_X, this.y + UpgradeTableScreenHandler.TOOL_Y);
		}

		// Kombinasi tidak valid: tanda silang merah di atas panah (seperti anvil/smithing table vanilla).
		if (evaluate().status().isError()) {
			context.drawTexture(TEXTURE, this.x + ARROW_X, this.y + ARROW_Y, CROSS_U, CROSS_V, ARROW_W, ARROW_H);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		UpgradeTableLogic.Result result = evaluate();
		int color = result.status().isError() ? COLOR_ERROR
				: result.status() == UpgradeTableLogic.Status.OK ? COLOR_OK : COLOR_HINT;
		List<OrderedText> lines = this.textRenderer.wrapLines(result.message(), STATUS_WRAP);
		context.getMatrices().push();
		context.getMatrices().translate(STATUS_X, STATUS_Y, 0.0F);
		context.getMatrices().scale(STATUS_SCALE, STATUS_SCALE, 1.0F);
		for (int i = 0; i < lines.size() && i < STATUS_MAX_LINES; i++) {
			context.drawText(this.textRenderer, lines.get(i), 0, i * 9, color, false);
		}
		context.getMatrices().pop();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}
}
