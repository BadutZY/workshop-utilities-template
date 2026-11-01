package com.workshoputilities.client.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.List;

/**
 * Satu display REI untuk semua kategori Workshop Utilities. Data aslinya ({@link #data()}) berasal dari
 * {@link com.workshoputilities.client.compat.ArcaneCompat}; tiap kategori di {@link ReiCategories} tahu tipe data miliknya.
 */
public class ArcaneDisplay extends BasicDisplay {
	private final CategoryIdentifier<ArcaneDisplay> category;
	private final Object data;

	public ArcaneDisplay(CategoryIdentifier<ArcaneDisplay> category, List<EntryIngredient> inputs,
			List<EntryIngredient> outputs, Object data) {
		super(inputs, outputs);
		this.category = category;
		this.data = data;
	}

	public Object data() {
		return this.data;
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return this.category;
	}
}
