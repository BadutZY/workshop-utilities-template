package com.workshoputilities.client;

import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.screen.UncraftingScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class UncraftingScreen extends HandledScreen<UncraftingScreenHandler> {
	private static final Identifier TEXTURE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/container/uncrafting_table.png");

	/** Kotak angka pilihan resep (digambar di texture), titik tengah horizontalnya relatif terhadap GUI. */
	private static final int COUNTER_CENTER_X = 36;
	private static final int COUNTER_TEXT_Y = 23;

	/** Posisi tombol < > tepat di bawah slot input. */
	private static final int BUTTON_Y = 55;
	private static final int PREV_BUTTON_X = 22;
	private static final int NEXT_BUTTON_X = 38;

	private ArrowButton prevButton;
	private ArrowButton nextButton;

	public UncraftingScreen(UncraftingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		this.prevButton = new ArrowButton(this.x + PREV_BUTTON_X, this.y + BUTTON_Y, true,
				() -> click(hasShiftDown() ? UncraftingScreenHandler.BUTTON_PREVIOUS_10 : UncraftingScreenHandler.BUTTON_PREVIOUS));
		this.prevButton.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.prev_recipe")));
		this.addDrawableChild(this.prevButton);

		this.nextButton = new ArrowButton(this.x + NEXT_BUTTON_X, this.y + BUTTON_Y, false,
				() -> click(hasShiftDown() ? UncraftingScreenHandler.BUTTON_NEXT_10 : UncraftingScreenHandler.BUTTON_NEXT));
		this.nextButton.setTooltip(Tooltip.of(Text.translatable("gui.workshop_utilities.next_recipe")));
		this.addDrawableChild(this.nextButton);
	}

	private void click(int id) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, id);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		int total = this.handler.getTotal();
		String label = total > 0 ? (this.handler.getSelected() + 1) + "/" + total : "-";
		int w = this.textRenderer.getWidth(label);
		context.drawText(this.textRenderer, label, COUNTER_CENTER_X - w / 2, COUNTER_TEXT_Y, 0xE0E0E0, false);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Tombol hanya aktif bila ada lebih dari satu pilihan resep.
		boolean enabled = this.handler.getTotal() > 1;
		if (this.prevButton != null) {
			this.prevButton.active = enabled;
		}
		if (this.nextButton != null) {
			this.nextButton.active = enabled;
		}
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	/** Tombol panah 12x12 yang sprite-nya diambil dari texture GUI (u=176..211, v=0..23). */
	private static class ArrowButton extends PressableWidget {
		private final boolean left;
		private final Runnable action;

		ArrowButton(int x, int y, boolean left, Runnable action) {
			super(x, y, 12, 12, Text.literal(left ? "<" : ">"));
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
			context.drawTexture(TEXTURE, this.getX(), this.getY(), u, v, 12, 12);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			this.appendDefaultNarrations(builder);
		}
	}
}