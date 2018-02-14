package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.List;
import org.babelomics.csvs.lib.models.Region;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */

@Entity(noClassnameStored = true)

public class Panel {
    @Id
    private ObjectId id;

    private String sum;

    @Property("n")
    private String panelName;

    public Panel(){ }

    public Panel(String sum, String panelName){
        this.sum = sum;
        this.panelName = panelName;
    }

    public ObjectId getId() { return id;}

    public String getPanelName() {
        return panelName;
    }


    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }




    /**
     * Check a panel contaion a variant.
     * @param v Variant
     * @return boolean true if exists
     */
    public boolean contains(Variant v, List<Region> regions){
        if (v == null || regions == null || regions.isEmpty()){
            return true;
        } else {
            return  regions.stream().anyMatch(r -> r.contains(v.getChromosome(), v.getPosition()));
        }
    }


}
