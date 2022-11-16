package com.ninni.species.block;

import com.ninni.species.block.entity.BirtDwellingBlockEntity;
import com.ninni.species.block.entity.SpeciesBlockEntities;
import com.ninni.species.block.property.SpeciesProperties;
import com.ninni.species.entity.BirtEntity;
import com.ninni.species.item.SpeciesItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class BirtDwellingBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty BIRTS = SpeciesProperties.BIRTS;
    public static final IntProperty EGGS = SpeciesProperties.EGGS;

    public BirtDwellingBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(BIRTS, 0).with(EGGS, 0));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity blockEntity;
        if (state.get(EGGS) > 0 && player.getStackInHand(hand).isEmpty()) {
            if (!world.isClient && (blockEntity = world.getBlockEntity(pos)) instanceof BirtDwellingBlockEntity) {
                BirtDwellingBlockEntity blockEntity1 = (BirtDwellingBlockEntity)blockEntity;
                blockEntity1.angerBirts(player, state, BirtDwellingBlockEntity.BirtState.EMERGENCY);
            }
            world.setBlockState(pos, state.with(EGGS, state.get(EGGS) - 1));
            BlockPos itemPos = pos.offset(state.get(FACING));
            world.playSound(null, itemPos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, 1 ,1);
            dropStack(world, itemPos, new ItemStack(SpeciesItems.BIRT_EGG));
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.afterBreak(world, player, pos, state, blockEntity, stack);
        if (!world.isClient && blockEntity instanceof BirtDwellingBlockEntity blockEntity1) {
            blockEntity1.angerBirts(player, state, BirtDwellingBlockEntity.BirtState.EMERGENCY);
            world.updateComparators(pos, this);
            this.angerNearbyBirts(world, pos);
        }
    }

    private void angerNearbyBirts(World world, BlockPos pos) {
        List<BirtEntity> birtList = world.getNonSpectatingEntities(BirtEntity.class, new Box(pos).expand(8.0, 6.0, 8.0));
        if (!birtList.isEmpty()) {
            List<PlayerEntity> playerList = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0, 6.0, 8.0));
            for (BirtEntity birt : birtList) {
                if (birt.getTarget() != null) continue;
                birt.setTarget(playerList.get(world.random.nextInt(playerList.size())));
            }
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite()).with(EGGS, this.getDefaultState().get(EGGS)).with(BIRTS, this.getDefaultState().get(BIRTS));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, BIRTS, EGGS);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BirtDwellingBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : BirtDwellingBlock.checkType(type, SpeciesBlockEntities.BIRT_DWELLING, BirtDwellingBlockEntity::serverTick);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity;
        if (!world.isClient && player.isCreative() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) && (blockEntity = world.getBlockEntity(pos)) instanceof BirtDwellingBlockEntity) {
            BirtDwellingBlockEntity blockEntity1 = (BirtDwellingBlockEntity)blockEntity;
            ItemStack itemStack = new ItemStack(this);
            boolean bl = !blockEntity1.hasNoBirts();
            if (bl) {
                NbtCompound nbtCompound = new NbtCompound();
                nbtCompound.put("Birts", blockEntity1.getBirts());
                BlockItem.setBlockEntityNbt(itemStack, SpeciesBlockEntities.BIRT_DWELLING, nbtCompound);
                nbtCompound = new NbtCompound();
                itemStack.setSubNbt("BlockStateTag", nbtCompound);
                ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        if (blockEntity instanceof BirtDwellingBlockEntity) {
            return BirtDwellingBlockEntity.getVibrationListener();
        }
        return null;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        BlockEntity blockEntity;
        Entity entity = builder.getNullable(LootContextParameters.THIS_ENTITY);
        if ((entity instanceof TntEntity || entity instanceof CreeperEntity || entity instanceof WitherSkullEntity || entity instanceof WitherEntity || entity instanceof TntMinecartEntity) && (blockEntity = builder.getNullable(LootContextParameters.BLOCK_ENTITY)) instanceof BirtDwellingBlockEntity) {
            BirtDwellingBlockEntity blockEntity1 = (BirtDwellingBlockEntity)blockEntity;
            blockEntity1.angerBirts(null, state, com.ninni.species.block.entity.BirtDwellingBlockEntity.BirtState.EMERGENCY);
        }
        return super.getDroppedStacks(state, builder);
    }
}

