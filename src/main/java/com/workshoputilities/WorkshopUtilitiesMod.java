package com.workshoputilities;

import net.fabricmc.api.ModInitializer;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.network.OpenBackpackPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.FloorSleep;
import com.workshoputilities.elytra.ElytraContext;
import com.workshoputilities.elytra.ElytraSlotHolder;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class WorkshopUtilitiesMod implements ModInitializer {
	public static final String MOD_ID = "workshop_utilities";

	@Override
	public void onInitialize() {
		ModItems.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItemGroups.init();
		ModScreenHandlers.init();
		InfiniteDurabilityRepair.init();
		EnderBlind.init();
		ToolEnchantments.init();
		ThunderSlash.init();
		Lifesteal.init();
		FloorSleep.init();
		// Tombol "buka backpack yang dipakai" (client mengirim, server membuka GUI tas di slot Backpack).
		PayloadTypeRegistry.playC2S().register(OpenBackpackPayload.ID, OpenBackpackPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenBackpackPayload.ID,
				(payload, context) -> BackpackItem.openWorn(context.player()));
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			UnlimitedScope.reset();
			ElytraContext.reset();
		});
		// Respawn / pindah dimensi: isi slot Elytra ikut pindah ke pemain baru (sudah kosong bila dijatuhkan saat mati).
		// Hal yang sama untuk slot Backpack.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			ElytraSlotHolder.of(newPlayer).workshop_utilities$setElytraSlot(
					ElytraSlotHolder.of(oldPlayer).workshop_utilities$getElytraSlot());
			BackpackSlotHolder.of(newPlayer).workshop_utilities$setBackpackSlot(
					BackpackSlotHolder.of(oldPlayer).workshop_utilities$getBackpackSlot());
		});
	}
}