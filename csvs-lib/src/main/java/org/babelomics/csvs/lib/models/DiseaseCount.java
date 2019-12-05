package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity(noClassnameStored = true)
//@Indexes(@Index(name = "index_diseasecount_diseasegroupid_technologyid", value = "dgid,tid", unique = true))
public class DiseaseCount {

    @Id
    private ObjectId id;

    @Reference("dg")
    private DiseaseGroup diseaseGroup;
    @Reference("t")
    private Technology technology;

    @Property("dgid")
    private int diseaseGroupId;

    @Property("tid")
    private int technologyId;

    private int gt00;
    private int gt01;
    private int gt11;
    @Property("gtm")
    private int gtmissing;

    @Property("m")
    private float maf;
    @Property("rm")
    private float refFreq;
    @Property("am")
    private float altFreq;


    public DiseaseCount() {
    }

    public DiseaseCount(int diseaseGroupId, int technologyId) {
        this.diseaseGroupId = diseaseGroupId;
        this.technologyId = technologyId;
    }

    public DiseaseCount(DiseaseGroup diseaseGroup, Technology technology, int gt00, int gt01, int gt11, int gtmissing) {
        this.diseaseGroup = diseaseGroup;
        this.technology = technology;
        this.gt00 = gt00;
        this.gt01 = gt01;
        this.gt11 = gt11;
        this.gtmissing = gtmissing;
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

    public float getMaf() {
        return maf;
    }

    public void setMaf(float maf) {
        this.maf = maf;
    }

    public float getRefFreq() {
        return refFreq;
    }

    public void setRefFreq(float refFreq) {
        this.refFreq = refFreq;
    }

    public float getAltFreq() {
        return altFreq;
    }

    public void setAltFreq(float altFreq) {
        this.altFreq = altFreq;
    }

    public int getTotalGts() {
        return gt00 + gt01 + gt11 + gtmissing;
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

    public void decGt00(int gt00) {
        this.gt00 -= gt00;

    }

    public void decGt01(int gt01) {
        this.gt01 -= gt01;

    }

    public void decGt11(int gt11) {
        this.gt11 -= gt11;

    }

    public void decGtMissing(int gtmissing) {
        this.gtmissing -= gtmissing;

    }

    public Technology getTechnology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    public int getTechnologyId() {
        return technologyId;
    }

    public int getDiseaseGroupId() {
        return diseaseGroupId;
    }

    public void setDiseaseGroupId(int diseaseGroupId) {
        this.diseaseGroupId = diseaseGroupId;
    }

    @PrePersist
    void prePersist() {
        this.diseaseGroupId = this.diseaseGroup.getGroupId();
        this.technologyId = this.technology.getTechnologyId();

        int refCount = gt00 * 2 + gt01;
        int altCount = gt11 * 2 + gt01;

        this.refFreq = (float) refCount / (refCount + altCount);
        this.altFreq = (float) altCount / (refCount + altCount);

        this.maf = Math.min(this.refFreq, this.altFreq);
    }


    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof DiseaseCount) {
            DiseaseCount dc = (DiseaseCount) object;
             if (this.getDiseaseGroupId() == dc.getDiseaseGroupId() && this.getTechnologyId() == dc.getTechnologyId())
                 return true;
        }
        return false;
    }


    @Override
    public String toString() {
        return "DiseaseCount{" +
                "id=" + id +
                ", diseaseGroupId=" + diseaseGroupId +
                ", technologyId=" + technologyId +
                ", gt00=" + gt00 +
                ", gt01=" + gt01 +
                ", gt11=" + gt11 +
                ", gtmissing=" + gtmissing +
                ", maf=" + maf +
                ", refFreq=" + refFreq +
                ", altFreq=" + altFreq +
                '}';
    }
}
