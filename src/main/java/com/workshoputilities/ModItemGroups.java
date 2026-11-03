package com.workshoputilities;

import com.workshoputilities.backpack.BackpackTier;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Tiga tab creative khusus mod ini, terpisah dari tab vanilla:
 *  - MAIN  (itemGroup.workshop_utilities.main)  : hanya block meja (Uncrafting, Dis-Enchanting, Upgrade Table, Upgrade Enchanting).
 *  - BOOKS (itemGroup.workshop_utilities.books) : Upgrade Enchanted, Upgrade Book, Upgrade Tool, Fused Enchanted Book contoh, dan semua enchanted book buatan mod.
 *  - BACKPACKS (itemGroup.workshop_utilities.backpacks): lima Backpack dan enam modul Backpack.
 * Nama tab ada di folder lang.
 */
public final class ModItemGroups {
    public static final ItemGroup MAIN = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(WorkshopUtilitiesMod.MOD_ID, "main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.workshop_utilities.main"))
                    .icon(() -> new ItemStack(ModBlocks.UNCRAFTING_TABLE))
                    .entries((context, entries) -> {
                        entries.add(ModBlocks.UNCRAFTING_TABLE);
                        entries.add(ModBlocks.DISENCHANTING_TABLE);
                        entries.add(ModBlocks.UPGRADE_TABLE);
                        entries.add(ModBlocks.UPGRADE_ENCHANTING_TABLE);
                    })
                    .build());

    public static final ItemGroup BOOKS = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(WorkshopUtilitiesMod.MOD_ID, "books"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.workshop_utilities.books"))
                    .icon(() -> new ItemStack(ModItems.UPGRADE_BOOK))
                    .entries((context, entries) -> {
                        entries.add(ModItems.UPGRADE_ENCHANTED);
                        entries.add(ModItems.UPGRADE_BOOK);
                        entries.add(ModItems.UPGRADE_TOOL);
                        // Fused Enchanted Book contoh (vanilla + mod) agar bisa dicoba di Creative.
                        context.lookup().getOptionalWrapper(RegistryKeys.ENCHANTMENT).ifPresent(wrapper -> {
                            ItemEnchantmentsComponent.Builder fused =
                                    new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                            wrapper.getOptional(ModEnchantments.INFINITE_DURABILITY).ifPresent(e -> fused.add(e, 1));
                            wrapper.getOptional(Enchantments.MENDING).ifPresent(e -> fused.add(e, 1));
                            wrapper.getOptional(Enchantments.UNBREAKING).ifPresent(e -> fused.add(e, 3));
                            ItemStack book = new ItemStack(ModItems.FUSED_ENCHANTED_BOOK);
                            book.set(DataComponentTypes.STORED_ENCHANTMENTS, fused.build());
                            entries.add(book);
                        });
                        // Enchanted book untuk semua enchantment buatan mod ini.
                        for (RegistryKey<Enchantment> key : ModEnchantments.UPGRADE_OPTIONS) {
                            context.lookup().getOptionalWrapper(RegistryKeys.ENCHANTMENT)
                                    .flatMap(wrapper -> wrapper.getOptional(key))
                                    .ifPresent(entry -> entries.add(ModEnchantments.createBook(entry)));
                        }
                    })
                    .build());

    public static final ItemGroup BACKPACKS = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(WorkshopUtilitiesMod.MOD_ID, "backpacks"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.workshop_utilities.backpacks"))
                    .icon(() -> new ItemStack(ModItems.backpack(BackpackTier.SMALL)))
                    .entries((context, entries) -> {
                        for (BackpackTier tier : BackpackTier.values()) {
                            entries.add(ModItems.backpack(tier));
                        }
                        entries.add(ModItems.CRAFTING_MODULE);
                        entries.add(ModItems.FURNACE_MODULE);
                        entries.add(ModItems.BLAST_FURNACE_MODULE);
                        entries.add(ModItems.SMOKER_MODULE);
                        entries.add(ModItems.UNCRAFTING_MODULE);
                        entries.add(ModItems.BED_MODULE);
                        entries.add(ModItems.TOOLS_HOLDER_MODULE);
                    })
                    .build());

    private ModItemGroups() {
    }

    /** Dipanggil agar class ini dimuat dan tab terdaftar. */
    public static void init() {
    }
}
