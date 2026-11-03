package com.workshoputilities.client.mixin;

import com.workshoputilities.client.PanelController;
import com.workshoputilities.client.PanelScreenAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
import net.minecraft.client.gui.screen.ingame.CrafterScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.LoomScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Menggambar panel Backpack di awal drawBackground GUI block vanilla, sebelum texture GUI utama, supaya sambungan panel
 * tertutup rapi (sama seperti GUI block mod ini). Anvil dan Smithing Table lewat ForgingScreen; Crafter juga ikut. Dispenser / Dropper, Hopper, serta Chest / Barrel (GenericContainerScreen), Shulker Box, jendela trading Villager (MerchantScreen), dan Beacon juga memakai panel yang sama.
 */
@Mixin({CraftingScreen.class, AbstractFurnaceScreen.class, ForgingScreen.class, GrindstoneScreen.class, StonecutterScreen.class,
		LoomScreen.class, CartographyTableScreen.class, EnchantmentScreen.class, BrewingStandScreen.class, CrafterScreen.class,
		Generic3x3ContainerScreen.class, HopperScreen.class, GenericContainerScreen.class, ShulkerBoxScreen.class,
		MerchantScreen.class, BeaconScreen.class})
public abstract class VanillaScreenBackgroundMixin {
	@Inject(method = "drawBackground", at = @At("HEAD"), require = 0)
	private void workshop_utilities$drawPanels(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		PanelController controller = ((PanelScreenAccess) (Object) this).workshop_utilities$getPanelController();
		if (controller != null) {
			controller.draw(context);
		}
	}
}
