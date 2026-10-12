package com.darwinreborn.genetics;

import java.util.Random;

/**
 * Dependency-free test runner for the genetics core.
 * Run with:  java -cp out com.darwinreborn.genetics.SelfTest
 */
public final class SelfTest {

    private static int failures = 0;
    private static int checks = 0;

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (detail.isEmpty() ? "" : "   [" + detail + "]"));
    }

    private static void near(String name, double actual, double expected, double tol) {
        check(name, Math.abs(actual - expected) <= tol, String.format("actual=%.4f expected=%.4f tol=%.4f", actual, expected, tol));
    }

    public static void main(String[] args) {
        mendelianRatio();
        independentAssortment();
        linkage();
        mutationRate();
        serialization();
        heterosis();
        lethal();
        predictor();
        archetypesLookLikeThemselves();
        wildAnimalsCarryHiddenAlleles();
        reports();
        langCoverage();
        determinism();
        cosmetics();
        hybrids();
        phenotypeRanges();
        System.out.println();
        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) System.exit(1);
    }

    /** Het x het for a simple dominant/recessive locus must give 3:1. */
    static void mendelianRatio() {
        // HEAD: allele 3 outranks allele 0 (0 = absent head is recessive)
        Genome het = Genome.blank().with(Loci.HEAD, 3, 0);
        Random r = new Random(1);
        int n = 60000, dominant = 0, recessive = 0;
        for (int i = 0; i < n; i++) {
            Genome c = Breeding.cross(het, het, r, BreedingConfig.MENDEL);
            if (c.expressed(Loci.HEAD) == 3) dominant++;
            if (c.expressed(Loci.HEAD) == 0 && c.isHomozygous(Loci.HEAD)) recessive++;
        }
        near("mendel: dominant phenotype fraction", dominant / (double) n, 0.75, 0.01);
        near("mendel: recessive phenotype fraction", recessive / (double) n, 0.25, 0.01);
    }

    /** Genes on different chromosomes assort independently: dihybrid 9:3:3:1. */
    static void independentAssortment() {
        // HEAD on chromosome 0, HUNTER on chromosome 4
        Genome het = Genome.blank().with(Loci.HEAD, 3, 0).with(Loci.HUNTER, 2, 0);
        Random r = new Random(2);
        int n = 80000, both = 0;
        for (int i = 0; i < n; i++) {
            Genome c = Breeding.cross(het, het, r, BreedingConfig.MENDEL);
            boolean headRec = c.expressed(Loci.HEAD) == 0;
            boolean hunterRec = c.expressed(Loci.HUNTER) == 0;
            if (headRec && hunterRec) both++;
        }
        near("dihybrid: double recessive = 1/16", both / (double) n, 1.0 / 16.0, 0.004);
    }

    /** Genes next to each other on one chromosome are inherited together, apart from crossing over. */
    static void linkage() {
        // HEAD and SNOUT are adjacent on chromosome 0. Coupling phase: strand A carries (3,2), strand B (0,0).
        Genome g = Genome.blank().with(Loci.HEAD, 3, 0).with(Loci.SNOUT, 2, 0);
        BreedingConfig cfg = new BreedingConfig(0.0, 0.10);
        Random r = new Random(3);
        int n = 100000, recombinant = 0;
        for (int i = 0; i < n; i++) {
            int[] gam = Breeding.gamete(g, r, cfg);
            boolean head = gam[Loci.HEAD.index()] == 3;
            boolean snout = gam[Loci.SNOUT.index()] == 2;
            if (head != snout) recombinant++;
        }
        near("linkage: recombinant gametes ~ crossover rate", recombinant / (double) n, 0.10, 0.006);
    }

    /** Mutation frequency of one discrete locus matches its configured rate. */
    static void mutationRate() {
        Genome g = Genome.blank();    // BODY = 0 on both strands
        Random r = new Random(4);
        int n = 300000, changed = 0;
        for (int i = 0; i < n; i++) {
            int[] gam = Breeding.gamete(g, r, BreedingConfig.DEFAULT);
            if (gam[Loci.BODY.index()] != 0) changed++;
        }
        near("mutation: BODY mutation frequency", changed / (double) n, Loci.BODY.mutationRate(), 0.0015);

        Random r2 = new Random(5);
        int off = 0;
        for (int i = 0; i < 20000; i++) {
            int[] gam = Breeding.gamete(g, r2, new BreedingConfig(0.0, 0.15));
            for (int v : gam) if (v != 0) off++;
        }
        check("mutation: multiplier 0 disables mutation", off == 0, "mutations=" + off);
    }

    static void serialization() {
        Random r = new Random(6);
        Genome g = Archetype.HOUND.wild(r, 0.05);
        Genome back = Genome.fromArray(g.toArray());
        check("serialization: round trip", g.equals(back), "");
        Genome broken = Genome.fromArray(new int[] {999, -5, 7});
        check("serialization: corrupted input is clamped, not thrown", broken.allele(Loci.BODY, 0) == Loci.BODY.maxAllele(), "");
        check("serialization: null input gives blank genome", Genome.fromArray(null).equals(Genome.blank()), "");
        check("serialization: array length", g.toArray().length == Loci.COUNT * 2, "len=" + g.toArray().length);
        check("serialization: hex round trip", Genome.fromHex(g.toHex()).equals(g), "");
        check("serialization: hex length", g.toHex().length() == Loci.COUNT * 4, "len=" + g.toHex().length());
        check("serialization: bad hex gives blank genome", Genome.fromHex("zz").equals(Genome.blank()) && Genome.fromHex(null).equals(Genome.blank()) && Genome.fromHex("abc").equals(Genome.blank()), "");
    }

    /** Inbred lines are less vigorous than the hybrids of two different lines. */
    static void heterosis() {
        Random r = new Random(7);
        double inbred = 0, hybrid = 0;
        int n = 400;
        for (int i = 0; i < n; i++) {
            Genome a = pureLine(Archetype.PIGLET, r);
            Genome b = pureLine(Archetype.HOUND, r);
            inbred += Phenotype.of(Breeding.cross(a, a, r, BreedingConfig.MENDEL)).vigor();
            hybrid += Phenotype.of(Breeding.cross(a, b, r, BreedingConfig.MENDEL)).vigor();
        }
        inbred /= n;
        hybrid /= n;
        check("heterosis: hybrid vigour > inbred vigour", hybrid > inbred + 0.1, String.format("inbred=%.3f hybrid=%.3f", inbred, hybrid));
    }

    private static Genome pureLine(Archetype arch, Random r) {
        Genome w = arch.wild(r, 0.0);
        int[] hap = new int[Loci.COUNT];
        for (Locus l : Loci.ALL) hap[l.index()] = l.isDiscrete() ? w.expressed(l) : (int) w.quantitative(l);
        return Genome.of(hap, hap);
    }

    static void lethal() {
        Genome carrier = Genome.blank().with(Loci.LETHAL, 1, 0);
        Genome doomed = Genome.blank().with(Loci.LETHAL, 1, 1);
        check("lethal: carrier is viable", Phenotype.of(carrier).viable(), "");
        check("lethal: homozygote is not viable", !Phenotype.of(doomed).viable(), "");
        Random r = new Random(8);
        int n = 40000, dead = 0;
        for (int i = 0; i < n; i++) {
            if (!Phenotype.of(Breeding.cross(carrier, carrier, r, BreedingConfig.MENDEL)).viable()) dead++;
        }
        near("lethal: carrier x carrier loses 1/4 of embryos", dead / (double) n, 0.25, 0.01);

        Genome defect = Genome.blank().with(Loci.DEFECT, 1, 1);
        Phenotype healthy = Phenotype.of(Genome.blank());
        Phenotype frail = Phenotype.of(defect);
        check("defect: homozygote has less health", frail.maxHealth() < healthy.maxHealth(), "");
        check("defect: homozygote is flagged", frail.defective() && !healthy.defective(), "");
    }

    static void predictor() {
        Genome het = Genome.blank().with(Loci.HEAD, 3, 0);
        double[] p = Breeding.predictDiscrete(Loci.HEAD, het, het);
        near("predictor: P(dominant) for het x het", p[3], 0.75, 1e-9);
        near("predictor: P(recessive) for het x het", p[0], 0.25, 1e-9);
        double sum = 0;
        for (double v : p) sum += v;
        near("predictor: probabilities sum to 1", sum, 1.0, 1e-9);
        Genome pureDom = Genome.blank().with(Loci.HEAD, 3, 3);
        double[] q = Breeding.predictDiscrete(Loci.HEAD, pureDom, het);
        near("predictor: dominant homozygote x het never shows recessive", q[0], 0.0, 1e-9);
    }

    /** With variants disabled every wild animal expresses exactly its archetype's discrete loci. */
    static void archetypesLookLikeThemselves() {
        Random r = new Random(9);
        int bad = 0, total = 0;
        for (Archetype arch : Archetype.all()) {
            for (int i = 0; i < 300; i++) {
                Genome g = arch.wild(r, 0.0);
                for (Locus l : Loci.ALL) {
                    int spec = arch.specified(l);
                    if (spec < 0 || !l.isDiscrete()) continue;
                    total++;
                    if (g.expressed(l) != spec) bad++;
                }
            }
        }
        check("archetypes: wild animals express their archetype", bad == 0, bad + " of " + total + " loci differ");
    }

    /** Hidden recessives exist: some wild animals carry an allele that is not visible. */
    static void wildAnimalsCarryHiddenAlleles() {
        Random r = new Random(10);
        int carriers = 0, n = 2000;
        for (int i = 0; i < n; i++) {
            Genome g = Archetype.PIGLET.wild(r, 0.0);
            if (g.heterozygosity() > 0.05) carriers++;
        }
        check("hidden carriers: a meaningful share of wild animals is heterozygous", carriers > n / 2, carriers + "/" + n);

        // and breeding two such parents can reveal a trait neither parent shows
        Random r2 = new Random(11);
        boolean revealed = false;
        for (int i = 0; i < 20000 && !revealed; i++) {
            Genome p1 = Archetype.PIGLET.wild(r2, 0.0);
            Genome p2 = Archetype.PIGLET.wild(r2, 0.0);
            Genome c = Breeding.cross(p1, p2, r2, BreedingConfig.MENDEL);
            for (Locus l : new Locus[] {Loci.EARS, Loci.TAIL, Loci.SNOUT}) {
                if (c.expressed(l) != Archetype.PIGLET.specified(l)) { revealed = true; break; }
            }
        }
        check("hidden carriers: two look-alike parents can produce a visibly new trait", revealed, "");
    }

    static void reports() {
        Genome carrier = Genome.blank().with(Loci.LETHAL, 1, 0).with(Loci.HEAD, 3, 0);
        near("report: lethal risk carrier x carrier", GenomeReport.lethalRisk(carrier, carrier), 0.25, 1e-9);
        near("report: lethal risk carrier x clean", GenomeReport.lethalRisk(carrier, Genome.blank()), 0.0, 1e-9);
        boolean headListed = false, lethalListed = false;
        for (GenomeReport.Forecast f : GenomeReport.forecast(carrier, carrier)) {
            if (f.locus() == Loci.HEAD) headListed = true;
            if (f.locus() == Loci.BODY) lethalListed = true;   // BODY is identical in both parents: must not be listed
        }
        check("report: forecast lists variable loci only", headListed && !lethalListed, "");
        int hiddenCount = 0;
        for (GenomeReport.Line line : GenomeReport.describe(carrier)) {
            if (line.hidden()) {
                hiddenCount++;
                check("report: hidden allele of " + line.locus().id() + " is the unexpressed one",
                        line.hiddenAllele() != line.expressed(), "");
            }
        }
        check("report: carriers detected", hiddenCount == 2, "count=" + hiddenCount);
        check("report: one line per locus", GenomeReport.describe(carrier).size() == Loci.COUNT, "");
    }

    /** Every gene and every discrete allele must have a display name in the lang file (run from the project root). */
    static void langCoverage() {
        java.nio.file.Path p = java.nio.file.Path.of("src/main/resources/assets/darwinreborn/lang/en_us.json");
        if (!java.nio.file.Files.exists(p)) {
            System.out.println("SKIP  lang coverage (run from project root)");
            return;
        }
        String text;
        try {
            text = java.nio.file.Files.readString(p);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        StringBuilder missing = new StringBuilder();
        for (Locus l : Loci.ALL) {
            if (!text.contains("\"" + l.nameKey() + "\"")) missing.append(l.nameKey()).append(' ');
            if (l.isDiscrete()) {
                for (int a = 0; a < l.alleleCount(); a++) {
                    if (!text.contains("\"" + l.alleleKey(a) + "\"")) missing.append(l.alleleKey(a)).append(' ');
                }
            }
        }
        for (int c = 0; c < Loci.CHROMOSOME_COUNT; c++) {
            if (!text.contains("\"darwinreborn.chromosome." + c + "\"")) missing.append("chromosome.").append(c).append(' ');
        }
        check("lang: all genes, alleles and chromosomes have names", missing.length() == 0, missing.toString());
    }

    static void determinism() {
        Genome a1 = Breeding.cross(Archetype.BOVINE.wild(new Random(12), 0.1), Archetype.EMBER.wild(new Random(13), 0.1), new Random(14), BreedingConfig.DEFAULT);
        Genome a2 = Breeding.cross(Archetype.BOVINE.wild(new Random(12), 0.1), Archetype.EMBER.wild(new Random(13), 0.1), new Random(14), BreedingConfig.DEFAULT);
        check("determinism: same seeds give the same child", a1.equals(a2), "");
    }

    /** Whatever the genome, derived numbers stay in sane ranges for the game. */
    static void phenotypeRanges() {
        Random r = new Random(15);
        boolean ok = true;
        String why = "";
        for (int i = 0; i < 20000 && ok; i++) {
            int[] a = new int[Loci.COUNT], b = new int[Loci.COUNT];
            for (Locus l : Loci.ALL) {
                a[l.index()] = r.nextInt(l.alleleCount());
                b[l.index()] = r.nextInt(l.alleleCount());
            }
            Phenotype p = Phenotype.of(Genome.of(a, b));
            if (!(p.size() >= 0.59 && p.size() <= 1.61)) { ok = false; why = "size " + p.size(); }
            else if (!(p.maxHealth() >= 1 && p.maxHealth() <= 40)) { ok = false; why = "health " + p.maxHealth(); }
            else if (!(p.speed() > 0.02 && p.speed() < 0.6)) { ok = false; why = "speed " + p.speed(); }
            else if (!(p.fertility() > 0 && p.fertility() <= 1.0)) { ok = false; why = "fertility " + p.fertility(); }
            else if (!(p.litterPotential() >= 1 && p.litterPotential() <= 3)) { ok = false; why = "litter " + p.litterPotential(); }
            else if (p.lifespanTicks() <= 0) { ok = false; why = "lifespan " + p.lifespanTicks(); }
            else if (p.color() < 0 || p.color() > 0xFFFFFF) { ok = false; why = "color " + p.color(); }
            else if (p.dropIndex() < 0 || p.dropIndex() > 15) { ok = false; why = "drop " + p.dropIndex(); }
            else if (p.pattern() < 0 || p.pattern() > 11) { ok = false; why = "pattern " + p.pattern(); }
            else if (p.pattern2() < 0 || p.pattern2() > 11 || (p.pattern2() != 0 && p.pattern2() == p.pattern())) { ok = false; why = "pattern2 " + p.pattern2(); }
        }
        check("phenotype: derived values stay in range for random genomes", ok, why);
    }

    /** Eyes, second colour, crest: ranges, and old saves with fewer loci still load without shifting strand B. */
    static void cosmetics() {
        Random r = new Random(21);
        boolean ok = true;
        String why = "";
        for (int i = 0; i < 5000 && ok; i++) {
            int[] a = new int[Loci.COUNT], b = new int[Loci.COUNT];
            for (Locus l : Loci.ALL) { a[l.index()] = r.nextInt(l.alleleCount()); b[l.index()] = r.nextInt(l.alleleCount()); }
            Phenotype p = Phenotype.of(Genome.of(a, b));
            if (p.eyeShape() < 0 || p.eyeShape() > 3) { ok = false; why = "eyeShape " + p.eyeShape(); }
            else if (p.crest() < 0 || p.crest() > 3) { ok = false; why = "crest " + p.crest(); }
            else if (p.coat() < 0 || p.coat() > 3) { ok = false; why = "coat " + p.coat(); }
            else if (p.markColor() < 0 || p.markColor() > 0xFFFFFF) { ok = false; why = "mark " + p.markColor(); }
            else if (p.eyeColor() < 0 || p.eyeColor() > 0xFFFFFF) { ok = false; why = "eye " + p.eyeColor(); }
        }
        check("cosmetics: eye, mark colour and crest stay in range", ok, why);

        // a genome saved before the cosmetic loci existed: two halves of the old length
        int old = Loci.COUNT - 6;
        Genome g = Archetype.PIGLET.wild(new Random(22), 0.0);
        int[] full = g.toArray();
        int[] legacy = new int[old * 2];
        for (int i = 0; i < old; i++) { legacy[i] = full[i]; legacy[old + i] = full[Loci.COUNT + i]; }
        Genome back = Genome.fromArray(legacy);
        boolean same = true;
        for (int i = 0; i < old; i++) {
            Locus l = Loci.ALL.get(i);
            if (back.allele(l, 0) != g.allele(l, 0) || back.allele(l, 1) != g.allele(l, 1)) same = false;
        }
        check("cosmetics: genome of an older version keeps both strands aligned", same, "");

        // every archetype defines the new genes, so each of the 8 creatures has deliberate eyes and markings
        boolean all = true;
        for (Archetype arch : Archetype.all()) {
            for (Locus l : new Locus[] {Loci.EYE_SHAPE, Loci.EYE_HUE, Loci.MARK_HUE, Loci.MARK_VAL, Loci.CREST, Loci.COAT}) {
                if (arch.specified(l) < 0) { all = false; why = arch.name() + " lacks " + l.id(); }
            }
        }
        check("cosmetics: every archetype specifies eyes, marking colour and crest", all, why);

        // the 8 wild species and the two farm species look different from each other
        java.util.Set<String> looks = new java.util.HashSet<>();
        for (Archetype arch : Archetype.all()) {
            Phenotype p = Phenotype.of(arch.wild(new Random(5), 0.0));
            looks.add(p.body() + "/" + p.head() + "/" + p.snout() + "/" + p.ears() + "/" + p.tail() + "/" + p.pattern() + "/" + p.eyeShape() + "/" + p.crest() + "/" + p.coat());
        }
        check("cosmetics: every archetype has its own look", looks.size() == Archetype.all().size(), looks.size() + " of " + Archetype.all().size());
    }

    /** Crossing two different archetypes (what the hybrid system does) gives viable, mixed offspring. */
    static void hybrids() {
        Random r = new Random(31);
        int viable = 0, mixedLooks = 0, n = 2000;
        double hetCross = 0, hetPure = 0;
        for (int i = 0; i < n; i++) {
            Genome cow = Archetype.BOVINE.wild(r, 0.04);
            Genome sheep = Archetype.WOOLLY.wild(r, 0.04);
            Genome child = Breeding.cross(cow, sheep, r, BreedingConfig.DEFAULT);
            Phenotype pc = Phenotype.of(child);
            if (pc.viable()) viable++;
            Phenotype pa = Phenotype.of(cow), pb = Phenotype.of(sheep);
            if (pc.body() != pa.body() || pc.head() != pb.head() || pc.pattern() != pa.pattern()) mixedLooks++;
            hetCross += child.heterozygosity();
            hetPure += Breeding.cross(cow, Archetype.BOVINE.wild(r, 0.04), r, BreedingConfig.DEFAULT).heterozygosity();
        }
        check("hybrid: cow x sheep is almost always viable", viable > n * 0.97, viable + "/" + n);
        check("hybrid: offspring look like a mix, not a copy of one parent", mixedLooks > n * 0.5, mixedLooks + "/" + n);
        check("hybrid: crossing species is more heterozygous than crossing within one", hetCross > hetPure * 1.2,
                String.format("%.3f vs %.3f", hetCross / n, hetPure / n));
        boolean farm = true;
        for (Archetype a : new Archetype[] {Archetype.FOWL, Archetype.HARE}) {
            Phenotype p = Phenotype.of(a.wild(new Random(1), 0.0));
            if (!p.viable() || p.maxHealth() < 1) farm = false;
        }
        // genomic shock: distant parents mutate more, and the new traits are counted
        Genome cowG = Archetype.BOVINE.wild(new Random(3), 0.0), sheepG = Archetype.WOOLLY.wild(new Random(4), 0.0);
        double far = Breeding.distance(cowG, sheepG), near = Breeding.distance(cowG, Archetype.BOVINE.wild(new Random(5), 0.0));
        check("shock: cow and sheep are further apart than two cows", far > near + 0.2, String.format("%.2f vs %.2f", far, near));
        check("shock: mutation multiplier grows with distance",
                Breeding.configFor(cowG, sheepG).mutationMultiplier() > Breeding.configFor(cowG, cowG).mutationMultiplier() * 2, "");
        int novelFar = 0, novelNear = 0;
        for (int i = 0; i < n; i++) {
            Genome pc = Archetype.BOVINE.wild(r, 0.0), ps = Archetype.WOOLLY.wild(r, 0.0), pc2 = Archetype.BOVINE.wild(r, 0.0);
            novelFar += Breeding.novelTraits(Breeding.cross(pc, ps, r, Breeding.configFor(pc, ps)), pc, ps);
            novelNear += Breeding.novelTraits(Breeding.cross(pc, pc2, r, Breeding.configFor(pc, pc2)), pc, pc2);
        }
        check("shock: crossing distant species produces more new traits than crossing relatives", novelFar > novelNear * 1.4,
                String.format("%.3f vs %.3f per child", (double) novelFar / n, (double) novelNear / n));
        check("hybrid: farm archetypes (fowl, hare) are valid animals", farm, "");

        // cow x sheep: the cow's patches and the sheep's dark extremities both show on the child
        int both = 0;
        for (int i = 0; i < n; i++) {
            Genome child = Breeding.cross(Archetype.BOVINE.wild(r, 0.0), Archetype.WOOLLY.wild(r, 0.0), r, BreedingConfig.DEFAULT);
            Phenotype pc = Phenotype.of(child);
            if ((pc.pattern() == 4 && pc.pattern2() == 11) || (pc.pattern() == 11 && pc.pattern2() == 4)) both++;
        }
        check("hybrid: cow x sheep usually shows both parents' markings", both > n * 0.6, both + "/" + n);

        // colour of the child lies between the parents
        int between = 0;
        for (int i = 0; i < n; i++) {
            Phenotype pa = Phenotype.of(Archetype.BOVINE.wild(r, 0.0));
            Phenotype pb = Phenotype.of(Archetype.WOOLLY.wild(r, 0.0));
            Phenotype pc = Phenotype.of(Breeding.cross(Archetype.BOVINE.wild(new Random(i), 0.0), Archetype.WOOLLY.wild(new Random(i + 7), 0.0), r, BreedingConfig.DEFAULT));
            int rc = (pc.color() >> 16) & 255, ra = (pa.color() >> 16) & 255, rb = (pb.color() >> 16) & 255;
            if (rc > Math.min(ra, rb) - 30 && rc < Math.max(ra, rb) + 30) between++;
        }
        check("hybrid: coat colour of the child lies between the parents", between > n * 0.9, between + "/" + n);
    }

    private SelfTest() {}
}
