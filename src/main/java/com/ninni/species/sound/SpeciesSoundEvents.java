package com.ninni.species.sound;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.ninni.species.Species.*;

public interface SpeciesSoundEvents {

    SoundEvent ENTITY_WRAPTOR_AGGRO        = wraptor("aggro");
    SoundEvent ENTITY_WRAPTOR_AGITATED     = wraptor("agitated");
    SoundEvent ENTITY_WRAPTOR_ATTACK       = wraptor("attack");
    SoundEvent ENTITY_WRAPTOR_DEATH        = wraptor("death");
    SoundEvent ENTITY_WRAPTOR_HURT         = wraptor("hurt");
    SoundEvent ENTITY_WRAPTOR_IDLE         = wraptor("idle");
    SoundEvent ENTITY_WRAPTOR_SHEAR        = wraptor("shear");
    SoundEvent ENTITY_WRAPTOR_STEP         = wraptor("step");
    SoundEvent ENTITY_WRAPTOR_FEATHER_LOSS = wraptor("feather_loss");
    private static SoundEvent wraptor(String type) {
        return createEntitySound("wraptor", type);
    }


    private static SoundEvent register(String id) {
        Identifier identifier = new Identifier(MOD_ID, id);
        return Registry.register(Registry.SOUND_EVENT, identifier, new SoundEvent(identifier));
    }
    private static SoundEvent createEntitySound(String entity, String id) {
        return register("entity." + entity + "." + id);
    }
}