package org.babelomics.csvs.lib.io;

import com.mongodb.*;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.models.feature.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSQueryManager {

    final Datastore datastore;
    static final int DECIMAL_POSITIONS = 3;


    public CSVSQueryManager(String host, String dbName) {
        Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.csvs.lib.models");
        this.datastore = morphia.createDatastore(new MongoClient(host), dbName);
        this.datastore.ensureIndexes();
    }

    public CSVSQueryManager(String dbName) {
        this("localhost", dbName);
    }

    public CSVSQueryManager(Datastore datastore) {
        this.datastore = datastore;
    }

    public DiseaseGroup getDiseaseById(int id) {
        DiseaseGroup dg = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(id).get();
        return dg;
    }

    public Technology getTechnologyById(int id) {
        Technology dg = datastore.createQuery(Technology.class).field("technologyId").equal(id).get();
        return dg;
    }

    public List<DiseaseGroup> getAllDiseaseGroups() {
        List<DiseaseGroup> res = datastore.createQuery(DiseaseGroup.class).order("groupId").asList();
        return res;
    }

    public List<Integer> getAllDiseaseGroupIds() {
        List<Integer> list = new ArrayList<>();
        for (DiseaseGroup dg : this.getAllDiseaseGroups()) {
            list.add(dg.getGroupId());
        }
        return list;
    }

    public List<Integer> getAllTechnologieIds() {
        List<Integer> list = new ArrayList<>();
        for (Technology t : this.getAllTechnologies()) {
            list.add(t.getTechnologyId());
        }
        return list;
    }

    public List<Technology> getAllTechnologies() {
        List<Technology> res = datastore.createQuery(Technology.class).order("technologyId").asList();
        return res;
    }


    public List<DiseaseGroup> getAllDiseaseGroupsOrderedBySample() {
        List<DiseaseGroup> res = datastore.createQuery(DiseaseGroup.class).order("-samples").asList();
        return res;
    }

    public List<Technology> getAllTechnologiesOrderedBySample() {
        List<Technology> res = datastore.createQuery(Technology.class).order("-samples").asList();
        return res;
    }

    public int getMaxDiseaseId() {
        int id = -1;
        DiseaseGroup query = datastore.createQuery(DiseaseGroup.class).order("-groupId").limit(1).get();
        if (query != null) {
            id = query.getGroupId();
        }
        return id;
    }

    public int getMaxTechnologyId() {
        int id = -1;
        Technology query = datastore.createQuery(Technology.class).order("-technologyId").limit(1).get();
        if (query != null) {
            id = query.getTechnologyId();
        }
        return id;
    }

    public List<List<Variant>> getVariantsByRegionList(List<Region> regions) {

        List<List<Variant>> res = new ArrayList<>();

        for (Region r : regions) {

            List<String> chunkIds = getChunkIds(r);
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            auxQuery.filter("_at.chIds in", chunkIds).
                    filter("chromosome =", r.getChromosome()).
                    filter("position >=", r.getStart()).
                    filter("position <=", r.getEnd());

            List<Variant> variants = auxQuery.asList();

            for (Variant v : variants) {
                v.setStats(null);
                v.setAnnots(null);
            }
            res.add(variants);
        }

        return res;
    }

    public Variant getVariant(String chromosome, int position, String reference, String alternate, List<Integer> diseaseIds, List<Integer> technologyIds) {

        Region r = new Region(chromosome, position, position);

        List<String> chunkIds = getChunkIds(r);

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.filter("_at.chIds in", chunkIds);
        query.filter("chromosome = ", chromosome);
        query.filter("position =", position);
        query.filter("reference =", reference.toUpperCase());
        query.filter("alternate =", alternate.toUpperCase());

        boolean disTechCheck = false;

        BasicDBList listDBObjects = new BasicDBList();

        if (diseaseIds != null && !diseaseIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseIds)));
            disTechCheck = true;

        }

        if (technologyIds != null && !technologyIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
            disTechCheck = true;
        }

        if (disTechCheck) {
            query.filter("diseases elem", new BasicDBObject("$and", listDBObjects));
        }

        Variant res = query.get();

        if (res != null) {

            if (diseaseIds == null || diseaseIds.size() == 0) {
                diseaseIds = new ArrayList<>();
                List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
                for (DiseaseGroup dg : dgList) {
                    diseaseIds.add(dg.getGroupId());
                }
            }
            if (technologyIds == null || technologyIds.size() == 0) {
                technologyIds = new ArrayList<>();
                List<Technology> techList = this.getAllTechnologies();
                for (Technology t : techList) {
                    technologyIds.add(t.getTechnologyId());
                }
            }
	    
            // Map with disease-technology-samples
            Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds);
            int sampleCount = calculateSampleCount(sampleCountMap);

            DiseaseCount diseaseCount = calculateStats(res, diseaseIds, technologyIds, sampleCount, sampleCountMap);

            res.setStats(diseaseCount);
            res.setDiseases(null);

        }
        return res;
    }


    public Variant getVariant(Variant variant, List<Integer> diseaseIds, List<Integer> technologyIds) {

        return this.getVariant(variant.getChromosome(), variant.getPosition(), variant.getReference(), variant.getAlternate(), diseaseIds, technologyIds);
    }

    public List<Variant> getVariants(List<Variant> variants, List<Integer> diseaseIds, List<Integer> technologyIds) {
        List<Variant> res = new ArrayList<>();

        for (Variant v : variants) {
            Variant resVariant = this.getVariant(v, diseaseIds, technologyIds);
            res.add(resVariant);
        }

        return res;
    }

    public List<List<IntervalFrequency>> getAllIntervalFrequencies(List<Region> regions, boolean histogramLogarithm, int histogramMax, int interval) {

        List<List<IntervalFrequency>> res = new ArrayList<>();
        for (Region r : regions) {
            res.add(getIntervalFrequencies(r, histogramLogarithm, histogramMax, interval));
        }

        return res;
    }


    public List<IntervalFrequency> getIntervalFrequencies(Region region, boolean histogramLogarithm, int histogramMax, int interval) {

        List<IntervalFrequency> res = new ArrayList<>();

        BasicDBObject start = new BasicDBObject("$gt", region.getStart());
        start.append("$lt", region.getEnd());

        BasicDBList andArr = new BasicDBList();
        andArr.add(new BasicDBObject("c", region.getChromosome()));
        andArr.add(new BasicDBObject("p", start));

        BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", andArr));


        BasicDBList divide1 = new BasicDBList();
        divide1.add("$p");
        divide1.add(interval);

        BasicDBList divide2 = new BasicDBList();
        divide2.add(new BasicDBObject("$mod", divide1));
        divide2.add(interval);

        BasicDBList subtractList = new BasicDBList();
        subtractList.add(new BasicDBObject("$divide", divide1));
        subtractList.add(new BasicDBObject("$divide", divide2));


        BasicDBObject subtract = new BasicDBObject("$subtract", subtractList);

        DBObject totalCount = new BasicDBObject("$sum", 1);

        BasicDBObject g = new BasicDBObject("_id", subtract);
        g.append("features_count", totalCount);
        BasicDBObject group = new BasicDBObject("$group", g);

        BasicDBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id", 1));


        DBCollection collection = datastore.getCollection(Variant.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(match);
        aggList.add(group);
        aggList.add(sort);

        AggregationOutput aggregation = collection.aggregate(aggList);

        Map<Long, IntervalFrequency> ids = new HashMap<>();

        for (DBObject intervalObj : aggregation.results()) {

            Long _id = Math.round((Double) intervalObj.get("_id"));//is double

            IntervalFrequency intervalVisited = ids.get(_id);

            if (intervalVisited == null) {
                intervalVisited = new IntervalFrequency();

                intervalVisited.setId(_id);
                intervalVisited.setStart(getChunkStart(_id.intValue(), interval));
                intervalVisited.setEnd(getChunkEnd(_id.intValue(), interval));
                intervalVisited.setChromosome(region.getChromosome());
                intervalVisited.setFeaturesCount(Math.log((int) intervalObj.get("features_count")));
                ids.put(_id, intervalVisited);
            } else {
                double sum = intervalVisited.getFeaturesCount() + Math.log((int) intervalObj.get("features_count"));
                intervalVisited.setFeaturesCount(sum);
            }
        }

        int firstChunkId = getChunkId(region.getStart(), interval);
        int lastChunkId = getChunkId(region.getEnd(), interval);

        IntervalFrequency intervalObj;
        for (int chunkId = firstChunkId; chunkId <= lastChunkId; chunkId++) {
            intervalObj = ids.get((long) chunkId);

            if (intervalObj == null) {
                intervalObj = new IntervalFrequency(chunkId, getChunkStart(chunkId, interval), getChunkEnd(chunkId, interval), region.getChromosome(), 0);
            }
            res.add(intervalObj);
        }

        return res;
    }

    public Iterable<Variant> getVariantsByRegionList(List<Region> regions, List<Integer> diseaseIds, List<Integer> technologyIds, Integer skip, Integer limit, boolean skipCount, MutableLong count) {

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

            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.or(or);


        boolean disTechCheck = false;

        BasicDBList listDBObjects = new BasicDBList();

        if (diseaseIds != null && !diseaseIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseIds)));
            disTechCheck = true;

        }

        if (technologyIds != null && !technologyIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
            disTechCheck = true;
        }

        if (disTechCheck) {
            query.filter("diseases elem", new BasicDBObject("$and", listDBObjects));
        }

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }


        System.out.println("query = " + query);

        Iterable<Variant> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        List<Variant> res = new ArrayList<>();

        if (diseaseIds == null || diseaseIds.isEmpty()) {
            diseaseIds = new ArrayList<>();
            List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
            for (DiseaseGroup dg : dgList) {
                diseaseIds.add(dg.getGroupId());
            }
        }

        if (technologyIds == null || technologyIds.isEmpty()) {
            technologyIds = new ArrayList<>();
            List<Technology> technologyList = this.getAllTechnologies();
            for (Technology t : technologyList) {
                technologyIds.add(t.getTechnologyId());
            }
        }
	
        // Map with disease-technology-samples
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds);
        int sampleCount = calculateSampleCount(sampleCountMap);

        for (Variant v : aux) {
            v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap));
            v.setDiseases(null);
            res.add(v);
        }

        return res;
    }


    public Map<Region, List<SaturationElement>> getSaturation(List<Region> regions, List<Integer> diseaseIds, List<Integer> technologyIds) {

        Map<Region, List<SaturationElement>> map = new LinkedHashMap<>();

        List<DiseaseGroup> diseaseOrder = getAllDiseaseGroupsOrderedBySample();
        List<Technology> technologyOrder = getAllTechnologiesOrderedBySample();


        for (Region r : regions) {

            List<String> chunkIds = getChunkIds(r);

            List<SaturationElement> list = new ArrayList<>();
            Map<Integer, Integer> diseaseCount = new HashMap<>();


            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));

            Query<Variant> query = this.datastore.createQuery(Variant.class);
            query.and(and.toArray(new Criteria[and.size()]));

            boolean disTechCheck = false;

            BasicDBList listDBObjects = new BasicDBList();

            if (diseaseIds != null && !diseaseIds.isEmpty()) {
                listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseIds)));
                disTechCheck = true;

            }

            if (technologyIds != null && !technologyIds.isEmpty()) {
                listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
                disTechCheck = true;
            }

            if (disTechCheck) {
                query.filter("diseases elem", new BasicDBObject("$and", listDBObjects));
            }


            System.out.println(query);

            Iterable<Variant> aux = query.fetch();

            for (Variant v : aux) {
                if (!v.getDiseases().isEmpty()) {

                    DiseaseCount dc = null;

                    Iterator<DiseaseGroup> dgIt = diseaseOrder.iterator();

                    while (dgIt.hasNext() && dc == null) {
                        DiseaseGroup dg = dgIt.next();
                        if (!diseaseIds.contains(dg.getGroupId())) {
                            continue;
                        }
                        Iterator<Technology> tIt = technologyOrder.iterator();
                        while (dgIt.hasNext() && tIt.hasNext() && dc == null) {
                            Technology t = tIt.next();
                            if (!technologyIds.contains(t.getTechnologyId())) {
                                continue;
                            }
                            dc = v.getDiseaseCount(dg, t);
                        }
                    }

                    if(dc == null){
                        continue;
                    }
                    int count = 0;
                    if (diseaseCount.containsKey(dc.getDiseaseGroup().getGroupId())) {
                        count = diseaseCount.get(dc.getDiseaseGroup().getGroupId());
                    }
                    count += 1;
                    diseaseCount.put(dc.getDiseaseGroup().getGroupId(), count);
                }
            }


            Iterator<DiseaseGroup> dgIt = diseaseOrder.iterator();

            while (dgIt.hasNext()) {
                DiseaseGroup dg = dgIt.next();
                if (diseaseCount.containsKey(dg.getGroupId())) {
                    list.add(new SaturationElement(
                            dg.getGroupId(),
                            diseaseCount.get(dg.getGroupId()),
                            dg.getSamples()
                    ));
                } else {
                    list.add(new SaturationElement(
                            dg.getGroupId(),
                            0,
                            dg.getSamples()
                    ));
                }

            }

