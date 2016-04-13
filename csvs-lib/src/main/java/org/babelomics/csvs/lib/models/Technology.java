package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
@Entity(noClassnameStored = true)
//@Indexes(@Index(name = "index", value = "n", unique = true))
public class Technology {
    @JsonIgnore
    @Id
    private ObjectId id;

    @Indexed(name = "index_technology_id", unique = true)
    @Property("tid")
    private int technologyId;

    @Indexed(name = "index_technology_name", unique = true)
    @Property("n")
    private String name;
    @Transient
    private int samples;
    @Transient
    int variants;


    public Technology() {
        this.samples = 0;
        this.variants = 0;
    }

    public Technology(int technologyId, String name) {
        this();
        this.technologyId = technologyId;
        this.name = name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTechnologyId() {
        return technologyId;
    }

    public void setTechnologyId(int technologyId) {
        this.technologyId = technologyId;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public int getVariants() {
        return variants;
    }

    public void setVariants(int variants) {
        this.variants = variants;
    }

    @Override
    public String toString() {
        return "Technology{" +
                "technologyId=" + technologyId +
                ", name='" + name + '\'' +
                ", samples=" + samples +
                ", variants=" + variants +
                '}';
    }
}
