package com.workshoputilities.client;

import com.workshoputilities.ModEnchantments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

/**
 * Double Jump (boots): setelah pemain melompat dari tanah, menekan tombol lompat sekali lagi (tombol harus dilepas
 * dulu lalu ditekan lagi) di udara memberi satu lompatan tambahan seperti lompatan biasa. Sekali per lompatan dari
 * tanah; kesempatannya pulih saat menyentuh tanah. Tanpa efek visual, suara, atau mekanik tambahan.
 *
 * Gerakan pemain ditentukan oleh client (server hanya memvalidasi hasilnya), jadi logikanya cukup di sisi client dan
 * tidak butuh paket jaringan. Tidak aktif saat terbang (creative/Elytra), di air/lava, memanjat, atau menunggang.
 */
public final class DoubleJump {
	/**
	 * Kecepatan vertikal lompatan ekstra. Lompatan vanilla (0.42) setinggi ~1.25 block; 0.502 menambah ~1.72 block,
	 * jadi bila ditekan di puncak lompatan pertama total tinggi sekitar 2.97 block (hampir 3). Sengaja tidak sampai
	 * 3.0 penuh supaya tidak ada damage jatuh 0.5 hati (damage baru muncul bila jarak jatuh lebih dari 3 block).
	 */
	private static final double JUMP_VELOCITY = 0.550;

	private static boolean wasOnGround = true;
	private static boolean wasJumping = false;
	/** True bila pemain meninggalkan tanah lewat lompatan (bukan jatuh dari tepi) dan belum memakai lompatan ekstra. */
	private static boolean canDoubleJump = false;

	private DoubleJump() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(DoubleJump::tick);
	}

	private static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			wasOnGround = true;
			wasJumping = false;
			canDoubleJump = false;
			return;
		}
		boolean jumping = player.input.jumping;
		boolean onGround = player.isOnGround();

		boolean grounded = onGround || player.isTouchingWater() || player.isInLava() || player.hasVehicle()
				|| player.isClimbing() || player.isFallFlying() || player.isSpectator()
				|| player.getAbilities().flying;
		if (grounded) {
			canDoubleJump = false;
		} else if (wasOnGround && player.getVelocity().y > 0.0) {
			// Baru saja meninggalkan tanah dengan gerak naik = melompat. Lompatan ekstra baru bisa di tick berikutnya.
			canDoubleJump = true;
		} else if (canDoubleJump && jumping && !wasJumping
				&& ModEnchantments.hasDoubleJump(player.getEquippedStack(EquipmentSlot.FEET))) {
			double velocity = JUMP_VELOCITY;
			StatusEffectInstance boost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
			if (boost != null) {
				velocity += 0.1 * (boost.getAmplifier() + 1);
			}
			Vec3d current = player.getVelocity();
			player.setVelocity(current.x, velocity, current.z);
			canDoubleJump = false;
		}
		wasOnGround = onGround;
		wasJumping = jumping;
	}
}
