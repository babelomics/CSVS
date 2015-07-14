package org.babelomics.pvs.lib.io;

import com.mongodb.MongoClient;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSQueryManager {

    final Morphia morphia;
    final Datastore datastore;

    public PVSQueryManager() {
        this.morphia = new Morphia();
        this.morphia.mapPackage("org.babelomics.pvs.lib.models");
        this.datastore = this.morphia.createDatastore(new MongoClient(), "pvs");
        this.datastore.ensureIndexes();
    }


    public List<DiseaseGroup> getAllDiseaseGroups() {
        List<DiseaseGroup> query = datastore.createQuery(DiseaseGroup.class).order("groupId").asList();
        return query;
    }

}
