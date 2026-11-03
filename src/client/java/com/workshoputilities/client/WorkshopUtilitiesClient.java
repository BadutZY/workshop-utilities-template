package com.workshoputilities.client;

import com.workshoputilities.ModBlockEntities;
import com.workshoputilities.backpack.BackpackSlotHolder;
import com.workshoputilities.backpack.BackpackTier;
import com.workshoputilities.ModEnchantments;
import com.workshoputilities.ModItems;
import com.workshoputilities.ModScreenHandlers;
import com.workshoputilities.WorkshopUtilitiesMod;
import com.workshoputilities.UnlimitedScope;
import com.workshoputilities.elytra.ElytraContext;
import com.workshoputilities.item.BackpackItem;
import com.workshoputilities.network.OpenBackpackPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class WorkshopUtilitiesClient implements ClientModInitializer {
	/** Tombol untuk membuka backpack yang sedang dipakai di punggung (default B, bisa diganti di Controls). */
	private static KeyBinding openBackpackKey;

	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.UNCRAFTING, UncraftingScreen::new);
		HandledScreens.register(ModScreenHandlers.DISENCHANTING, DisenchantingScreen::new);
		HandledScreens.register(ModScreenHandlers.UPGRADE_ENCHANTING, UpgradeEnchantingScreen::new);
		HandledScreens.register(ModScreenHandlers.UPGRADE_TABLE, UpgradeTableScreen::new);
		for (BackpackTier tier : BackpackTier.values()) {
			HandledScreens.register(ModScreenHandlers.backpack(tier), BackpackScreen::new);
		}
		BlockEntityRendererFactories.register(ModBlockEntities.UPGRADE_ENCHANTING_TABLE, UpgradeEnchantingTableRenderer::new);

		// Roda mouse di panel Backpack pada GUI block vanilla (Crafting Table, Furnace, Anvil, dst.).
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof HandledScreen<?> && screen instanceof PanelScreenAccess access) {
				PanelController controller = access.workshop_utilities$getPanelController();
				if (controller != null) {
					ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) ->
							!controller.scrolled(mouseX, mouseY, verticalAmount));
				}
			}
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			UnlimitedScope.reset();
			ElytraContext.reset();
		});

		DoubleJump.init();
		registerUpgradeBookTextures();
		registerBookTooltips();
		registerWornBackpack();
	}

	/** Backpack 3D di punggung pemain (slot Backpack) + tombol untuk membukanya tanpa melepas. */
	private static void registerWornBackpack() {
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
				registrationHelper.register(new BackpackFeatureRenderer(playerRenderer));
			}
		});

		openBackpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.workshop_utilities.open_backpack",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.workshop_utilities"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openBackpackKey.wasPressed()) {
				if (client.player != null && BackpackSlotHolder.of(client.player)
						.workshop_utilities$getBackpackSlot().getItem() instanceof BackpackItem) {
					ClientPlayNetworking.send(new OpenBackpackPayload());
				}
			}
		});
	}

	/**
	 * Tiap enchantment di {@link ModEnchantments#UPGRADE_OPTIONS} mendapat predicate model "workshop_utilities:<nama>"
	 * pada item enchanted book. Texture per enchantment diatur di
	 * assets/minecraft/models/item/enchanted_book.json (override -> workshop_utilities:item/<nama>_book).
	 * Untuk enchanted book baru: tambahkan ke UPGRADE_OPTIONS, buat model + texture, lalu tambah satu override di JSON tsb.
	 * Glint tetap berasal dari enchanted book vanilla.
	 */
	private static void registerUpgradeBookTextures() {
		for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
			Identifier predicateId = Identifier.of(WorkshopUtilitiesMod.MOD_ID, key.getValue().getPath());
			ModelPredicateProviderRegistry.register(Items.ENCHANTED_BOOK, predicateId,
					(stack, world, entity, seed) -> storesEnchantment(stack, key) ? 1.0F : 0.0F);
		}
	}

	/**
	 * Tooltip enchanted book buatan mod ini di inventory: tanpa SHIFT hanya muncul petunjuk "Tahan Shift",
	 * dengan SHIFT tampil deskripsi fungsi dan item yang bisa dipasangi.
	 * Teks diambil dari lang: tooltip.workshop_utilities.<nama>.desc.1, .desc.2, ... dan tooltip.workshop_utilities.<nama>.applies.
	 */
	private static void registerBookTooltips() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (stack.isOf(ModItems.FUSED_ENCHANTED_BOOK)) {
				lines.add(Text.translatable("tooltip." + WorkshopUtilitiesMod.MOD_ID + ".use_upgrade_table")
						.formatted(Formatting.DARK_PURPLE));
				return;
			}
			if (!stack.isOf(Items.ENCHANTED_BOOK)) {
				return;
			}
			for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
				if (storesEnchantment(stack, key)) {
					addBookTooltip(lines, key.getValue().getPath());
					lines.add(Text.translatable("tooltip." + WorkshopUtilitiesMod.MOD_ID + ".use_upgrade_table")
							.formatted(Formatting.DARK_PURPLE));
				}
			}
		});
	}

	/** Dipakai juga oleh UpgradeEnchantingScreen: petunjuk "Tahan Shift", atau deskripsi + item yang bisa dipasangi bila Shift ditekan. */
	static void addBookTooltip(List<Text> lines, String name) {
		String base = "tooltip." + WorkshopUtilitiesMod.MOD_ID + "." + name;
		if (!Screen.hasShiftDown()) {
			lines.add(Text.translatable("tooltip." + WorkshopUtilitiesMod.MOD_ID + ".hold_shift")
					.formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
			return;
		}
		for (int i = 1; i <= 8 && I18n.hasTranslation(base + ".desc." + i); i++) {
			lines.add(Text.translatable(base + ".desc." + i).formatted(Formatting.GRAY));
		}
		if (I18n.hasTranslation(base + ".applies")) {
			lines.add(Text.translatable(base + ".applies").formatted(Formatting.DARK_AQUA));
		}
	}

	private static boolean storesEnchantment(ItemStack stack, RegistryKey<Enchantment> key) {
		ItemEnchantmentsComponent stored = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
		if (stored == null) {
			return false;
		}
		for (RegistryEntry<Enchantment> entry : stored.getEnchantments()) {
			if (entry.matchesKey(key)) {
				return true;
			}
		}
		return false;
	}
}
