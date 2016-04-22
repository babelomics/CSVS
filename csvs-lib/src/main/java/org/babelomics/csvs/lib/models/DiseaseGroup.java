package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
@Entity(noClassnameStored = true)
public class DiseaseGroup {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Indexed(name = "index_diseasegroup_name", unique = true)
    @Property("n")
    private String name;

    @Indexed(name = "index_diseasegroup_groupid", unique = true)
    @Property("gid")
    private int groupId;

//    @Transient
    @Property("s")
    private int samples;
//    @Transient
    @Property("v")
    int variants;

    public DiseaseGroup() {
        this.samples = 0;
        this.variants = 0;
    }

    public DiseaseGroup(int groupId, String name) {
        this();
        this.groupId = groupId;
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

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
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

    public void incVariants(int variants) {
        this.variants += variants;
    }

    public void incSamples(int samples) {
        this.samples += samples;
    }

    @Override
    public String toString() {
        return "DiseaseGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", groupId=" + groupId +
                ", samples=" + samples +
                ", variants=" + variants +
                '}';
    }
}
