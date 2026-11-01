package com.workshoputilities.mixin;

import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.WornBackpackSlot;
import com.workshoputilities.elytra.ElytraContext;
import com.workshoputilities.elytra.ElytraSlot;
import com.workshoputilities.elytra.ElytraSlotHolder;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Elytra Slot (server): (1) saat client meminta mulai terbang (tekan lompat di udara), server mengecek elytra di slot dada.
 * Dibungkus supaya elytra di slot khusus ikut dipertimbangkan. Hanya aktif di thread server (paket diproses ulang
 * di thread utama; panggilan pertama dari thread jaringan tidak boleh membuka scope).
 * (2) Mode Creative (slot Elytra dan slot Backpack): perubahan isi inventory dikirim client lewat CreativeInventoryActionC2SPacket, dan vanilla di
 * server HANYA menerima nomor slot 1..45. Slot khusus bernomor 46 dan 47, jadi dulu perubahannya ditolak diam-diam: client
 * mengira elytra sudah dilepas, server masih menyimpannya, sehingga saat masuk game lagi elytra muncul dobel.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	@Inject(method = "onClientCommand", at = @At("HEAD"))
	private void workshop_utilities$commandEnter(ClientCommandC2SPacket packet, CallbackInfo ci) {
		if (player != null && player.server.isOnThread()) {
			ElytraContext.enterGlide();
		}
	}

	@Inject(method = "onClientCommand", at = @At("RETURN"))
	private void workshop_utilities$commandExit(ClientCommandC2SPacket packet, CallbackInfo ci) {
		if (player != null && player.server.isOnThread()) {
			ElytraContext.exitGlide();
		}
	}

	/**
	 * Creative: terima perubahan slot Elytra dan slot Backpack dari client. Paket untuk slot lain tetap diproses vanilla.
	 * Isi paket divalidasi (kosong, atau satu item yang sesuai) supaya aturan slot sama dengan di mode Survival; bila
	 * ditolak, isi slot yang sebenarnya dikirim ulang ke client agar kedua sisi tidak berbeda.
	 */
	@Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$creativeExtraSlots(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
		// Panggilan pertama datang dari thread jaringan; vanilla menjadwalkannya ulang ke thread server.
		if (player == null || !player.server.isOnThread()) {
			return;
		}
		ScreenHandler handler = player.playerScreenHandler;
		int slotId = packet.slot();
		if (slotId < 0 || slotId >= handler.slots.size()) {
			return;
		}
		Slot slot = handler.slots.get(slotId);
		ItemStack incoming = packet.stack();
		boolean creative = player.interactionManager.isCreative();
		if (slot instanceof ElytraSlot) {
			ci.cancel();
			ElytraSlotHolder holder = ElytraSlotHolder.of(player);
			boolean valid = creative
					&& (incoming.isEmpty() || (incoming.isOf(Items.ELYTRA) && incoming.getCount() <= 1));
			// setElytraSlot menyalin stack, jadi selalu tersinkron ke client (juga saat hanya "mengirim ulang").
			holder.workshop_utilities$setElytraSlot(valid ? incoming : holder.workshop_utilities$getElytraSlot());
			handler.sendContentUpdates();
		} else if (slot instanceof WornBackpackSlot) {
			ci.cancel();
			BackpackSlotHolder holder = BackpackSlotHolder.of(player);
			boolean valid = creative
					&& (incoming.isEmpty() || (incoming.getItem() instanceof BackpackItem && incoming.getCount() <= 1));
			holder.workshop_utilities$setBackpackSlot(valid ? incoming : holder.workshop_utilities$getBackpackSlot());
			handler.sendContentUpdates();
		}
	}
}
