package com.ninni.species.entity.ai.tasks;

import com.google.common.collect.ImmutableMap;
import com.ninni.species.entity.BirtEntity;
import com.ninni.species.entity.SpeciesMemoryModuleTypes;
import com.ninni.species.tag.SpeciesTags;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindDwellingTask extends Task<BirtEntity> {

    public FindDwellingTask() {
        super(ImmutableMap.of(SpeciesMemoryModuleTypes.TICKS_LEFT_TO_FIND_DWELLING, MemoryModuleState.VALUE_ABSENT));
    }

    @Override
    protected boolean shouldRun(ServerWorld world, BirtEntity entity) {
        return !entity.hasDwelling() && entity.canEnterDwelling();
    }

    @Override
    protected boolean isTimeLimitExceeded(long time) {
        return false;
    }

    @Override
    protected void run(ServerWorld world, BirtEntity entity, long time) {
        Optional<List<BlockPos>> optionalMemory = entity.getBrain().getOptionalMemory(SpeciesMemoryModuleTypes.NEAREST_BIRT_DWELLING);
        entity.getBrain().remember(SpeciesMemoryModuleTypes.TICKS_LEFT_TO_FIND_DWELLING, 200);
        List<BlockPos> list = this.getNearbyFreeDwellings(entity);
        if (list.isEmpty()) {
            return;
        }
        for (BlockPos blockPos : list) {
            if (optionalMemory.isPresent() && optionalMemory.get().contains(blockPos)) continue;
            entity.setDwellingPos(blockPos);
            return;
        }
        optionalMemory.ifPresent(List::clear);
        entity.setDwellingPos(list.get(0));
    }

    private List<BlockPos> getNearbyFreeDwellings(BirtEntity entity) {
        BlockPos blockPos = entity.getBlockPos();
        PointOfInterestStorage pointOfInterestStorage = ((ServerWorld)entity.world).getPointOfInterestStorage();
        Stream<PointOfInterest> stream = pointOfInterestStorage.getInCircle((poiType) -> poiType.isIn(SpeciesTags.BIRT_HOME), blockPos, 20, PointOfInterestStorage.OccupationStatus.ANY);
        return stream.map(PointOfInterest::getPos).filter(entity::doesDwellingHaveSpace).sorted(Comparator.comparingDouble((blockPos2) -> blockPos2.getSquaredDistance(blockPos))).collect(Collectors.toList());
    }

}
