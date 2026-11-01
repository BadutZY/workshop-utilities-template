package com.workshoputilities;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Utilitas XP pemain (dipakai server dan client).
 *
 * PENTING: field {@code player.totalExperience} bukan XP saat ini. Field itu tidak ikut berubah saat level
 * dikurangi/ditambah lewat anvil, enchanting table, atau command /xp ... levels, jadi tidak boleh dipakai untuk
 * mengecek atau mengurangi XP. XP asli pemain = level + progress bar, dan itulah yang dihitung di sini.
 */
public final class XpHelper {
	private XpHelper() {
	}

	/** Jumlah poin XP yang dibutuhkan untuk naik dari {@code level} ke {@code level + 1} (rumus vanilla). */
	public static int pointsToNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	/** Total poin XP yang dimiliki pemain saat ini (dihitung dari level + progress bar). */
	public static int getPoints(PlayerEntity player) {
		int level = Math.max(0, player.experienceLevel);
		int points = 0;
		for (int i = 0; i < level; i++) {
			points += pointsToNextLevel(i);
		}
		points += Math.round(player.experienceProgress * pointsToNextLevel(level));
		return points;
	}

	/**
	 * Menetapkan XP pemain ke jumlah poin tertentu (level dan progress bar dihitung ulang), lalu disinkronkan ke client.
	 * Level tidak dinaikkan lewat addExperience sehingga tidak ada suara level-up palsu.
	 */
	public static void setPoints(PlayerEntity player, int points) {
		int remaining = Math.max(0, points);
		int level = 0;
		while (remaining >= pointsToNextLevel(level)) {
			remaining -= pointsToNextLevel(level);
			level++;
		}
		player.experienceLevel = level;
		player.experienceProgress = remaining / (float) pointsToNextLevel(level);
		player.totalExperience = Math.max(0, points);
		// Paksa sinkronisasi ke client agar bar XP langsung berubah.
		player.addExperience(0);
		if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer.networkHandler != null) {
			serverPlayer.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(
					player.experienceProgress, player.totalExperience, player.experienceLevel));
		}
	}

	/** Mengurangi XP sebanyak {@code amount} poin. Mengembalikan false (dan tidak mengubah apa pun) bila XP kurang. */
	public static boolean spend(PlayerEntity player, int amount) {
		int current = getPoints(player);
		if (current < amount) {
			return false;
		}
		setPoints(player, current - amount);
		return true;
	}
}
