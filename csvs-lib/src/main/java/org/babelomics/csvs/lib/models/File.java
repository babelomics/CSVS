package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.Date;
/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity(noClassnameStored = true)
@Indexes({
        @Index(name = "index_file_d_t", fields = {@Field("d"), @Field("t")}),
        @Index(name = "index_file_pid", fields = {@Field("pid")})

})
public class File {

    @Id
    private ObjectId id;

    private String sum;

    @Reference("d")
    private DiseaseGroup disease;

    @Reference("t")
    private Technology technology;

    @Property("dgid")
    private int diseaseGroupId;

    @Property("tid")
    private int technologyId;

    @Property("s")
    private int samples;

    @Property("n")
    private String nameFile;

    @Property("gender")
    private String chromGender;

    @Property("pr")
    private String personReference;

    @Property("da")
    private Date date;

    @Property("pid")
    private ObjectId idPanel;

    public File() {
        this.samples = 0;
    }

    public File(String sum) {
        this();
        this.sum = sum;
    }

    public File(String sum, DiseaseGroup disease, Technology technology) {
        this();
        this.sum = sum;
        this.disease = disease;
        this.technology = technology;

        this.diseaseGroupId = this.disease.getGroupId();
        this.technologyId = this.technology.getTechnologyId();
    }

    public File(String sum, DiseaseGroup disease, Technology technology,  Panel panel ) {
        this();
        this.sum = sum;
        this.disease = disease;
        this.technology = technology;

        this.diseaseGroupId = this.disease.getGroupId();
        this.technologyId = this.technology.getTechnologyId();

        if (panel != null) {
            this.idPanel = (ObjectId) panel.getId();
        }
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

    public DiseaseGroup getDisease() {
        return disease;
    }

    public void setDisease(DiseaseGroup disease) {
        this.disease = disease;
    }

    public Technology getTechnology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public String getNameFile() {
        return nameFile;
    }

    public void setNameFile(String nameFile) {
        this.nameFile = nameFile;
    }

    public String getChromGender() {
        return chromGender;
    }

    public void setChromGender(String chromGender) {
        this.chromGender = chromGender;
    }

    public String getPersonReference() {
        return personReference;
    }

    public void setPersonReference(String personReference) {
        this.personReference = personReference;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public ObjectId getIdPanel() {
        return this.idPanel;
    }

    public void setIdPanel(ObjectId idPanel) {
        this.idPanel =    idPanel;
    }
}