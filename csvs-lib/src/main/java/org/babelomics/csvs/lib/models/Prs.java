package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babelomics.csvs.lib.models.prs.Ancestry;
import org.babelomics.csvs.lib.models.prs.Efo;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * @author grg.
 */
@Entity(noClassnameStored = true)
// Poligenic Risk Score
public class Prs {

    static public final String NOT_REPORTED = "Not Reported";

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("idPgs")
    private String idPgs;

    @Embedded("sources")
    private List<Ancestry> sources;

    @Embedded("scores")
    private List<Ancestry> scores;

    @Embedded("listPgs")
    private List<Ancestry> listPgs;

    @Embedded("efos")
    private List<Efo> efos;

    @Property("numVar")
    private int numVar;

    @Property("releaseDate")
    private String releaseDate;

    public Prs() {
        this.efos =  new ArrayList<>();
        this.sources =  new ArrayList<>();
        this.scores =  new ArrayList<>();
        this.listPgs =  new ArrayList<>();
    }

    public String getIdPgs() {
        return idPgs;
    }

    public void setIdPgs(String idPgs) {
        this.idPgs = idPgs;
    }

    public List<Ancestry> getSources() {
        return sources;
    }

    public void setSources(List<Ancestry> sources) {
        this.sources = sources;
    }

    public List<Ancestry> getScores() {
        return scores;
    }

    public void setScores(List<Ancestry> scores) {
        this.scores = scores;
    }

    public List<Ancestry> getListPgs() {
        return listPgs;
    }

    public void setListPgs(List<Ancestry> listPgs) {
        this.listPgs = listPgs;
    }

    public List<Efo> getEfos() {
        return efos;
    }

    public void setEfos(List<Efo> efos) {
        this.efos = efos;
    }

    public int getNumVar() {
        return numVar;
    }

    public void setNumVar(int numVar) {
        this.numVar = numVar;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public String toString() {

        List<String> efos = new ArrayList<>();

       // if (this.efos != null) {
         //   for (Efo efo : this.efos) {
           //     efos.add(efo.getId() + "_" + efo.getLabel());
           // }
        //}

        return "Prs{" +
                "id='" + id + '\'' +

//                ", attr=" + attr +
                ", efos=" + efos +
//                ", annots=" + annots +
                '}';
    }
}


