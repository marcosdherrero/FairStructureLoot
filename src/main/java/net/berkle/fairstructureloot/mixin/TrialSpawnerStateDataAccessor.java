package net.berkle.fairstructureloot.mixin;

import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;

@Mixin(TrialSpawnerStateData.class)
public interface TrialSpawnerStateDataAccessor {

	@Accessor("detectedPlayers")
	Set<UUID> fairstructureloot$getDetectedPlayers();
}
