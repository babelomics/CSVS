package org.babelomics.exomeserver.lib.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.opencga.storage.core.variant.io.json.VariantStatsJsonMixin;

import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public abstract class ExomeServerVariantStatsJsonMixin extends VariantStatsJsonMixin {

    @JsonIgnore
    public abstract String getChromosome();

    @JsonIgnore
    public abstract Long getPosition();

    @JsonIgnore
    public abstract String getRefAllele();

    @JsonIgnore
    public abstract String getAltAllele();

    @JsonIgnore
    public abstract int getRefAlleleCount();

    @JsonIgnore
    public abstract int getAltAlleleCount();

    @JsonIgnore
    public abstract int getMissingAlleles();

    @JsonIgnore
    public abstract int getMissingGenotypes();

    @JsonIgnore
    public abstract float getRefAlleleFreq();

    @JsonIgnore
    public abstract float getAltAlleleFreq();

    @JsonIgnore
    public abstract Map<Genotype, Float> getGenotypesFreq();

    @JsonIgnore
    public abstract boolean isPedigreeStatsAvailable();

    @JsonIgnore
    public abstract int getMendelianErrors();

    @JsonIgnore
    public abstract float getControlsPercentDominant();

    @JsonIgnore
    public abstract float getCasesPercentRecessive();

    @JsonIgnore
    public abstract float getControlsPercentRecessive();

    @JsonIgnore
    public abstract float getCasesPercentDominant();

    @JsonIgnore
    public abstract int getTransitionsCount();

    @JsonIgnore
    public abstract int getTransversionsCount();

    @JsonIgnore
    public abstract int getQuality();

    @JsonIgnore
    public abstract int getNumSamples();

}