//            for (Map.Entry<Integer, Integer> entry : diseaseCount.entrySet()) {
//                DiseaseGroup diseaseGroup = this.getDiseaseById(entry.getKey());
//                list.add(new SaturationElement(
//                        entry.getKey(),
//                        entry.getValue(),
//                        diseaseGroup.getSamples()
//                ));
//            }

//            Collections.sort(list, new SaturationElementSampleDescComparator());
            map.put(r, list);
        }

        return map;
    }

    public Iterable<Variant> getAllVariants(List<Integer> diseaseIds, List<Integer> technologyIds, Integer skip, Integer limit, MutableLong count) {

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }

        if (diseaseIds == null || diseaseIds.size() == 0) {
            diseaseIds = new ArrayList<>();
            List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
            for (DiseaseGroup dg : dgList) {
                diseaseIds.add(dg.getGroupId());
            }

        }

        if (technologyIds == null || technologyIds.isEmpty()) {
            technologyIds = new ArrayList<>();
            List<Technology> technologyList = this.getAllTechnologies();
            for (Technology t : technologyList) {
                technologyIds.add(t.getTechnologyId());
            }
        }
        // Map with disease-technology-samples
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds);
        int sampleCount = calculateSampleCount(sampleCountMap);

        Iterable<Variant> aux = query.fetch();
        Iterable<Variant> customIterable = new AllVariantsIterable(aux, diseaseIds, technologyIds, sampleCount, sampleCountMap);

        return customIterable;
    }

    /**
     * Calculte sum of samples by diseased and technology
     * @param sampleCountMap
     * @return
     */
    private int calculateSampleCount(Map<String, Integer> sampleCountMap) {
	    return sampleCountMap.values().stream().mapToInt(i -> i.intValue()).sum();
    }

    /**
     * Return map samples by diseased and technology (without regions)
     * @param diseaseId
     * @param technologyId
     * @return
     */
    public Map<String, Integer> calculateSampleCount(List<Integer> diseaseId, List<Integer> technologyId) {
        BasicDBList listDBObjects = new BasicDBList();

        listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseId)));
        listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyId)));


        listDBObjects.add(new BasicDBObject("pid", new BasicDBObject("$eq", null)));
        BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", listDBObjects));

        BasicDBList listDBObjectsGroupSub = new BasicDBList();
        listDBObjectsGroupSub.add("$dgid");
        listDBObjectsGroupSub.add(0);
        listDBObjectsGroupSub.add(-1);

        BasicDBList listDBObjectsGroupSubT = new BasicDBList();
        listDBObjectsGroupSubT.add("$tid");
        listDBObjectsGroupSubT.add(0);
        listDBObjectsGroupSubT.add(-1);

        BasicDBList listDBObjectsGroup = new BasicDBList();
        listDBObjectsGroup.add(new BasicDBObject("$substr", listDBObjectsGroupSub));
        listDBObjectsGroup.add("-");
        listDBObjectsGroup.add(new BasicDBObject("$substr", listDBObjectsGroupSubT));

        BasicDBObject g = new BasicDBObject("_id", new BasicDBObject("$concat", listDBObjectsGroup));
        g.append("Sum", new BasicDBObject("$sum", "$s"));
        BasicDBObject group = new BasicDBObject("$group", g);


        DBCollection collection = datastore.getCollection(File.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(match);
        aggList.add(group);

        // System.out.println("QUERY: " + aggList);
        AggregationOutput aggregation = collection.aggregate(aggList);
        //  System.out.println("RESULT: " + aggregation.results());
        Map<String, Integer> res = new HashMap<>();

        for (DBObject fileObj : aggregation.results()) {
            res.put((String) fileObj.get("_id"), (Integer) fileObj.get("Sum"));
        }

        return res;
    }


    /**
     * Calculate sum all examples search in the all files (only with regions).
     * @param v Variant to calc samples
     * @param diseaseId
     * @param technologyId
     * @param datastore
     * @return Sum all examples
     */
    public static int initialCalculateSampleCount(Variant v, int diseaseId, int technologyId, Datastore datastore) {
        int res = 0;

        BasicDBList listDBObjects = new BasicDBList();

        BasicDBList listDBObjectsExists = new BasicDBList();
        listDBObjectsExists.add(new BasicDBObject("reg.chromosome", v.getChromosome()));
        listDBObjectsExists.add(new BasicDBObject("reg.start", new BasicDBObject("$lte",v.getPosition())));
        listDBObjectsExists.add(new BasicDBObject("reg.end", new BasicDBObject("$gte",v.getPosition())));

        listDBObjects.addAll(listDBObjectsExists);

        BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", listDBObjects));


        BasicDBObject unwind = new BasicDBObject("$unwind", new BasicDBObject("path","$reg"));
        DBCollection collection = datastore.getCollection(Panel.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(unwind);
        aggList.add(match);

        //System.out.println(aggList);
        AggregationOutput aggregation = collection.aggregate(aggList);

        List objtid = new ArrayList<>();
        for (DBObject oObj : aggregation.results()) {
            objtid.add(oObj.get("_id"));
        }

        Query<File> queryFile = datastore.createQuery(File.class);
        queryFile.field("dgid").equal(diseaseId);
        queryFile.field("tid").equal(technologyId);
        queryFile.field("pid").in(objtid);
        Iterable<File> auxFile = queryFile.fetch();

        for (File file : auxFile) {
            res = res + (int) file.getSamples();
        }

        return res;
    }


    private DiseaseCount calculateStats(Variant v, List<Integer> diseaseId, List<Integer> technologyId, int sampleCount, Map<String, Integer> sampleCountMap) {
        DiseaseCount dc;

        int gt00 = 0;
        int gt01 = 0;
        int gt11 = 0;
        int gtmissing = 0;
        int sampleCountVariant = 0;
        Map<String, Integer> sampleCountTemp = new HashMap<>(sampleCountMap);
	    boolean existsRegions = false;

        // Variants by regions
        System.out.println("\nCSVS (calculateStats): Variant= "+ v +  " Samples: "  + sampleCountTemp);

        for (DiseaseCount auxDc : v.getDiseases()) {
            if (diseaseId.contains(auxDc.getDiseaseGroup().getGroupId()) && technologyId.contains(auxDc.getTechnology().getTechnologyId())) {
                gt00 += auxDc.getGt00();
                gt01 += auxDc.getGt01();
                gt11 += auxDc.getGt11();
                gtmissing += auxDc.getGtmissing();

                // exists samples load in the panel
                if(auxDc.getSumSampleRegions() != 0) {
                    String key = auxDc.getDiseaseGroup().getGroupId() + "-" + auxDc.getTechnology().getTechnologyId();
                    int sum =  sampleCountMap.containsKey(key) ? sampleCountMap.get(key) : 0;
                    sampleCountTemp.put(key, auxDc.getSumSampleRegions() + sum);
                    existsRegions = true;
                }
            }
        }


        if ( v.getNoDiseases() != null) {
            for (DiseaseSum auxDs : v.getNoDiseases()) {
                if (diseaseId.contains(auxDs.getDiseaseGroupId()) && technologyId.contains(auxDs.getTechnologyId())) {
                    // exists samples load in the panel
                    if (auxDs.getSumSampleRegions() != 0){
                        String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                        int sum =  sampleCountMap.containsKey(key) ? sampleCountMap.get(key) : 0;
                        sampleCountTemp.put(key, auxDs.getSumSampleRegions() + sum);
			            existsRegions = true;
                    }
                }
            }
        }

	    if (existsRegions)
	        sampleCountVariant = sampleCountTemp.values().stream().mapToInt(i -> i.intValue()).sum();
	    else
		    sampleCountVariant = sampleCount;

        gt00 = sampleCountVariant - gt01 - gt11 - gtmissing;

        int refCount = gt00 * 2 + gt01;
        int altCount = gt11 * 2 + gt01;

        float refFreq = (float) refCount / (refCount + altCount);
        float altFreq = (float) altCount / (refCount + altCount);

        float maf = Math.min(refFreq, altFreq);

        dc = new DiseaseCount(null, null, gt00, gt01, gt11, gtmissing);

        if (!Float.isNaN(refFreq)) {
            dc.setRefFreq(round(refFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(altFreq)) {
            dc.setAltFreq(round(altFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(maf)) {
            dc.setMaf(round(maf, DECIMAL_POSITIONS));
        }

        return dc;
    }

    private List<String> getChunkIds(Region region) {
        List<String> chunkIds = new LinkedList<>();

        int chunkSize = (region.getEnd() - region.getStart() > CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG) ?
                CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG : CSVSVariantCountsMongoWriter.CHUNK_SIZE_SMALL;
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

    protected int getChunkId(int position, int chunksize) {
        return position / chunksize;
    }

    private int getChunkStart(int id, int chunksize) {
        return (id == 0) ? 1 : id * chunksize;
    }

    private int getChunkEnd(int id, int chunksize) {
        return (id * chunksize) + chunksize - 1;
    }


    class AllVariantsIterable implements Iterable<Variant> {

        private Iterable iterable;
        private List<Integer> diseaseIds;
        private List<Integer> technologyIds;
        private int sampleCount;
        private Map<String, Integer> sampleCountMap;

        public AllVariantsIterable(Iterable iterable, List<Integer> diseaseIds, List<Integer> technologyIds, int sampleCount, Map<String, Integer> sampleCountMap) {
            this.iterable = iterable;
            this.diseaseIds = diseaseIds;
            this.technologyIds = technologyIds;
            this.sampleCount = sampleCount;
            this.sampleCountMap = sampleCountMap;
        }

        @Override
        public Iterator<Variant> iterator() {
            Iterator<Variant> it = new AllVariantsIterator(this.iterable.iterator());
            return it;
        }

        class AllVariantsIterator implements Iterator<Variant> {

            private Iterator<Variant> it;

            public AllVariantsIterator(Iterator<Variant> it) {
                this.it = it;
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Variant next() {
                Variant v = this.it.next();
                v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap));
                v.setDiseases(null);
                return v;
            }

            @Override
            public void remove() {

            }
        }
    }

    static class AggregationElem {
        DBRef ref;
        int samples;
        int variants;

        public AggregationElem() {
        }

        public DBRef getRef() {
            return ref;
        }

        public void setRef(DBRef ref) {
            this.ref = ref;
        }


        public int getSamples() {
            return samples;
        }

        public void setSamples(int samples) {
            this.samples = samples;
        }

        public int getVariants() {
            return variants;
        }

        public void setVariants(int variants) {
            this.variants = variants;
        }

        @Override
        public String toString() {
            return "DiseaseElem{" +
                    ", ref=" + ref +
                    ", samples=" + samples +
                    ", variants=" + variants +
                    '}';
        }
    }
}
