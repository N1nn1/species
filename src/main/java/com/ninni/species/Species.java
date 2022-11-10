package com.ninni.species;

import com.google.common.reflect.Reflection;
import com.ninni.species.block.SpeciesBlocks;
import com.ninni.species.block.entity.SpeciesBlockEntities;
import com.ninni.species.client.particles.SpeciesParticles;
import com.ninni.species.entity.BirtEggEntity;
import com.ninni.species.entity.SpeciesEntities;
import com.ninni.species.entity.SpeciesMemoryModuleTypes;
import com.ninni.species.entity.SpeciesSensorTypes;
import com.ninni.species.entity.effect.SpeciesStatusEffects;
import com.ninni.species.item.SpeciesItems;
import com.ninni.species.sound.SpeciesSoundEvents;
import com.ninni.species.structure.SpeciesStructurePieceTypes;
import com.ninni.species.structure.SpeciesStructureSets;
import com.ninni.species.world.gen.features.SpeciesFeatures;
import com.ninni.species.world.gen.features.SpeciesTreeDecorators;
import com.ninni.species.world.gen.structure.SpeciesStructureTypes;
import com.ninni.species.world.gen.structure.SpeciesStructures;
import com.ninni.species.world.poi.SpeciesPointsOfInterests;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Position;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class Species implements ModInitializer {
	public static final String MOD_ID = "species";
	public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(new Identifier(MOD_ID, "item_group"), () -> new ItemStack(SpeciesItems.WRAPTOR_EGG));

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public void onInitialize() {
		Reflection.initialize(
			SpeciesBlocks.class,
			SpeciesBlockEntities.class,
			SpeciesPointsOfInterests.class,
			SpeciesItems.class,
			SpeciesSoundEvents.class,
			SpeciesStatusEffects.class,
			SpeciesParticles.class,
			SpeciesEntities.class,
			SpeciesMemoryModuleTypes.class,
			SpeciesSensorTypes.class,
			SpeciesStructures.class,
			SpeciesStructureTypes.class,
			SpeciesStructureSets.class,
			SpeciesStructurePieceTypes.class
		);
		SpeciesTreeDecorators.init();
		SpeciesFeatures.init();
		SpeciesFeatures.BIRTED_BIRCH_TREES.getKey().ifPresent(this::addFeature);

		DispenserBlock.registerBehavior(SpeciesItems.BIRT_EGG, new ProjectileDispenserBehavior(){
			@Override
			protected ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
				return Util.make(new BirtEggEntity(world, position.getX(), position.getY(), position.getZ()), entity -> entity.setItem(stack));
			}
		});
	}

	private void addFeature(RegistryKey<PlacedFeature> placedFeatureRegistryKey) {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.VEGETAL_DECORATION, placedFeatureRegistryKey);
	}
}
