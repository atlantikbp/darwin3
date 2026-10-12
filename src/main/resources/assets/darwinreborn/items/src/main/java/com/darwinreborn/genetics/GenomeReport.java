package com.darwinreborn.genetics;

import java.util.ArrayList;
import java.util.List;

/** Structured, Minecraft-free summaries used by the inspector item. */
public final class GenomeReport {

    /**
     * One locus of one animal.
     *
     * @param expressed expressed allele (discrete) or the rounded mean value 0..255 (quantitative)
     * @param hidden    a discrete locus where the animal carries an allele that it does not show
     */
    public record Line(Locus locus, int alleleA, int alleleB, int expressed, boolean heterozygous, boolean hidden) {

        /** The recessive allele that is carried but not shown, or -1. */
        public int hiddenAllele() {
            if (!hidden) return -1;
            return alleleA == expressed ? alleleB : alleleA;
        }
    }

    /** Outcome probabilities of one discrete locus for a planned mating (index = allele number). */
    public record Forecast(Locus locus, double[] probabilities) {}

    public static List<Line> describe(Genome g) {
        List<Line> out = new ArrayList<>();
        for (Locus l : Loci.ALL) {
            int a = g.allele(l, 0);
            int b = g.allele(l, 1);
            if (l.isDiscrete()) {
                int e = l.express(a, b);
                out.add(new Line(l, a, b, e, a != b, a != b));
            } else {
                out.add(new Line(l, a, b, (int) Math.round(g.quantitative(l)), a != b, false));
            }
        }
        return out;
    }

    /** Single-locus forecasts for every discrete locus where the offspring can differ. */
    public static List<Forecast> forecast(Genome p1, Genome p2) {
        List<Forecast> out = new ArrayList<>();
        for (Locus l : Loci.ALL) {
            if (!l.isDiscrete()) continue;
            double[] p = Breeding.predictDiscrete(l, p1, p2);
            int outcomes = 0;
            for (double v : p) if (v > 1e-9) outcomes++;
            if (outcomes > 1) out.add(new Forecast(l, p));
        }
        return out;
    }

    /** Probability that an embryo from this pairing is lost to the homozygous recessive lethal. */
    public static double lethalRisk(Genome p1, Genome p2) {
        return Breeding.predictDiscrete(Loci.LETHAL, p1, p2)[1];
    }

    /** Probability that an offspring is a homozygous-defect (frail) animal. */
    public static double defectRisk(Genome p1, Genome p2) {
        return Breeding.predictDiscrete(Loci.DEFECT, p1, p2)[1];
    }

    private GenomeReport() {}
}
