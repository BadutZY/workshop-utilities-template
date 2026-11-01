package com.workshoputilities.block;

import com.workshoputilities.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Block entity untuk animasi buku melayang di atas Upgrade Enchanting Table.
 * Animasinya (buku menoleh ke pemain terdekat dan membuka halaman) memakai logika vanilla:
 * satu EnchantingTableBlockEntity disimpan di dalam sini sebagai penampung state animasi,
 * lalu di-tick dan dirender oleh renderer vanilla (lihat UpgradeEnchantingTableRenderer di sisi client).
 */
public class UpgradeEnchantingTableBlockEntity extends BlockEntity {
    /** Penampung state animasi buku. Bukan block entity yang terpasang di world. */
    public final EnchantingTableBlockEntity book;

    public UpgradeEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPGRADE_ENCHANTING_TABLE, pos, state);
        // State vanilla dipakai karena konstruktor block entity vanilla memeriksa kecocokan block-nya.
        this.book = new EnchantingTableBlockEntity(pos, Blocks.ENCHANTING_TABLE.getDefaultState());
    }

    public static void tick(World world, BlockPos pos, BlockState state, UpgradeEnchantingTableBlockEntity entity) {
        EnchantingTableBlockEntity.tick(world, pos, state, entity.book);
    }
}