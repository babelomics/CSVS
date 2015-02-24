package org.babelomics.pvs.lib.tasks;

import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.pedigree.Pedigree;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.stats.VariantStats;

import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSVariantAggregatedStats extends VariantStats {

    public PVSVariantAggregatedStats() {
    }

    public PVSVariantAggregatedStats(Variant variant) {
        super(variant);
    }

    @Override
    public VariantStats calculate(Map<String, Map<String, String>> samplesData, Map<String, String> attributes, Pedigree pedigree) {
        super.calculate(samplesData, attributes, pedigree);

        if (attributes.containsKey("AN") && attributes.containsKey("AC")) {
            int total = Integer.parseInt(attributes.get("AN"));
            String[] alleleCountString = attributes.get("AC").split(",");

            int[] alleleCount = new int[alleleCountString.length];

            String mafAllele = this.getRefAllele();
            int referenceCount = total;

            for (int i = 0; i < alleleCountString.length; i++) {
                alleleCount[i] = Integer.parseInt(alleleCountString[i]);
                referenceCount -= alleleCount[i];
            }

            float maf = (float) referenceCount / total;

            for (int i = 0; i < alleleCount.length; i++) {
                float auxMaf = (float) alleleCount[i] / total;
                if (auxMaf < maf) {
                    maf = auxMaf;
                    mafAllele = this.getAltAllele();
                }
            }

            setMaf(maf);
            setMafAllele(mafAllele);
        }

        if (attributes.containsKey("HPG_GTC")) {
            String[] gtCount = attributes.get("HPG_GTC").split(",");

            for (int i = 0; i < gtCount.length; i++) {
                String gt = gtCount[i];
                String[] splits = gt.split(":");

                Genotype g = new Genotype(splits[0]);

                this.addGenotype(g, Integer.parseInt(splits[1]));

            }

        }

        return this;
    }

}

