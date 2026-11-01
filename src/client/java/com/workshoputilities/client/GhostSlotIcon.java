package com.workshoputilities.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.workshoputilities.WorkshopUtilitiesMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Menggambar ikon "hantu" pada slot kosong memakai texture item ASLI (vanilla) dengan opacity dikurangi,
 * persis seperti ikon slot kosong bawaan Minecraft (mis. lapis pada enchanting table).
 * Texture yang dipakai adalah gambar mentah item, jadi tidak ada efek glint.
 */
public final class GhostSlotIcon {
	/** Opacity ikon (0..1). Semakin kecil semakin pudar; tetap terlihat jelas, tidak hilang total. */
	public static final float ALPHA = 0.3F;

	public static final Identifier BOOK = Identifier.of("minecraft", "textures/item/book.png");
	public static final Identifier ENCHANTED_BOOK = Identifier.of("minecraft", "textures/item/enchanted_book.png");
	public static final Identifier DIAMOND_PICKAXE = Identifier.of("minecraft", "textures/item/diamond_pickaxe.png");
	/** Texture Upgrade Tool milik mod (16x16), ikon hantu slot bawah Upgrade Table. */
	public static final Identifier UPGRADE_TOOL = Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/item/upgrade_tool.png");
	/** Texture Upgrade Book milik mod (16x16), dipakai sebagai ikon hantu slot input Upgrade Enchanting Table. */
	public static final Identifier UPGRADE_BOOK = Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/item/upgrade_book.png");

	/** Texture Upgrade Enchanted milik mod (16x16), ikon hantu slot bawah Upgrade Enchanting Table. */
	public static final Identifier UPGRADE_ENCHANTED =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/item/upgrade_enchanted.png");

	/** Ikon outline Upgrade Tool (16x16, hanya garis tepi, tanpa isi) untuk slot bawah Upgrade Table. */
	public static final Identifier UPGRADE_TOOL_OUTLINE =
			Identifier.of(WorkshopUtilitiesMod.MOD_ID, "textures/gui/upgrade_tool_slot_icon.png");

	private GhostSlotIcon() {
	}

	/** Menggambar texture 16x16 pada posisi item (x, y) layar dengan opacity {@link #ALPHA}. */
	public static void drawVanilla(DrawContext context, Identifier texture, int x, int y) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ALPHA);
		context.drawTexture(texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	/**
	 * Menggambar ikon outline (garis tepi gelap saja, bagian dalam transparan) pada posisi item (x, y) layar,
	 * seperti ikon slot armor kosong vanilla. Opacity penuh karena texture sudah berupa garis tepi.
	 */
	public static void drawOutline(DrawContext context, Identifier texture, int x, int y) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		context.drawTexture(texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
		RenderSystem.disableBlend();
	}
}
