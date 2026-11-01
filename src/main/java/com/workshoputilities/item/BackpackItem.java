package com.workshoputilities.item;

import com.workshoputilities.ModScreenHandlers;
import com.workshoputilities.backpack.BackpackData;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.screen.BackpackScreenHandler;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Backpack: klik kanan untuk membuka. Isi disimpan di item itu sendiri, jadi ikut terbawa ke mana pun tasnya pergi.
 *
 * Tas dipakai di slot Backpack khusus (bukan slot chestplate), sehingga chestplate dan Elytra tetap bisa dipakai
 * bersamaan: Sneak + klik kanan, drag ke slot Backpack di inventory, atau Shift-click. Saat dipakai, tas tampil 3D
 * di punggung pemain (BackpackFeatureRenderer) dan dibuka dengan tombol B (bisa diganti di Controls),
 * lihat {@link #openWorn(ServerPlayerEntity)}.
 */
public class BackpackItem extends Item {
	private final BackpackTier tier;

	public BackpackItem(Settings settings, BackpackTier tier) {
		super(settings);
		this.tier = tier;
	}

	public BackpackTier getTier() {
		return this.tier;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		// Sneak + klik kanan: pakai di punggung (slot Backpack); bila slot terisi, isinya ditukar ke tangan.
		if (player.isSneaking()) {
			return this.wear(world, player, hand, stack);
		}
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		// Slot hotbar tas ini dikunci selama GUI terbuka (tangan kiri tidak punya slot di GUI).
		int lockedSlot = hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : -1;
		openScreen(player, stack, this.tier, lockedSlot);
		return TypedActionResult.consume(stack);
	}

	/** Memasang tas di slot Backpack. Perilakunya meniru Equipment.equipAndSwap vanilla, tetapi tidak memakai slot armor. */
	private TypedActionResult<ItemStack> wear(World world, PlayerEntity player, Hand hand, ItemStack stack) {
		BackpackSlotHolder holder = BackpackSlotHolder.of(player);
		ItemStack worn = holder.workshop_utilities$getBackpackSlot();
		if (!player.isCreative() && !worn.isEmpty()
				&& EnchantmentHelper.hasAnyEnchantmentsWith(worn, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE)) {
			return TypedActionResult.fail(stack);
		}
		// Bila slot kosong, hasilnya adalah stack di tangan itu sendiri (jadi kosong di Survival, tetap ada di Creative).
		ItemStack result = worn.isEmpty() ? stack : worn.copy();
		ItemStack toWear = player.isCreative() ? stack.copyWithCount(1) : stack.split(1);
		holder.workshop_utilities$setBackpackSlot(toWear);
		if (!world.isClient) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
		}
		return TypedActionResult.success(result, world.isClient());
	}

	private static void openScreen(PlayerEntity player, ItemStack stack, BackpackTier tier, int lockedSlot) {
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new BackpackScreenHandler(
						ModScreenHandlers.backpack(tier), syncId, inventory, tier, stack, lockedSlot),
				stack.getName()));
	}

	/** Membuka tas yang sedang dipakai di slot Backpack (dipanggil dari paket tombol B). */
	public static void openWorn(ServerPlayerEntity player) {
		ItemStack stack = BackpackSlotHolder.of(player).workshop_utilities$getBackpackSlot();
		if (!(stack.getItem() instanceof BackpackItem item)) {
			return;
		}
		// Jangan menimpa GUI lain yang sedang terbuka.
		if (player.currentScreenHandler != player.playerScreenHandler) {
			return;
		}
		openScreen(player, stack, item.tier, BackpackScreenHandler.WORN_SLOT);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.capacity", this.tier.slots())
				.formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.used",
				BackpackData.usedSlots(stack), this.tier.slots()).formatted(Formatting.DARK_GRAY));
		BackpackTier next = this.tier.next();
		if (next != null) {
			// Dipecah jadi beberapa baris pendek supaya tooltip memanjang ke bawah, bukan ke samping.
			tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.upgrade",
					Text.translatable("item.workshop_utilities." + next.id())).formatted(Formatting.DARK_AQUA));
			tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.upgrade.table")
					.formatted(Formatting.DARK_AQUA));
			tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.upgrade.cost",
					this.tier.upgradeMaterial().getName()).formatted(Formatting.DARK_AQUA));
		} else {
			tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.max").formatted(Formatting.GOLD));
		}
		tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.open").formatted(Formatting.DARK_GRAY,
				Formatting.ITALIC));
		tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.wear").formatted(Formatting.DARK_GRAY,
				Formatting.ITALIC));
		tooltip.add(Text.translatable("tooltip.workshop_utilities.backpack.wear.back").formatted(Formatting.DARK_GRAY,
				Formatting.ITALIC));
	}
}
