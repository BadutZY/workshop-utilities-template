package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ThunderSlash;
import com.workshoputilities.UnlimitedScope;
import com.workshoputilities.elytra.ElytraContext;
import com.workshoputilities.elytra.ElytraSlotHolder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Frozen Ward: bila salah satu armor yang sedang dipakai (helmet, chestplate, leggings, atau boots) ter-enchant
 * Frozen Ward, pemakainya tidak bisa freeze. Efek beku di powder snow (layar membeku dan damage dingin)
 * tidak muncul, sama seperti pemakai leather armor.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	@Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$frozenWard(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (ModEnchantments.hasFrozenWard(self.getEquippedStack(slot))) {
				cir.setReturnValue(false);
				return;
			}
		}
	}

	/** Thunder Slash: dipanggil setiap kali makhluk ini benar-benar menerima damage (damage() mengembalikan true). */
	@Inject(method = "damage", at = @At("RETURN"))
	private void workshop_utilities$thunderSlash(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			ThunderSlash.onHit((LivingEntity) (Object) this, source);
		}
	}

	/**
	 * Water Breathing: helmet ber-Water Breathing membuat pemakai dianggap bisa bernapas di air (seperti Drowned/ikan),
	 * jadi oksigen tidak berkurang dan malah terisi lagi. Tanpa status effect, jadi tidak ada ikon di layar.
	 */
	@Inject(method = "canBreatheInWater", at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$waterBreathing(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (ModEnchantments.hasWaterBreathing(self.getEquippedStack(EquipmentSlot.HEAD))) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Swim Fast: chestplate ber-Swim Fast mengganti redaman air vanilla (0.8, atau 0.9 saat sprint) menjadi 0.96 seperti
	 * Dolphin's Grace. Vanilla sudah mengalikan kecepatan dengan redaman itu, jadi cukup dikalikan rasio selisihnya.
	 */
	@Inject(method = "travel", at = @At("TAIL"))
	private void workshop_utilities$swimFast(Vec3d movementInput, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.isTouchingWater() || self.hasVehicle()
				|| !ModEnchantments.hasSwimFast(self.getEquippedStack(EquipmentSlot.CHEST))) {
			return;
		}
		if (self instanceof PlayerEntity player && player.getAbilities().flying) {
			return;
		}
		double vanillaDrag = self.isSprinting() ? 0.9 : 0.8;
		double ratio = 0.96 / vanillaDrag;
		Vec3d v = self.getVelocity();
		self.setVelocity(v.x * ratio, self.isSwimming() ? v.y * ratio : v.y, v.z * ratio);
	}

	/**
	 * Water Breathing: pastikan oksigen langsung penuh selama helmet dipakai (bukan hanya berhenti berkurang),
	 * sehingga oksigen sisa dari menyelam tanpa helmet tidak "membeku" di angka lama.
	 */
	@Inject(method = "baseTick", at = @At("TAIL"))
	private void workshop_utilities$refillAir(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.getAir() < self.getMaxAir()
				&& ModEnchantments.hasWaterBreathing(self.getEquippedStack(EquipmentSlot.HEAD))) {
			self.setAir(self.getMaxAir());
		}
	}

	/**
	 * Unlimited: totem dikonsumsi di tryUseTotem (saat damage fatal), bukan lewat use() sehingga tidak ikut scope
	 * pemakaian. Dibungkus di sini supaya totem ber-Unlimited tidak berkurang saat menyelamatkan pemakainya.
	 */
	@Inject(method = "tryUseTotem", at = @At("HEAD"))
	private void workshop_utilities$totemEnter(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		UnlimitedScope.enter();
	}

	@Inject(method = "tryUseTotem", at = @At("RETURN"))
	private void workshop_utilities$totemExit(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		UnlimitedScope.exit();
	}

	/**
	 * Elytra Slot: tickFallFlying mengecek dan men-damage elytra di slot dada. Dibungkus supaya Elytra di slot khusus
	 * ikut terpakai; sesudahnya, perubahan durability disinkronkan ke client.
	 */
	@Inject(method = "tickFallFlying", at = @At("HEAD"))
	private void workshop_utilities$glideEnter(CallbackInfo ci) {
		ElytraContext.enterGlide();
	}

	@Inject(method = "tickFallFlying", at = @At("RETURN"))
	private void workshop_utilities$glideExit(CallbackInfo ci) {
		ElytraContext.exitGlide();
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof PlayerEntity player && !player.getWorld().isClient) {
			ElytraSlotHolder.of(player).workshop_utilities$syncElytraSlot();
		}
	}
}
