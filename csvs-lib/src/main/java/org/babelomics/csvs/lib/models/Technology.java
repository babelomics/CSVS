package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
@Entity(noClassnameStored = true)
//@Indexes(@Index(name = "index", value = "n", unique = true))
public class Technology {
    @Id
    private ObjectId id;

    @Indexed(name = "index_technology_id", unique = true)
    @Property("tid")
    private int technologyId;

    @Indexed(name = "index_technology_name", unique = true)
    @Property("n")
    private String name;

    public Technology() {
    }

    public Technology(int technologyId, String name) {
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

    @Override
    public String toString() {
        return "Technology{" +
                "id=" + id +
                ", technologyId=" + technologyId +
                ", name='" + name + '\'' +
                '}';
    }
}
