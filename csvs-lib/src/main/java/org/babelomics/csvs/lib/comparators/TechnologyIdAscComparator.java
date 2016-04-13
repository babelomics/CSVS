package org.babelomics.csvs.lib.comparators;

import org.babelomics.csvs.lib.models.Technology;

import java.util.Comparator;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class TechnologyIdAscComparator implements Comparator<Technology> {
    @Override
    public int compare(Technology o1, Technology o2) {
        return o1.getTechnologyId() - o2.getTechnologyId();
    }

}
