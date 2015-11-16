package org.babelomics.pvs.lib.io;

import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSourceEntry;
import org.opencb.biodata.models.variant.stats.VariantStats;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSVariantCountsCSVDataWriter implements VariantWriter {
    private PrintWriter printer;
    private String filename;

    public PVSVariantCountsCSVDataWriter(String filename) {
        this.filename = filename;
    }

    @Override
    public boolean open() {
        try {
            printer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean close() {
        printer.close();
        return true;
    }

    @Override
    public boolean pre() {

        StringBuilder sb = new StringBuilder();
        sb.append("chr").append("\t");
        sb.append("pos").append("\t");
        sb.append("ref").append("\t");
        sb.append("alt").append("\t");
        sb.append("id").append("\t");
        sb.append("0/0").append("\t");
        sb.append("0/1").append("\t");
        sb.append("1/1").append("\t");
        sb.append("./.").append("\t");
        sb.append("total").append("\t");

        printer.append(sb.toString()).append("\n");
        return true;
    }

    @Override
    public boolean post() {
        return true;
    }

    @Override
    public boolean write(Variant elem) {
        StringBuilder sb = new StringBuilder();
        sb.append(elem.getChromosome()).append("\t");
        sb.append(elem.getStart()).append("\t");
        sb.append(elem.getReference()).append("\t");
        sb.append(elem.getAlternate()).append("\t");
        if (elem.getId() == null || elem.getId().isEmpty()) {
            sb.append(".").append("\t");
        } else {
            sb.append(elem.getId()).append("\t");
        }

        VariantSourceEntry file = elem.getSourceEntries().values().iterator().next();
        if (file == null) {
            // There must be a file associated with this variant
            return false;
        }

        int gt00 = 0;
        int gt01 = 0;
        int gt11 = 0;
        int gtmissing = 0;
        int total = 0;

        if (file.getStats() != null) {
            VariantStats stats = file.getStats();

            for (Map.Entry<Genotype, Integer> entry : stats.getGenotypesCount().entrySet()) {

                Genotype g = entry.getKey();
                int count = entry.getValue();

                if (elem.getChromosome().toLowerCase().equals("y")) {
                    if (g.getAllelesIdx().length == 2) {
                        if (g.getAllele(0) == 0 && g.getAllele(1) == 0) {
                            gt00 += count;
                        } else if (g.getAllele(0) == -1 || g.getAllele(1) == -1) {
                            gtmissing += count;
                        } else {
                            gt11 += count;
                        }
                    } else if (g.getAllelesIdx().length == 1) {
                        if (g.getAllele(0) == 0) {
                            gt00 += count;
                        } else if (g.getAllele(0) == 1) {
                            gt11 += count;
                        } else if (g.getAllele(0) == -1) {
                            gtmissing += count;
                        } else {
                            gt11 += count;
                        }
                    } else {
                        System.err.println("Alleles size > 2");
                    }
                } else {
                    if (g.getAllelesIdx().length == 2) {
                        if (g.getAllele(0) == 0 && g.getAllele(1) == 0) {
                            gt00 += count;
                        } else if (g.getAllele(0) == 1 && g.getAllele(1) == 1) {
                            gt11 += count;
                        } else if (g.getAllele(0) == -1 || g.getAllele(1) == -1) {
                            gtmissing += count;
                        } else if (g.getAllele(0) == 0 || g.getAllele(1) == 0) {
                            gt01 += count;
                        } else if (g.getAllele(0) == g.getAllele(1)) {
                            gt11 += count;
                        } else {
                            gt11 += count;
                        }
                    } else if (g.getAllelesIdx().length == 1) {
                        if (g.getAllele(0) == 0) {
                            gt00 += count;
                        } else if (g.getAllele(0) == 1) {
                            gt11 += count;
                        } else if (g.getAllele(0) == -1) {
                            gtmissing += count;
                        } else {
                            gt11 += count;
                        }
                    } else {
                        System.err.println("Alleles size > 2");
                    }
                }
                total += count;
            }

            sb.append(gt00).append("\t");
            sb.append(gt01).append("\t");
            sb.append(gt11).append("\t");
            sb.append(gtmissing).append("\t");
            sb.append(total).append("\t");

        }

        printer.append(sb.toString()).

                append("\n");

        return true;
    }

    @Override
    public boolean write(List<Variant> batch) {
        for (Variant record : batch) {
            this.write(record);
        }

        return true;
    }

    @Override
    public void includeStats(boolean stats) {
    }

    @Override
    public void includeSamples(boolean samples) {
    }

    @Override
    public void includeEffect(boolean effect) {
    }
}
