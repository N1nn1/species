package com.ninni.species.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.ninni.species.entity.BirtEntity;
import com.ninni.species.entity.SpeciesMemoryModuleTypes;
import com.ninni.species.entity.ai.tasks.BirtCommunicatingTask;
import com.ninni.species.entity.ai.tasks.BirtSendMessageTicksTask;
import com.ninni.species.entity.ai.tasks.EnterDwellingTask;
import com.ninni.species.entity.ai.tasks.FindDwellingTask;
import com.ninni.species.entity.ai.tasks.MoveToDwellingTask;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ConditionalTask;
import net.minecraft.entity.ai.brain.task.ForgetAttackTargetTask;
import net.minecraft.entity.ai.brain.task.GoTowardsLookTarget;
import net.minecraft.entity.ai.brain.task.LookAroundTask;
import net.minecraft.entity.ai.brain.task.MeleeAttackTask;
import net.minecraft.entity.ai.brain.task.NoPenaltyStrollTask;
import net.minecraft.entity.ai.brain.task.RandomTask;
import net.minecraft.entity.ai.brain.task.RangedApproachTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.TemptTask;
import net.minecraft.entity.ai.brain.task.TemptationCooldownTask;
import net.minecraft.entity.ai.brain.task.UpdateAttackTargetTask;
import net.minecraft.entity.ai.brain.task.WaitTask;
import net.minecraft.entity.ai.brain.task.WalkTask;
import net.minecraft.entity.ai.brain.task.WanderAroundTask;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class BirtAi {

    public static Brain<?> makeBrain(Brain<BirtEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivities(brain);
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
                new TemptationCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                new TemptationCooldownTask(SpeciesMemoryModuleTypes.TICKS_LEFT_TO_FIND_DWELLING)
        ));
    }

    private static void initIdleActivity(Brain<BirtEntity> brain) {
        brain.setTaskList(Activity.IDLE, ImmutableList.of(
                Pair.of(0, new FindDwellingTask()),
                Pair.of(1, new TemptTask((entity) -> 1.25F)),
                Pair.of(1, new MoveToDwellingTask()),
                Pair.of(2, new EnterDwellingTask()),
                Pair.of(3, new UpdateAttackTargetTask<>(birt -> birt.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_ATTACKABLE))),
                Pair.of(4, new BirtSendMessageTicksTask(ImmutableMap.of(MemoryModuleType.IS_TEMPTED, MemoryModuleState.VALUE_ABSENT))),
                Pair.of(4, new BirtCommunicatingTask(ImmutableMap.of())),
                Pair.of(5, new RandomTask<>(
                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(new BirtNoPenaltyStrollTask(1.5F), 1),
                                Pair.of(new GoTowardsLookTarget(1.5F, 3), 1),
                                Pair.of(new ConditionalTask<>(Entity::isOnGround, new WaitTask(5, 20)), 2)
                        )))
        ), ImmutableSet.of());
    }

    private static void initFightActivities(Brain<BirtEntity> brain) {
        brain.setTaskList(Activity.FIGHT,
                0,
                ImmutableList.of(
                        new ForgetAttackTargetTask<>(),
                        new RangedApproachTask(1.0F),
                        new MeleeAttackTask(20)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static class BirtNoPenaltyStrollTask extends NoPenaltyStrollTask {

        public BirtNoPenaltyStrollTask(float f) {
            super(f);
        }

        @Override
        protected boolean shouldRun(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
            if (pathAwareEntity instanceof BirtEntity birt) {
                Optional<Integer> optionalMemory = birt.getBrain().getOptionalMemory(SpeciesMemoryModuleTypes.GROUND_TICKS);
                if (optionalMemory.isPresent() && optionalMemory.get() < 0) {
                    return true;
                } else if (birt.isInAir()) {
                    return birt.getNavigation().isIdle() && birt.getRandom().nextInt(10) == 0;
                }
            }
            return false;
        }

        @Override
        protected Vec3d findWalkTarget(PathAwareEntity entity) {
            Vec3d vec3d2;
            if (entity instanceof BirtEntity birt) {
                if (birt.getDwellingPos() != null && birt.isDwellingValid() && !birt.isWithinDistance(birt.getDwellingPos(), 22)) {
                    vec3d2 = Vec3d.ofCenter(birt.getDwellingPos()).subtract(birt.getPos()).normalize();
                } else {
                    vec3d2 = birt.getRotationVec(0.0F);
                }
                Vec3d vec3d3 = AboveGroundTargeting.find(birt, 12, 5, vec3d2.x, vec3d2.z, 1.5707964F, 3, 1);
                return vec3d3 != null ? vec3d3 : NoPenaltySolidTargeting.find(birt, 12, 2, -2, vec3d2.x, vec3d2.z, 1.5707963705062866);
            }
            return null;
        }
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
        entity.getBrain().resetPossibleActivities(ImmutableList.of(Activity.SWIM, Activity.FIGHT, Activity.IDLE));
    }

    public static Ingredient getTemptItems() {
        return Ingredient.fromTag(ItemTags.FLOWERS);
    }

}
