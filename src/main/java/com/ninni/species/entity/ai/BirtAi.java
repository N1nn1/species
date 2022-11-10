package com.ninni.species.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.ninni.species.entity.BirtEntity;
import com.ninni.species.entity.ai.tasks.BirtCommunicatingTask;
import com.ninni.species.entity.ai.tasks.BirtSendMessageTicksTask;
import com.ninni.species.entity.ai.tasks.EnterDwellingTask;
import com.ninni.species.entity.ai.tasks.FindDwellingTask;
import com.ninni.species.entity.ai.tasks.MoveToDwellingTask;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ConditionalTask;
import net.minecraft.entity.ai.brain.task.FollowMobTask;
import net.minecraft.entity.ai.brain.task.GoTowardsLookTarget;
import net.minecraft.entity.ai.brain.task.LookAroundTask;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.NoPenaltyStrollTask;
import net.minecraft.entity.ai.brain.task.RandomTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.TemptTask;
import net.minecraft.entity.ai.brain.task.TemptationCooldownTask;
import net.minecraft.entity.ai.brain.task.TimeLimitedTask;
import net.minecraft.entity.ai.brain.task.UpdateAttackTargetTask;
import net.minecraft.entity.ai.brain.task.WaitTask;
import net.minecraft.entity.ai.brain.task.WalkTask;
import net.minecraft.entity.ai.brain.task.WanderAroundTask;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public class BirtAi {

    public static Brain<?> makeBrain(Brain<BirtEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.resetPossibleActivities();
        return brain;
    }

    private static void initCoreActivity(Brain<BirtEntity> brain) {
        brain.setTaskList(Activity.CORE, 0, ImmutableList.of(
                new StayAboveWaterTask(0.8F),
                new WalkTask(2.0F),
                new BirtLookAroundTask(45, 90),
                new WanderAroundTask(),
                new TemptationCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
        ));
    }

    private static void initIdleActivity(Brain<BirtEntity> brain) {
        brain.setTaskList(Activity.IDLE, ImmutableList.of(
                Pair.of(0, new FindDwellingTask()),
                Pair.of(0, new EnterDwellingTask()),
                Pair.of(0, new MoveToDwellingTask()),
                Pair.of(1, new TimeLimitedTask<>(new FollowMobTask(EntityType.PLAYER, 6.0F), UniformIntProvider.create(30, 60))),
                Pair.of(2, new TemptTask((entity) -> 1.25F)),
                Pair.of(3, new UpdateAttackTargetTask<>(LookTargetUtil::hasBreedTarget, birt -> birt.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_ATTACKABLE))),
                Pair.of(4, new BirtSendMessageTicksTask(ImmutableMap.of())),
                Pair.of(4, new BirtCommunicatingTask(ImmutableMap.of())),
                Pair.of(5, new RandomTask<>(
                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(new NoPenaltyStrollTask(1.5F), 1),
                                Pair.of(new GoTowardsLookTarget(1.5F, 3), 1),
                                Pair.of(new ConditionalTask<>(Entity::isOnGround, new WaitTask(5, 20)), 2)
                        )))
        ), ImmutableSet.of());
    }

    private static class BirtLookAroundTask extends LookAroundTask {

        public BirtLookAroundTask(int minRunTime, int maxRunTime) {
            super(minRunTime, maxRunTime);
        }

        @Override
        protected boolean shouldRun(ServerWorld world, MobEntity entity) {
            return entity.isOnGround() && super.shouldRun(world, entity);
        }

        @Override
        protected boolean shouldKeepRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
            return mobEntity.isOnGround() && super.shouldKeepRunning(serverWorld, mobEntity, l);
        }

    }

    public static void updateActivity(BirtEntity entity) {
        entity.getBrain().resetPossibleActivities(ImmutableList.of(Activity.SWIM, Activity.IDLE));
    }

    public static Ingredient getTemptItems() {
        return Ingredient.fromTag(ItemTags.FLOWERS);
    }

}
