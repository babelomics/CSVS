package org.babelomics.csvs.lib.io;

import com.google.common.collect.Lists;
import com.mongodb.*;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.models.feature.Region;

import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSQueryManager {

    final Datastore datastore;
    private static final String EMPTY_TERM = "EMPTY SO TERMS" ;
    final int DECIMAL_POSITIONS = 3;
    private Map<String, String> mapConfig = new HashMap<>();
    public static final String NUM_MAX_QUERY = "NUM_MAX_QUERY";
    public static final String NUM_MAX_MINUT = "NUM_MAX_MINUT";
    public static final String SIZE_REGION_MAX = "SIZE_REGION_MAX";
    public static final String SIZE_GENE_MAX = "SIZE_GENE_MAX";
    public static final String SIZE_SNP_HGVS_MAX = "SIZE_SNP_HGVS_MAX";


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

    public void setParametersConfig( Map<String, String> mapConfig){
        this.mapConfig = mapConfig;
    }
    public Integer getParConfig(String key){
        if (!this.mapConfig.containsKey(key))
            return -1;
        else
            return Integer.parseInt(this.mapConfig.get(key));
    }


    /**
     * Check num querys and time last query.
     * @param logQuery
     * @return
     */

    public  String  checkLogQuery(LogQuery logQuery) {
        String result = "";
        if (getParConfig(SIZE_REGION_MAX) != -1 && logQuery.getRegion().size() > 0) {
            int sumBase = 0;
            List<String> regions = logQuery.getRegion();
            for (int i = 0; i < regions.size() ; i++) {
                Region r = new Region(regions.get(i));
                sumBase = sumBase + r.getEnd()-r.getStart();
            }
            // num genes + cromosomal location + snpid
            if (sumBase > getParConfig(SIZE_GENE_MAX) * getParConfig(SIZE_REGION_MAX)  + getParConfig(SIZE_REGION_MAX) + getParConfig(SIZE_SNP_HGVS_MAX) )
                result = "The total size of all provided regions can't exceed " + getParConfig(SIZE_REGION_MAX) + " positions";
        }
        if (getParConfig(SIZE_SNP_HGVS_MAX) != -1 && (logQuery.getProteinsList().size() + logQuery.getCdnasList().size()) > getParConfig(SIZE_SNP_HGVS_MAX) )
            result = "The maximum number of HGVSc/HGVS is " + getParConfig(SIZE_SNP_HGVS_MAX)+ ".";


        if (getParConfig(NUM_MAX_QUERY) != -1 && getParConfig(NUM_MAX_MINUT) != -1 ) {
            long expiremilis = getParConfig(NUM_MAX_MINUT);

            DBObject obj = new BasicDBObject();
            obj.put("userId", logQuery.getUserId());
            Date firstDate = new Date();
            obj.put("date", new BasicDBObject("$lte", new Date(logQuery.getDate().getTime() - expiremilis)));
            datastore.getCollection(LogQuery.class).remove(obj);

            Iterator ilistLogQueryUser = datastore.createQuery(LogQuery.class).field("userId").equal(logQuery.getUserId()).iterator();

            List<String> newQuerysRegion = logQuery.getRegion();
            List<String> newQuerysCdna = logQuery.getCdnasList();
            List<String> newQuerysProteins = logQuery.getProteinsList();

            int numLogQueryUser = 0;

            while (ilistLogQueryUser.hasNext() && !newQuerysRegion.isEmpty()) {
                LogQuery oObj = (LogQuery) ilistLogQueryUser.next();
                firstDate = oObj.getDate();

                // Delete querys repeats
                Lists.newArrayList(oObj.getRegion()).stream().forEach(x -> newQuerysRegion.remove(x));
                Lists.newArrayList(oObj.getCdnasList()).stream().forEach(x -> newQuerysCdna.remove(x));
                Lists.newArrayList(oObj.getProteinsList()).stream().forEach(x -> newQuerysProteins.remove(x));

                numLogQueryUser++;
            }

            // Add new query
            if (newQuerysRegion.size() > 0 || newQuerysCdna.size() > 0 || newQuerysProteins.size() > 0) {
                if (numLogQueryUser >= getParConfig(NUM_MAX_QUERY)) {
                    Duration d = Duration.between(  logQuery.getDate().toInstant()  , new Date(firstDate.getTime() + expiremilis).toInstant() );
                    long minutesPart = d.toMinutes();
                    long secondsPart = d.minusMinutes( minutesPart ).getSeconds() ;
                    long min= getParConfig(NUM_MAX_MINUT)/60000;
                    result = "You can't make more than " + getParConfig(NUM_MAX_QUERY) + " queries distinct in " + min + " minutes. The time remaining to search is " + (minutesPart != 0 ? minutesPart+" minutes ": "") + (secondsPart != 0 ? secondsPart+" seconds ": "") + ".";
                } else {
                    logQuery.setRegion(newQuerysRegion);
                    datastore.save(logQuery);
                }
            }
        }

        return result;
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
            Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
            Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
            Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
            int sampleCount = calculateSampleCount(sampleCountMap);
            int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
            int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

            DiseaseCount diseaseCount = calculateStats(res, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY);

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
        return getVariantsByRegionList(regions,diseaseIds,technologyIds,skip,limit,skipCount,count, null, null, null);
    }

    public Iterable<Variant> getVariantsByRegionList(List<Region> regions, List<Integer> diseaseIds, List<Integer> technologyIds, Integer skip, Integer limit, boolean skipCount, MutableLong count,
                                                     List<String> cdnas, List<String> proteins, List<String> consequenceTypes) {

        int numCriteria = regions.size();

        List<String> listP = new ArrayList<>();
        List<String> listC = new ArrayList<>();

        List<Criteria> hgvs = new ArrayList<Criteria>();
        List<String> ann_c_p = new ArrayList<String>();
        // Add new filters cdnas and aa
        if (cdnas != null && !cdnas.isEmpty()) {

            listC = cdnas.stream().filter(c -> c.startsWith("ENS")).collect(Collectors.toList());
            if (listC != null && listC.size() > 0) {
                numCriteria++;
                //hgvs.add(query.criteria("annot-s.hgvsc").in(listC));
            }

            List<String> ann_c = cdnas.stream().filter(c -> !c.startsWith("ENS")).collect(Collectors.toList());
            if (ann_c != null && ann_c.size() > 0) {
                ann_c_p.addAll(ann_c);
            }
        }
        if (proteins != null && !proteins.isEmpty()) {
            //hgvs.add(query.criteria("annots.hgvsp").in(proteins));
            listP = proteins.stream().filter(p -> p.startsWith("ENS")).collect(Collectors.toList());
            if (listP != null && listP.size() > 0) {
                //hgvs.add(query.criteria("annots.hgvsc").in(listP));
                numCriteria++;
            }

            List<String> ann_p = proteins.stream().filter(p -> !p.startsWith("ENS")).collect(Collectors.toList());
            if (ann_p != null && ann_p.size() > 0) {
                ann_c_p.addAll(ann_p);
            }
        }

        //if (hgvs.size()  > 0) {
            //if (hgvs.size()  == 1)
            //    query.and(hgvs.get(0));
           // else
          //      query.or((Criteria[])hgvs.toArray());
        //}



        Criteria[] or = new Criteria[numCriteria];

        int i = 0;
        for (Region r : regions) {
            // List<String> chunkIds = getChunkIds(r);
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            List<Criteria> and = new ArrayList<>();
            // and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));

            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }


        Query<Variant> query = this.datastore.createQuery(Variant.class);

        if (listC != null && listC.size() > 0) {
            or[i++] = query.criteria("annots.hgvsc").in(listC);
        }
        if (listP != null && listP.size() > 0) {
            or[i++] = query.criteria("annots.hgvsp").in(listP);
        }

        if (or.length > 0)
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



        if (ann_c_p != null && !ann_c_p.isEmpty()) {
            query.filter("annots.ann_c_p", new BasicDBObject("$in", ann_c_p));
        }

        if (consequenceTypes != null && !consequenceTypes.isEmpty()) {
            Optional op = consequenceTypes.stream()
                    .filter(c -> EMPTY_TERM.contains(c.toUpperCase()))
                    .findFirst();
            if (op.isPresent()){
                query.or(query.criteria("annots.ct").doesNotExist(), query.criteria("annots.ct").in(consequenceTypes) );
            } else {
                query.filter("annots.ct", new BasicDBObject("$in", consequenceTypes));
            }
        }

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }


       // System.out.println("query = " + query);

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
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
        Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
        Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
        int sampleCount = calculateSampleCount(sampleCountMap);
        int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
        int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

        for (Variant v : aux) {
            v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY));
            v.setDiseases(null);
            res.add(v);
        }

        return res;
    }

    /**
     * Get variants by rs
     * @param rsList
     */
    public List<Variant> getVariantsByRsList(List<String> rsList) {

        Query<Variant> query = this.datastore.createQuery(Variant.class);
        List<Variant> res = new ArrayList<>();


        if (rsList != null && rsList.size() > 0) {
            query.criteria("rs").in(rsList);
        }

        //System.out.println("query = " + query);

        Iterable<Variant> aux = query.fetch();
        for (Variant v : aux) {
            res.add(v);
        }
        return res;
    }

    /**
     * Get saturation order by num variants new / num samples disease
     * @param regions
     * @param diseaseIdsOriginal
     * @param technologyIds
     * @return
     */
    public Map<Region, List<SaturationElement>> getSaturationOrderIncrement(List<Region> regions, List<Integer> diseaseIdsOriginal, List<Integer> technologyIds) {

        Map<Region, List<SaturationElement>> map = new LinkedHashMap<>();

        List<DiseaseGroup> diseaseOrder = getAllDiseaseGroupsOrderedBySample();

        for (Region r : regions) {
            List<Integer> diseaseIds = new ArrayList<> (diseaseIdsOriginal);
            List<String> chunkIds = getChunkIds(r);

            List<SaturationElement> list = new ArrayList<>();
            Map<Integer, Integer> diseaseCount = new HashMap<>();

            // Get Variants
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);
            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));
            Query<Variant> query = this.datastore.createQuery(Variant.class);
            query.and(and.toArray(new Criteria[and.size()]));

            // Get Panels
            List<Criteria> andPanels = new ArrayList<>();
            Query<org.babelomics.csvs.lib.models.Region> auxQueryPanels = this.datastore.createQuery(org.babelomics.csvs.lib.models.Region.class);
            andPanels.add(auxQueryPanels.criteria("c").equal(r.getChromosome()));
            andPanels.add(auxQueryPanels.criteria("e").greaterThanOrEq(r.getStart()));
            andPanels.add(auxQueryPanels.criteria("s").lessThanOrEq(r.getEnd()));
            Query<org.babelomics.csvs.lib.models.Region> queryRegion = this.datastore.createQuery(org.babelomics.csvs.lib.models.Region.class);
            queryRegion.and(andPanels.toArray(new Criteria[andPanels.size()]));
            List panelsRegions = this.datastore.getCollection(Region.class).distinct("pid", queryRegion.getQueryObject());

            List<Integer> diseaseView = new ArrayList<>();

            while (diseaseIds.size() > 0) {
                // Order disease
                Map<Integer, Long> mapDiseaseIncrement = new HashMap<>();
                Map<Integer, Double> mapDiseaseIncrementSample = new HashMap<>();
                Map<Integer, Integer> mapDiseaseSample = new HashMap<>();
                int sumAcum = list.stream().mapToInt(o -> o.getCount()).sum();

                // Calculate the largest increase ( num variant increase / num samples disease) by disease, and select the largest
                diseaseIds.forEach(dId -> {
                   // if (! diseaseView.contains(dId)) {
                        // Num variant when add a disease                        
                        Query<Variant> queryDisease = this.datastore.createQuery(Variant.class);
                        queryDisease.disableValidation();
                        queryDisease.and(and.toArray(new Criteria[and.size()]));
                        BasicDBList listDBObjectsDisease = new BasicDBList();
                        listDBObjectsDisease.add(new BasicDBObject("dgid", new BasicDBObject("$in", ListUtils.union(diseaseView, Arrays.asList(dId)))));
                        if (technologyIds != null && !technologyIds.isEmpty()) {
                            listDBObjectsDisease.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
                        }
                        queryDisease.filter("diseases elem", new BasicDBObject("$and", listDBObjectsDisease));

                        // Num samples when add a disease (genome+exome+panels)
                        int samplesUnionPanels = 0;
                        Query<File> querySampleDisease = this.datastore.createQuery(File.class);
                        querySampleDisease.disableValidation();
                        querySampleDisease.filter("dgid in ",  Arrays.asList(dId));
                        if (technologyIds != null && !technologyIds.isEmpty()) {
                            querySampleDisease.filter("tid in ", technologyIds);
                        }
                        if (panelsRegions != null && !panelsRegions.isEmpty() && panelsRegions.size() > 0) {
                            Query<File> auxQueryPid = this.datastore.createQuery(org.babelomics.csvs.lib.models.File.class);
                            querySampleDisease.or(auxQueryPid.criteria("pid").in(panelsRegions),auxQueryPid.criteria("pid").doesNotExist() );
                        } else{
                            querySampleDisease.criteria("pid").doesNotExist();
                        }
                        List<File> sampl = querySampleDisease.asList();
                        if (querySampleDisease != null && sampl.size() > 0)
                            samplesUnionPanels = sampl.stream().mapToInt(f -> f.getSamples()).sum();


                        // Increment variantes / num. samples disease
                        mapDiseaseIncrementSample.put(dId, samplesUnionPanels > 0 ? ((double) queryDisease.countAll() - sumAcum)/ samplesUnionPanels : 0);
                        mapDiseaseIncrement.put(dId, (queryDisease.countAll()- sumAcum));
                        mapDiseaseSample.put(dId, samplesUnionPanels);

                    //}
                });

                // Order
                LinkedHashMap<Integer, Double> sortedMap =
                        mapDiseaseIncrementSample.entrySet().stream().
                                filter(line -> !diseaseView.contains(line.getKey())).
                                sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                                collect(Collectors.toMap(e -> e.getKey(), (Map.Entry<Integer, Double> e) -> e.getValue(),
                                        (v1, v2) -> v2, LinkedHashMap::new));

                Integer key = sortedMap.keySet().iterator().next();

                // Select first
                Optional<DiseaseGroup> dgFirst = diseaseOrder.stream()
                        .filter(dg -> key.equals(dg.getGroupId()))
                        .findFirst();

                if (dgFirst.isPresent()) {
                    diseaseView.add(key);
                    diseaseIds.remove(key);
                    long increment = Integer.parseInt(String.valueOf(mapDiseaseIncrement.get(key))) ;
                    list.add(new SaturationElement(
                            key,
                            increment > 0 ? Integer.parseInt(String.valueOf(mapDiseaseIncrement.get(key))) : 0,
                            /// gest sample calculate ( genome + beds)
                            mapDiseaseSample.get(key)

                    ));
                }
            }

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

        // Map with disease-technology-samples
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
        Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
        Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
        int sampleCount = calculateSampleCount(sampleCountMap);
        int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
        int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

        Iterable<Variant> aux = query.fetch();
        Iterable<Variant> customIterable = new AllVariantsIterable(aux, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY);

        return customIterable;
    }

    /**
     * Calculate sum of all samples.
     * @return
     */
    public int calculateSampleCount() {
        int res = 0;

        BasicDBObject g = new BasicDBObject("_id", "");
        g.put("total", new BasicDBObject("$sum", "$s"));

        BasicDBObject p = new BasicDBObject("_id", 0);
        p.put("total", "$total");

        BasicDBObject group = new BasicDBObject("$group", g);
        BasicDBObject project = new BasicDBObject("$project", p);

        DBCollection collection = datastore.getCollection(File.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(group);
        aggList.add(project);

        AggregationOutput aggregation = collection.aggregate(aggList);

        for (DBObject fileObj : aggregation.results()) {
            res = (Integer) fileObj.get("total");
        }
        return res;
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
    public Map<String, Integer> calculateSampleCount(List<Integer> diseaseId, List<Integer> technologyId, String gender) {
        BasicDBList listDBObjects = new BasicDBList();

        listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseId)));
        listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyId)));


        listDBObjects.add(new BasicDBObject("pid", new BasicDBObject("$eq", null)));
        if (gender != null && !"".equals(gender))
            listDBObjects.add(new BasicDBObject("gender", new BasicDBObject("$eq", gender)));
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

        List objtIdRegion = new ArrayList<>();
        BasicDBObject filter = new BasicDBObject();
        filter.append("c", v.getChromosome());
        filter.append("s", new BasicDBObject("$lte",v.getPosition()));
        filter.append("e", new BasicDBObject("$gte",v.getPosition()));
        objtIdRegion = datastore.getCollection(org.babelomics.csvs.lib.models.Region.class).distinct("pid", filter);

        if (objtIdRegion != null && objtIdRegion.size() > 0) {
            // Replace with calculateSampleRegions
            List<BasicDBObject> aggList = new ArrayList<>();
            BasicDBObject match = new BasicDBObject().append("pid", new BasicDBObject("$in", objtIdRegion)).append("dgid",diseaseId).append("tid", technologyId);
            BasicDBObject group = new BasicDBObject().append("_id", new BasicDBObject().append("dgid", "$dgid").append("tid","$tid")).append("samples",new BasicDBObject("$sum","$s"));
            BasicDBObject project = new BasicDBObject().append("_id", 0).append("samples","1");
            aggList.add(new BasicDBObject("$match", match));
            aggList.add(new BasicDBObject("$group", group));

            Iterator aggregation = datastore.getCollection(File.class).aggregate(aggList).results().iterator();

            if(aggregation.hasNext()){
                BasicDBObject oObj = (BasicDBObject) aggregation.next();
                res = (int) oObj.get("samples");
            }
        }

        return res;
    }






    public static Map calculateSampleRegions(Datastore datastore) {
        Map<String, Map> result = new HashMap<>();

        String[]  listGender = {"","XX","XY"};
        for ( String gender : listGender) {
            List<BasicDBObject> aggList = new ArrayList<>();
            BasicDBObject match = new BasicDBObject().append("pid", new BasicDBObject("$exists", true));
            BasicDBObject group = new BasicDBObject().append("_id", new BasicDBObject().append("dgid", "$dgid").append("tid", "$tid")
                    .append("pid", "$pid")).append("samples", new BasicDBObject("$sum", "$s"));
            if (!"".equals(gender))
                match.append("gender", gender);
            //BasicDBObject project = new BasicDBObject().append("_id", "$_id.pid").append("samples","1");

            aggList.add(new BasicDBObject("$match", match));
            aggList.add(new BasicDBObject("$group", group));

            Iterator aggregation = datastore.getCollection(File.class).aggregate(aggList).results().iterator();

            while (aggregation.hasNext()) {
                BasicDBObject oObj = (BasicDBObject) aggregation.next();
                String key = ((Map) oObj.get("_id")).get("dgid") + "_" + ((Map) oObj.get("_id")).get("tid") + (!"".equals(gender) ? "_"+ gender : "");
                if (result.containsKey(key)) {
                    Map value = result.get(key);
                    value.put(((Map) oObj.get("_id")).get("pid"), (int) oObj.get("samples"));
                    result.put(key, value);
                } else {
                    Map value = new HashMap();
                    value.put(((Map) oObj.get("_id")).get("pid"), (int) oObj.get("samples"));
                    result.put(key, value);
                }
            }
        }

        return result;
    }





    private DiseaseCount calculateStats(Variant v, List<Integer> diseaseId, List<Integer> technologyId, int sampleCount, Map<String, Integer> sampleCountMap,
                                        int sampleCount_XX,  Map<String, Integer> sampleCountMap_XX, int sampleCount_XY, Map<String, Integer> sampleCountMap_XY) {
        DiseaseCount dc;

        int gt00 = 0;
        int gt01 = 0;
        int gt11 = 0;
        int gtmissing = 0;
        int sampleCountVariant = 0;
        Map<String, Integer> sampleCountTemp = new HashMap<>();
        boolean existsRegions = false;


        if("X".equals(v.getChromosome())){
            sampleCountTemp =  new HashMap<>(sampleCountMap_XX);
            for(String key : sampleCountMap_XY.keySet()){
                sampleCountTemp.put(key, sampleCountTemp.containsKey(key)? sampleCountTemp.get(key)+sampleCountMap_XY.get(key):sampleCountMap_XY.get(key));
            }

        } else {
            if("Y".equals(v.getChromosome()))
                sampleCountTemp =  new HashMap<>(sampleCountMap_XY);
            else
                sampleCountTemp =  new HashMap<>(sampleCountMap);
        }

        // Variants by regions
        // System.out.println("\nCSVS (calculateStats): Variant= "+ v +  " Samples: "  + sampleCountTemp);
        for (DiseaseCount auxDc : v.getDiseases()) {
            if (diseaseId.contains(auxDc.getDiseaseGroup().getGroupId()) && technologyId.contains(auxDc.getTechnology().getTechnologyId())) {
                gt00 += auxDc.getGt00();
                gt01 += auxDc.getGt01();
                gt11 += auxDc.getGt11();
                gtmissing += auxDc.getGtmissing();
            }
        }

        // exists samples load in the panel
        if (v.getDiseasesSamplePanel() != null) {
            for (DiseaseSum auxDs : v.getDiseasesSamplePanel()) {
                if (diseaseId.contains(auxDs.getDiseaseGroupId()) && technologyId.contains(auxDs.getTechnologyId())) {
                    switch (v.getChromosome()){
                        case "X":
                            // exists samples load in the panel XX + XY
                            if (auxDs.getSumSampleRegionsXX() != 0 || auxDs.getSumSampleRegionsXY() != 0 ) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap_XX.containsKey(key) ? sampleCountMap.get(key) : 0;
                                sum = sampleCountMap_XY.containsKey(key) ? sum + sampleCountMap_XY.get(key) : sum;
                                sampleCountTemp.put(key, (auxDs.getSumSampleRegionsXX() != 0 ? auxDs.getSumSampleRegionsXX(): 0) + (auxDs.getSumSampleRegionsXY() != 0 ? auxDs.getSumSampleRegionsXY(): 0) + sum);
                                existsRegions = true;
                            } else{
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                sampleCountTemp.put(key, 0);
                                existsRegions = true;
                            }
                            break;
                        case "Y":
                            // exists samples load in the panel XY
                            if (auxDs.getSumSampleRegionsXY() != 0) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap_XY.containsKey(key) ? sampleCountMap_XY.get(key) : 0;
                                sampleCountTemp.put(key, auxDs.getSumSampleRegionsXY() + sum);
                                existsRegions = true;
                            }
                            break;
                        default:
                            // exists samples load in the panel (All)
                            if (auxDs.getSumSampleRegions() != 0) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap.containsKey(key) ? sampleCountMap.get(key) : 0;
                                sampleCountTemp.put(key, auxDs.getSumSampleRegions() + sum);
                                existsRegions = true;
                            }
                    }
                }
            }
        }

        if (existsRegions)
            sampleCountVariant = sampleCountTemp.values().stream().mapToInt(i -> i.intValue()).sum();
        else {
            if("X".equals(v.getChromosome())){
                sampleCountVariant = sampleCount_XX + sampleCount_XY;
            }else{
                if("Y".equals(v.getChromosome()))
                    sampleCountVariant = sampleCount_XY;
                else
                    sampleCountVariant = sampleCount;
            }
        }


        gt00 = sampleCountVariant - gt01 - gt11 - gtmissing;

        float refFreq = 0;
        float altFreq;
        switch (v.getChromosome()) {
            case "X":
                //(mujeres01+mujeres00*2+hombres01+hombres00)/(mujeres totales * 2 + hombres totales)
                //refFreq = (float) (gt01 + gt00*2 + gt01) / ( 2*(gt00+gt01+gt11) + gt00 + gt01);
                altFreq = (float) (gt01 + gt11*2 + gt01) / ( 2*(gt00+gt01+gt11) + gt00 + gt01);
                refFreq = (float) 1 - altFreq;
                break;

            case "Y":
                refFreq = (float) gt00 / (gt00 + gt11);
                altFreq = (float) gt11 / (gt00 + gt11);
                break;

            default:
                int refCount, altCount;
                refCount = gt00 * 2 + gt01;
                altCount = gt11 * 2 + gt01;
                refFreq = (float) refCount / (refCount + altCount);
                altFreq = (float) altCount / (refCount + altCount);
        }

        float maf = Math.min(refFreq, altFreq);

        dc = new DiseaseCount(null, null, gt00, gt01, gt11, gtmissing);
 
        if (!Float.isNaN(refFreq) && !Float.isInfinite(refFreq)) {
            dc.setRefFreq(round(refFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(altFreq) && !Float.isInfinite(altFreq)) {
            dc.setAltFreq(round(altFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(maf) && !Float.isInfinite(maf)) {
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
        private int sampleCount_XX;
        private int sampleCount_XY;
        private Map<String, Integer> sampleCountMap;
        private Map<String, Integer> sampleCountMap_XX;
        private Map<String, Integer> sampleCountMap_XY;

        public AllVariantsIterable(Iterable iterable, List<Integer> diseaseIds, List<Integer> technologyIds, int sampleCount, Map<String, Integer> sampleCountMap,
                                   int sampleCount_XX, Map<String, Integer> sampleCountMap_XX, int sampleCount_XY, Map<String, Integer> sampleCountMap_XY) {
            this.iterable = iterable;
            this.diseaseIds = diseaseIds;
            this.technologyIds = technologyIds;
            this.sampleCount = sampleCount;
            this.sampleCount_XX = sampleCount_XX;
            this.sampleCount_XY = sampleCount_XY;
            this.sampleCountMap = sampleCountMap;
            this.sampleCountMap_XX = sampleCountMap_XX;
            this.sampleCountMap_XY = sampleCountMap_XY;
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
                v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY));
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
