package org.babelomics.exomeserver.lib.mongodb.converter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.stats.VariantGlobalStats;
import org.opencb.opencga.storage.variant.mongodb.DBObjectToVariantSourceConverter;

import java.util.Calendar;

/**
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
public class ExomeServerDBObjectToVariantSourceConverter extends DBObjectToVariantSourceConverter {
    @Override
    public VariantSource convertToDataModelType(DBObject object) {
        VariantSource source = new VariantSource((String) object.get(FILENAME_FIELD), (String) object.get(FILEID_FIELD),
                (String) object.get(STUDYID_FIELD), (String) object.get(STUDYNAME_FIELD));

        // Samples
//        source.setSamplesPosition((Map) object.get(SAMPLES_FIELD));

        // Statistics
        DBObject statsObject = (DBObject) object.get(STATS_FIELD);
        VariantGlobalStats stats = new VariantGlobalStats();
        if (statsObject != null) {
            stats.setSamplesCount((int) statsObject.get(NUMSAMPLES_FIELD));
            stats.setVariantsCount((int) statsObject.get(NUMVARIANTS_FIELD));
            stats.setSnpsCount((int) statsObject.get(NUMSNPS_FIELD));
            stats.setIndelsCount((int) statsObject.get(NUMINDELS_FIELD));
            stats.setPassCount((int) statsObject.get(NUMPASSFILTERS_FIELD));
            stats.setTransitionsCount((int) statsObject.get(NUMTRANSITIONS_FIELD));
            stats.setTransversionsCount((int) statsObject.get(NUMTRANSVERSIONS_FIELD));
            stats.setMeanQuality(((Double) statsObject.get(MEANQUALITY_FIELD)).floatValue());
        }
        source.setStats(stats);

        // Metadata
//        BasicDBObject metadata = (BasicDBObject) object.get(METADATA_FIELD);
//        for (Map.Entry<String, Object> o : metadata.entrySet()) {
//                source.addMetadata(o.getKey(), o.getValue().toString());
//        }

        return source;
    }

    @Override
    public DBObject convertToStorageType(VariantSource object) {
        BasicDBObject studyMongo = new BasicDBObject(FILENAME_FIELD, object.getFileName())
                .append(FILEID_FIELD, object.getFileId())
                .append(STUDYNAME_FIELD, object.getStudyName())
                .append(STUDYID_FIELD, object.getStudyId())
                .append(DATE_FIELD, Calendar.getInstance().getTime());
//                .append(SAMPLES_FIELD, object.getSamplesPosition());

        // TODO Pending how to manage the consequence type ranking (calculate during reading?)
//        BasicDBObject cts = new BasicDBObject();
//        for (Map.Entry<String, Integer> entry : conseqTypes.entrySet()) {
//            cts.append(entry.getKey(), entry.getValue());
//        }

        // Statistics
        VariantGlobalStats global = object.getStats();
        if (global != null) {
            DBObject globalStats = new BasicDBObject(NUMSAMPLES_FIELD, global.getSamplesCount())
                    .append(NUMVARIANTS_FIELD, global.getVariantsCount())
                    .append(NUMSNPS_FIELD, global.getSnpsCount())
                    .append(NUMINDELS_FIELD, global.getIndelsCount())
                    .append(NUMPASSFILTERS_FIELD, global.getPassCount())
                    .append(NUMTRANSITIONS_FIELD, global.getTransitionsCount())
                    .append(NUMTRANSVERSIONS_FIELD, global.getTransversionsCount())
                    .append(MEANQUALITY_FIELD, (double) global.getMeanQuality());

            studyMongo = studyMongo.append(STATS_FIELD, globalStats);
        }

        // TODO Save pedigree information

//        Metadata
//        Map<String, String> meta = object.getMetadata();
//        DBObject metadataMongo = new BasicDBObject();
//        studyMongo = studyMongo.append(METADATA_FIELD, metadataMongo);
//
        return studyMongo;
    }

}
