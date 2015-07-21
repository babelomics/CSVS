package org.babelomics.pvs.lib.io;

import com.mongodb.MongoClient;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.babelomics.pvs.lib.models.Variant;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.models.feature.Region;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSQueryManager {

    final Datastore datastore;

    public PVSQueryManager(String dbName) {
        Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.pvs.lib.models");
        this.datastore = morphia.createDatastore(new MongoClient(), dbName);
        this.datastore.ensureIndexes();
    }

    public PVSQueryManager(Datastore datastore) {
        this.datastore = datastore;
    }


    public List<DiseaseGroup> getAllDiseaseGroups() {
        List<DiseaseGroup> query = datastore.createQuery(DiseaseGroup.class).order("groupId").asList();
        return query;
    }


    public Iterable<Variant> getVariantsByRegionList(List<Region> regions, List<Integer> diseaseId, Integer skip, Integer limit) {

        Criteria[] or = new Criteria[regions.size()];

        int i = 0;
        for (Region r : regions) {
            List<String> chunkIds = getChunkIds(r);
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));

            if (diseaseId != null && diseaseId.size() > 0) {
                and.add(auxQuery.criteria("diseases.diseaseGroupId").in(diseaseId));
            }
            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.or(or);

        System.out.println("query = " + query);


        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);

        }


        return query.fetch();
    }


    private List<String> getChunkIds(Region region) {
        List<String> chunkIds = new LinkedList<>();

        int chunkSize = (region.getEnd() - region.getStart() > PVSVariantCountsMongoWriter.CHUNK_SIZE_BIG) ?
                PVSVariantCountsMongoWriter.CHUNK_SIZE_BIG : PVSVariantCountsMongoWriter.CHUNK_SIZE_SMALL;
        int ks = chunkSize / 1000;
        int chunkStart = region.getStart() / chunkSize;
        int chunkEnd = region.getEnd() / chunkSize;

        for (int i = chunkStart; i <= chunkEnd; i++) {
            String chunkId = region.getChromosome() + "_" + i + "_" + ks + "k";
            chunkIds.add(chunkId);
        }

        return chunkIds;
    }

}
