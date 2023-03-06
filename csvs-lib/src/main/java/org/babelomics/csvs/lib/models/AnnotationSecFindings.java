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

public class AnnotationSecFindings {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("gene")
    private String gene;

    @Property("inheritance")
    private String inheritance;

    @Property("genericPhenotype")
    private String genericPhenotype;

    @Property("c")
    private String chromosome;
    @Property("p")
    private int position;
    @Property("r")
    private String reference;
    @Property("a")
    private String alternate;
    @Property("i")
    private String ids;

    private Region region;

    @Embedded("omims")
    private List<String> omims;


    public AnnotationSecFindings() {

    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public String getInheritance() {
        return inheritance;
    }

    public void setInheritance(String inheritance) {
        this.inheritance = inheritance;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAlternate() {
        return alternate;
    }

    public void setAlternate(String alternate) {
        this.alternate = alternate;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public List<String> getOmims() {
        return omims;
    }

    public void setOmims(List<String> omims) {
        this.omims = omims;
    }

    public String getGenericPhenotype() {
        return genericPhenotype;
    }

    public void setGenericPhenotype(String genericPhenotype) {
        this.genericPhenotype = genericPhenotype;
    }

    @Override
    public String toString() {
        List<String> omims = new ArrayList<>();

        if (this.omims != null) {
            for (String omimId : this.omims) {
                omims.add(omimId);
            }
        }

        return "SecFindings{" +
                ", gene='" + gene + '\'' +
                ", omims='" + omims + '\'' +
                ", region='" + region + '\'' +
                ", inheritance='" + inheritance + '\'' +
                ", genericPhenotype='" + genericPhenotype + '\'' +
                '}';
    }
}
