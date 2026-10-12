package com.darwinreborn.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalogue of all genes. Loci are grouped into chromosomes; loci that share a
 * chromosome are <em>linked</em>: they tend to be inherited together, and only
 * separate through crossing over. Loci on different chromosomes assort independently.
 *
 * <p>Declaration order inside a chromosome is the physical order on it, so loci that
 * are declared next to each other are closely linked.</p>
 */
public final class Loci {

    private static final List<Locus> TMP = new ArrayList<>();
    private static final Map<String, Locus> BY_ID = new HashMap<>();

    private static Locus disc(String id, int chromosome, double mutationRate, int... rank) {
        Locus l = new Locus(TMP.size(), id, chromosome, Locus.Type.DISCRETE, rank.length, rank, mutationRate);
        TMP.add(l);
        BY_ID.put(id, l);
        return l;
    }

    private static Locus quant(String id, int chromosome, double mutationRate) {
        Locus l = new Locus(TMP.size(), id, chromosome, Locus.Type.QUANTITATIVE, Locus.Q_MAX + 1, null, mutationRate);
        TMP.add(l);
        BY_ID.put(id, l);
        return l;
    }

    // ---- Chromosome 0: body plan. Allele 0 of an appendage means "absent" (recessive). ----
    public static final Locus BODY      = disc("body", 0, 0.010, 2, 3, 1, 0);
    public static final Locus HEAD      = disc("head", 0, 0.010, 0, 3, 1, 2);
    public static final Locus SNOUT     = disc("snout", 0, 0.010, 0, 2, 3, 1);
    public static final Locus EARS      = disc("ears", 0, 0.010, 0, 1, 3, 2);
    public static final Locus TAIL      = disc("tail", 0, 0.010, 0, 2, 1, 3);

    // ---- Chromosome 1: limbs ----
    public static final Locus REAR_LEGS  = disc("rear_legs", 1, 0.010, 0, 3, 2, 1);
    public static final Locus FRONT_LEGS = disc("front_legs", 1, 0.010, 0, 1, 3, 2);
    public static final Locus LEG_LENGTH = quant("leg_length", 1, 0.04);
    public static final Locus TILT       = quant("tilt", 1, 0.04);

    // ---- Chromosome 2: colour ----
    public static final Locus COLOR_R  = quant("color_r", 2, 0.05);
    public static final Locus COLOR_G  = quant("color_g", 2, 0.05);
    public static final Locus COLOR_B  = quant("color_b", 2, 0.05);
    /** 0 = normal, 1 = melanistic (dominant, dark), 2 = albino (recessive, washed out). */
    public static final Locus MELANIN  = disc("melanin", 2, 0.003, 1, 2, 0);
    /** 12 coat patterns (11 = dark extremities); dominance order is a fixed permutation. Both strands show: see Phenotype.pattern2. */
    public static final Locus PATTERN  = disc("pattern", 2, 0.010, 0, 3, 6, 9, 1, 4, 7, 10, 2, 5, 8, 11);

    // ---- Chromosome 3: physiology (all polygenic) ----
    public static final Locus SIZE      = quant("size", 3, 0.04);
    public static final Locus HEALTH    = quant("health", 3, 0.04);
    public static final Locus SPEED     = quant("speed", 3, 0.04);
    public static final Locus LONGEVITY = quant("longevity", 3, 0.04);
    public static final Locus FERTILITY = quant("fertility", 3, 0.04);
    public static final Locus LITTER    = quant("litter", 3, 0.04);

    // ---- Chromosome 4: behaviour ----
    /** 0 = passive (recessive), 1 = hunts livestock, 2 = predator of smaller genetic creatures. */
    public static final Locus HUNTER    = disc("hunter", 4, 0.004, 0, 1, 2);
    public static final Locus FLEE      = disc("flee", 4, 0.004, 0, 1);
    public static final Locus SHADE     = disc("shade", 4, 0.004, 0, 1);
    public static final Locus HERD      = disc("herd", 4, 0.004, 0, 1);
    public static final Locus NOCTURNAL = disc("nocturnal", 4, 0.004, 0, 1);

