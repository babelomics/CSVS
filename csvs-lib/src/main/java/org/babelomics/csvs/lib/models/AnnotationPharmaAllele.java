package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.List;

@Entity(noClassnameStored = true)

public class AnnotationPharmaAllele {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("gene")
    private String gene;

    @Property("starAllele")
    private String starAllele;


    @Embedded("variants")
    private List<Variant> variants;

    private Region region;

    @Embedded("pharmaIds")
    private List<AnnotationPharmaIds> pharmaIds;


    public AnnotationPharmaAllele() {
        this.variants = new ArrayList<>();
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

    public String getStarAllele() {
        return starAllele;
    }

    public void setStarAllele(String starAllele) {
        this.starAllele = starAllele;
    }

    public List<Variant> getVariants() {
        return variants;
    }

    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public List<AnnotationPharmaIds> getPharmaIds() {
        return pharmaIds;
    }

    public void setPharmaIds(List<AnnotationPharmaIds> pharmaIds) {
        this.pharmaIds = pharmaIds;
    }

    @Override
    public String toString() {
        List<String> variants = new ArrayList<>();

        if (this.variants != null) {
            for (Variant variant : this.variants) {
                variants.add(variant.getChromosome() + ":" + variant.getPosition() + ":" + variant.getReference() + ":" + variant.getAlternate());
            }
        }

        return "PharmaVariant{" +
                ", gene='" + gene + '\'' +
                ", starAllele='" + starAllele + '\'' +
                ", variant='" + variants + '\'' +
                ", region='" + region + '\'' +

                '}';
    }
}
