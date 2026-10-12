package com.darwinreborn.genetics;

import java.util.Arrays;

/**
 * A diploid genome: two alleles (one from each parent) for every locus.
 * Immutable; use {@link #with} to derive modified copies.
 *
 * <p>Storage is a flat int array, {@code [a0..aN-1, b0..bN-1]}, which maps directly onto
 * a Minecraft NBT int-array.</p>
 */
public final class Genome {

    private final int[] a;
    private final int[] b;

    private Genome(int[] a, int[] b) {
        this.a = a;
        this.b = b;
    }

    /** All alleles 0. */
    public static Genome blank() {
        return new Genome(new int[Loci.COUNT], new int[Loci.COUNT]);
    }

    /** Builds a genome from two haplotypes, clamping every value into its legal range. */
    public static Genome of(int[] hapA, int[] hapB) {
        int[] x = new int[Loci.COUNT];
        int[] y = new int[Loci.COUNT];
        for (Locus l : Loci.ALL) {
            int i = l.index();
            x[i] = l.clamp(i < hapA.length ? hapA[i] : 0);
            y[i] = l.clamp(i < hapB.length ? hapB[i] : 0);
        }
        return new Genome(x, y);
    }

    public int allele(Locus l, int side) {
        return side == 0 ? a[l.index()] : b[l.index()];
    }

    public Genome with(Locus l, int alleleA, int alleleB) {
        int[] x = a.clone();
        int[] y = b.clone();
        x[l.index()] = l.clamp(alleleA);
        y[l.index()] = l.clamp(alleleB);
        return new Genome(x, y);
    }

    /** Expressed allele of a discrete locus (dominance applied). */
    public int expressed(Locus l) {
        return l.express(a[l.index()], b[l.index()]);
    }

    /** Mean allele value (0..255) of a quantitative locus. */
    public double quantitative(Locus l) {
        return (a[l.index()] + b[l.index()]) / 2.0;
    }

    public boolean isHomozygous(Locus l) {
        return a[l.index()] == b[l.index()];
    }

    /** True if the animal carries {@code allele} on exactly one strand but it is not expressed. */
    public boolean isHiddenCarrier(Locus l, int allele) {
        if (!l.isDiscrete()) return false;
        int x = a[l.index()];
        int y = b[l.index()];
        return (x == allele ^ y == allele) && expressed(l) != allele;
    }

    /** Fraction of discrete loci where the two alleles differ (0 = fully inbred, 1 = maximally mixed). */
    public double heterozygosity() {
        int het = 0;
        int total = 0;
        for (Locus l : Loci.ALL) {
            if (!l.isDiscrete()) continue;
            total++;
            if (a[l.index()] != b[l.index()]) het++;
        }
        return total == 0 ? 0 : (double) het / total;
    }

    /** Flat int-array form for saving. */
    public int[] toArray() {
        int[] out = new int[Loci.COUNT * 2];
        System.arraycopy(a, 0, out, 0, Loci.COUNT);
        System.arraycopy(b, 0, out, Loci.COUNT, Loci.COUNT);
        return out;
    }

    /** Inverse of {@link #toArray()}. Tolerates wrong lengths and out-of-range values from old or corrupted saves. */
    public static Genome fromArray(int[] arr) {
        int[] x = new int[Loci.COUNT];
        int[] y = new int[Loci.COUNT];
        if (arr != null) {
            // The array holds two equal halves. Saves from older versions had fewer loci: read them with their own
            // half-length so strand B is not shifted; loci that did not exist yet stay at allele 0.
            int half = arr.length / 2;
            for (int i = 0; i < Loci.COUNT && i < half; i++) {
                x[i] = arr[i];
                y[i] = arr[half + i];
            }
        }
        return of(x, y);
    }

    /** Compact text form (two hex digits per allele) for network sync and saving. */
    public String toHex() {
        int[] arr = toArray();
        StringBuilder sb = new StringBuilder(arr.length * 2);
        for (int v : arr) {
            sb.append(Character.forDigit((v >> 4) & 0xF, 16));
            sb.append(Character.forDigit(v & 0xF, 16));
        }
        return sb.toString();
    }

    /** Inverse of {@link #toHex()}; any malformed or empty input yields a blank genome rather than an exception. */
    public static Genome fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) return blank();
        int[] arr = new int[hex.length() / 2];
        for (int i = 0; i < arr.length; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) return blank();
            arr[i] = (hi << 4) | lo;
        }
        return fromArray(arr);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Genome g && Arrays.equals(a, g.a) && Arrays.equals(b, g.b);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(a) + Arrays.hashCode(b);
    }
}
