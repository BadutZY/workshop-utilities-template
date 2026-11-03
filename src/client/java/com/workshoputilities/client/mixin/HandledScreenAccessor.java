package com.workshoputilities.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Akses posisi dan ukuran GUI (x, y, backgroundWidth, backgroundHeight) layar bertipe HandledScreen. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x")
	int workshop_utilities$getX();

	@Accessor("x")
	void workshop_utilities$setX(int x);

	@Accessor("y")
	int workshop_utilities$getY();

	@Accessor("backgroundWidth")
	int workshop_utilities$getBackgroundWidth();

	@Accessor("backgroundHeight")
	int workshop_utilities$getBackgroundHeight();
}
