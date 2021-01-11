package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.Date;
import java.util.List;

/**
 * Information about database versions.
 *
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
@Entity(noClassnameStored = true)
public class Metadata {
    @Id
    private ObjectId id;

    @Property("version")
    String version;

    @Property("date")
    Date date;

    @Embedded("individuals")
    int individuals;

    @Embedded("filesName")
    List<String> filesName;

    @Property("versionJava")
    String versionJava;

    public Metadata() {}

    public Metadata(String version, Date date, int individuals) {
        this.version = version;
        this.date = date;
        this.individuals = individuals;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public int getIndividuals() {
        return individuals;
    }

    public void setIndividuals(int individuals) {
        this.individuals = individuals;
    }


    public String getVersionJava() {
        return versionJava;
    }

    public void setVersionJava(String versionJava) {
        this.versionJava = versionJava;
    }

    public List<String> getFilesName() {
        return filesName;
    }

    public void setFilesName(List<String> filesName) {
        this.filesName = filesName;
    }


}