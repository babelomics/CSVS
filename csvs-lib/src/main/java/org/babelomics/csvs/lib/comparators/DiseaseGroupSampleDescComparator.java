package org.babelomics.csvs.lib.comparators;

import org.babelomics.csvs.lib.models.DiseaseGroup;

import java.util.Comparator;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class DiseaseGroupSampleDescComparator implements Comparator<DiseaseGroup> {
    @Override
    public int compare(DiseaseGroup o1, DiseaseGroup o2) {
        return o2.getSamples() - o1.getSamples();
    }
}
