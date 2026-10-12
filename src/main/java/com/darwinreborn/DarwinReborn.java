package com.darwinreborn;

import com.darwinreborn.entity.ModEntities;
import com.darwinreborn.hybrid.HybridBridge;
import com.darwinreborn.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DarwinReborn implements ModInitializer {

    public static final String MOD_ID = "darwinreborn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        HybridBridge.register();

        // Wild creatures appear in the overworld in small groups. Which species a newborn wild
        // creature becomes is decided from its biome on its first tick (see GeneticCreature).
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                ModEntities.CREATURE,
                8, 2, 4);

        LOGGER.info("Darwin Reborn loaded");
    }
}
