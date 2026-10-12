package com.darwinreborn.genetics;

/**
 * Tunable genetics parameters.
 *
 * @param mutationMultiplier scales every locus mutation rate (0 = no mutations, 1 = default)
 * @param recombination      probability of crossing over between two neighbouring loci on one chromosome
 */
public record BreedingConfig(double mutationMultiplier, double recombination) {

    public static final BreedingConfig DEFAULT = new BreedingConfig(1.0, 0.15);

    /** Pure Mendelian behaviour for testing: no mutations, no linkage. */
    public static final BreedingConfig MENDEL = new BreedingConfig(0.0, 0.5);
}
