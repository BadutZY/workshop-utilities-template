package com.workshoputilities;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Pemberi advancement untuk aksi yang tidak punya trigger vanilla (memakai table, tidur dengan Bed Module).
 * Advancement lainnya (craft / punya item / punya enchantment) sepenuhnya lewat datapack:
 * data/workshop_utilities/advancement/&lt;nama&gt;.json. Advancement yang diberikan lewat kode memakai trigger
 * {@code minecraft:impossible} dengan satu kriteria bernama {@code done}.
 */
public final class ModAdvancements {
	private static final String CRITERION = "done";

	private ModAdvancements() {
	}

	/** Beri advancement {@code workshop_utilities:<name>} ke pemain (server saja; tidak berbuat apa-apa bila bukan server / tidak ada). */
	public static void grant(PlayerEntity player, String name) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		MinecraftServer server = serverPlayer.getServer();
		if (server == null) {
			return;
		}
		AdvancementEntry entry = server.getAdvancementLoader().get(Identifier.of(WorkshopUtilitiesMod.MOD_ID, name));
		if (entry != null) {
			serverPlayer.getAdvancementTracker().grantCriterion(entry, CRITERION);
		}
	}
}
