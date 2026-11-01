package com.workshoputilities;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Lifesteal (sword dan axe): setiap kali pemain membunuh makhluk dengan serangan langsung memakai senjata
 * ber-Lifesteal di tangan, health pemain dipulihkan secara acak {@link #MIN_HEARTS}..{@link #MAX_HEARTS} hati
 * (1 hati = 2 health). Pemulihan tidak pernah melewati health maksimal.
 */
public final class Lifesteal {
	/** Pemulihan terkecil, dalam hati (1 hati = 2 health). */
	private static final int MIN_HEARTS = 1;
	/** Pemulihan terbesar, dalam hati. */
	private static final int MAX_HEARTS = 5;

	private Lifesteal() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Lifesteal::onDeath);
	}

	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (victim.getWorld().isClient) {
			return;
		}
		// Hanya pembunuhan langsung oleh pemain (bukan panah/api/petir), dan bukan bunuh diri.
		if (!(source.getAttacker() instanceof ServerPlayerEntity player) || source.getSource() != player || player == victim) {
			return;
		}
		if (player.isSpectator() || !player.isAlive() || !ModEnchantments.hasLifesteal(player.getMainHandStack())) {
			return;
		}
		int hearts = MIN_HEARTS + player.getRandom().nextInt(MAX_HEARTS - MIN_HEARTS + 1);
		player.heal(hearts * 2.0F);
	}
}
