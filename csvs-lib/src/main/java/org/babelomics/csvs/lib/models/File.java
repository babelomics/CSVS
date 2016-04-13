package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity(noClassnameStored = true)
//@Indexes(@Index(name = "index_file_sum", value = "sum", unique = true))
public class File {

    @Id
    private ObjectId id;

    private String sum;

    @Reference("d")
    private DiseaseGroup disease;

    @Reference("t")
    private Technology technology;

    @Property("dgid")
    private int diseaseGroupId;
    @Property("tid")
    private int technologyId;

    @Property("s")
    private int samples;

    public File() {
        this.samples = 0;
    }

    public File(String sum) {
        this();
        this.sum = sum;
    }

    public File(String sum, DiseaseGroup disease, Technology technology) {
        this();
        this.sum = sum;
        this.disease = disease;
        this.technology = technology;

        this.diseaseGroupId = this.disease.getGroupId();
        this.technologyId = this.technology.getTechnologyId();
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public ObjectId getId() {
        return id;
    }

    public DiseaseGroup getDisease() {
        return disease;
    }

    public void setDisease(DiseaseGroup disease) {
        this.disease = disease;
    }

    public Technology getTechnology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

}
