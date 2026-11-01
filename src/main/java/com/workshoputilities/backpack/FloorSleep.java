package com.workshoputilities.backpack;

import com.workshoputilities.ModAdvancements;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tidur di lantai untuk Bed Module (spawn point tidak berubah, bed tidak tersisa).
 *
 * Cara kerjanya: sebuah bed ASLI dipasang sementara di lantai tempat pemain berdiri (2 blok), lalu pemain ditidurkan
 * lewat jalur tidur vanilla ({@code LivingEntity.sleep}) sehingga animasi tidur, kamera terkunci, layar menggelap, dan
 * tombol "Leave Bed" semuanya persis seperti tidur di bed biasa. Yang TIDAK dipakai: {@code trySleep} vanilla, jadi
 * spawn point tidak berubah. Begitu pemain bangun, bed langsung dihapus dan blok lama (udara / rumput) dikembalikan
 * tanpa drop apa pun.
 *
 * Waktu dunia berjalan persis seperti tidur di bed biasa: layar menggelap perlahan selama 100 tick, lalu waktu
 * langsung ke pagi berikutnya dan pemain bangun (tanpa animasi waktu tambahan).
 *
 * Tidur berhenti (waktu tidak dilompati, bed dihapus) bila pemain menekan Leave Bed, terluka, mati, keluar dari game,
 * atau server berhenti.
 */
public final class FloorSleep {
	/** Tick pertama: layar tas ditutup. Tick kedua: bed dipasang dan pemain tidur. */
	private static final int BEGIN_AGE = 2;
	/** Lama tidur sebelum pagi, sama dengan timer tidur vanilla (tick). */
	public static final int SLEEP_TICKS = 100;

	/** Pasang / hapus blok tanpa memicu update tetangga (agar bed tidak pecah menjadi item). */
	private static final int SILENT = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

	private static final class Session {
		final ServerWorld world;
		final long target;
		int age;
		boolean begun;
		final List<BlockPos> bedPositions = new ArrayList<>();
		final List<BlockState> oldStates = new ArrayList<>();

		Session(ServerWorld world, long target) {
			this.world = world;
			this.target = target;
		}
	}

