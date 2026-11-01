package com.workshoputilities.block;

import com.mojang.serialization.MapCodec;
import com.workshoputilities.screen.UncraftingScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UncraftingTableBlock extends Block {
	public static final MapCodec<UncraftingTableBlock> CODEC = createCodec(UncraftingTableBlock::new);
	private static final Text TITLE = Text.translatable("container.workshop_utilities.uncrafting_table");

	public UncraftingTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	public MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new UncraftingScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
				TITLE));
		return ActionResult.CONSUME;
	}
}
