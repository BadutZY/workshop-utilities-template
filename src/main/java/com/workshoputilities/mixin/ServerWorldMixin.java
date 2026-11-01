package com.workshoputilities.mixin;

import com.workshoputilities.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Silent Step (getaran): semua game event bergetar (langkah, mendarat setelah loncat, memasang/menghancurkan block,
 * menyerang, dll; yaitu tag {@code #minecraft:vibrations}) yang dipicu makhluk berleggings Silent Step dibuang sebelum
 * dikirim ke listener. Akibatnya sculk sensor, calibrated sculk sensor, sculk shrieker, dan Warden tidak mendeteksinya sama sekali.
 * Game event non-getaran (misalnya yang dipakai Allay atau sculk catalyst) tidak disentuh.
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
	private static final TagKey<GameEvent> VIBRATIONS =
			TagKey.of(RegistryKeys.GAME_EVENT, Identifier.of("minecraft", "vibrations"));

	@Inject(method = "emitGameEvent(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/event/GameEvent$Emitter;)V",
			at = @At("HEAD"), cancellable = true)
	private void workshop_utilities$silentStep(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter,
			CallbackInfo ci) {
		Entity source = emitter.sourceEntity();
		if (source instanceof LivingEntity living && event.isIn(VIBRATIONS) && ModEnchantments.isSilentStepping(living)) {
			ci.cancel();
		}
	}
}
