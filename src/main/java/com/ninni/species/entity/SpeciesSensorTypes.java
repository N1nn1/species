package com.ninni.species.entity;

import com.ninni.species.Species;
import com.ninni.species.entity.ai.BirtAi;
import com.ninni.species.mixin.SensorTypeAccessor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.sensor.TemptationsSensor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Supplier;

public class SpeciesSensorTypes {

    public static final SensorType<TemptationsSensor> BIRT_TEMPTATIONS = register("birt_temptations", () -> new TemptationsSensor(BirtAi.getTemptItems()));

    private static <U extends Sensor<?>> SensorType<U> register(String id, Supplier<U> factory) {
        return Registry.register(Registry.SENSOR_TYPE, new Identifier(Species.MOD_ID, id), SensorTypeAccessor.createSensorType(factory));
    }

}
