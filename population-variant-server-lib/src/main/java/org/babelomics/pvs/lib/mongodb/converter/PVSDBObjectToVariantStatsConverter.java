package org.babelomics.pvs.lib.mongodb.converter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.opencga.storage.variant.mongodb.DBObjectToVariantStatsConverter;

import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSDBObjectToVariantStatsConverter extends DBObjectToVariantStatsConverter {

    @Override
    public VariantStats convertToDataModelType(DBObject object) {
        // Basic fields
        VariantStats stats = new VariantStats();
        stats.setMaf(((Double) object.get(MAF_FIELD)).floatValue());
        stats.setMafAllele((String) object.get(MAFALLELE_FIELD));

        // Genotype counts
        BasicDBObject genotypes = (BasicDBObject) object.get(NUMGT_FIELD);
        for (Map.Entry<String, Object> o : genotypes.entrySet()) {
            String genotype = o.getKey().toString().replace("-1", ".");
            stats.addGenotype(new Genotype(genotype), (int) o.getValue());
        }

        return stats;
    }

    @Override
    public DBObject convertToStorageType(VariantStats vs) {
        // Basic fields
        BasicDBObject mongoStats = new BasicDBObject(MAF_FIELD, vs.getMaf());
        mongoStats.append(MAFALLELE_FIELD, vs.getMafAllele());

        // Genotype counts
        BasicDBObject genotypes = new BasicDBObject();
        for (Map.Entry<Genotype, Integer> g : vs.getGenotypesCount().entrySet()) {
            String genotype = g.getKey().toString().replace(".", "-1");
            genotypes.append(genotype, g.getValue());
        }
        mongoStats.append(NUMGT_FIELD, genotypes);
        return mongoStats;
    }

}
