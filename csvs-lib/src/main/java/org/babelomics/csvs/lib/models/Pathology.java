package org.babelomics.csvs.lib.models;

import java.util.*;

/**
 * Class to describe opinions about a variant
 * @author Gema Roldán González <>
 */

public class Pathology {

    private Variant variant;
    private int total = 0;
    private Map<String, Integer> mapTotalTypeOpinion;

    public Pathology() {
    }

    public Pathology(Variant variant, Map<String, Integer> mapTotalTypeOpinion) {
        this.variant = variant;
        this.mapTotalTypeOpinion = mapTotalTypeOpinion;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public int getTotal() {
        int t = 0;
        if (mapTotalTypeOpinion != null)
            for (String k : mapTotalTypeOpinion.keySet()) {
                t = t + mapTotalTypeOpinion.get(k);
            }

        return t;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Map<String, Integer> getMapTotalTypeOpinion() {
        return mapTotalTypeOpinion;
    }

    public void setMapTotalTypeOpinion(Map<String, Integer> mapTotalTypeOpinion) {
        this.mapTotalTypeOpinion = mapTotalTypeOpinion;
    }


    @Override
    public String toString() {

        return "Pathology{"+
                "variant:{" +
                    "chromosome='" + this.getVariant().getChromosome() + '\'' +
                    ", position=" + this.getVariant().getPosition() +
                    ", reference='" + this.getVariant().getReference() + '\'' +
                    ", alternate='" + this.getVariant().getAlternate() + '\'' +
                    "'}" +
                ",total:" + this.getTotal()  +
                ",mapTotalTypeOpinion:" + this.mapTotalTypeOpinion +
                "}";
    }
}