package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.ArrayList;
import java.util.List;

@Entity(noClassnameStored = true)

public class AnnotationPharmaIds {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("ty")
    private String ty;

    @Property("name")
    private String name;

    @Property("idPA")
    private String idPA;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }


    public String getTy() {
        return ty;
    }

    public void setTy(String ty) {
        this.ty = ty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdPA() {
        return idPA;
    }

    public void setIdPA(String idPA) {
        this.idPA = idPA;
    }

    @Override
    public String toString() {

        return "PharmaVariant{" +
                ", type='" + ty + '\'' +
                ", name='" + name + '\'' +
                ", idPA='" + idPA + '\'' +
                '}';
    }
}
