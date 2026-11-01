package com.workshoputilities.client.mixin;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Posisi slot (x, y) bersifat final di vanilla; dibuka supaya slot Elytra bisa dipindah di layar Creative. */
@Mixin(Slot.class)
public interface SlotAccessor {
	@Mutable
	@Accessor("x")
	void workshop_utilities$setX(int x);

	@Mutable
	@Accessor("y")
	void workshop_utilities$setY(int y);
}
