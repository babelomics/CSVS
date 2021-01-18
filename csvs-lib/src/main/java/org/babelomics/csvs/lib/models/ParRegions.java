package org.babelomics.csvs.lib.models;

import  org.opencb.biodata.models.feature.Region;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */

public class ParRegions {
    Region x;
    Region y;
    int diference;

    public ParRegions(Region x, Region y, int diference) {
        this.x = x;
        this.y = y;
        this.diference = diference;
    }

    public Region getX() {
        return x;
    }

    public void setX(Region x) {
        this.x = x;
    }

    public Region getY() {
        return y;
    }

    public void setY(Region y) {
        this.y = y;
    }

    public int getDiference() {
        return diference;
    }

    public void setDiference(int diference) {
        this.diference = diference;
    }
}
