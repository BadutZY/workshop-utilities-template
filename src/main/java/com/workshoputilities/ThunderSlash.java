package com.workshoputilities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thunder Slash: setiap serangan ketiga yang berhasil mengenai makhluk lain (dengan sword ber-Thunder Slash di tangan)
 * memanggil petir ke target dan memberi bonus damage. Petir dibuat kosmetik supaya tidak ikut menyambar pemain yang
 * berdiri dekat target; damage petir (5) dan api diberikan langsung ke target lewat {@code onStruckByLightning}.
 * Hook-nya ada di {@link com.workshoputilities.mixin.LivingEntityMixin}.
 */
public final class ThunderSlash {
	/** Serangan ke-berapa yang memicu petir. */
	private static final int HITS_REQUIRED = 3;
	/** Bonus damage tambahan di luar damage petir bawaan (5). 2 damage = 1 hati. */
	private static final float BONUS_DAMAGE = 4.0F;

	private static final Map<UUID, Integer> HITS = new HashMap<>();

	private ThunderSlash() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HITS.remove(handler.getPlayer().getUuid()));
	}

	public static void onHit(LivingEntity victim, DamageSource source) {
		if (victim.getWorld().isClient || !source.isOf(DamageTypes.PLAYER_ATTACK)) {
			return;
		}
		if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
			return;
		}
		UUID id = player.getUuid();
		if (!ModEnchantments.hasThunderSlash(player.getMainHandStack())) {
			HITS.remove(id);
			return;
		}
		int count = HITS.merge(id, 1, Integer::sum);
		if (count < HITS_REQUIRED) {
			return;
		}
		HITS.put(id, 0);

		ServerWorld world = (ServerWorld) victim.getWorld();
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(victim.getPos());
			bolt.setCosmetic(true);
			bolt.setChanneler(player);
			world.spawnEntity(bolt);
			// Hapus invulnerability frame supaya damage petir dan bonus tidak tertelan oleh pukulan sword barusan.
			victim.timeUntilRegen = 0;
			victim.onStruckByLightning(world, bolt);
		}
		victim.timeUntilRegen = 0;
		victim.damage(world.getDamageSources().indirectMagic(player, player), BONUS_DAMAGE);
	}
}
