package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

/**
 * Contains info about samples number (in case there are panels)
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */

@Entity(noClassnameStored = true)
//@Indexes(@Index(name = "index_diseasesun_diseasegroupid_technologyid", value = "dgid,tid", unique = true))
public class DiseaseSum {

    @Id
    private ObjectId id;

    @Property("dgid")
    private int diseaseGroupId;

    @Property("tid")
    private int technologyId;

    @Property("sr")
    private int sumSampleRegions;

    @Property("srXX")
    private int sumSampleRegionsXX;


    @Property("srXY")
    private int sumSampleRegionsXY;

    public DiseaseSum() {
    }

    public DiseaseSum(int diseaseGroupId, int technologyId) {
        this.diseaseGroupId = diseaseGroupId;
        this.technologyId = technologyId;
    }

    public DiseaseSum(int diseaseGroupId, int technologyId, int sumSampleRegions) {
        this.diseaseGroupId = diseaseGroupId;
        this.technologyId = technologyId;
        this.sumSampleRegions = sumSampleRegions;
    }

    public DiseaseSum(int diseaseGroupId, int technologyId, int sumSampleRegions, int sumSampleRegionsXX, int sumSampleRegionsXY) {
        this.diseaseGroupId = diseaseGroupId;
        this.technologyId = technologyId;
        this.sumSampleRegions = sumSampleRegions;
        this.sumSampleRegionsXX = sumSampleRegionsXX;
        this.sumSampleRegionsXY = sumSampleRegionsXY;
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

    public int getSumSampleRegions() {
        return sumSampleRegions;
    }

    public void setSumSampleRegions(int sumSampleRegions) {
        this.sumSampleRegions = sumSampleRegions;
    }

    public int getSumSampleRegionsXX() {
        return sumSampleRegionsXX;
    }

    public void setSumSampleRegionsXX(int sumSampleRegionsXX) {
        this.sumSampleRegionsXX = sumSampleRegionsXX;
    }

    public int getSumSampleRegionsXY() {
        return sumSampleRegionsXY;
    }

    public void setSumSampleRegionsXY(int sumSampleRegionsXY) {
        this.sumSampleRegionsXY = sumSampleRegionsXY;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof DiseaseSum) {
            DiseaseSum ds = (DiseaseSum) object;
            return this.getDiseaseGroupId() == ds.getDiseaseGroupId() && this.getTechnologyId() == ds.getTechnologyId();
        }
        return false;
    }


    @Override
    public String toString() {
        return "DiseaseSum{" +
                "id=" + id +
                ", diseaseGroupId=" + diseaseGroupId +
                ", technologyId=" + technologyId +
                ", sumSampleRegions=" + sumSampleRegions +
                ", sumSampleRegionsXX=" + sumSampleRegionsXX +
                ", sumSampleRegionsXY=" + sumSampleRegionsXY +
                '}';
    }
}
