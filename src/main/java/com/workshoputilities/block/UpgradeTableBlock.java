package com.workshoputilities.block;

import com.workshoputilities.screen.UpgradeTableScreenHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
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
 * Upgrade Table adalah anvil vanilla dengan GUI sendiri. Semuanya diwarisi dari {@link AnvilBlock}:
 * jatuh seperti pasir/gravel bila tidak ada alas (FallingBlock), melukai makhluk yang tertimpa
 * (2 damage per blok jatuh, maksimal 40, sumber damage "anvil"), suara mendarat/hancur anvil, bentuk 3D,
 * dan arah hadap. Sifat block disalin dari {@code Blocks.ANVIL} di {@link com.workshoputilities.ModBlocks}.
 * Hanya cara membuka GUI yang berbeda.
 */
public class UpgradeTableBlock extends AnvilBlock {
	private static final Text TITLE = Text.translatable("container.workshop_utilities.upgrade_table");

	public UpgradeTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new UpgradeTableScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
				TITLE));
		player.incrementStat(Stats.INTERACT_WITH_ANVIL);
		return ActionResult.CONSUME;
	}
}
