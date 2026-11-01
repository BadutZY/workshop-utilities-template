package com.workshoputilities.block;

import com.mojang.serialization.MapCodec;
import com.workshoputilities.ModBlockEntities;
import com.workshoputilities.screen.UpgradeEnchantingScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Upgrade Enchanting Table: bentuk dan efek partikel seperti enchanting table vanilla,
 * tetapi baru bisa dipakai bila ada minimal {@link UpgradeEnchantingScreenHandler#REQUIRED_BOOKSHELVES} bookshelf di sekitarnya.
 * Block entity-nya hanya dipakai untuk animasi buku melayang di atas meja (dirender di sisi client).
 */
public class UpgradeEnchantingTableBlock extends Block implements BlockEntityProvider {
	public static final MapCodec<UpgradeEnchantingTableBlock> CODEC = createCodec(UpgradeEnchantingTableBlock::new);
	private static final Text TITLE = Text.translatable("container.workshop_utilities.upgrade_enchanting_table");
	private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	// Memakai tag vanilla yang sama dengan enchanting table, jadi aturan "penghalang" tetap sama.
	private static final TagKey<Block> POWER_PROVIDER =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "enchantment_power_provider"));
	private static final TagKey<Block> POWER_TRANSMITTER =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "enchantment_power_transmitter"));

	/** 32 posisi bookshelf yang mungkin: cincin jarak 2 di sekeliling meja, setinggi meja dan satu blok di atasnya. */
	public static final List<BlockPos> SHELF_OFFSETS = buildOffsets();

	private static List<BlockPos> buildOffsets() {
		List<BlockPos> list = new ArrayList<>();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
					for (int dy = 0; dy <= 1; dy++) {
						list.add(new BlockPos(dx, dy, dz));
					}
				}
			}
		}
		return List.copyOf(list);
	}

	public UpgradeEnchantingTableBlock(Settings settings) {
		super(settings);
	}

	/** Bookshelf dihitung hanya bila blok di antaranya kosong (tidak terhalang), sama seperti enchanting table vanilla. */
	public static boolean isShelfConnected(World world, BlockPos tablePos, BlockPos offset) {
		return world.getBlockState(tablePos.add(offset)).isIn(POWER_PROVIDER)
				&& world.getBlockState(tablePos.add(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).isIn(POWER_TRANSMITTER);
	}

	public static int countBookshelves(World world, BlockPos tablePos) {
		int count = 0;
		for (BlockPos offset : SHELF_OFFSETS) {
			if (isShelfConnected(world, tablePos, offset)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new UpgradeEnchantingTableBlockEntity(pos, state);
	}

	/** Animasi buku hanya perlu dijalankan di client. */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (!world.isClient || type != ModBlockEntities.UPGRADE_ENCHANTING_TABLE) {
			return null;
		}
		BlockEntityTicker<UpgradeEnchantingTableBlockEntity> ticker = UpgradeEnchantingTableBlockEntity::tick;
		return (BlockEntityTicker<T>) ticker;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		for (BlockPos offset : SHELF_OFFSETS) {
			if (random.nextInt(16) == 0 && isShelfConnected(world, pos, offset)) {
				world.addParticle(ParticleTypes.ENCHANT,
						pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
						offset.getX() + random.nextFloat() - 0.5,
						offset.getY() - random.nextFloat() - 1.0F,
						offset.getZ() + random.nextFloat() - 0.5);
			}
		}
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new UpgradeEnchantingScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
				TITLE));
		return ActionResult.CONSUME;
	}
}