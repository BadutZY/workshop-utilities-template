package com.workshoputilities.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Item biasa dengan satu baris keterangan abu-abu di tooltip, dan (opsional) kilau enchanted seperti enchanted book. */
public class LoreItem extends Item {
	private final String loreKey;
	private final boolean glint;

	public LoreItem(Settings settings, String loreKey) {
		this(settings, loreKey, false);
	}

	public LoreItem(Settings settings, String loreKey, boolean glint) {
		super(settings);
		this.loreKey = loreKey;
		this.glint = glint;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return this.glint || super.hasGlint(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(this.loreKey).formatted(Formatting.GRAY));
	}
}