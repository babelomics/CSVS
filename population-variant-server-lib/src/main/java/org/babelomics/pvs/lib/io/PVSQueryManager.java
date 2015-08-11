package org.babelomics.pvs.lib.io;

import com.mongodb.MongoClient;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.pvs.lib.models.DiseaseCount;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.babelomics.pvs.lib.models.Variant;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.models.feature.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSQueryManager {

    final Datastore datastore;
    static final int DECIMAL_POSITIONS = 3;


    public PVSQueryManager(String host, String dbName) {
        Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.pvs.lib.models");
        this.datastore = morphia.createDatastore(new MongoClient(host), dbName);
        this.datastore.ensureIndexes();
    }

    public PVSQueryManager(String dbName) {
        this("localhost", dbName);
    }

    public PVSQueryManager(Datastore datastore) {
        this.datastore = datastore;
    }


    public List<DiseaseGroup> getAllDiseaseGroups() {
        List<DiseaseGroup> query = datastore.createQuery(DiseaseGroup.class).order("groupId").asList();
        return query;
    }

    public int getMaxDiseaseId() {
        int id = -1;

        DiseaseGroup query = datastore.createQuery(DiseaseGroup.class).order("-groupId").limit(1).get();

        if (query != null) {
            id = query.getGroupId();
        }

        return id;
    }

    public Iterable<Variant> getVariantsByRegionList(List<Region> regions, List<Integer> diseaseIds, Integer skip, Integer limit, MutableLong count) {

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

            if (diseaseIds != null && diseaseIds.size() > 0) {
                and.add(auxQuery.criteria("diseases.diseaseGroupId").in(diseaseIds));
//                and.add(auxQuery.criteria("diseases.diseaseGroupId").hasAllOf(diseaseId));
            }
            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.or(or);

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }

        Iterable<Variant> aux = query.fetch();
        count.setValue(query.countAll());

        List<Variant> res = new ArrayList<>();

        int sampleCount = calculateSampleCount(diseaseIds);

        for (Variant v : aux) {
            v.setStats(calculateStats(v, diseaseIds, sampleCount));
            res.add(v);
        }

        return res;
    }

    private int calculateSampleCount(List<Integer> diseaseId) {
        int res = 0;


        Iterable<DiseaseGroup> groupId = this.datastore.createQuery(DiseaseGroup.class).field("groupId").in(diseaseId).fetch();

        for (DiseaseGroup dg : groupId) {
            res += dg.getSamples();
        }

        return res;
    }

    private DiseaseCount calculateStats(Variant v, List<Integer> diseaseId, int sampleCount) {
        DiseaseCount dc;

        int gt00 = 0;
        int gt01 = 0;
        int gt11 = 0;
        int gtmissing = 0;

        for (DiseaseCount auxDc : v.getDiseases()) {
            if (diseaseId.contains(auxDc.getDiseaseGroup().getGroupId())) {
                gt00 += auxDc.getGt00();
                gt01 += auxDc.getGt01();
                gt11 += auxDc.getGt11();
                gtmissing += auxDc.getGtmissing();
            }
        }

        gt00 = sampleCount - gt01 - gt11 - gtmissing;


        int refCount = gt00 * 2 + gt01;
        int altCount = gt11 * 2 + gt01;

        float refFreq = (float) refCount / (refCount + altCount);
        float altFreq = (float) altCount / (refCount + altCount);

        float maf = Math.min(refFreq, altFreq);

        dc = new DiseaseCount(null, gt00, gt01, gt11, gtmissing);

        dc.setRefFreq(round(refFreq, DECIMAL_POSITIONS));
        dc.setAltFreq(round(altFreq, DECIMAL_POSITIONS));
        dc.setMaf(round(maf, DECIMAL_POSITIONS));

        return dc;
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

    private static float round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    @Entity
    private static class CountResult {

        @Id
        private int count;

        public int getCount() {
            return count;
        }
    }

}
