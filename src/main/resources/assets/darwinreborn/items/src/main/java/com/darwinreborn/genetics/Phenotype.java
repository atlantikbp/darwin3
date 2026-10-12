package com.darwinreborn.genetics;

/**
 * Everything observable about a creature, derived from its {@link Genome}.
 * Pure data and pure function: no Minecraft types.
 */
public record Phenotype(
        // body plan (0 = absent for appendages)
        int body, int head, int snout, int ears, int tail, int rearLegs, int frontLegs,
        double legLength,      // 0..1
        double tiltDegrees,    // 0..90
        int color,             // 0xRRGGBB
        int pattern,           // 0..11
        int pattern2,          // 0..11, 0 = none: the pattern on the other strand, shown as well (codominance)
        int markColor,         // 0xRRGGBB, colour of stripes, spots and crest
        int eyeColor,          // 0xRRGGBB iris colour
        int eyeShape,          // 0 round, 1 big, 2 slit, 3 tiny
        int crest,             // 0 none, 1 spikes, 2 sail, 3 mane
        int coat,              // 0 smooth, 1 fur, 2 scales, 3 fluffy
        // physiology
        double size,           // model/hitbox scale, ~0.6..1.6
        double maxHealth,      // hit points
        double speed,          // movement speed attribute
        int lifespanTicks,     // adult lifespan
        double fertility,      // 0..1, scales breeding cooldown
        double litterPotential,// expected offspring, 1..3
        // behaviour
        HunterType hunter,
        boolean fleesWhenHit, boolean seeksShade, boolean herdAnimal, boolean nocturnal,
        // resistances and traits
        boolean burnsInSun, boolean hurtByWater, boolean fireImmune, boolean coldResistant, boolean glows,
        int dropIndex,         // 0..15
        // genetic quality
        double vigor,          // ~0.85..1.25, inbreeding depression / hybrid vigour
        double heterozygosity, // 0..1
        boolean defective,     // homozygous defect: frail, less fertile
        boolean viable         // false for homozygous lethal
) {

    public enum HunterType { PASSIVE, LIVESTOCK, PREDATOR }

    public static Phenotype of(Genome g) {
        int body = g.expressed(Loci.BODY);
        int head = g.expressed(Loci.HEAD);
        int snout = g.expressed(Loci.SNOUT);
        int ears = g.expressed(Loci.EARS);
        int tail = g.expressed(Loci.TAIL);
        int rear = g.expressed(Loci.REAR_LEGS);
        int front = g.expressed(Loci.FRONT_LEGS);

        double legLength = g.quantitative(Loci.LEG_LENGTH) / 255.0;
        double tilt = g.quantitative(Loci.TILT) / 255.0 * 90.0;

        // ---- colour ----
        double r = g.quantitative(Loci.COLOR_R);
        double gr = g.quantitative(Loci.COLOR_G);
        double bl = g.quantitative(Loci.COLOR_B);
        int melanin = g.expressed(Loci.MELANIN);
        if (melanin == 1) {            // melanistic
            r *= 0.40; gr *= 0.40; bl *= 0.40;
        } else if (melanin == 2) {     // albino
            r += (255 - r) * 0.85; gr += (255 - gr) * 0.85; bl += (255 - bl) * 0.85;
        }
        int color = (clamp255(r) << 16) | (clamp255(gr) << 8) | clamp255(bl);
        int pattern = g.expressed(Loci.PATTERN);
        int markColor = hsv(g.quantitative(Loci.MARK_HUE) / 255.0 * 300.0, g.quantitative(Loci.MARK_SAT) / 255.0,
                0.15 + 0.85 * g.quantitative(Loci.MARK_VAL) / 255.0);
        int eyeColor = hsv(g.quantitative(Loci.EYE_HUE) / 255.0 * 300.0, 0.85, 1.0);
        int patternA = g.allele(Loci.PATTERN, 0), patternB = g.allele(Loci.PATTERN, 1);
        int pattern2 = patternA == patternB ? 0 : (patternA == pattern ? patternB : patternA);
        if (pattern2 == pattern) pattern2 = 0;
        int eyeShape = g.expressed(Loci.EYE_SHAPE);
        int crest = g.expressed(Loci.CREST);
        int coat = g.expressed(Loci.COAT);

        // ---- genetic quality ----
        double het = g.heterozygosity();
        double vigor = clamp(0.85 + 0.40 * het, 0.85, 1.25);
        boolean defective = g.expressed(Loci.DEFECT) == 1;
        boolean viable = g.expressed(Loci.LETHAL) != 1;

        // ---- physiology ----
        double size = 0.6 + 1.0 * g.quantitative(Loci.SIZE) / 255.0;
        double health = (6.0 + 20.0 * g.quantitative(Loci.HEALTH) / 255.0) * Math.sqrt(size) * vigor;
        if (defective) health *= 0.6;

        int missingLegPairs = (rear == 0 ? 1 : 0) + (front == 0 ? 1 : 0);
        double legFactor = missingLegPairs == 0 ? 1.0 : missingLegPairs == 1 ? 0.7 : 0.4;
        double speed = (0.16 + 0.18 * g.quantitative(Loci.SPEED) / 255.0)
                * (0.8 + 0.5 * legLength) * legFactor * Math.pow(size, -0.25) * (0.9 + 0.1 * vigor);

        int lifespan = (int) (24000 * (3.0 + 9.0 * g.quantitative(Loci.LONGEVITY) / 255.0));
        double fertility = (0.5 + 0.5 * g.quantitative(Loci.FERTILITY) / 255.0) * (0.6 + 0.4 * Math.min(1.0, het));
        if (defective) fertility *= 0.5;
        double litter = 1.0 + 2.0 * g.quantitative(Loci.LITTER) / 255.0;

        // ---- behaviour ----
        HunterType hunter = switch (g.expressed(Loci.HUNTER)) {
            case 1 -> HunterType.LIVESTOCK;
            case 2 -> HunterType.PREDATOR;
            default -> HunterType.PASSIVE;
        };

        return new Phenotype(
                body, head, snout, ears, tail, rear, front,
                legLength, tilt, color, pattern, pattern2, markColor, eyeColor, eyeShape, crest, coat,
                size, health, speed, lifespan, fertility, litter,
                hunter,
                g.expressed(Loci.FLEE) == 1, g.expressed(Loci.SHADE) == 1,
                g.expressed(Loci.HERD) == 1, g.expressed(Loci.NOCTURNAL) == 1,
                g.expressed(Loci.SUNBURN) == 1, g.expressed(Loci.WATER) == 1,
                g.expressed(Loci.FIRE) == 1, g.expressed(Loci.COLD) == 1,
                g.expressed(Loci.BIOLUM) == 1,
                g.expressed(Loci.DROP),
                vigor, het, defective, viable);
    }

    /** Hue in degrees 0..360, saturation and value 0..1, returned as 0xRRGGBB. */
    static int hsv(double hueDeg, double sat, double val) {
        double h = (hueDeg % 360.0) / 60.0;
        int sector = (int) Math.floor(h);
        double f = h - sector;
        double p = val * (1 - sat), q = val * (1 - sat * f), t = val * (1 - sat * (1 - f));
        double r, g, b;
        switch (sector) {
            case 0 -> { r = val; g = t; b = p; }
            case 1 -> { r = q; g = val; b = p; }
            case 2 -> { r = p; g = val; b = t; }
            case 3 -> { r = p; g = q; b = val; }
            case 4 -> { r = t; g = p; b = val; }
            default -> { r = val; g = p; b = q; }
        }
        return (clamp255(r * 255) << 16) | (clamp255(g * 255) << 8) | clamp255(b * 255);
    }

    private static int clamp255(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
