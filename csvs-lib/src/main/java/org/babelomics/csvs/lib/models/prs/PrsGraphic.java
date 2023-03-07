package org.babelomics.csvs.lib.models.prs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.List;
/**
 * @author grg.
 */
@Entity(noClassnameStored = true)
public class PrsGraphic {

    public static String GENOME = "G";
    public static String EXOME = "E";

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("idPgs")
    private String idPgs;

    @Property("seqType")
    private String seqType;

    @Property("plotX")
    private List<Double> plotX;

    @Property("plotY")
    private List<Integer> plotY;

    @Property("isNormal")
    private Boolean isNormal;

    @Property("min")
    private Double min;

    @Property("max")
    private Double max;

    @Embedded("decile")
    private List<Double> decile;

    @Property("stdDev")
    private Double stdDev;

    @Property("mean")
    private Double mean;


    public PrsGraphic() {
    }

    public Boolean getNormal() {
        return isNormal;
    }

    public void setNormal(Boolean normal) {
        isNormal = normal;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public List<Double> getDecile() {
        return decile;
    }

    public void setDecile(List<Double> decile) {
        this.decile = decile;
    }

    public Double getStdDev() {
        return stdDev;
    }

    public void setStdDev(Double stdDev) {
        this.stdDev = stdDev;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public String getIdPgs() {
        return idPgs;
    }

    public void setIdPgs(String idPgs) {
        this.idPgs = idPgs;
    }

    public String getSeqType() {
        return seqType;
    }

    public void setSeqType(String seqType) {
        this.seqType = seqType;
    }

    public List<Double> getPlotX() {
        return plotX;
    }

    public void setPlotX(List<Double> plotX) {
        this.plotX = plotX;
    }

    public List<Integer> getPlotY() {
        return plotY;
    }

    public void setPlotY(List<Integer> plotY) {
        this.plotY = plotY;
    }

    @Override
    public String toString() {
        return "PrsGraphic{" +
                "idPgs=" + idPgs +
                "seqType=" + seqType +
                "plotX=" +  plotX +
                "plotY=" + plotY +
                ", isNormal=" + isNormal +
                ", min=" + min +
                ", max=" + max +
                ", decile=" + decile +
                ", stdDev=" + stdDev +
                ", mean=" + mean +
                "}";
    }
}
