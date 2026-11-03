package com.workshoputilities.client.mixin;

import com.workshoputilities.client.PanelController;
import com.workshoputilities.client.PanelScreenAccess;
import com.workshoputilities.client.PanelHandledScreen;
import com.workshoputilities.screen.PanelSupport;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Menempelkan panel Backpack ke GUI block vanilla yang handler-nya diberi {@link PanelSupport} (lihat VanillaHandlerPanelMixin):
 * membuat {@link PanelController} saat GUI di-init dan meneruskan klik / seret / lepas mouse ke scrollbar panel.
 * Roda mouse ditangani lewat event Fabric (lihat WorkshopUtilitiesClient). GUI block mod sendiri tidak terpengaruh.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenPanelMixin implements PanelScreenAccess {
	@Unique
	private PanelController workshop_utilities$controller;

	@Override
	public PanelController workshop_utilities$getPanelController() {
		return this.workshop_utilities$controller;
	}

	@Inject(method = "init()V", at = @At("TAIL"))
	private void workshop_utilities$initPanel(CallbackInfo ci) {
		HandledScreen<?> self = (HandledScreen<?>) (Object) this;
		if (this.workshop_utilities$controller != null) {
			this.workshop_utilities$controller.dispose();
			this.workshop_utilities$controller = null;
		}
		if (self instanceof PanelHandledScreen<?>) {
			return;
		}
		PanelSupport support = PanelController.supportOf(self.getScreenHandler());
		if (support == null) {
			return;
		}
		// Panel backpack selalu di kiri (sama di semua GUI); PanelController menggesernya bila recipe book terbuka.
		this.workshop_utilities$controller = new PanelController(self, support, false);
		this.workshop_utilities$controller.init();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$panelClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (this.workshop_utilities$controller != null && this.workshop_utilities$controller.mouseClicked(mouseX, mouseY, button)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$panelDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
			CallbackInfoReturnable<Boolean> cir) {
		if (this.workshop_utilities$controller != null && this.workshop_utilities$controller.mouseDragged(mouseY)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$panelReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (this.workshop_utilities$controller != null && this.workshop_utilities$controller.mouseReleased(button)) {
			cir.setReturnValue(true);
		}
	}

	/** Metode vanilla (diakses lewat shadow supaya override milik subclass, mis. CraftingScreen / AbstractFurnaceScreen, tetap dipakai). */
	@Shadow
	protected abstract boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button);

	/**
	 * Klik di panel / tombol bukan klik di luar GUI (kalau tidak, item yang diambil dari panel malah terbuang: vanilla mengubah
	 * klik "di luar GUI" menjadi aksi THROW / drop).
	 *
	 * Penanganan dipasang di PEMANGGILNYA ({@code mouseClicked} dan {@code mouseReleased}), bukan di isi method
	 * {@code isClickOutsideBounds}. Beberapa GUI vanilla (Crafting Table, Furnace, Blast Furnace, Smoker, karena recipe book)
	 * meng-override method itu, sehingga injeksi di HandledScreen tidak pernah dijalankan untuk mereka dan item di panel
	 * terbuang saat diklik. Dengan cara ini semua GUI vanilla, termasuk yang meng-override, tercakup.
	 */
	@Redirect(method = {"mouseClicked", "mouseReleased"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;isClickOutsideBounds(DDIII)Z"))
	private boolean workshop_utilities$clickOutsideBounds(HandledScreen<?> instance, double mouseX, double mouseY, int left, int top,
			int button) {
		if (this.workshop_utilities$controller != null && this.workshop_utilities$controller.isOverExtraArea(mouseX, mouseY)) {
			return false;
		}
		return this.isClickOutsideBounds(mouseX, mouseY, left, top, button);
	}

	/** Jaring cadangan untuk GUI yang tidak meng-override isClickOutsideBounds (alur utama ada di {@link #workshop_utilities$clickOutsideBounds}). */
	@Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$panelOutside(double mouseX, double mouseY, int left, int top, int button,
			CallbackInfoReturnable<Boolean> cir) {
		if (this.workshop_utilities$controller != null && this.workshop_utilities$controller.isOverExtraArea(mouseX, mouseY)) {
			cir.setReturnValue(false);
		}
	}
}
