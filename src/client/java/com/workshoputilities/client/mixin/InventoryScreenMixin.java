package com.workshoputilities.client.mixin;

import com.workshoputilities.client.ExtraSlotRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Menggambar bingkai slot Backpack dan slot Elytra beserta ikon pudar saat kosong, di atas slot offhand. Digambar di
 * drawBackground, sebelum item slot, sehingga item dan highlight hover tampil di atasnya persis seperti slot vanilla.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void workshop_utilities$drawExtraSlots(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		HandledScreenAccessor screen = (HandledScreenAccessor) (HandledScreen<?>) (Object) this;
		ExtraSlotRenderer.drawAll(context, screen.workshop_utilities$getX(), screen.workshop_utilities$getY(), false);
	}
}
