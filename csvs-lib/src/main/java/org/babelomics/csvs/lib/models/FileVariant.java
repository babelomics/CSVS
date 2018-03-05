package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.Date;
/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */

@Entity(noClassnameStored = true)
@Indexes({
        @Index(name = "index_fileVariant_f_v", fields = {@Field("idFile"),@Field("idVariant") }),
})
public class FileVariant {
    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("fid")
    private ObjectId idFile;

    @Property("vid")
    private ObjectId idVariant;

    public FileVariant(){}

    public FileVariant( ObjectId idFile, ObjectId idVariant) {
        this.idFile = idFile;
        this.idVariant = idVariant;
    }

    public ObjectId getId() {
        return id;
    }



    public ObjectId getIdFile() {
        return idFile;
    }

    public ObjectId getIdVariant() {
        return idVariant;
    }


}