package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;
import org.opencb.biodata.models.feature.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */

@Entity(noClassnameStored = true)
@Indexes({
        // @Index(name = "index_file_reg", value = "reg")

        @Index(name = "index_file_reg", value = "reg.chromosome, reg.start, reg.end"),
        @Index(name = "index_file_c", value = "reg.chromosome"),
        @Index(name = "index_file_s", value = "reg.start"),
        @Index(name = "index_file_e", value = "reg.end")
})
public class Panel {
    @Id
    private ObjectId id;

    private String sum;

    @Property("n")
    private String panelName;

    @Embedded("reg")
    private List<Region> regions;

    public Panel(){ }

    public Panel(String sum, String panelName){
        this.sum = sum;
        this.panelName = panelName;
    }

    public Object getId() { return id;}

    public String getPanelName() {
        return panelName;
    }


    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

    public void addRegion(Region r) {
        this.regions.add(r);
    }

    public void addRegion(List<Region> r) {
        if (this.regions == null)
            this.regions = new ArrayList<>();
        this.regions.addAll(r);
    }

    public void deleteRegion(Region r) {
        this.regions.remove(r);
    }


    /**
     * Check a panel contaion a variant.
     * @param v Variant
     * @return boolean true if exists
     */
    public boolean contains(Variant v){
        if (v == null || this.regions == null || this.regions.isEmpty()){
            return true;
        } else {
            return  this.regions.stream().anyMatch(r -> r.contains(v.getChromosome(), v.getPosition()));
        }
    }


}
