package com.workshoputilities.client.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Membuka addDrawableChild (protected) supaya tombol panel bisa dipasang ke screen vanilla. */
@Mixin(Screen.class)
public interface ScreenInvoker {
	@Invoker("addDrawableChild")
	<T extends Element & Drawable & Selectable> T workshop_utilities$addDrawableChild(T drawableElement);
}
