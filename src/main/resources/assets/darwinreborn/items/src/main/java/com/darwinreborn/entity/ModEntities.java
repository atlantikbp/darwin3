package com.darwinreborn.entity;

import com.darwinreborn.DarwinReborn;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

    public static final ResourceKey<EntityType<?>> CREATURE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, DarwinReborn.id("creature"));

    public static final EntityType<GeneticCreature> CREATURE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CREATURE_KEY,
            EntityType.Builder.<GeneticCreature>of(GeneticCreature::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(8)
                    .build(CREATURE_KEY));

    public static void register() {
        FabricDefaultAttributeRegistry.register(CREATURE, GeneticCreature.createAttributes());
    }

    private ModEntities() {}
}
