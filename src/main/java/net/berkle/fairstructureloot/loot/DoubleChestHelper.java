package net.berkle.fairstructureloot.loot;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Resolves connected double-chest halves and a stable storage key for instanced loot. */
public final class DoubleChestHelper {

	public static final int DOUBLE_CHEST_SLOTS = 54;

	public record ChestPair(ChestBlockEntity first, ChestBlockEntity second, BlockPos storagePos) {
		public int containerSize() {
			return DOUBLE_CHEST_SLOTS;
		}
	}

	private DoubleChestHelper() {
	}

	public static Optional<ChestPair> findPair(Level level, ChestBlockEntity chest) {
		BlockState state = chest.getBlockState();
		if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return Optional.empty();
		}
		BlockPos partnerPos = ChestBlock.getConnectedBlockPos(chest.getBlockPos(), state);
		BlockEntity partnerEntity = level.getBlockEntity(partnerPos);
		if (!(partnerEntity instanceof ChestBlockEntity partner)) {
			return Optional.empty();
		}
		BlockPos storagePos = canonicalPos(chest.getBlockPos(), partnerPos);
		return Optional.of(new ChestPair(chest, partner, storagePos));
	}

	public static Optional<ChestBlockEntity> findPartner(Level level, ChestBlockEntity chest) {
		return findPair(level, chest).map(pair -> pair.first() == chest ? pair.second() : pair.first());
	}

	public static BlockPos canonicalPos(BlockPos first, BlockPos second) {
		if (first.getX() != second.getX()) {
			return first.getX() < second.getX() ? first : second;
		}
		if (first.getY() != second.getY()) {
			return first.getY() < second.getY() ? first : second;
		}
		return first.getZ() <= second.getZ() ? first : second;
	}

	public static BlockPos storagePos(ServerLevel level, ChestBlockEntity chest) {
		return findPair(level, chest)
			.map(ChestPair::storagePos)
			.orElse(chest.getBlockPos());
	}
}
