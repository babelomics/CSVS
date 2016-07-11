package org.babelomics.csvs.lib.models;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class SaturationElement {


    private int diseaseId;
    private int count;
    private int samples;

    public SaturationElement() {
    }

    public SaturationElement(int diseaseId, int count, int samples) {
        this.diseaseId = diseaseId;
        this.count = count;
        this.samples = samples;
    }

    public int getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(int diseaseId) {
        this.diseaseId = diseaseId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return "SaturationElement{" +
                "diseaseId=" + diseaseId +
                ", count=" + count +
                ", samples=" + samples +
                '}';
    }
}
