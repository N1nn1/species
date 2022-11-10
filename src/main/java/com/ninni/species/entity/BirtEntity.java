package com.ninni.species.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import com.ninni.species.block.entity.BirtDwellingBlockEntity;
import com.ninni.species.block.entity.SpeciesBlockEntities;
import com.ninni.species.entity.ai.BirtAi;
import com.ninni.species.sound.SpeciesSoundEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoWaterTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.PositionSourceType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BirtEntity extends AnimalEntity implements Angerable, Flutterer {
    public final AnimationState flyingAnimationState = new AnimationState();
    public float flapProgress;
    public float maxWingDeviation;
    public float prevMaxWingDeviation;
    public float prevFlapProgress;
    public float flap = 1;
    public int antennaTicks;
    private float flapSpeed = 1.0f;
    public int groundTicks;
    public int messageTicks = 0;
    protected static final ImmutableList<SensorType<? extends Sensor<? super BirtEntity>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SpeciesSensorTypes.BIRT_TEMPTATIONS, SensorType.IS_IN_WATER);
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.BREED_TARGET, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_ATTACKABLE, MemoryModuleType.IS_IN_WATER, MemoryModuleType.IS_PANICKING, SpeciesMemoryModuleTypes.TICKS_LEFT_TO_FIND_DWELLING, SpeciesMemoryModuleTypes.NEAREST_BIRT_DWELLING);
    private static final TrackedData<Byte> BIRT_FLAGS = DataTracker.registerData(BirtEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Integer> ANGER = DataTracker.registerData(BirtEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    @Nullable
    private UUID angryAt;
    private int cannotEnterDwellingTicks;
    int ticksLeftToFindDwelling;
    @Nullable
    BlockPos dwellingPos;

    public BirtEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 20, false);
    }

    @Override
    public Brain<BirtEntity> getBrain() {
        return (Brain<BirtEntity>) super.getBrain();
    }

    @Override
    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return BirtAi.makeBrain(this.createBrainProfile().deserialize(dynamic));
    }

    @Override
    protected Brain.Profile<BirtEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected void mobTick() {
        if (!this.world.isClient) {
            for (Task<?> task : this.getBrain().getRunningTasks()) {
                if (task.getStatus() == Task.Status.RUNNING) {
                    System.out.println(task);
                }
            }
        }
        this.world.getProfiler().push("birtBrain");
        this.getBrain().tick((ServerWorld) this.world, this);
        this.world.getProfiler().pop();
        this.world.getProfiler().push("birtActivityUpdate");
        BirtAi.updateActivity(this);
        this.world.getProfiler().pop();
        super.mobTick();
    }

    @Override
    protected void sendAiDebugData() {
        super.sendAiDebugData();
        DebugInfoSender.sendBrainDebugData(this);
    }

    public static DefaultAttributeContainer.Builder createBirtAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(BIRT_FLAGS, (byte)0);
        this.dataTracker.startTracking(ANGER, 0);
    }
    
    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (POSE.equals(data)) {
            EntityPose entityPose = this.getPose();
            if (entityPose == EntityPose.FALL_FLYING) {
                this.flyingAnimationState.start(this.age);
            } else {
                this.flyingAnimationState.stop();
            }
        }
        super.onTrackedDataSet(data);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("canMessage", this.messageTicks);
        nbt.putInt("CannotEnterDwellingTicks", this.cannotEnterDwellingTicks);
        if (this.hasDwelling()) {
            assert this.getDwellingPos() != null;
            nbt.put("DwellingPos", NbtHelper.fromBlockPos(this.getDwellingPos()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.messageTicks = nbt.getInt("canMessage");
        this.cannotEnterDwellingTicks = nbt.getInt("CannotEnterDwellingTicks");
        if (nbt.contains("DwellingPos")) {
            this.dwellingPos = NbtHelper.toBlockPos(nbt.getCompound("DwellingPos"));
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        Vec3d vec3d = this.getVelocity();
        if (this.antennaTicks > 0) {
            this.antennaTicks--;
        }
        if (!this.onGround && vec3d.y < 0.0 && this.getTarget() == null) {
            this.setVelocity(vec3d.multiply(1.0, 0.6, 1.0));
        }
        if (this.isInAir()) {
            this.groundTicks = random.nextInt(300) + 20;
            this.setPose(EntityPose.FALL_FLYING);
        }
        else {
            this.groundTicks--;
            this.setPose(EntityPose.STANDING);
        }
        if (this.cannotEnterDwellingTicks > 0) {
            --this.cannotEnterDwellingTicks;
        }

        if (this.ticksLeftToFindDwelling > 0) {
            --this.ticksLeftToFindDwelling;
        }
        boolean bl = this.hasAngerTime() && this.getTarget() != null && this.getTarget().squaredDistanceTo(this) < 4.0;
        this.setNearTarget(bl);
        if (this.age % 20 == 0 && !this.isDwellingValid()) {
            this.dwellingPos = null;
        }
        
        if (messageTicks > 0) this.messageTicks--;
        this.flapWings();
    }

    public void startMovingTo(BlockPos pos) {
        Vec3d vec3d = Vec3d.ofBottomCenter(pos);
        int i = 0;
        BlockPos blockPos = this.getBlockPos();
        int j = (int)vec3d.y - blockPos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int m = blockPos.getManhattanDistance(pos);
        if (m < 15) {
            k = m / 2;
            l = m / 2;
        }

        Vec3d vec3d2 = NoWaterTargeting.find(this, k, l, i, vec3d, 0.3141592741012573);
        if (vec3d2 != null) {
            this.navigation.setRangeMultiplier(0.5F);
            this.navigation.startMovingTo(vec3d2.x, vec3d2.y, vec3d2.z, 1.0);
        }
    }
    
    @Nullable
    public BirtEntity findReciever() {
        List<? extends BirtEntity> list = this.world.getTargets(BirtEntity.class, TargetPredicate.DEFAULT, this, this.getBoundingBox().expand(8.0));
        double d = Double.MAX_VALUE;
        BirtEntity birt = null;
        for (BirtEntity birt2 : list) {
            if (!(this.squaredDistanceTo(birt2) < d)) continue;
            birt = birt2;
            d = this.squaredDistanceTo(birt2);
        }
        return birt;
    }

    public boolean canSendMessage() {
        return this.messageTicks > 0 && this.getTarget() == null;
    }

    public void setMessageTicks(int messageTicks) {
        this.messageTicks = messageTicks;
    }

    public void resetMessageTicks() {
        this.messageTicks = 0;
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 10) {
            this.antennaTicks = 60;
        }
        else {
            super.handleStatus(status);
        }
    }

    public void sendMessage(ServerWorld world, BirtEntity other) {
        this.resetMessageTicks();
        other.resetMessageTicks();

        PositionSource positionSource = new PositionSource() {
            @Override
            public Optional<Vec3d> getPos(World world) {
                return Optional.of(new Vec3d(BirtEntity.this.getX(), BirtEntity.this.getY() + 0.75, BirtEntity.this.getZ()));
            }

            @Override
            public PositionSourceType<?> getType() {
                return PositionSourceType.ENTITY;
            }
        };

        world.playSound(null, other.getBlockPos(), SpeciesSoundEvents.ENTITY_BIRT_MESSAGE, SoundCategory.NEUTRAL, 1,  0.6f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        world.spawnParticles(new VibrationParticleEffect(positionSource, 20), other.getX(), other.getY() + 0.75, other.getZ(), 0, 0, 0, 0, 0);
    }

    private void flapWings() {
        this.prevFlapProgress = this.flapProgress;
        this.prevMaxWingDeviation = this.maxWingDeviation;
        this.maxWingDeviation += (float)(this.onGround || this.hasVehicle() ? -1 : 4) * 0.3f;
        this.maxWingDeviation = MathHelper.clamp(this.maxWingDeviation, 0.0f, 1.0f);
        if (!this.onGround && this.flapSpeed < 1.0f) {
            this.flapSpeed = 1.0f;
        }
        this.flapSpeed *= 0.9f;
        Vec3d vec3d = this.getVelocity();
        if (!this.onGround && vec3d.y < 0.0) {
            this.setVelocity(vec3d.multiply(1.0, 0.6, 1.0));
        }
        this.flapProgress += this.flapSpeed * 2.0f;
    }

    @Override
    protected boolean hasWings() {
        return this.speed > this.flap;
    }

    @Override
    protected void addFlapEffects() {
        this.playSound(SpeciesSoundEvents.ENTITY_BIRT_FLY, 0.15f, 1.0f);
        this.flap = this.speed + this.maxWingDeviation / 2.0f;
    }

    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER);
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.dataTracker.set(ANGER, angerTime);
    }

    @Nullable
    @Override
    public UUID getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }
    
    public boolean canEnterDwelling() {
        if (this.cannotEnterDwellingTicks <= 0 && this.getTarget() == null) {
            return this.world.isRaining() || this.world.isNight();
        } else {
            return false;
        }
    }

    public void setCannotEnterDwellingTicks(int cannotEnterDwellingTicks) {
        this.cannotEnterDwellingTicks = cannotEnterDwellingTicks;
    }

    boolean isDwellingValid() {
        if (!this.hasDwelling()) {
            return false;
        } else {
            BlockEntity blockEntity = this.world.getBlockEntity(this.dwellingPos);
            return blockEntity != null && blockEntity.getType() == SpeciesBlockEntities.BIRT_DWELLING;
        }
    }

    private void setNearTarget(boolean nearTarget) {
        this.setBirtFlag(2, nearTarget);
    }

    public boolean isTooFar(BlockPos pos) {
        return !this.isWithinDistance(pos, 32);
    }

    private void setBirtFlag(int bit, boolean value) {
        if (value) {
            this.dataTracker.set(BIRT_FLAGS, (byte)(this.dataTracker.get(BIRT_FLAGS) | bit));
        } else {
            this.dataTracker.set(BIRT_FLAGS, (byte)(this.dataTracker.get(BIRT_FLAGS) & ~bit));
        }

    }

    @Debug
    public boolean hasDwelling() {
        return this.dwellingPos != null;
    }

    @Nullable
    @Debug
    public BlockPos getDwellingPos() {
        return this.dwellingPos;
    }

    public boolean isWithinDistance(BlockPos pos, int distance) {
        return pos.isWithinDistance(this.getBlockPos(), distance);
    }
    
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    public boolean isInAir() {
        return !this.onGround;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }
    
    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };

        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(false);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    @SuppressWarnings("unused")
    public static boolean canSpawn(EntityType<? extends PassiveEntity> type, WorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
        return false;
    }
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SpeciesSoundEvents.ENTITY_BIRT_IDLE;
    }
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SpeciesSoundEvents.ENTITY_BIRT_HURT;
    }
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SpeciesSoundEvents.ENTITY_BIRT_DEATH;
    }

    public void setTicksLeftToFindDwelling(int ticksLeftToFindDwelling) {
        this.ticksLeftToFindDwelling = ticksLeftToFindDwelling;
    }

    public void setDwellingPos(BlockPos dwellingPos) {
        this.dwellingPos = dwellingPos;
    }

    public boolean doesDwellingHaveSpace(BlockPos pos) {
        BlockEntity blockEntity = this.world.getBlockEntity(pos);
        if (blockEntity instanceof BirtDwellingBlockEntity birtDwellingBlockEntity) {
            return !birtDwellingBlockEntity.isFullOfBirts();
        } else {
            return false;
        }
    }
    
}
