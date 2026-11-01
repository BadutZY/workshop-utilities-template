package com.workshoputilities;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logika empat enchantment alat:
 *  - Vein Miner (pickaxe)  : ore sejenis yang berdekatan ikut ditambang.
 *  - Woodcutter (axe)      : satu log dihancurkan -> seluruh pohon ikut tumbang.
 *  - More Harvest (hoe)    : klik kanan memanen tanaman matang 3x3 dan menanam ulang.
 *  - Anti-Gravity (shovel) : pengecekan untuk {@link com.workshoputilities.mixin.FallingBlockMixin}.
 *
 * Block tambahan dihancurkan lewat {@code interactionManager.tryBreakBlock}, jadi semua aturan vanilla tetap berlaku
 * (tool yang tepat untuk drop, durability berkurang per block, spawn protection, loot, XP, dll).
 */
public final class ToolEnchantments {
	/** Maksimal ore tambahan sekali tambang. */
	private static final int VEIN_MAX_BLOCKS = 64;
	/** Maksimal log tambahan sekali tebang. */
	private static final int TREE_MAX_BLOCKS = 256;
	/** Jarak (blok) pemain pemegang shovel Anti-Gravity yang melindungi block bergravitasi di sekitarnya. */
	private static final double ANTI_GRAVITY_RANGE = 8.0;

	private static final List<TagKey<Block>> ORE_TAGS = List.of(
			BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.COPPER_ORES, BlockTags.GOLD_ORES,
			BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES);

	/** Mencegah pemicu berantai: tryBreakBlock memanggil event yang sama lagi untuk tiap block tambahan. */
	private static boolean breaking = false;

	private ToolEnchantments() {
	}

	public static void init() {
		PlayerBlockBreakEvents.AFTER.register(ToolEnchantments::onBlockBroken);
		UseBlockCallback.EVENT.register(ToolEnchantments::onUseBlock);
	}

	// ---------------------------------------------------------------- Vein Miner & Woodcutter

