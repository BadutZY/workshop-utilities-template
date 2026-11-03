package com.workshoputilities.screen;

/**
 * Dipasang ke {@link net.minecraft.screen.ScreenHandler} lewat mixin: menyimpan {@link PanelSupport} untuk GUI vanilla
 * (Crafting Table, Furnace, Anvil, dst.) yang diberi panel Backpack. Bernilai null untuk handler lain.
 */
public interface PanelHost {
	PanelSupport workshop_utilities$getPanel();

	void workshop_utilities$setPanel(PanelSupport panel);
}
