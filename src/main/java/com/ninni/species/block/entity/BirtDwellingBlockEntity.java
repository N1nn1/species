package com.ninni.species.block.entity;

import com.google.common.collect.Lists;
import com.ninni.species.block.BirtDwellingBlock;
import com.ninni.species.entity.BirtEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.ninni.species.block.BirtDwellingBlock.BIRTS;

public class BirtDwellingBlockEntity extends BlockEntity {
    public static final String MIN_OCCUPATION_TICKS_KEY = "MinOccupationTicks";
    public static final String ENTITY_DATA_KEY = "EntityData";
    public static final String TICKS_IN_DWELLING_KEY = "TicksInDwelling";
    public static final String BIRTS_KEY = "Birts";
    private static final List<String> IRRELEVANT_BIRT_NBT_KEYS = Arrays.asList("Air", "Bees", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot", "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "CannotEnterDwellingTicks", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "DwellingPos", "HivePos", "Passengers", "Leash", "UUID");
    private final List<Birt> birts = Lists.newArrayList();

    public BirtDwellingBlockEntity(BlockPos pos, BlockState state) {
        super(SpeciesBlockEntities.BIRT_DWELLING, pos, state);
    }

    public boolean hasNoBirts() {
        return this.birts.isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isFullOfBirts() {
        return this.birts.size() == 3;
    }

    public void angerBirts(@Nullable PlayerEntity player, BlockState state, BirtState birtState) {
        List<Entity> list = this.tryReleaseBirt(state, birtState);
        if (player != null) {
            for (Entity entity : list) {
                if (!(entity instanceof BirtEntity birt)) continue;
                if (!(player.getPos().squaredDistanceTo(entity.getPos()) <= 16.0)) continue;
                birt.setTarget(player);
                birt.getBrain().remember(MemoryModuleType.ATTACK_TARGET, player);
                birt.setCannotEnterDwellingTicks(400);
            }
        }
    }

    private List<Entity> tryReleaseBirt(BlockState state, BirtState birtState) {
        ArrayList<Entity> list = Lists.newArrayList();
        this.birts.removeIf(birt -> {
            assert this.world != null;
            return BirtDwellingBlockEntity.releaseBirt(this.world, this.pos, state, birt, list, birtState);
        });
        if (!list.isEmpty()) {
            super.markDirty();
        }
        return list;
    }

    public void tryEnterDwelling(Entity entity) {
        this.tryEnterDwelling(entity, 0);
    }

    public void tryEnterDwelling(Entity entity, int ticksInDwelling) {
        if (this.birts.size() >= 3) {
            return;
        }
        entity.stopRiding();
        entity.removeAllPassengers();
        NbtCompound nbtCompound = new NbtCompound();
        entity.saveNbt(nbtCompound);
        BlockPos blockPos = this.getPos();
        this.addBirt(nbtCompound, ticksInDwelling);
        if (this.world != null) {
            this.world.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0f, 1.0f);
            this.world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(entity, this.getCachedState()));
        }
        entity.discard();
        super.markDirty();
    }

    public void addBirt(NbtCompound nbtCompound, int ticksInDwelling) {
        assert this.world != null;
        this.birts.add(new Birt(nbtCompound, ticksInDwelling, 1200));
    }

    private static boolean releaseBirt(World world, BlockPos pos, BlockState state, Birt birt, @Nullable List<Entity> entities, BirtState birtState) {
        if ((world.isNight() || world.isRaining()) && birtState != BirtState.EMERGENCY) {
            return false;
        }
        NbtCompound nbtCompound = birt.entityData.copy();
        BirtDwellingBlockEntity.removeIrrelevantNbtKeys(nbtCompound);
        nbtCompound.put("DwellingPos", NbtHelper.fromBlockPos(pos));
        Direction direction = state.get(BirtDwellingBlock.FACING);
        BlockPos blockPos = pos.offset(direction);
        boolean bl = !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty();

        if (bl && birtState != BirtState.EMERGENCY) return false;
        Entity newBirt = EntityType.loadEntityWithPassengers(nbtCompound, world, entity -> entity);
        if (newBirt != null) {
            if (newBirt instanceof BirtEntity birtEntity) {
                BirtDwellingBlockEntity.ageBirt(Birt.ticksInDwelling, birtEntity);
                if (entities != null) entities.add(birtEntity);
                float f = newBirt.getWidth();
                double d = bl ? 0.0 : 0.55 + (double)(f / 2.0f);
                double x = (double)pos.getX() + 0.5 + d * (double)direction.getOffsetX();
                double y = (double)pos.getY() + 0.5 - (double)(newBirt.getHeight() / 2.0f);
                double z = (double)pos.getZ() + 0.5 + d * (double)direction.getOffsetZ();
                newBirt.refreshPositionAndAngles(x, y, z, newBirt.getYaw(), newBirt.getPitch());
            } else {
                return false;
            }
            world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(newBirt, world.getBlockState(pos)));
            return world.spawnEntity(newBirt);
        }
        return false;
    }

    static void removeIrrelevantNbtKeys(NbtCompound compound) {
        for (String string : IRRELEVANT_BIRT_NBT_KEYS) compound.remove(string);
    }

    private static void ageBirt(int ticks, BirtEntity birt) {
        int i = birt.getBreedingAge();
        if (i < 0) birt.setBreedingAge(Math.min(0, i + ticks));
        else if (i > 0) birt.setBreedingAge(Math.max(0, i - ticks));
    }

    private static void tickBirts(World world, BlockPos pos, BlockState state, List<Birt> birts) {
        boolean bl = false;
        Iterator<Birt> iterator = birts.iterator();
        world.setBlockState(pos, state.with(BIRTS, birts.size()));
        while (iterator.hasNext()) {
            Birt birt = iterator.next();
            if (Birt.ticksInDwelling > birt.minOccupationTicks) {
                if (BirtDwellingBlockEntity.releaseBirt(world, pos, state, birt, null, BirtState.BIRT_RELEASED)) {
                    bl = true;
                    iterator.remove();
                }
            }
            ++Birt.ticksInDwelling;
        }
        if (bl) BirtDwellingBlockEntity.markDirty(world, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, BirtDwellingBlockEntity blockEntity) {
        BirtDwellingBlockEntity.tickBirts(world, pos, state, blockEntity.birts);
        if (!blockEntity.birts.isEmpty() && world.getRandom().nextDouble() < 0.005) {
            double d = (double)pos.getX() + 0.5;
            double e = pos.getY();
            double f = (double)pos.getZ() + 0.5;
            world.playSound(null, d, e, f, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.birts.clear();
        NbtList nbtList = nbt.getList(BIRTS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            Birt birt = new Birt(nbtCompound.getCompound(ENTITY_DATA_KEY), nbtCompound.getInt(TICKS_IN_DWELLING_KEY), nbtCompound.getInt(MIN_OCCUPATION_TICKS_KEY));
            this.birts.add(birt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put(BIRTS_KEY, this.getBirts());
    }

    public NbtList getBirts() {
        NbtList nbtList = new NbtList();
        for (Birt birt : this.birts) {
            NbtCompound nbtCompound = birt.entityData.copy();
            nbtCompound.remove("UUID");
            NbtCompound nbtCompound2 = new NbtCompound();
            nbtCompound2.put(ENTITY_DATA_KEY, nbtCompound);
            nbtCompound2.putInt(IRRELEVANT_BIRT_NBT_KEYS.toString(), Birt.ticksInDwelling);
            nbtCompound2.putInt(MIN_OCCUPATION_TICKS_KEY, birt.minOccupationTicks);
            nbtList.add(nbtCompound2);
        }
        return nbtList;
    }

    public enum BirtState {
        BIRT_RELEASED,
        EMERGENCY
    }

    static class Birt {
        final NbtCompound entityData;
        static int ticksInDwelling;
        final int minOccupationTicks;

        Birt(NbtCompound entityData, int ticksInDwelling, int minOccupationTicks) {
            BirtDwellingBlockEntity.removeIrrelevantNbtKeys(entityData);
            this.entityData = entityData;
            Birt.ticksInDwelling = ticksInDwelling;
            this.minOccupationTicks = minOccupationTicks;
        }
    }
}
