package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity(noClassnameStored = true)
@Indexes(@Index(name = "index_file_sum", value = "sum", unique = true))
public class File {

    @Id
    private ObjectId id;

    private String sum;

    public File() {
    }

    public File(String sum) {
        this.sum = sum;
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
