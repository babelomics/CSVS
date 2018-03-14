package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

import java.util.*;

/**
 * Class to describe info about opinion.
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
@Entity(noClassnameStored = true)
public class Opinion {

    static final Map<Integer , String> mapStates = new HashMap<Integer, String>();
    static final Map<String , String> mapType = new HashMap<String, String>();
    public static final int PENDING=0, PUBLISHED=1, REJECTED=2;
    public static final String BENING="B", LIKELY_BENING="LB", LIKELY_PATHOGENIC="LP", PATHOGENIC="P";
    static {
            mapStates.put(PENDING, "PENDING TO PUBLIC");
            mapStates.put(PUBLISHED, "PUBLISHED");
            mapStates.put(REJECTED, "REJECTED");

            mapType.put(BENING, "BENING");
            mapType.put(LIKELY_BENING, "LIKELY BENING");
            mapType.put(LIKELY_PATHOGENIC, "LIKELY PATHOGENIC");
            mapType.put(PATHOGENIC, "PATHOGENIC");
    }
    @Id
    private ObjectId id;

    @Reference("v")
    private Variant variant;

    @Property("n")
    private String name;

    @Property("i")
    private String institution;

    @Property("e")
    private String evidence;

    @Property("t")
    private String type;

    @Property("s")
    private int state = PENDING;

    @Property("c")
    private Date created;


    public Opinion(){

    }

    public Opinion(String name, String institution, String evidence, String type) {
        this.name = name;
        this.institution = institution;
        this.evidence = evidence;
        this.type = type;
        this.created = new Date();
    }

    public Opinion(Variant variant, String name, String institution, String evidence, String type) {
        this.variant = variant;
        this.name = name;
        this.institution = institution;
        this.evidence = evidence;
        this.type = type;
        this.created = new Date();
    }

    public static Map<Integer, String> getMapStates() {
        return mapStates;
    }

    public static Map<String, String> getMapType() {
        return mapType;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) { this.created = created;}
    /**
     * Get Description Type
     * @return
     */
    public String getTypeDesc(){
        if (this.mapType.containsKey(this.type))
            return   mapType.get(this.type);
        return "";
    }

    /**
     * Get Description State
     * @return
     */
    public String getStateDesc(){
        if (this.mapStates.containsKey(this.state))
            return   mapStates.get(this.state);
        return "";
    }

    @Override
    public String toString() {

        return "Opinion{"+
                this.variant.toString() +
                ", name:" + this.getName() +
                ", institution:" + this.getInstitution() +
                ", evidence:" + this.getEvidence() +
                ", state: {" + this.getState() +
                            ", " + this.getStateDesc() + "}" +
                ", type: {" + this.getType() +
                        ", " + this.getTypeDesc() + "}" +
                ", created:" + this.getCreated() +
                "}";
    }

}