    // ---- Chromosome 5: resistances and drops ----
    /** Allele 1 = burns in sunlight (dominant). */
    public static final Locus SUNBURN = disc("sunburn", 5, 0.004, 0, 1);
    /** Allele 1 = hurt by water (dominant). */
    public static final Locus WATER   = disc("water", 5, 0.004, 0, 1);
    /** Allele 1 = fire immune (recessive). */
    public static final Locus FIRE    = disc("fire", 5, 0.004, 1, 0);
    /** Allele 1 = cold resistant (dominant). */
    public static final Locus COLD    = disc("cold", 5, 0.004, 0, 1);
    /** 16 drop kinds; the glue layer maps an index to an item. */
    public static final Locus DROP    = disc("drop", 5, 0.006, 0, 7, 14, 5, 12, 3, 10, 1, 8, 15, 6, 13, 4, 11, 2, 9);

    // ---- Chromosome 6: genetic load and rare traits ----
    /** Allele 1 = defect (recessive): homozygous animals are frail and less fertile. */
    public static final Locus DEFECT  = disc("defect", 6, 0.010, 1, 0);
    /** Allele 1 = recessive lethal: homozygous embryos are not viable. */
    public static final Locus LETHAL  = disc("lethal", 6, 0.002, 1, 0);
    /** Allele 1 = glows in the dark (recessive). */
    public static final Locus BIOLUM  = disc("biolum", 6, 0.002, 1, 0);

    // ---- Cosmetic genes added in 0.3.0. They are declared last so that the indices of older genomes stay valid;
    //      the chromosome number alone decides which linkage group they join. ----
    /** Crest on the back: 0 none, 1 spikes, 2 sail, 3 mane. Linked to the body plan (chromosome 0). */
    public static final Locus CREST     = disc("crest", 0, 0.010, 0, 1, 3, 2);
    /** Eye shape: 0 round (dominant), 1 big, 2 slit, 3 tiny (recessive). Linked to the head (chromosome 0). */
    public static final Locus EYE_SHAPE = disc("eye_shape", 0, 0.010, 3, 1, 2, 0);
    /** Iris colour along a rainbow, 0..255. */
    public static final Locus EYE_HUE   = quant("eye_hue", 2, 0.05);
    /** Colour of the markings (stripes, spots, crest) along a rainbow, 0..255. */
    public static final Locus MARK_HUE  = quant("mark_hue", 2, 0.05);
    /** Brightness of the markings: 0 almost black, 255 bright. */
    public static final Locus MARK_VAL  = quant("mark_val", 2, 0.05);

    /** Coat texture: 0 smooth, 1 fur, 2 scales, 3 fluffy. */
    public static final Locus COAT      = disc("coat", 2, 0.008, 0, 2, 1, 3);

    /** How strongly coloured the markings are: 0 white/grey, 255 fully saturated. */
    public static final Locus MARK_SAT  = quant("mark_sat", 2, 0.05);

    public static final List<Locus> ALL = List.copyOf(TMP);
    public static final int COUNT = ALL.size();
    public static final int CHROMOSOME_COUNT;
    /** For each chromosome, the locus indices in physical order. */
    public static final int[][] CHROMOSOMES;

    static {
        int max = 0;
        for (Locus l : ALL) max = Math.max(max, l.chromosome());
        CHROMOSOME_COUNT = max + 1;
        List<List<Integer>> tmp = new ArrayList<>();
        for (int i = 0; i < CHROMOSOME_COUNT; i++) tmp.add(new ArrayList<>());
        for (Locus l : ALL) tmp.get(l.chromosome()).add(l.index());
        CHROMOSOMES = new int[CHROMOSOME_COUNT][];
        for (int i = 0; i < CHROMOSOME_COUNT; i++) {
            CHROMOSOMES[i] = tmp.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public static Locus byId(String id) {
        Locus l = BY_ID.get(id);
        if (l == null) throw new IllegalArgumentException("Unknown locus: " + id);
        return l;
    }

    public static int discreteCount() {
        int n = 0;
        for (Locus l : ALL) if (l.isDiscrete()) n++;
        return n;
    }

    private Loci() {}
}
