package org.babelomics.exomeserver.lib.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.opencga.storage.variant.mongodb.DBObjectToVariantStatsConverter;

import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class ExomeServerDBObjectVariantStatsConverter extends DBObjectToVariantStatsConverter {

    @Override
    public VariantStats convertToDataModelType(DBObject object) {
        // Basic fields
        VariantStats stats = new VariantStats();
        stats.setMaf(((Double) object.get(MAF_FIELD)).floatValue());
//        stats.setMgf(((Double) object.get(MGF_FIELD)).floatValue());
        stats.setMafAllele((String) object.get(MAFALLELE_FIELD));
//        stats.setMgfGenotype((String) object.get(MGFGENOTYPE_FIELD));

//        stats.setMissingAlleles((int) object.get(MISSALLELE_FIELD));
//        stats.setMissingGenotypes((int) object.get(MISSGENOTYPE_FIELD));

        // Genotype counts
        BasicDBObject genotypes = (BasicDBObject) object.get(NUMGT_FIELD);
        for (Map.Entry<String, Object> o : genotypes.entrySet()) {
            stats.addGenotype(new Genotype(o.getKey()), (int) o.getValue());
        }

        return stats;
    }

    @Override
    public DBObject convertToStorageType(VariantStats vs) {
        // Basic fields
        BasicDBObject mongoStats = new BasicDBObject(MAF_FIELD, vs.getMaf());
//        mongoStats.append(MGF_FIELD, vs.getMgf());
        mongoStats.append(MAFALLELE_FIELD, vs.getMafAllele());
//        mongoStats.append(MGFGENOTYPE_FIELD, vs.getMgfGenotype());
//        mongoStats.append(MISSALLELE_FIELD, vs.getMissingAlleles());
//        mongoStats.append(MISSGENOTYPE_FIELD, vs.getMissingGenotypes());

        // Genotype counts
        BasicDBObject genotypes = new BasicDBObject();
        for (Map.Entry<Genotype, Integer> g : vs.getGenotypesCount().entrySet()) {
            genotypes.append(g.getKey().toString(), g.getValue());
        }
        mongoStats.append(NUMGT_FIELD, genotypes);
        return mongoStats;
    }

}
