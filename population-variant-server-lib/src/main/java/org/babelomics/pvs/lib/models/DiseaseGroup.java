package org.babelomics.pvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
@Entity
public class DiseaseGroup {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Property("n")
    private String name;

    @Property("gid")
    private int groupId;


    public DiseaseGroup(int groupId, String name) {
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

    public DiseaseGroup() {}

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "DiseaseGroup{" +
                ", name='" + name + '\'' +
                ", groupId=" + groupId +
                '}';
    }
}
