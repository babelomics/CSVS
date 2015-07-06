package org.babelomics.pvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */

@Entity
public class DiseaseCount {

    @Id
    private ObjectId id;

    @Reference
    private DiseaseGroup diseaseGroup;

    private int gt00;
    private int gt01;
    private int gt11;
    @Property("gtm")
    private int gtmissing;

    public DiseaseCount(DiseaseGroup diseaseGroup, int gt00, int gt01, int gt11, int gtmissing) {
        this.diseaseGroup = diseaseGroup;
        this.gt00 = gt00;
        this.gt01 = gt01;
        this.gt11 = gt11;
        this.gtmissing = gtmissing;
    }

    public DiseaseCount() {
    }

    public DiseaseGroup getDiseaseGroup() {
        return diseaseGroup;
    }

    public void setDiseaseGroup(DiseaseGroup diseaseGroup) {
        this.diseaseGroup = diseaseGroup;
    }

    public int getGt00() {
        return gt00;
    }

    public void setGt00(int gt00) {
        this.gt00 = gt00;
    }

    public int getGt01() {
        return gt01;
    }

    public void setGt01(int gt01) {
        this.gt01 = gt01;
    }

    public int getGt11() {
        return gt11;
    }

    public void setGt11(int gt11) {
        this.gt11 = gt11;
    }

    public int getGtmissing() {
        return gtmissing;
    }

    public void setGtmissing(int gtmissing) {
        this.gtmissing = gtmissing;
    }

    public void incGt00(int gt00) {
        this.gt00 += gt00;

    }

    public void incGt01(int gt01) {
        this.gt01 += gt01;

    }

    public void incGt11(int gt11) {
        this.gt11 += gt11;

    }

    public void incGtMissing(int gtmissing) {
        this.gtmissing += gtmissing;

    }
}
