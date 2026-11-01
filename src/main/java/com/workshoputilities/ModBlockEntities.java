package com.workshoputilities;

import com.workshoputilities.block.UpgradeEnchantingTableBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    /** Hanya untuk animasi buku melayang di atas Upgrade Enchanting Table. */
    public static final BlockEntityType<UpgradeEnchantingTableBlockEntity> UPGRADE_ENCHANTING_TABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(WorkshopUtilitiesMod.MOD_ID, "upgrade_enchanting_table"),
            BlockEntityType.Builder.create(UpgradeEnchantingTableBlockEntity::new, ModBlocks.UPGRADE_ENCHANTING_TABLE).build());

    private ModBlockEntities() {
    }

    /** Dipanggil (setelah ModBlocks.init) agar class ini dimuat dan block entity terdaftar. */
    public static void init() {
    }
}