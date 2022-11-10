package com.ninni.species.entity.ai.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ninni.species.block.SpeciesBlocks;
import com.ninni.species.entity.BirtEntity;
import com.ninni.species.entity.SpeciesMemoryModuleTypes;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MoveToDwellingTask extends Task<BirtEntity> {
    int ticks;
    @Nullable
    private Path path;
    private int ticksUntilLost;

    public MoveToDwellingTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean isTimeLimitExceeded(long time) {
        return false;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, BirtEntity entity) {
        return entity.getDwellingPos() != null && !entity.hasPositionTarget() && entity.canEnterDwelling() && !this.isCloseEnough(entity, entity.getDwellingPos()) && world.getBlockState(entity.getDwellingPos()).isOf(SpeciesBlocks.BIRT_DWELLING);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, BirtEntity entity, long time) {
        return this.shouldRun(world, entity);
    }

    @Override
    protected void run(ServerWorld world, BirtEntity entity, long time) {
        this.ticks = 0;
        this.ticksUntilLost = 0;
    }

    @Override
    protected void finishRunning(ServerWorld world, BirtEntity entity, long time) {
        this.ticks = 0;
        this.ticksUntilLost = 0;
        entity.getNavigation().stop();
        entity.getNavigation().resetRangeMultiplier();
    }

    @Override
    protected void keepRunning(ServerWorld world, BirtEntity entity, long time) {
        if (entity.getDwellingPos() != null) {
            ++this.ticks;
            if (this.ticks > MathHelper.ceilDiv(600, 2)) {
                this.makeChosenDwellingPossibleDwelling(entity);
            } else if (!entity.getNavigation().isFollowingPath()) {
                if (!entity.isWithinDistance(entity.getDwellingPos(), 16)) {
                    if (entity.isTooFar(entity.getDwellingPos())) {
                        this.setLost(entity);
                    } else {
                        entity.startMovingTo(entity.getDwellingPos());
                    }
                } else {
                    boolean bl = this.startMovingToFar(entity, entity.getDwellingPos());
                    if (!bl) {
                        this.makeChosenDwellingPossibleDwelling(entity);
                    } else if (this.path != null && Objects.requireNonNull(entity.getNavigation().getCurrentPath()).equalsPath(this.path)) {
                        ++this.ticksUntilLost;
                        if (this.ticksUntilLost > 60) {
                            this.setLost(entity);
                            this.ticksUntilLost = 0;
                        }
                    } else {
                        this.path = entity.getNavigation().getCurrentPath();
                    }

                }
            }
        }
    }

    private boolean startMovingToFar(BirtEntity entity, BlockPos pos) {
        entity.getNavigation().setRangeMultiplier(10.0F);
        entity.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
        return entity.getNavigation().getCurrentPath() != null && entity.getNavigation().getCurrentPath().reachesTarget();
    }

    private void addPossibleDwelling(BirtEntity entity, BlockPos pos) {
        List<BlockPos> list = entity.getBrain().getOptionalMemory(SpeciesMemoryModuleTypes.NEAREST_BIRT_DWELLING).isEmpty() ? Lists.newArrayList() : entity.getBrain().getOptionalMemory(SpeciesMemoryModuleTypes.NEAREST_BIRT_DWELLING).get();
        list.add(pos);

        while (list.size() > 3) {
            list.remove(0);
        }
        entity.getBrain().remember(SpeciesMemoryModuleTypes.NEAREST_BIRT_DWELLING, list);

    }

    private void makeChosenDwellingPossibleDwelling(BirtEntity entity) {
        if (entity.getDwellingPos() != null) {
            this.addPossibleDwelling(entity, entity.getDwellingPos());
        }

        this.setLost(entity);
    }

    private void setLost(BirtEntity entity) {
        entity.setDwellingPos(null);
        entity.setTicksLeftToFindDwelling(200);
    }

    private boolean isCloseEnough(BirtEntity entity, BlockPos pos) {
        if (entity.isWithinDistance(pos, 2)) {
            return true;
        } else {
            Path path = entity.getNavigation().getCurrentPath();
            return path != null && path.getTarget().equals(pos) && path.reachesTarget() && path.isFinished();
        }
    }

}
