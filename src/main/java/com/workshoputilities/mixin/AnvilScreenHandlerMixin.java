package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anvil: enchantment buatan mod tidak boleh dipindah lewat Anvil, dan hasil anvil yang ber-Infinite Durability
 * langsung dikembalikan ke durability penuh. (Upgrade Book kini dibuat di crafting table, bukan di Anvil.)
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
	@Shadow
	private int repairItemUsage;

	@Shadow
	@Final
	private Property levelCost;

	/**
	 * Setelah anvil selesai menghitung hasil: bila item kanan (enchanted book atau item ber-enchant) membawa
	 * enchantment buatan mod yang belum dimiliki item kiri, hasil dikosongkan sehingga anvil menampilkan "X".
	 * Enchantment buatan mod hanya bisa dipasang lewat Upgrade Table. Berlaku juga di Creative.
	 * Selain itu, hasil anvil yang ter-enchant Infinite Durability langsung penuh durability-nya.
	 */
	@Inject(method = "updateResult", at = @At("RETURN"))
	private void workshop_utilities$validateAndRestore(CallbackInfo ci) {
		ScreenHandler self = (ScreenHandler) (Object) this;
		ItemStack left = self.getSlot(0).getStack();
		ItemStack right = self.getSlot(1).getStack();
		if (!self.getSlot(2).getStack().isEmpty() && ModEnchantments.wouldTransferModEnchantment(left, right)) {
			self.getSlot(2).setStack(ItemStack.EMPTY);
			this.levelCost.set(0);
			this.repairItemUsage = 0;
			self.sendContentUpdates();
			return;
		}
		ModEnchantments.restoreIfInfinite(self.getSlot(2).getStack());
	}
}
