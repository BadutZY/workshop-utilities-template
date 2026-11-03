package com.workshoputilities.mixin;

import com.workshoputilities.screen.PanelHost;
import com.workshoputilities.screen.PanelSupport;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Menyimpan {@link PanelSupport} di ScreenHandler vanilla yang diberi panel Backpack, lalu meneruskan tiga kejadian
 * yang dibutuhkan panel: sinkronisasi tiap tick, Shift-click pada slot panel, dan penyimpanan saat GUI ditutup.
 * Untuk handler tanpa panel ({@code panel == null}) semuanya tidak berbuat apa-apa.
 */
@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerPanelMixin implements PanelHost {
	@Unique
	private PanelSupport workshop_utilities$panel;

	@Override
	public PanelSupport workshop_utilities$getPanel() {
		return this.workshop_utilities$panel;
	}

	@Override
	public void workshop_utilities$setPanel(PanelSupport panel) {
		this.workshop_utilities$panel = panel;
	}

	@Inject(method = "sendContentUpdates", at = @At("HEAD"))
	private void workshop_utilities$refreshPanel(CallbackInfo ci) {
		if (this.workshop_utilities$panel == null) {
			PanelSupport.attachVanillaIfMissing((ScreenHandler) (Object) this);
		}
		if (this.workshop_utilities$panel != null) {
			this.workshop_utilities$panel.refresh();
		}
	}

	@Inject(method = "onClosed", at = @At("TAIL"))
	private void workshop_utilities$savePanel(PlayerEntity player, CallbackInfo ci) {
		if (this.workshop_utilities$panel != null) {
			this.workshop_utilities$panel.saveAll();
		}
	}

	/**
	 * Setelah setiap klik slot selesai diproses, samakan panel dengan backpack yang sebenarnya (dua arah). Ini jaring pengaman
	 * di atas listener slot: apa pun cara vanilla mengubah slot panel, isinya tidak akan berbeda dari backpack.
	 */
	@Inject(method = "onSlotClick", at = @At("RETURN"))
	private void workshop_utilities$reconcileAfterClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		if (this.workshop_utilities$panel != null) {
			this.workshop_utilities$panel.refresh();
		}
	}

	/** Shift-click pada slot panel ditangani sendiri supaya quickMove vanilla tidak pernah menerima indeks slot yang tidak dikenalnya. */
	@Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$panelQuickMove(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		if (this.workshop_utilities$panel != null && actionType == SlotActionType.QUICK_MOVE
				&& this.workshop_utilities$panel.interceptQuickMove(player, slotIndex)) {
			ci.cancel();
		}
	}
}
