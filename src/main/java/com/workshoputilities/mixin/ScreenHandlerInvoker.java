package com.workshoputilities.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Membuka method protected ScreenHandler (addSlot, addProperties, insertItem) untuk {@link com.workshoputilities.screen.PanelSupport}. */
@Mixin(ScreenHandler.class)
public interface ScreenHandlerInvoker {
	@Invoker("addSlot")
	Slot workshop_utilities$addSlot(Slot slot);

	@Invoker("addProperties")
	void workshop_utilities$addProperties(PropertyDelegate delegate);

	@Invoker("insertItem")
	boolean workshop_utilities$insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
}
