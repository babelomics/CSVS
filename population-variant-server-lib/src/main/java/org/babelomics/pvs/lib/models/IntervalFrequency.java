package org.babelomics.pvs.lib.models;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class IntervalFrequency {
    private long id;
    private long start;
    private long end;
    private String chromosome;
    private double featuresCount;


    public IntervalFrequency() {
    }

    public IntervalFrequency(long id, long start, long end, String chromosome, double featuresCount) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.chromosome = chromosome;
        this.featuresCount = featuresCount;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public void setFeaturesCount(double featuresCount) {
        this.featuresCount = featuresCount;
    }

    public double getFeaturesCount() {
        return featuresCount;
    }

    public long getEnd() {
        return end;
    }

    public String getChromosome() {
        return chromosome;
    }

    @Override
    public String toString() {
        return "IntervalFrequency{" +
                "id=" + id +
                ", start=" + start +
                ", end=" + end +
                ", chromosome='" + chromosome + '\'' +
                ", featuresCount=" + featuresCount +
                '}';
    }
}
