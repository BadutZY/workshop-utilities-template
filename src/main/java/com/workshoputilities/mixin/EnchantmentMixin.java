package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlimited: "semua item kecuali yang punya durability" tidak bisa ditulis sebagai tag/daftar item di JSON
 * (apalagi item dari mod lain), jadi aturan dukungan item untuk enchantment ini dihitung di sini:
 * didukung bila stack tidak kosong dan tidak memakai sistem durability. Dipakai oleh anvil, /enchant, dll.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$unlimitedSupport(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		if (self.description().getContent() instanceof TranslatableTextContent content
				&& ModEnchantments.UNLIMITED_TRANSLATION_KEY.equals(content.getKey())) {
			cir.setReturnValue(!stack.isEmpty() && !stack.isDamageable());
		}
	}
}
