package com.ninni.species.entity.ai.tasks;

import com.ninni.species.entity.BirtEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Map;

public class BirtSendMessageTicksTask extends Task<BirtEntity> {

    public BirtSendMessageTicksTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState) {
        super(requiredMemoryState);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, BirtEntity entity) {
        return entity.findReciever() != null && entity.getRandom().nextInt(300) == 0 && !entity.canSendMessage();
    }

    @Override
    protected void run(ServerWorld world, BirtEntity entity, long time) {
        entity.setMessageTicks(600);
    }

}
