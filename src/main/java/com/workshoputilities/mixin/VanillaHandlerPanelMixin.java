package com.workshoputilities.mixin;

import com.workshoputilities.screen.PanelSupport;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Memberi panel Backpack (+ Tools Holder) ke GUI block vanilla yang memakai item dari luar: Crafting Table, Furnace,
 * Blast Furnace, Smoker, Anvil, Smithing Table, Grindstone, Stonecutter, Loom, Cartography Table, Enchanting Table,
 * Brewing Stand, Crafter, serta block penyimpanan: Dispenser, Dropper ({@link Generic3x3ContainerScreenHandler}), Hopper dan
 * Minecart Hopper ({@link HopperScreenHandler}), Chest, Trapped Chest, Double Chest, Minecart Chest, dan Barrel
 * ({@link GenericContainerScreenHandler}), Shulker Box ({@link ShulkerBoxScreenHandler}), jendela trading Villager / Wandering Trader
 * ({@link MerchantScreenHandler}), dan Beacon ({@link BeaconScreenHandler}). Block yang fungsi utamanya soal armor / tool / senjata (Anvil, Smithing Table, Grindstone, Enchanting
 * Table) juga mendapat panel equipment. Crafter ikut mendapat panel supaya semua block utility vanilla konsisten. Slot ditambahkan di akhir constructor, jadi nomor slot vanilla tidak bergeser.
 * Constructor yang saling memanggil ({@code this(...)}) aman: pemasangan kedua dilewati.
 */
@Mixin({CraftingScreenHandler.class, AbstractFurnaceScreenHandler.class, ForgingScreenHandler.class,
		GrindstoneScreenHandler.class, StonecutterScreenHandler.class, LoomScreenHandler.class,
		CartographyTableScreenHandler.class, EnchantmentScreenHandler.class, BrewingStandScreenHandler.class,
		CrafterScreenHandler.class, Generic3x3ContainerScreenHandler.class, HopperScreenHandler.class,
		GenericContainerScreenHandler.class, ShulkerBoxScreenHandler.class, MerchantScreenHandler.class,
		BeaconScreenHandler.class})
public abstract class VanillaHandlerPanelMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void workshop_utilities$attachPanel(CallbackInfo ci) {
		ScreenHandler self = (ScreenHandler) (Object) this;
		boolean equipment = PanelSupport.vanillaHasEquipment(self);
		PanelSupport.attachVanilla(self, equipment);
	}
}
