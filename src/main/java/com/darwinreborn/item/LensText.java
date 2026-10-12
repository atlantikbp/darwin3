package com.darwinreborn.item;

import com.darwinreborn.genetics.Genome;
import com.darwinreborn.genetics.GenomeReport;
import com.darwinreborn.genetics.Loci;
import com.darwinreborn.genetics.Locus;
import com.darwinreborn.genetics.Phenotype;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Builds the chat output of the genetic lens. All wording comes from the lang files. */
final class LensText {

    /** Gene report grouped by chromosome. */
    static List<Component> report(Genome genome, Phenotype p) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("darwinreborn.lens.header",
                percent(p.vigor() - 0.85, 0.40), percent(p.heterozygosity(), 1.0),
                String.format("%.1f", p.maxHealth() / 2.0))
                .withStyle(ChatFormatting.GOLD));

        List<GenomeReport.Line> lines = GenomeReport.describe(genome);
        for (int chromosome = 0; chromosome < Loci.CHROMOSOME_COUNT; chromosome++) {
            MutableComponent row = Component.translatable("darwinreborn.chromosome." + chromosome)
                    .append(Component.literal(": "))
                    .withStyle(ChatFormatting.YELLOW);
            boolean first = true;
            for (GenomeReport.Line line : lines) {
                if (line.locus().chromosome() != chromosome) continue;
                if (!first) row.append(Component.literal("; ").withStyle(ChatFormatting.GRAY));
                first = false;
                row.append(describeLine(line));
            }
            out.add(row);
        }
        if (p.defective()) out.add(Component.translatable("darwinreborn.lens.defective").withStyle(ChatFormatting.RED));
        out.add(Component.translatable("darwinreborn.lens.hint").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    private static Component describeLine(GenomeReport.Line line) {
        Locus l = line.locus();
        MutableComponent c = Component.translatable(l.nameKey()).append(Component.literal(" ")).withStyle(ChatFormatting.WHITE);
        if (l.isDiscrete()) {
            c.append(Component.translatable(l.alleleKey(line.expressed())).withStyle(ChatFormatting.AQUA));
            if (line.hidden()) {
                c.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable("darwinreborn.lens.carries").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(l.alleleKey(line.hiddenAllele())).withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
            }
        } else {
            c.append(Component.literal(Math.round(line.expressed() * 100.0 / 255.0) + "%").withStyle(ChatFormatting.AQUA));
        }
        return c;
    }

    /** Forecast for the offspring of two inspected animals. Only loci that can actually vary are listed. */
    static List<Component> forecast(Genome a, Genome b) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("darwinreborn.lens.forecast").withStyle(ChatFormatting.GREEN));

        int shown = 0;
        for (GenomeReport.Forecast f : GenomeReport.forecast(a, b)) {
            Locus l = f.locus();
            if (l == Loci.LETHAL || l == Loci.DEFECT) continue;   // reported as risks below
            if (shown >= 8) break;
            MutableComponent row = Component.translatable(l.nameKey()).append(Component.literal(": ")).withStyle(ChatFormatting.WHITE);
            boolean first = true;
            for (int allele = 0; allele < f.probabilities().length; allele++) {
                double pr = f.probabilities()[allele];
                if (pr <= 1e-9) continue;
                if (!first) row.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                first = false;
                row.append(Component.literal(Math.round(pr * 100) + "% ").withStyle(ChatFormatting.AQUA))
                        .append(Component.translatable(l.alleleKey(allele)).withStyle(ChatFormatting.WHITE));
            }
            out.add(row);
            shown++;
        }
        if (shown == 0) out.add(Component.translatable("darwinreborn.lens.forecast.same").withStyle(ChatFormatting.GRAY));

        double lethal = GenomeReport.lethalRisk(a, b);
        double defect = GenomeReport.defectRisk(a, b);
        if (lethal > 0) {
            out.add(Component.translatable("darwinreborn.lens.risk.lethal", Math.round(lethal * 100)).withStyle(ChatFormatting.RED));
        }
        if (defect > 0) {
            out.add(Component.translatable("darwinreborn.lens.risk.defect", Math.round(defect * 100)).withStyle(ChatFormatting.RED));
        }
        return out;
    }

    private static String percent(double value, double max) {
        long v = Math.round(Math.max(0, Math.min(1, value / max)) * 100);
        return v + "%";
    }

    private LensText() {}
}