	private FloorSleep() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (SESSIONS.isEmpty()) {
				return;
			}
			Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Session> entry = it.next();
				Session session = entry.getValue();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
				if (player == null || !player.isAlive() || player.isRemoved() || player.getServerWorld() != session.world) {
					restoreBlocks(session);
					it.remove();
					continue;
				}
				if (!tick(player, session)) {
					it.remove();
				}
			}
		});
		// Keluar dari game saat tidur: bangunkan dulu (sebelum data pemain disimpan), lalu hapus bed.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Session session = SESSIONS.remove(handler.player.getUuid());
			if (session != null) {
				wake(handler.player);
				restoreBlocks(session);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
				if (player != null) {
					wake(player);
				}
				restoreBlocks(entry.getValue());
			}
			SESSIONS.clear();
		});
	}

	public static boolean isSleeping(ServerPlayerEntity player) {
		return SESSIONS.containsKey(player.getUuid());
	}

	/** Mulai tidur di lantai; {@code targetTime} = waktu dunia pagi berikutnya (kelipatan 24000). */
	public static void start(ServerPlayerEntity player, long targetTime) {
		if (isSleeping(player)) {
			return;
		}
		if (!player.isOnGround() || findSpot(player) == null) {
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.no_space"), true);
			return;
		}
		SESSIONS.put(player.getUuid(), new Session(player.getServerWorld(), targetTime));
	}

	/** Cari arah bed (kaki di blok pemain, kepala di sebelahnya) yang muat. Null bila tidak ada tempat. */
	private static Direction findSpot(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos foot = player.getBlockPos();
		Direction first = player.getHorizontalFacing();
		Direction[] order = {first, first.rotateYClockwise(), first.rotateYCounterclockwise(), first.getOpposite()};
		for (Direction dir : order) {
			BlockPos head = foot.offset(dir);
			if (canHold(world, foot) && canHold(world, head)
					&& world.getBlockState(head.down()).isSideSolidFullSquare(world, head.down(), Direction.UP)) {
				return dir;
			}
		}
		return null;
	}

	private static boolean canHold(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		BlockState above = world.getBlockState(pos.up());
		return (state.isAir() || state.isReplaceable()) && state.getFluidState().isEmpty()
				&& above.getCollisionShape(world, pos.up()).isEmpty() && above.getFluidState().isEmpty();
	}

	/** @return false bila sesi selesai / dibatalkan. */
	private static boolean tick(ServerPlayerEntity player, Session session) {
		ServerWorld world = session.world;
		session.age++;

		if (!session.begun) {
			if (session.age == 1) {
				// Layar tas ditutup dulu agar kamera tidur dan tombol Leave Bed bisa tampil.
				if (player.currentScreenHandler != player.playerScreenHandler) {
					player.closeHandledScreen();
				}
				return true;
			}
			if (session.age < BEGIN_AGE) {
				return true;
			}
			if (!begin(player, session)) {
				player.sendMessage(Text.translatable("message.workshop_utilities.sleep.no_space"), true);
				return false;
			}
			return true;
		}

		int sleepAge = session.age - BEGIN_AGE;

		// Pemain bangun sendiri (Leave Bed, terluka, dll): berhenti, malam tidak dilewati.
		if (!player.isSleeping() || !world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
			wake(player);
			restoreBlocks(session);
			player.sendMessage(Text.translatable("message.workshop_utilities.sleep.interrupted"), true);
			return false;
		}

		// Sama seperti tidur di bed biasa: layar menggelap selama 100 tick, lalu waktu langsung pagi dan pemain bangun.
		if (sleepAge >= SLEEP_TICKS) {
			long next = world.getTimeOfDay() + 24000L;
			world.setTimeOfDay(next - next % 24000L);
			finish(player, session);
			return false;
		}
		return true;
	}

	/** Pasang bed sementara dan tidurkan pemain lewat jalur tidur vanilla. */
	private static boolean begin(ServerPlayerEntity player, Session session) {
		ServerWorld world = session.world;
		Direction dir = findSpot(player);
		if (dir == null) {
			return false;
		}
		BlockPos foot = player.getBlockPos();
		BlockPos head = foot.offset(dir);
		session.bedPositions.add(foot);
		session.bedPositions.add(head);
		session.oldStates.add(world.getBlockState(foot));
		session.oldStates.add(world.getBlockState(head));

		BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, dir);
		world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), SILENT);
		world.setBlockState(head, bed.with(BedBlock.PART, BedPart.HEAD), SILENT);
		session.begun = true;

		if (player.hasVehicle()) {
			player.stopRiding();
		}
		player.sleep(head);
		player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
		world.playSound(null, head, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 1.0F, 0.8F);
		return true;
	}

	private static void finish(ServerPlayerEntity player, Session session) {
		ServerWorld world = session.world;
		if (world.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
			world.setWeather(0, 0, false, false);
		}
		// Tidur membuat Phantom tidak muncul (hitung mundur insomnia diulang), seperti tidur di bed.
		player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 0);
		wake(player);
		restoreBlocks(session);
		ModAdvancements.grant(player, "sleep_anywhere");
		player.sendMessage(Text.translatable("message.workshop_utilities.sleep.done"), true);
	}

	private static void wake(ServerPlayerEntity player) {
		if (player.isSleeping()) {
			player.wakeUp(false, true);
		}
	}

	/** Hapus bed dan kembalikan blok lama, tanpa drop dan tanpa update tetangga. */
	private static void restoreBlocks(Session session) {
		for (int i = 0; i < session.bedPositions.size(); i++) {
			BlockPos pos = session.bedPositions.get(i);
			if (session.world.getBlockState(pos).getBlock() instanceof BedBlock) {
				session.world.setBlockState(pos, session.oldStates.get(i), SILENT);
			}
		}
		session.bedPositions.clear();
		session.oldStates.clear();
	}
}
