package com.darwinreborn.item;

import com.darwinreborn.DarwinReborn;
import com.darwinreborn.entity.ModEntities;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {

    public static final ResourceKey<Item> GENETIC_LENS_KEY = key("genetic_lens");
    public static final ResourceKey<Item> CREATURE_SPAWN_EGG_KEY = key("creature_spawn_egg");

    public static final Item GENETIC_LENS = register(
            GENETIC_LENS_KEY, GeneticLensItem::new, new Item.Properties().stacksTo(1));

    public static final Item CREATURE_SPAWN_EGG = register(
            CREATURE_SPAWN_EGG_KEY, SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.CREATURE));

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, DarwinReborn.id(name));
    }

    private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(tab -> tab.accept(CREATURE_SPAWN_EGG));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> tab.accept(GENETIC_LENS));
    }

    private ModItems() {}
}
