package org.babelomics.csvs.lib.comparators;

import org.babelomics.csvs.lib.models.DiseaseGroup;

import java.util.Comparator;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class DiseaseGroupIdAscComparator implements Comparator<DiseaseGroup> {
    @Override
    public int compare(DiseaseGroup o1, DiseaseGroup o2) {
        return o1.getGroupId() - o2.getGroupId();
    }

}
