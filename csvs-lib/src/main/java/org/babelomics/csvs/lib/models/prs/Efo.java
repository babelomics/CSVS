package org.babelomics.csvs.lib.models.prs;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;

/**
 * @author grg.
 */
@Entity(noClassnameStored = true)
public class Efo {
    @Property("id")
    private String id;

    @Property("label")
    private String label;

    public Efo() {
    }

    public Efo(String id, String label) {
        this();
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Efo{" +
                "id=" + id +
                ", label=" + label +
                "}";
    }
}
