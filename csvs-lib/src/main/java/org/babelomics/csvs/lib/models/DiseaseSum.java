package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

/**
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
                '}';
    }
}
