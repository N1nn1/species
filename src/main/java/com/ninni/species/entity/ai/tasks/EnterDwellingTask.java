package com.ninni.species.entity.ai.tasks;

import com.google.common.collect.ImmutableMap;
import com.ninni.species.block.entity.BirtDwellingBlockEntity;
import com.ninni.species.entity.BirtEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;

public class EnterDwellingTask extends Task<BirtEntity> {

    public EnterDwellingTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean isTimeLimitExceeded(long time) {
        return false;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, BirtEntity entity) {
        if (entity.hasDwelling() && entity.canEnterDwelling() && entity.getDwellingPos() != null) {
            if (entity.getDwellingPos().isWithinDistance(entity.getPos(), 2.0)) {
                BlockEntity blockEntity = entity.world.getBlockEntity(entity.getDwellingPos());
                if (blockEntity instanceof BirtDwellingBlockEntity blockEntity1) {
                    if (!blockEntity1.isFullOfBirts()) {
                        return true;
                    }
                    entity.setDwellingPos(null);
                }
            }
        }

        return false;
    }

    @Override
    protected void run(ServerWorld world, BirtEntity entity, long time) {
        BlockEntity blockEntity = entity.world.getBlockEntity(entity.getDwellingPos());
        if (blockEntity instanceof BirtDwellingBlockEntity birtDwellingBlockEntity) {
            birtDwellingBlockEntity.tryEnterDwelling(entity);
        }
    }

}
