package com.darwinreborn.genetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A wild "species": a recognisable set of allele values used to seed the world.
 * Individuals of one archetype look alike, but carry hidden recessive alleles
 * (the raw material for selective breeding) and rare visible variants.
 */
public final class Archetype {

    private final String name;
    private final Map<Locus, Integer> values = new HashMap<>();

    private Archetype(String name, Object... pairs) {
        this.name = name;
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(Loci.byId((String) pairs[i]), (Integer) pairs[i + 1]);
        }
    }

    public String name() { return name; }

    // ------------------------------------------------------------------ registry

    private static final Map<String, Archetype> REGISTRY = new HashMap<>();

    private static Archetype reg(String name, Object... pairs) {
        Archetype a = new Archetype(name, pairs);
        REGISTRY.put(name, a);
        return a;
    }

    // Wild species (also the genome of the vanilla pig): pink, stocky, broad snout, small ears.
    public static final Archetype PIGLET = reg("piglet",
            "body", 3, "head", 3, "snout", 2, "ears", 1, "tail", 1, "rear_legs", 2, "front_legs", 2,
            "leg_length", 60, "tilt", 5, "color_r", 240, "color_g", 170, "color_b", 180, "melanin", 0,
            "pattern", 0, "size", 110, "health", 100, "speed", 90, "longevity", 130, "fertility", 170,
            "litter", 150, "hunter", 0, "flee", 1, "shade", 0, "herd", 0, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 1, "crest", 0, "eye_shape", 0, "eye_hue", 35,
            "mark_hue", 8, "mark_val", 120, "mark_sat", 150, "coat", 0);

    // Wild species (also the sheep): fluffy white wool, dark face, ears and legs.
    public static final Archetype WOOLLY = reg("woolly",
            "body", 1, "head", 2, "snout", 1, "ears", 2, "tail", 1, "rear_legs", 1, "front_legs", 1,
            "leg_length", 100, "tilt", 0, "color_r", 238, "color_g", 236, "color_b", 228, "melanin", 0,
            "pattern", 11, "size", 100, "health", 90, "speed", 100, "longevity", 140, "fertility", 130,
            "litter", 70, "hunter", 0, "flee", 1, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 1, "drop", 2, "crest", 0, "eye_shape", 2, "eye_hue", 51,
            "mark_hue", 20, "mark_val", 55, "mark_sat", 110, "coat", 3);

    // Wild species (also the cow): big, brown with pale patches, horns.
    public static final Archetype BOVINE = reg("bovine",
            "body", 1, "head", 1, "snout", 1, "ears", 3, "tail", 2, "rear_legs", 3, "front_legs", 3,
            "leg_length", 130, "tilt", 0, "color_r", 100, "color_g", 70, "color_b", 50, "melanin", 0,
            "pattern", 4, "size", 160, "health", 170, "speed", 80, "longevity", 180, "fertility", 100,
            "litter", 20, "hunter", 0, "flee", 0, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 3, "crest", 0, "eye_shape", 0, "eye_hue", 38,
            "mark_hue", 40, "mark_val", 245, "mark_sat", 30, "coat", 1);

    // Wild species (also the wolf): grey, lean, fast.
    public static final Archetype HOUND = reg("hound",
            "body", 2, "head", 2, "snout", 3, "ears", 2, "tail", 3, "rear_legs", 2, "front_legs", 2,
            "leg_length", 170, "tilt", 25, "color_r", 130, "color_g", 130, "color_b", 140, "melanin", 0,
            "pattern", 6, "size", 100, "health", 110, "speed", 190, "longevity", 150, "fertility", 90,
            "litter", 110, "hunter", 1, "flee", 0, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 1, "drop", 4, "crest", 1, "eye_shape", 0, "eye_hue", 40,
            "mark_hue", 30, "mark_val", 70, "mark_sat", 80, "coat", 1);

    public static final Archetype SKITTER = reg("skitter",
            "body", 3, "head", 1, "snout", 0, "ears", 0, "tail", 0, "rear_legs", 3, "front_legs", 3,
            "leg_length", 200, "tilt", 45, "color_r", 40, "color_g", 35, "color_b", 45, "melanin", 0,
            "pattern", 5, "size", 70, "health", 60, "speed", 150, "longevity", 90, "fertility", 200,
            "litter", 220, "hunter", 2, "flee", 0, "shade", 1, "herd", 0, "nocturnal", 1, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 5, "crest", 0, "eye_shape", 1, "eye_hue", 0, "mark_hue", 0,
            "mark_val", 230, "mark_sat", 190, "coat", 2);

    public static final Archetype SLUG = reg("slug",
            "body", 0, "head", 1, "snout", 0, "ears", 3, "tail", 0, "rear_legs", 0, "front_legs", 0,
            "leg_length", 20, "tilt", 0, "color_r", 150, "color_g", 190, "color_b", 90, "melanin", 0,
            "pattern", 7, "size", 60, "health", 50, "speed", 40, "longevity", 220, "fertility", 210,
            "litter", 180, "hunter", 0, "flee", 0, "shade", 1, "herd", 0, "nocturnal", 1, "sunburn", 1,
            "water", 1, "fire", 0, "cold", 0, "drop", 6, "crest", 0, "eye_shape", 3, "eye_hue", 60,
            "mark_hue", 51, "mark_val", 230, "mark_sat", 190, "coat", 0);

    public static final Archetype EMBER = reg("ember",
            "body", 2, "head", 3, "snout", 3, "ears", 3, "tail", 2, "rear_legs", 3, "front_legs", 3,
            "leg_length", 150, "tilt", 15, "color_r", 220, "color_g", 90, "color_b", 30, "melanin", 0,
            "pattern", 2, "size", 130, "health", 140, "speed", 130, "longevity", 170, "fertility", 80,
            "litter", 60, "hunter", 1, "flee", 0, "shade", 0, "herd", 0, "nocturnal", 0, "sunburn", 0,
            "water", 1, "fire", 1, "cold", 0, "drop", 7, "biolum", 1, "crest", 2, "eye_shape", 2, "eye_hue", 45,
            "mark_hue", 8, "mark_val", 60, "mark_sat", 190, "coat", 2);

    public static final Archetype SHADE = reg("shade",
            "body", 3, "head", 2, "snout", 1, "ears", 3, "tail", 3, "rear_legs", 1, "front_legs", 1,
            "leg_length", 120, "tilt", 30, "color_r", 50, "color_g", 30, "color_b", 90, "melanin", 1,
            "pattern", 8, "size", 90, "health", 80, "speed", 140, "longevity", 160, "fertility", 110,
            "litter", 90, "hunter", 0, "flee", 1, "shade", 1, "herd", 0, "nocturnal", 1, "sunburn", 1,
            "water", 0, "fire", 0, "cold", 0, "drop", 8, "biolum", 1, "crest", 3, "eye_shape", 1, "eye_hue", 153,
            "mark_hue", 153, "mark_val", 255, "mark_sat", 190, "coat", 0);

    // ---- Genomes of other vanilla animals. They never spawn in the wild as creatures of the mod: the hybrid system
    //      (see HybridBridge) derives a genome for a vanilla animal so that it can be crossed with any other. ----

    // Chicken: small, white, fluffy, red comb.
    public static final Archetype FOWL = reg("fowl",
            "body", 0, "head", 1, "snout", 3, "ears", 0, "tail", 1, "rear_legs", 1, "front_legs", 0,
            "leg_length", 110, "tilt", 40, "color_r", 240, "color_g", 240, "color_b", 232, "melanin", 0,
            "pattern", 0, "size", 55, "health", 40, "speed", 100, "longevity", 100, "fertility", 220,
            "litter", 200, "hunter", 0, "flee", 1, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 12, "crest", 3, "eye_shape", 0, "eye_hue", 30,
            "mark_hue", 0, "mark_val", 230, "mark_sat", 210, "coat", 3);

    // Rabbit: brown, long ears, long hind legs.
    public static final Archetype HARE = reg("hare",
            "body", 0, "head", 1, "snout", 1, "ears", 2, "tail", 1, "rear_legs", 2, "front_legs", 1,
            "leg_length", 160, "tilt", 35, "color_r", 170, "color_g", 140, "color_b", 110, "melanin", 0,
            "pattern", 5, "size", 70, "health", 60, "speed", 170, "longevity", 90, "fertility", 230,
            "litter", 230, "hunter", 0, "flee", 1, "shade", 0, "herd", 0, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 1, "drop", 3, "crest", 0, "eye_shape", 1, "eye_hue", 15,
            "mark_hue", 25, "mark_val", 90, "mark_sat", 150, "coat", 1);

    // Goat: grey-white, horns, short tail.
    public static final Archetype GOAT = reg("goat",
            "body", 0, "head", 2, "snout", 1, "ears", 3, "tail", 1, "rear_legs", 3, "front_legs", 3,
            "leg_length", 120, "tilt", 5, "color_r", 205, "color_g", 195, "color_b", 180, "melanin", 0,
            "pattern", 0, "size", 85, "health", 90, "speed", 130, "longevity", 140, "fertility", 120,
            "litter", 90, "hunter", 0, "flee", 0, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 1, "drop", 3, "crest", 0, "eye_shape", 2, "eye_hue", 60,
            "mark_hue", 25, "mark_val", 90, "mark_sat", 100, "coat", 1);

    // Horse: tall, brown, long legs, a mane.
    public static final Archetype HORSE = reg("horse",
            "body", 1, "head", 2, "snout", 2, "ears", 1, "tail", 3, "rear_legs", 3, "front_legs", 3,
            "leg_length", 210, "tilt", 18, "color_r", 130, "color_g", 85, "color_b", 55, "melanin", 0,
            "pattern", 0, "size", 170, "health", 150, "speed", 200, "longevity", 190, "fertility", 70,
            "litter", 20, "hunter", 0, "flee", 0, "shade", 0, "herd", 1, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 3, "crest", 3, "eye_shape", 0, "eye_hue", 20,
            "mark_hue", 20, "mark_val", 45, "mark_sat", 140, "coat", 1);

    // Cat: lean, orange tabby, slit pupils.
    public static final Archetype FELINE = reg("feline",
            "body", 0, "head", 1, "snout", 1, "ears", 1, "tail", 2, "rear_legs", 1, "front_legs", 1,
            "leg_length", 100, "tilt", 15, "color_r", 205, "color_g", 140, "color_b", 80, "melanin", 0,
            "pattern", 1, "size", 60, "health", 60, "speed", 160, "longevity", 130, "fertility", 150,
            "litter", 150, "hunter", 0, "flee", 1, "shade", 0, "herd", 0, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 0, "drop", 5, "crest", 0, "eye_shape", 2, "eye_hue", 95,
            "mark_hue", 18, "mark_val", 85, "mark_sat", 200, "coat", 1);

    // Fox: orange, pointed face, bushy tail, dark socks and ears.
    public static final Archetype VULPINE = reg("vulpine",
            "body", 0, "head", 2, "snout", 3, "ears", 3, "tail", 3, "rear_legs", 1, "front_legs", 1,
            "leg_length", 125, "tilt", 20, "color_r", 215, "color_g", 110, "color_b", 40, "melanin", 0,
            "pattern", 11, "size", 70, "health", 70, "speed", 170, "longevity", 120, "fertility", 110,
            "litter", 100, "hunter", 0, "flee", 1, "shade", 0, "herd", 0, "nocturnal", 0, "sunburn", 0,
            "water", 0, "fire", 0, "cold", 1, "drop", 0, "crest", 0, "eye_shape", 2, "eye_hue", 45,
            "mark_hue", 15, "mark_val", 45, "mark_sat", 120, "coat", 1);

    public static Archetype byName(String name) {
        Archetype a = REGISTRY.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown archetype: " + name);
        return a;
    }

    public static List<Archetype> all() {
        List<Archetype> list = new ArrayList<>(REGISTRY.values());
        list.sort((x, y) -> x.name.compareTo(y.name));
        return Collections.unmodifiableList(list);
    }

    // ------------------------------------------------------------------ generation

    /** Probability that a given strand of a wild animal carries a hidden recessive at a specified locus. */
    private static final double CARRIER_CHANCE = 0.35;

    /**
     * Creates a wild individual.
     *
     * @param variantChance probability per specified discrete locus that one strand holds a completely
     *                      random allele (may be visible); 0 gives pure archetype appearance with only
     *                      hidden recessives
     */
    public Genome wild(Random rnd, double variantChance) {
        int[] a = new int[Loci.COUNT];
        int[] b = new int[Loci.COUNT];
        for (Locus l : Loci.ALL) {
            int i = l.index();
            Integer v = values.get(l);
            if (v == null) {
                fillUnspecified(l, rnd, a, b);
                continue;
            }
            if (l.isDiscrete()) {
                a[i] = v;
                b[i] = hiddenOrVariant(l, v, rnd, variantChance);
            } else {
                a[i] = l.clamp(v + (int) Math.round(rnd.nextGaussian() * 12));
                b[i] = l.clamp(v + (int) Math.round(rnd.nextGaussian() * 12));
            }
            if (rnd.nextBoolean()) {   // do not always put the archetype allele on strand A
                int t = a[i];
                a[i] = b[i];
                b[i] = t;
            }
        }
        return Genome.of(a, b);
    }

    private static int hiddenOrVariant(Locus l, int archetypeAllele, Random rnd, double variantChance) {
        if (rnd.nextDouble() < variantChance) return rnd.nextInt(l.alleleCount());
        if (rnd.nextDouble() < CARRIER_CHANCE) {
            // an allele that is recessive to (or equal to) the archetype allele: invisible, but heritable
            List<Integer> hidden = new ArrayList<>();
            for (int x = 0; x < l.alleleCount(); x++) {
                if (l.rank(x) <= l.rank(archetypeAllele)) hidden.add(x);
            }
            return hidden.get(rnd.nextInt(hidden.size()));
        }
        return archetypeAllele;
    }

    private static void fillUnspecified(Locus l, Random rnd, int[] a, int[] b) {
        int i = l.index();
        if (l == Loci.DEFECT || l == Loci.LETHAL || l == Loci.BIOLUM) {
            double carrier = l == Loci.DEFECT ? 0.03 : l == Loci.LETHAL ? 0.01 : 0.02;
            a[i] = 0;
            b[i] = rnd.nextDouble() < carrier ? 1 : 0;
        } else if (l.isDiscrete()) {
            a[i] = rnd.nextInt(l.alleleCount());
            b[i] = rnd.nextInt(l.alleleCount());
        } else {
            a[i] = l.clamp(128 + (int) Math.round(rnd.nextGaussian() * 45));
            b[i] = l.clamp(128 + (int) Math.round(rnd.nextGaussian() * 45));
        }
    }

    /** Value this archetype specifies for a locus, or -1 if unspecified. */
    public int specified(Locus l) {
        Integer v = values.get(l);
        return v == null ? -1 : v;
    }
}
