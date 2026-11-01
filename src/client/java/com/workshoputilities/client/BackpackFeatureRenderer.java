package com.workshoputilities.client;

import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.elytra.ElytraSlotHolder;
import com.workshoputilities.item.BackpackItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;

/**
 * Menggambar Backpack 3D di punggung pemain saat tas dipakai di slot Backpack.
 *
 * Yang digambar adalah model item itu sendiri (models/item/<tier>_backpack.json), jadi bentuk di punggung,
 * di inventory, dan di tangan selalu sama dan ikut berubah bila model/texture diganti (juga lewat resource pack).
 * Posisi mengikuti badan pemain (body.rotate), sehingga ikut membungkuk saat sneak, berenang, atau terbang.
 *
 * Konstanta TRANSLATE_* (piksel model) memetakan koordinat model item ke ruang model pemain; nilainya sama dengan
 * T_X/T_Y/T_Z di tools/preview_backpack.py. Sisi NORTH model (rata) menempel di punggung, sisi SOUTH menghadap keluar.
 */
public class BackpackFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final float TRANSLATE_X = 0.0F;
	private static final float TRANSLATE_Y = 4.5F;
	private static final float TRANSLATE_Z = 7.3F;
	/**
	 * Sisi dalam model tas berada sekitar 2,3 px di belakang pusat badan. Chestplate (dilation 1) menonjol sampai
	 * 3 px dan sayap Elytra sampai 5 px, jadi tas digeser ke belakang sesuai yang dipakai agar tidak menembusnya.
	 */
	private static final float ARMOR_EXTRA_Z = 1.0F;
	private static final float ELYTRA_EXTRA_Z = 3.0F;

	public BackpackFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
			float animationProgress, float headYaw, float headPitch) {
		ItemStack stack = BackpackSlotHolder.of(player).workshop_utilities$getBackpackSlot();
		if (!(stack.getItem() instanceof BackpackItem) || player.isSpectator() || player.isInvisible()) {
			return;
		}
		// getEquippedStack(CHEST) di sini nilai asli slot dada (bukan dalam konteks render elytra).
		ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
		float extraZ = 0.0F;
		if (chest.isOf(Items.ELYTRA) || !ElytraSlotHolder.of(player).workshop_utilities$getElytraSlot().isEmpty()) {
			extraZ = ELYTRA_EXTRA_Z;
		} else if (!chest.isEmpty()) {
			extraZ = ARMOR_EXTRA_Z;
		}

		matrices.push();
		// Mulai dari pivot badan (ikut pose sneak/berenang), lalu geser ke punggung.
		this.getContextModel().body.rotate(matrices);
		matrices.translate(TRANSLATE_X / 16.0F, TRANSLATE_Y / 16.0F, (TRANSLATE_Z + extraZ) / 16.0F);
		// Ruang model pemain berbalik (Y ke bawah); putar 180 derajat di sumbu Z agar model item tegak.
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.NONE, light,
				LivingEntityRenderer.getOverlay(player, 0.0F), matrices, vertexConsumers, player.getWorld(),
				player.getId());
		matrices.pop();
	}
}
