package com.ninni.species.entity;

import com.mojang.serialization.Codec;
import com.ninni.species.Species;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Optional;

public class SpeciesMemoryModuleTypes {

    public static final MemoryModuleType<Integer> TICKS_LEFT_TO_FIND_DWELLING = register("ticks_left_to_find_dwelling", Codec.INT);
    public static final MemoryModuleType<Integer> GROUND_TICKS = register("ground_ticks", Codec.INT);
    public static final MemoryModuleType<List<BlockPos>> NEAREST_BIRT_DWELLING = register("nearest_birt_dwelling");

    private static <U> MemoryModuleType<U> register(String id) {
        return Registry.register(Registry.MEMORY_MODULE_TYPE, new Identifier(Species.MOD_ID, id), new MemoryModuleType<>(Optional.empty()));
    }

    private static <U> MemoryModuleType<U> register(String id, Codec<U> codec) {
        return Registry.register(Registry.MEMORY_MODULE_TYPE, new Identifier(Species.MOD_ID, id), new MemoryModuleType<U>(Optional.of(codec)));
    }

}
