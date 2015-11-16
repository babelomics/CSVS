package org.babelomics.pvs.lib.stats;

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
public class PVSVariantStatsTask extends Task<Variant> {

    private VariantReader reader;
    private VariantSource source;
    private VariantSourceStats stats;

    public PVSVariantStatsTask(VariantReader reader, VariantSource study) {
        super();
        this.reader = reader;
        this.source = study;
        stats = new VariantSourceStats(study.getFileId(), study.getStudyId());
    }

    public PVSVariantStatsTask(VariantReader reader, VariantSource study, int priority) {
        super(priority);
        this.reader = reader;
        this.source = study;
        stats = new VariantSourceStats(study.getFileId(), study.getStudyId());
    }

    @Override
    public boolean apply(List<Variant> batch) throws IOException {
//        VariantStats.calculateStatsForVariantsList(batch, source.getPedigree());
        for (Variant variant : batch) {
            for (VariantSourceEntry file : variant.getSourceEntries().values()) {
                VariantStats variantStats = new VariantStats(variant);

                file.setStats(variantStats.calculate(file.getSamplesData(), file.getAttributes(), source.getPedigree()));
            }
        }

        stats.updateFileStats(batch);
//        stats.updateSampleStats(batch, source.getPedigree());
        return true;
    }

    @Override
    public boolean post() {
        source.setStats(stats.getFileStats());
        return true;
    }

}
