package org.babelomics.pvs.lib.tasks;

import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.commons.run.Task;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.MongoCredentials;
import org.opencb.opencga.storage.variant.VariantDBAdaptor;
import org.opencb.opencga.storage.variant.mongodb.VariantMongoDBAdaptor;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSVariantUpdateStatsTask extends Task<Variant> {

    private final VariantSource source;
    private final VariantDBAdaptor dbAdaptor;

    public PVSVariantUpdateStatsTask(MongoCredentials credentials, VariantSource source) throws UnknownHostException {
        super(Integer.MAX_VALUE);
        this.source = source;

        dbAdaptor = new VariantMongoDBAdaptor(credentials);

    }

    public PVSVariantUpdateStatsTask(VariantSource source) {
        this.source = source;
        this.dbAdaptor = null;

    }

    @Override
    public boolean apply(List<Variant> variants) throws IOException {

        QueryOptions qo = new QueryOptions();

        for (Variant batchVariant : variants) {
            Region r = new Region(batchVariant.getChromosome(), batchVariant.getStart(), batchVariant.getEnd());
            QueryResult<Variant> queryResult = this.dbAdaptor.getAllVariantsByRegion(r, qo);

            for (Variant v : queryResult.getResult()) {
                if (batchVariant.equals(v)) {
                    VariantStats variantDBStats = v.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();
                    VariantStats variantNewStats = batchVariant.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();
                    updateStats(variantNewStats, variantDBStats);

                    v.getSourceEntry(source.getFileId(), source.getStudyId()).setStats(variantNewStats);
                }
            }
        }

        return true;
    }

    protected void updateStats(VariantStats variantNewStats, VariantStats variantDBStats) {

        // Genotypes
        for (Map.Entry<Genotype, Integer> entry : variantNewStats.getGenotypesCount().entrySet()) {
            variantDBStats.addGenotype(entry.getKey(), entry.getValue());
        }

        // Update MAF
        int[] allelesCount = new int[2];
        int totalAllelesCount = 0, totalGenotypesCount = 0;

        for (Map.Entry<Genotype, Integer> entry : variantDBStats.getGenotypesCount().entrySet()) {
            Genotype g = entry.getKey();
            for (int i = 0; i < entry.getValue(); i++) {
                // Check missing alleles and genotypes
                switch (g.getCode()) {
                    case ALLELES_OK:
                        // Both alleles set
                        allelesCount[g.getAllele(0)]++;
                        allelesCount[g.getAllele(1)]++;

                        totalAllelesCount += 2;
                        totalGenotypesCount++;

                        break;
                    case HAPLOID:
                        // Haploid (chromosome X/Y)
                        try {
                            allelesCount[g.getAllele(0)]++;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.err.println("g = " + g);
                        }
                        totalAllelesCount++;
                        break;
                    case MULTIPLE_ALTERNATES:
                        // Alternate with different "index" than the one that is being handled
                        break;
                    default:
                        // Missing genotype (one or both alleles missing)
                        variantDBStats.setMissingGenotypes(variantDBStats.getMissingGenotypes() + 1);
                        if (g.getAllele(0) < 0) {
                            variantDBStats.setMissingAlleles(variantDBStats.getMissingAlleles() + 1);
                        } else {
                            allelesCount[g.getAllele(0)]++;
                            totalAllelesCount++;
                        }

                        if (g.getAllele(1) < 0) {
                            variantDBStats.setMissingAlleles(variantDBStats.getMissingAlleles() + 1);
                        } else {
                            allelesCount[g.getAllele(1)]++;
                            totalAllelesCount++;
                        }
                        break;

                }
            }

        }

        // Set counts for each allele
        variantDBStats.setRefAlleleCount(allelesCount[0]);
        variantDBStats.setAltAlleleCount(allelesCount[1]);

        calculateAlleleAndGenotypeFrequencies(variantDBStats, totalAllelesCount, totalGenotypesCount);

    }

    protected void calculateAlleleAndGenotypeFrequencies(VariantStats variantDBStats, int totalAllelesCount, int totalGenotypesCount) {

        // MAF
        float refAlleleFreq = (totalAllelesCount > 0) ? variantDBStats.getRefAlleleCount() / (float) totalAllelesCount : 0;
        float altAlleleFreq = (totalAllelesCount > 0) ? variantDBStats.getAltAlleleCount() / (float) totalAllelesCount : 0;

        if (refAlleleFreq <= altAlleleFreq) {
            variantDBStats.setMaf(refAlleleFreq);
            variantDBStats.setMafAllele(variantDBStats.getRefAllele());
        } else {
            variantDBStats.setMaf(altAlleleFreq);
            variantDBStats.setMafAllele(variantDBStats.getAltAllele());
        }


        variantDBStats.setMgf((float) -1.0);
        variantDBStats.setMgfGenotype("-");
    }


}
