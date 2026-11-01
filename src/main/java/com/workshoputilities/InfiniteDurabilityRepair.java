package com.workshoputilities;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.workshoputilities.elytra.ElytraSlotHolder;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Memastikan item ber-Infinite Durability selalu kembali penuh durability-nya, dari sumber apa pun enchantment itu
 * didapat (anvil, enchanting table, /enchant, loot, trade, command block, dll).
 * Dicek tiap {@link #INTERVAL_TICKS} tick pada seluruh inventory pemain (termasuk armor, offhand, dan item di kursor).
 * Anvil ditangani juga secara langsung di {@link com.workshoputilities.mixin.AnvilScreenHandlerMixin}
 * sehingga hasil di slot output sudah tampil penuh sebelum diambil.
 */
public final class InfiniteDurabilityRepair {
    private static final int INTERVAL_TICKS = 5;

    private InfiniteDurabilityRepair() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(InfiniteDurabilityRepair::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getTicks() % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerInventory inventory = player.getInventory();
            // size() mencakup slot utama, armor, dan offhand.
            for (int i = 0; i < inventory.size(); i++) {
                ModEnchantments.restoreIfInfinite(inventory.getStack(i));
            }
            ModEnchantments.restoreIfInfinite(player.currentScreenHandler.getCursorStack());
            // Elytra di slot khusus juga ikut dipulihkan (lalu disinkronkan ke client).
            ElytraSlotHolder holder = ElytraSlotHolder.of(player);
            if (ModEnchantments.restoreIfInfinite(holder.workshop_utilities$getElytraSlot())) {
                holder.workshop_utilities$setElytraSlot(holder.workshop_utilities$getElytraSlot());
            }
        }
    }
}