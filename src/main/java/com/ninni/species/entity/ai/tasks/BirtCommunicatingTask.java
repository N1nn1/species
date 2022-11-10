package com.ninni.species.entity.ai.tasks;

import com.ninni.species.entity.BirtEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BirtCommunicatingTask extends Task<BirtEntity> {
    private int timer;
    @Nullable
    protected BirtEntity reciever;

    public BirtCommunicatingTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState) {
        super(requiredMemoryState);
    }

    @Override
    protected boolean isTimeLimitExceeded(long time) {
        return false;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, BirtEntity entity) {
        if (!entity.canSendMessage()) {
            return false;
        } else {
            this.reciever = entity.findReciever();
            return this.reciever != null;
        }
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, BirtEntity entity, long time) {
        return this.reciever != null && this.reciever.isAlive() && entity.canSendMessage() && this.timer < 60;
    }

    @Override
    protected void run(ServerWorld world, BirtEntity entity, long time) {
        entity.world.sendEntityStatus(entity, (byte) 10);
        entity.world.sendEntityStatus(this.reciever, (byte) 10);
    }

    @Override
    protected void keepRunning(ServerWorld world, BirtEntity entity, long time) {
        if (this.reciever == null) return;
        entity.getLookControl().lookAt(this.reciever, 10.0f, entity.getMaxLookPitchChange());
        entity.getNavigation().stop();
        this.reciever.getLookControl().lookAt(entity, 10.0f, this.reciever.getMaxLookPitchChange());
        this.reciever.getNavigation().stop();
        ++this.timer;
        if (this.timer >= MathHelper.ceilDiv(60, 2)) {
            entity.sendMessage((ServerWorld)entity.world, this.reciever);
        }
    }

    @Override
    protected void finishRunning(ServerWorld world, BirtEntity entity, long time) {
        this.reciever = null;
        this.timer = 0;
    }

}
