package com.workshoputilities.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Akses posisi panel (x, y) layar bertipe HandledScreen. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x")
	int workshop_utilities$getX();

	@Accessor("y")
	int workshop_utilities$getY();
}
