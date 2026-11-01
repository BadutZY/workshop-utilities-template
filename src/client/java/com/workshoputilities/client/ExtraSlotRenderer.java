package com.workshoputilities.client;

import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.WornBackpackSlot;
import com.workshoputilities.elytra.ElytraSlot;
import com.workshoputilities.elytra.ElytraSlotHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Menggambar bingkai slot Backpack dan slot Elytra (gaya slot vanilla) dan ikon pudar saat slot kosong.
 * Dipakai oleh layar inventory survival dan tab Survival Inventory di layar Creative.
 */
public final class ExtraSlotRenderer {
	private static final Identifier ELYTRA_FRAME = texture("elytra_slot");
	private static final Identifier ELYTRA_ICON = texture("elytra_slot_icon");
	private static final Identifier BACKPACK_FRAME = texture("backpack_slot");
	private static final Identifier BACKPACK_ICON = texture("backpack_slot_icon");

	private ExtraSlotRenderer() {
	}

	private static Identifier texture(String name) {
		return Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/" + name + ".png");
	}

	/** (panelX, panelY) = pojok kiri atas panel layar; creative = true bila di layar Creative. */
	public static void drawAll(DrawContext context, int panelX, int panelY, boolean creative) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		boolean backpackEmpty = player == null || BackpackSlotHolder.of(player).workshop_utilities$getBackpackSlot().isEmpty();
		boolean elytraEmpty = player == null || ElytraSlotHolder.of(player).workshop_utilities$getElytraSlot().isEmpty();
		draw(context, BACKPACK_FRAME, BACKPACK_ICON, backpackEmpty,
				panelX + (creative ? WornBackpackSlot.CREATIVE_X : WornBackpackSlot.X),
				panelY + (creative ? WornBackpackSlot.CREATIVE_Y : WornBackpackSlot.Y));
		draw(context, ELYTRA_FRAME, ELYTRA_ICON, elytraEmpty,
				panelX + (creative ? ElytraSlot.CREATIVE_X : ElytraSlot.X),
				panelY + (creative ? ElytraSlot.CREATIVE_Y : ElytraSlot.Y));
	}

	/** (x, y) = posisi item 16x16 di layar; bingkai 18x18 digambar mengelilinginya. */
	private static void draw(DrawContext context, Identifier frame, Identifier icon, boolean empty, int x, int y) {
		context.drawTexture(frame, x - 1, y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
		if (empty) {
			context.drawTexture(icon, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
		}
	}
}
