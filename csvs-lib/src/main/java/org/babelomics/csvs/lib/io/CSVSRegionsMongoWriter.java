package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.*;
import org.mongodb.morphia.Datastore;
import org.opencb.commons.io.DataWriter;
import org.opencb.biodata.models.feature.Region;


import java.util.List;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
public class CSVSRegionsMongoWriter implements DataWriter<Region> {


    private Datastore datastore;
    private Panel panel;

    public static final int CHUNK_SIZE_SMALL = 1000;
    public static final int CHUNK_SIZE_BIG = 10000;

    public CSVSRegionsMongoWriter(Panel p, Datastore datastore) {

        this.panel = p;
        this.datastore = datastore;

    }

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public boolean close() {

        return true;
    }

    @Override
    public boolean pre() {

        return true;
    }

    @Override
    public boolean post() {

//        this.file.setRegions(this.regions);

        return true;
    }

    @Override
    public boolean write(Region elem) {
        return true;
    }

    @Override
    public boolean write(List<Region> batch) {
        this.panel.addRegion(batch);
        return true;
    }
}
