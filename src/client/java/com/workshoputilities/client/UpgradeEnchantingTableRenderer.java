package com.workshoputilities.client;

import com.workshoputilities.block.UpgradeEnchantingTableBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.EnchantingTableBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

/** Menggambar buku melayang di atas Upgrade Enchanting Table dengan renderer enchanting table vanilla. */
public class UpgradeEnchantingTableRenderer implements BlockEntityRenderer<UpgradeEnchantingTableBlockEntity> {
    private final EnchantingTableBlockEntityRenderer vanilla;

    public UpgradeEnchantingTableRenderer(BlockEntityRendererFactory.Context context) {
        this.vanilla = new EnchantingTableBlockEntityRenderer(context);
    }

    @Override
    public void render(UpgradeEnchantingTableBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        this.vanilla.render(entity.book, tickDelta, matrices, vertexConsumers, light, overlay);
    }
}