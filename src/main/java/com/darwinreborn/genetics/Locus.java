package com.darwinreborn.genetics;

/**
 * A gene position (locus) on a chromosome.
 *
 * <p>DISCRETE loci have a small set of alleles with a dominance ranking
 * (the allele with the higher rank is expressed; ties are impossible for
 * distinct alleles by construction). QUANTITATIVE loci hold a value 0..255 and
 * are expressed additively (mean of both alleles), like polygenic traits.</p>
 *
 * <p>This class has no Minecraft dependency on purpose, so the genetics can be
 * unit-tested without launching the game.</p>
 */
public final class Locus {

    public enum Type { DISCRETE, QUANTITATIVE }

    public static final int Q_MAX = 255;

    private final int index;
    private final String id;
    private final int chromosome;
    private final Type type;
    private final int alleleCount;
    private final int[] rank;
    private final double mutationRate;

    Locus(int index, String id, int chromosome, Type type, int alleleCount, int[] rank, double mutationRate) {
        this.index = index;
        this.id = id;
        this.chromosome = chromosome;
        this.type = type;
        this.alleleCount = alleleCount;
        this.rank = rank;
        this.mutationRate = mutationRate;
    }

    /** Position in the genome arrays. */
    public int index() { return index; }

    public String id() { return id; }

    public int chromosome() { return chromosome; }

    public Type type() { return type; }

    public boolean isDiscrete() { return type == Type.DISCRETE; }

    public int alleleCount() { return alleleCount; }

    /** Probability that one allele changes while a gamete is formed (before the global multiplier). */
    public double mutationRate() { return mutationRate; }

    public int maxAllele() { return alleleCount - 1; }

    public int clamp(int allele) {
        return Math.max(0, Math.min(maxAllele(), allele));
    }

    /** Dominance rank of a discrete allele; a larger rank dominates. */
    public int rank(int allele) {
        if (!isDiscrete()) throw new IllegalStateException(id + " is quantitative");
        return rank[allele];
    }

    /** Which of two discrete alleles is expressed. */
    public int express(int x, int y) {
        if (!isDiscrete()) throw new IllegalStateException(id + " is quantitative");
        int rx = rank[x];
        int ry = rank[y];
        if (rx > ry) return x;
        if (ry > rx) return y;
        return Math.min(x, y);
    }

    /** Translation key for an allele name, e.g. darwinreborn.allele.ears.2 */
    public String alleleKey(int allele) {
        return "darwinreborn.allele." + id + "." + allele;
    }

    /** Translation key for the locus name. */
    public String nameKey() {
        return "darwinreborn.locus." + id;
    }

    @Override
    public String toString() { return id; }
}
