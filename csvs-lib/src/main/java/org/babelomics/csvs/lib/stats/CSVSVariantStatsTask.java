package org.babelomics.csvs.lib.stats;

import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.VariantSourceEntry;
import org.opencb.biodata.models.variant.stats.VariantSourceStats;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.commons.run.Task;

import java.io.IOException;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSVariantStatsTask extends Task<Variant> {

    private VariantReader reader;
    private VariantSource source;
    private VariantSourceStats stats;
    private static final String PASS = "PASS";
    private static final String PASS_INDETERMIATE = ".";
    private static final String FILTER = "FILTER";
    private static final String GT = "GT";
    private static final String NOT_SEQ = "./.";

    public CSVSVariantStatsTask(VariantReader reader, VariantSource study) {
        super();
        this.reader = reader;
        this.source = study;
        stats = new VariantSourceStats(study.getFileId(), study.getStudyId());
    }

    public CSVSVariantStatsTask(VariantReader reader, VariantSource study, int priority) {
        super(priority);
        this.reader = reader;
        this.source = study;
        stats = new VariantSourceStats(study.getFileId(), study.getStudyId());
    }

    @Override
    public boolean apply(List<Variant> batch) throws IOException {
        for (Variant variant : batch) {
            for (VariantSourceEntry file : variant.getSourceEntries().values()) {
                VariantStats variantStats = new VariantStats(variant);

                java.util.Map<String, java.util.Map<String, String>> linkedHashMap = file.getSamplesData();
                java.util.Iterator it= linkedHashMap.keySet().iterator();
                while(it.hasNext()){
                    Object key = it.next();

                    // Replace value GT when FILTER distinct PASS
                    if (file.getAttributes() != null && file.getAttributes().containsKey(FILTER) && !(PASS.equals(file.getAttributes().get(FILTER)) || PASS_INDETERMIATE.equals(file.getAttributes().get(FILTER)))) {
                        if (!NOT_SEQ.equals(linkedHashMap.get(key).get(GT))) {
                            System.out.println(variant.getChromosome() + ":" + variant.getStart() + " " + variant.getReference() + "-> " + variant.getAlternate() + "  FILTER = " + file.getAttributes().get(FILTER) + "   Replaced: GT = " + linkedHashMap.get(key).get(GT) + "-> " + NOT_SEQ);
                            linkedHashMap.get(key).put(GT, NOT_SEQ);
                        }
                    }
                }

                file.setStats(variantStats.calculate(linkedHashMap, file.getAttributes(), source.getPedigree()));
            }
        }

        stats.updateFileStats(batch);

        return true;
    }

    @Override
    public boolean post() {
        source.setStats(stats.getFileStats());
        return true;
    }

}
