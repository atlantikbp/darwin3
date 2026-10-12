package com.darwinreborn.genetics;

import java.util.Random;

/**
 * Meiosis and mating.
 *
 * <ul>
 *   <li>Each chromosome of a parent starts from a random one of its two strands (independent assortment).</li>
 *   <li>Between neighbouring loci the gamete may switch strand (crossing over), so linked genes
 *       are usually, but not always, inherited together.</li>
 *   <li>Each inherited allele may mutate.</li>
 * </ul>
 */
public final class Breeding {

    /** Builds one gamete (a haplotype) from a parent. */
    public static int[] gamete(Genome parent, Random rnd, BreedingConfig cfg) {
        int[] out = new int[Loci.COUNT];
        for (int[] chromosome : Loci.CHROMOSOMES) {
            int strand = rnd.nextInt(2);
            for (int i = 0; i < chromosome.length; i++) {
                if (i > 0 && rnd.nextDouble() < cfg.recombination()) strand ^= 1;
                Locus locus = Loci.ALL.get(chromosome[i]);
                int value = parent.allele(locus, strand);
                out[locus.index()] = mutate(locus, value, rnd, cfg);
            }
        }
        return out;
    }

    /** Child genome from two parents. */
    public static Genome cross(Genome parent1, Genome parent2, Random rnd, BreedingConfig cfg) {
        return Genome.of(gamete(parent1, rnd, cfg), gamete(parent2, rnd, cfg));
    }

    static int mutate(Locus locus, int value, Random rnd, BreedingConfig cfg) {
        double p = locus.mutationRate() * cfg.mutationMultiplier();
        if (p <= 0 || rnd.nextDouble() >= p) return value;
        if (locus.isDiscrete()) {
            if (locus.alleleCount() < 2) return value;
            int other = rnd.nextInt(locus.alleleCount() - 1);
            return other >= value ? other + 1 : other;   // uniformly any allele except the current one
        }
        // quantitative: usually a small nudge, occasionally a large jump
        if (rnd.nextDouble() < 0.10) return rnd.nextInt(Locus.Q_MAX + 1);
        return locus.clamp(value + (int) Math.round(rnd.nextGaussian() * 18.0));
    }

    /**
     * Probability of each expressed allele of one discrete locus in the offspring of two parents
     * (single-locus Punnett square; ignores mutation and linkage). Index = allele number.
     */
    public static double[] predictDiscrete(Locus locus, Genome p1, Genome p2) {
        double[] out = new double[locus.alleleCount()];
        for (int s1 = 0; s1 < 2; s1++) {
            for (int s2 = 0; s2 < 2; s2++) {
                int e = locus.express(p1.allele(locus, s1), p2.allele(locus, s2));
                out[e] += 0.25;
            }
        }
        return out;
    }

    /** Number of offspring for one mating, 1..3, from the parents' litter potential. */
    public static int litterSize(Phenotype mother, Phenotype father, Random rnd) {
        double expected = (mother.litterPotential() + father.litterPotential()) / 2.0;
        int n = (int) Math.floor(expected);
        if (rnd.nextDouble() < expected - n) n++;
        return Math.max(1, Math.min(3, n));
    }

    private Breeding() {}
}
