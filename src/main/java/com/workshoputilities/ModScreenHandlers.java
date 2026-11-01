package com.workshoputilities;

import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.screen.BackpackScreenHandler;
import com.workshoputilities.screen.DisenchantingScreenHandler;
import com.workshoputilities.screen.UncraftingScreenHandler;
import com.workshoputilities.screen.UpgradeEnchantingScreenHandler;
import com.workshoputilities.screen.UpgradeTableScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class ModScreenHandlers {
	public static final ScreenHandlerType<UncraftingScreenHandler> UNCRAFTING =
			register("uncrafting_table", UncraftingScreenHandler::new);

	public static final ScreenHandlerType<DisenchantingScreenHandler> DISENCHANTING =
			register("disenchanting_table", DisenchantingScreenHandler::new);

	public static final ScreenHandlerType<UpgradeEnchantingScreenHandler> UPGRADE_ENCHANTING =
			register("upgrade_enchanting_table", UpgradeEnchantingScreenHandler::new);

	public static final ScreenHandlerType<UpgradeTableScreenHandler> UPGRADE_TABLE =
			register("upgrade_table", UpgradeTableScreenHandler::new);

	/** Satu tipe screen per tier Backpack (ukuran GUI berbeda-beda). */
	private static final Map<BackpackTier, ScreenHandlerType<BackpackScreenHandler>> BACKPACKS = new EnumMap<>(BackpackTier.class);

	static {
		for (BackpackTier tier : BackpackTier.values()) {
			BACKPACKS.put(tier, register(tier.id(),
					(syncId, inventory) -> new BackpackScreenHandler(BACKPACKS.get(tier), syncId, inventory, tier)));
		}
	}

	private ModScreenHandlers() {
	}

	public static ScreenHandlerType<BackpackScreenHandler> backpack(BackpackTier tier) {
		return BACKPACKS.get(tier);
	}

	private static <T extends ScreenHandler> ScreenHandlerType<T> register(String name, ScreenHandlerType.Factory<T> factory) {
		return Registry.register(Registries.SCREEN_HANDLER, Identifier.of(WorkshopUtilitiesMod.MOD_ID, name),
				new ScreenHandlerType<>(factory, FeatureFlags.VANILLA_FEATURES));
	}

	public static void init() {
	}
}