	private static void onBlockBroken(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (breaking || world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		ItemStack tool = player.getMainHandStack();
		if (tool.isEmpty()) {
			return;
		}
		List<BlockPos> targets;
		if (ModEnchantments.hasVeinMiner(tool) && isOre(state)) {
			targets = collectVein(world, pos, state);
		} else if (ModEnchantments.hasWoodcutter(tool) && state.isIn(BlockTags.LOGS)) {
			targets = collectTree(world, pos);
		} else {
			return;
		}
		breakAll(serverPlayer, tool, targets);
	}

	private static void breakAll(ServerPlayerEntity player, ItemStack tool, List<BlockPos> targets) {
		breaking = true;
		try {
			for (BlockPos target : targets) {
				// Berhenti bila tool sudah hancur (stack jadi kosong).
				if (tool.isEmpty()) {
					break;
				}
				player.interactionManager.tryBreakBlock(target);
			}
		} finally {
			breaking = false;
		}
	}

	public static boolean isOre(BlockState state) {
		if (state.isOf(Blocks.NETHER_QUARTZ_ORE) || state.isOf(Blocks.NETHER_GOLD_ORE) || state.isOf(Blocks.ANCIENT_DEBRIS)) {
			return true;
		}
		for (TagKey<Block> tag : ORE_TAGS) {
			if (state.isIn(tag)) {
				return true;
			}
		}
		return false;
	}

	/** Ore dianggap sejenis bila blocknya sama, atau sama-sama masuk satu tag ore (mis. iron ore dan deepslate iron ore). */
	private static boolean sameVein(BlockState origin, BlockState other) {
		if (other.isAir()) {
			return false;
		}
		if (other.isOf(origin.getBlock())) {
			return true;
		}
		for (TagKey<Block> tag : ORE_TAGS) {
			if (origin.isIn(tag) && other.isIn(tag)) {
				return true;
			}
		}
		return false;
	}

	/** Flood fill 26 arah dari block yang dihancurkan; origin sudah hancur sehingga tidak ikut dimasukkan. */
	private static List<BlockPos> collectVein(World world, BlockPos origin, BlockState originState) {
		List<BlockPos> result = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(origin);
		queue.add(origin);
		while (!queue.isEmpty() && result.size() < VEIN_MAX_BLOCKS) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1 && result.size() < VEIN_MAX_BLOCKS; dx++) {
				for (int dy = -1; dy <= 1 && result.size() < VEIN_MAX_BLOCKS; dy++) {
					for (int dz = -1; dz <= 1 && result.size() < VEIN_MAX_BLOCKS; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.add(dx, dy, dz);
						if (seen.add(next) && sameVein(originState, world.getBlockState(next))) {
							result.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Mencari semua log yang tersambung (26 arah) dari log yang ditebang, hanya ke atas atau samping (bagian bawah
	 * yang tidak ikut dipukul tidak disentuh). Baru dianggap pohon bila ada daun alami (atau wart block) menempel,
	 * supaya rumah/bangunan dari log buatan pemain tidak ikut runtuh.
	 */
	private static List<BlockPos> collectTree(World world, BlockPos origin) {
		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(origin);
		queue.add(origin);
		boolean isTree = touchesNaturalLeaves(world, origin);
		while (!queue.isEmpty() && logs.size() < TREE_MAX_BLOCKS) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1 && logs.size() < TREE_MAX_BLOCKS; dx++) {
				for (int dy = -1; dy <= 1 && logs.size() < TREE_MAX_BLOCKS; dy++) {
					for (int dz = -1; dz <= 1 && logs.size() < TREE_MAX_BLOCKS; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.add(dx, dy, dz);
						if (next.getY() < origin.getY() || !seen.add(next)) {
							continue;
						}
						if (world.getBlockState(next).isIn(BlockTags.LOGS)) {
							logs.add(next);
							queue.add(next);
							isTree |= touchesNaturalLeaves(world, next);
						}
					}
				}
			}
		}
		return isTree ? logs : List.of();
	}

	private static boolean touchesNaturalLeaves(World world, BlockPos pos) {
		for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
			BlockState neighbor = world.getBlockState(pos.offset(direction));
			if (neighbor.isIn(BlockTags.LEAVES)) {
				if (!neighbor.contains(LeavesBlock.PERSISTENT) || !neighbor.get(LeavesBlock.PERSISTENT)) {
					return true;
				}
			} else if (neighbor.isIn(BlockTags.WART_BLOCKS)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- More Harvest

	private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
		if (world.isClient || !(world instanceof ServerWorld serverWorld) || player.isSpectator()) {
			return ActionResult.PASS;
		}
		ItemStack tool = player.getStackInHand(hand);
		if (!ModEnchantments.hasMoreHarvest(tool)) {
			return ActionResult.PASS;
		}
		BlockPos clicked = hit.getBlockPos();
		BlockPos center;
		if (isHarvestable(world.getBlockState(clicked))) {
			center = clicked;
		} else if (world.getBlockState(clicked).isOf(Blocks.FARMLAND) && isHarvestable(world.getBlockState(clicked.up()))) {
			center = clicked.up();
		} else {
			return ActionResult.PASS;
		}

		int harvested = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = center.add(dx, 0, dz);
				BlockState state = world.getBlockState(pos);
				if (isMature(state) && world.canPlayerModifyAt(player, pos)) {
					harvest(serverWorld, player, tool, pos, state);
					harvested++;
				}
			}
		}
		if (harvested == 0) {
			return ActionResult.PASS;
		}
		tool.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		return ActionResult.SUCCESS;
	}

	private static boolean isHarvestable(BlockState state) {
		return state.getBlock() instanceof CropBlock || state.isOf(Blocks.NETHER_WART);
	}

	private static boolean isMature(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMature(state);
		}
		return state.isOf(Blocks.NETHER_WART) && state.get(NetherWartBlock.AGE) >= 3;
	}

	/** Drop semua hasil panen, ambil satu bibit dari drop itu untuk menanam ulang (seperti replant vanilla). */
	private static void harvest(ServerWorld world, PlayerEntity player, ItemStack tool, BlockPos pos, BlockState state) {
		List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, tool);
		ItemStack seed = state.getBlock().getPickStack(world, pos, state);
		boolean replant = seed.isEmpty();
		if (!replant) {
			for (ItemStack drop : drops) {
				if (!drop.isEmpty() && drop.isOf(seed.getItem())) {
					drop.decrement(1);
					replant = true;
					break;
				}
			}
		}
		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				Block.dropStack(world, pos, drop);
			}
		}
		if (replant) {
			BlockState fresh = state.getBlock() instanceof CropBlock crop
					? crop.withAge(0)
					: state.with(NetherWartBlock.AGE, 0);
			world.setBlockState(pos, fresh, Block.NOTIFY_ALL);
		} else {
			world.removeBlock(pos, false);
		}
		world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	// ---------------------------------------------------------------- Anti-Gravity

	/** True bila ada pemain yang memegang shovel Anti-Gravity di dekat {@code pos}. */
	public static boolean isAntiGravityActive(ServerWorld world, BlockPos pos) {
		double maxSq = ANTI_GRAVITY_RANGE * ANTI_GRAVITY_RANGE;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator() || !ModEnchantments.hasAntiGravity(player.getMainHandStack())) {
				continue;
			}
			if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxSq) {
				return true;
			}
		}
		return false;
	}
}
