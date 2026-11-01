package com.workshoputilities;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Ender Blind (bagian "menenangkan"): selama pemain memakai helmet ber-Ender Blind, setiap Enderman di sekitarnya
 * yang sedang marah kepada pemain itu langsung dibuat normal lagi (target dan amarah dihapus).
 * Bagian "tidak marah saat dilihat" ada di {@link com.workshoputilities.mixin.EndermanEntityMixin}.
 */
public final class EnderBlind {
	private static final int INTERVAL_TICKS = 2;
	/** Jarak (blok) Enderman yang ditenangkan di sekitar pemain. */
	private static final double RANGE = 64.0;

	private EnderBlind() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(EnderBlind::onServerTick);
	}

	private static void onServerTick(MinecraftServer server) {
		if (server.getTicks() % INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isSpectator() || !ModEnchantments.hasEnderBlind(player.getEquippedStack(EquipmentSlot.HEAD))) {
				continue;
			}
			ServerWorld world = player.getServerWorld();
			Box area = player.getBoundingBox().expand(RANGE);
			List<EndermanEntity> endermen = world.getEntitiesByClass(EndermanEntity.class, area,
					enderman -> enderman.getTarget() == player || player.getUuid().equals(enderman.getAngryAt()));
			for (EndermanEntity enderman : endermen) {
				enderman.stopAnger();
				enderman.setTarget(null);
			}
		}
	}
}
