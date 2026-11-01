package com.workshoputilities.block;

import com.workshoputilities.screen.DisenchantingScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Dis-Enchanting Table adalah grindstone vanilla dengan GUI sendiri. Semuanya diwarisi dari {@link GrindstoneBlock}:
 * bisa menempel di lantai, dinding, dan langit-langit (properti face + facing), bentuk tabrakan/outline,
 * penempatan, rotasi/mirror, dan tidak ikut lepas bila penyangganya hilang. Sifat block (kekerasan, suara batu,
 * butuh tool, piston) disalin dari {@code Blocks.GRINDSTONE} di {@link com.workshoputilities.ModBlocks}.
 * Hanya cara membuka GUI yang berbeda.
 */
public class DisenchantingTableBlock extends GrindstoneBlock {
	private static final Text TITLE = Text.translatable("container.workshop_utilities.disenchanting_table");

	public DisenchantingTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new DisenchantingScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
				TITLE));
		player.incrementStat(Stats.INTERACT_WITH_GRINDSTONE);
		return ActionResult.CONSUME;
	}
}
