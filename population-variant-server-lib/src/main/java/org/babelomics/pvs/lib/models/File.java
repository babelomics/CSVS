package org.babelomics.pvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity
@Indexes(@Index(name = "index", value = "sum", unique = true))
public class File {

    @Id
    private ObjectId id;

    private String sum;

    public File(String sum) {
        this.sum = sum;
    }

    public File() {
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
}
