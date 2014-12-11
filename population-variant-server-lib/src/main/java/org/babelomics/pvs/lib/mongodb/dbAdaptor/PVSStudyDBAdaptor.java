package org.babelomics.pvs.lib.mongodb.dbAdaptor;

import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.storage.variant.StudyDBAdaptor;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public interface PVSStudyDBAdaptor extends StudyDBAdaptor {

    QueryResult listDiseases();
}
