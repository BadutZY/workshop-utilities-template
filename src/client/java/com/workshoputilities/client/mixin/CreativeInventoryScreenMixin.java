package com.workshoputilities.client.mixin;

import com.workshoputilities.backpack.WornBackpackInventory;
import com.workshoputilities.backpack.WornBackpackSlot;
import com.workshoputilities.client.ExtraSlotRenderer;
import com.workshoputilities.elytra.ElytraSlot;
import com.workshoputilities.elytra.ElytraSlotInventory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slot Backpack dan slot Elytra di inventory Creative (tab Survival Inventory). Layar Creative memakai ulang semua slot dari inventory
 * pemain dan menyusunnya sendiri; tanpa perlakuan khusus kedua slot jatuh menimpa slot hotbar. Di sini keduanya
 * dipindah ke kolom di kanan armor kanan (Backpack di atas, Elytra di bawah, seimbang dengan offhand di kiri),
 * lalu bingkai dan ikonnya digambar.
 * Klik pada slot diteruskan vanilla ke inventory pemain, jadi aturan slot (hanya Elytra, Curse of Binding) sama
 * dengan di inventory survival.
 */
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
	@Shadow
	private static ItemGroup selectedTab;

	@Inject(method = "setSelectedTab", at = @At("TAIL"))
	private void workshop_utilities$moveExtraSlots(ItemGroup group, CallbackInfo ci) {
		for (Slot slot : ((HandledScreen<?>) (Object) this).getScreenHandler().slots) {
			if (slot.inventory instanceof ElytraSlotInventory) {
				SlotAccessor accessor = (SlotAccessor) slot;
				accessor.workshop_utilities$setX(ElytraSlot.CREATIVE_X);
				accessor.workshop_utilities$setY(ElytraSlot.CREATIVE_Y);
			} else if (slot.inventory instanceof WornBackpackInventory) {
				SlotAccessor accessor = (SlotAccessor) slot;
				accessor.workshop_utilities$setX(WornBackpackSlot.CREATIVE_X);
				accessor.workshop_utilities$setY(WornBackpackSlot.CREATIVE_Y);
			}
		}
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void workshop_utilities$drawExtraSlots(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (selectedTab == null || selectedTab.getType() != ItemGroup.Type.INVENTORY) {
			return;
		}
		HandledScreenAccessor screen = (HandledScreenAccessor) (HandledScreen<?>) (Object) this;
		ExtraSlotRenderer.drawAll(context, screen.workshop_utilities$getX(), screen.workshop_utilities$getY(), true);
	}
}
