package org.babelomics.pvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */

@Entity
@Indexes(@Index(name = "index", value = "c,p,r,a", unique = true))
public class Variant {

    @Id
    private ObjectId id;

    @Property("c")
    private String chromosome;
    @Property("p")
    private int position;
    @Property("r")
    private String reference;
    @Property("a")
    private String alternate;

    private List<DiseaseCount> diseases;

    public Variant(String chromosome, int position, String reference, String alternate) {
        this.chromosome = chromosome;
        this.position = position;
        this.reference = reference;
        this.alternate = alternate;
        this.diseases = new ArrayList<>();
    }

    public Variant() {

    }

    @Override
    public String toString() {
        return "Variant{" +
                "chromosome='" + chromosome + '\'' +
                ", position=" + position +
                ", reference='" + reference + '\'' +
                ", alternate='" + alternate + '\'' +
                '}';
    }

    public void addGenotypesToDisease(DiseaseGroup diseaseId, int gt00, int gt01, int gt11, int gtmissing) {
        DiseaseCount dc = new DiseaseCount(diseaseId, gt00, gt01, gt11, gtmissing);
        this.diseases.add(dc);
    }

    public void addDiseaseCount(DiseaseCount dc) {
        this.diseases.add(dc);
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAlternate() {
        return alternate;
    }

    public void setAlternate(String alternate) {
        this.alternate = alternate;
    }

    public List<DiseaseCount> getDiseases() {
        return diseases;
    }

    public void setDiseases(List<DiseaseCount> diseases) {
        this.diseases = diseases;
    }

    public DiseaseCount getDiseaseCount(DiseaseGroup dg) {
        for (DiseaseCount dc : this.diseases) {
            if (dc.getDiseaseGroup().getGroupId() == dg.getGroupId()) {
                return dc;
            }
        }
        return null;
    }
}
