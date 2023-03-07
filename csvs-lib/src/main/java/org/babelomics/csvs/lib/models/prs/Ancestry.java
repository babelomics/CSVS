package org.babelomics.csvs.lib.models.prs;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;

/**
 * @author grg.
 */
@Entity(noClassnameStored = true)
public class Ancestry {
    @Property("ancestry")
    private String ancestry;

    @Property("value")
    private Double value;

    public Ancestry() {
    }

    public Ancestry(String ancestry, Double value) {
        this();
        this.ancestry = ancestry;
        this.value = value;
    }

    public String getAncestry() {
        return ancestry;
    }

    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Ancestry{" +
                "ancestry=" + ancestry +
                ", value=" + value +
                "}";
    }
}
