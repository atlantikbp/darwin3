package com.darwinreborn.entity;

import com.darwinreborn.genetics.Archetype;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/** Chooses which wild species a creature belongs to from the biome it spawns in. */
public final class Species {

    public static Archetype forBiome(Holder<Biome> biome, Random rnd) {
        double roll = rnd.nextDouble();
        if (biome.is(BiomeTags.IS_FOREST)) {
            return pick(roll, Archetype.PIGLET, 0.30, Archetype.WOOLLY, 0.30, Archetype.HOUND, 0.15, Archetype.SHADE, 0.15, Archetype.SKITTER);
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return pick(roll, Archetype.WOOLLY, 0.35, Archetype.HOUND, 0.35, Archetype.PIGLET, 0.15, Archetype.SHADE, 0.10, Archetype.BOVINE);
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return pick(roll, Archetype.SKITTER, 0.30, Archetype.SLUG, 0.30, Archetype.PIGLET, 0.20, Archetype.HOUND, 0.10, Archetype.SHADE);
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return pick(roll, Archetype.BOVINE, 0.40, Archetype.HOUND, 0.25, Archetype.WOOLLY, 0.20, Archetype.EMBER, 0.10, Archetype.SKITTER);
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return pick(roll, Archetype.EMBER, 0.40, Archetype.SKITTER, 0.25, Archetype.BOVINE, 0.20, Archetype.HOUND, 0.10, Archetype.SHADE);
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN)) {
            return pick(roll, Archetype.WOOLLY, 0.40, Archetype.HOUND, 0.30, Archetype.SHADE, 0.15, Archetype.BOVINE, 0.10, Archetype.PIGLET);
        }
        // plains, swamps, deserts and anything else
        return pick(roll, Archetype.PIGLET, 0.25, Archetype.WOOLLY, 0.25, Archetype.BOVINE, 0.20, Archetype.SLUG, 0.12, Archetype.HOUND);
    }

    /** Cumulative selection: (a, pa, b, pb, c, pc, d, pd, fallback). */
    private static Archetype pick(double roll, Archetype a, double pa, Archetype b, double pb, Archetype c, double pc,
                                  Archetype d, double pd, Archetype fallback) {
        if (roll < pa) return a;
        roll -= pa;
        if (roll < pb) return b;
        roll -= pb;
        if (roll < pc) return c;
        roll -= pc;
        if (roll < pd) return d;
        return fallback;
    }

    private Species() {}
}
