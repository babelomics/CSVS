package org.babelomics.pvs.lib.mongodb.dbAdaptor;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.babelomics.pvs.lib.mongodb.converter.PVSDBObjectToVariantConverter;
import org.babelomics.pvs.lib.mongodb.converter.PVSDBObjectToVariantSourceEntryConverter;
import org.babelomics.pvs.lib.mongodb.converter.PVSDBObjectToVariantStatsConverter;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.datastore.mongodb.MongoDBCollection;
import org.opencb.datastore.mongodb.MongoDBConfiguration;
import org.opencb.datastore.mongodb.MongoDataStore;
import org.opencb.datastore.mongodb.MongoDataStoreManager;
import org.opencb.opencga.lib.auth.MongoCredentials;
import org.opencb.opencga.storage.variant.VariantDBAdaptor;
import org.opencb.opencga.storage.variant.mongodb.*;

import java.net.UnknownHostException;
import java.util.*;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSVariantMongoDBAdaptor implements VariantDBAdaptor {

    private final MongoDataStoreManager mongoManager;
    private final MongoDataStore db;
    private final PVSDBObjectToVariantConverter variantConverter;
    private final PVSDBObjectToVariantSourceEntryConverter archivedVariantFileConverter;
    private final String collectionName = "variants";

    public PVSVariantMongoDBAdaptor(MongoCredentials credentials) throws UnknownHostException {
        // Mongo configuration
        mongoManager = new MongoDataStoreManager(credentials.getMongoHost(), credentials.getMongoPort());
        MongoDBConfiguration mongoDBConfiguration = MongoDBConfiguration.builder()
                .add("username", credentials.getUsername())
                .add("password", credentials.getPassword() != null ? new String(credentials.getPassword()) : null).build();
        db = mongoManager.get(credentials.getMongoDbName(), mongoDBConfiguration);

        // Converters from DBObject to Java classes
        // TODO Allow to configure depending on the type of study?
        archivedVariantFileConverter = new PVSDBObjectToVariantSourceEntryConverter(true,
                new PVSDBObjectToVariantStatsConverter(), credentials);
        variantConverter = new PVSDBObjectToVariantConverter(archivedVariantFileConverter);
    }

    @Override
    public QueryResult getAllVariantsByRegion(Region region, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        QueryBuilder qb = QueryBuilder.start();
        getRegionFilter(region, qb);
        parseQueryOptions(options, qb);

        return coll.find(qb.get(), options, variantConverter);
    }

    @Override
    public List<QueryResult> getAllVariantsByRegionList(List<Region> regions, QueryOptions options) {
        List<QueryResult> allResults = new LinkedList<>();
        // If the user asks to merge the results, run only one query,
        // otherwise delegate in the method to query regions one by one
        if (options.getBoolean("merge", false)) {
            MongoDBCollection coll = db.getCollection(collectionName);
            QueryBuilder qb = QueryBuilder.start();
            getRegionFilter(regions, qb);
            parseQueryOptions(options, qb);
            allResults.add(coll.find(qb.get(), options, variantConverter));
        } else {
            for (Region r : regions) {
                QueryResult queryResult = getAllVariantsByRegion(r, options);
                allResults.add(queryResult);
            }
        }
        return allResults;
    }


    @Override
    public QueryResult getAllVariantsByRegionAndStudies(Region region, List<String> studyId, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        // Aggregation for filtering when more than one study is present
        QueryBuilder qb = QueryBuilder.start(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STUDYID_FIELD).in(studyId);
        getRegionFilter(region, qb);
        parseQueryOptions(options, qb);

        DBObject match = new BasicDBObject("$match", qb.get());
        DBObject unwind = new BasicDBObject("$unwind", "$" + DBObjectToVariantConverter.FILES_FIELD);
        DBObject match2 = new BasicDBObject("$match",
                new BasicDBObject(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STUDYID_FIELD,
                        new BasicDBObject("$in", studyId)));

        return coll.aggregate("$variantsRegionStudies", Arrays.asList(match, unwind, match2), options);
    }

    @Override
    public QueryResult getVariantsHistogramByRegion(Region region, QueryOptions options) {
        // db.variants.aggregate( { $match: { $and: [ {chr: "1"}, {start: {$gt: 251391, $lt: 2701391}} ] }},
        //                        { $group: { _id: { $subtract: [ { $divide: ["$start", 20000] }, { $divide: [{$mod: ["$start", 20000]}, 20000] } ] },
        //                                  totalCount: {$sum: 1}}})
        MongoDBCollection coll = db.getCollection(collectionName);

        int interval = options.getInt("interval", 20000);

        BasicDBObject start = new BasicDBObject("$gt", region.getStart());
        start.append("$lt", region.getEnd());

        BasicDBList andArr = new BasicDBList();
        andArr.add(new BasicDBObject(DBObjectToVariantConverter.CHROMOSOME_FIELD, region.getChromosome()));
        andArr.add(new BasicDBObject(DBObjectToVariantConverter.START_FIELD, start));

        DBObject match = new BasicDBObject("$match", new BasicDBObject("$and", andArr));


        BasicDBList divide1 = new BasicDBList();
        divide1.add("$start");
        divide1.add(interval);

        BasicDBList divide2 = new BasicDBList();
        divide2.add(new BasicDBObject("$mod", divide1));
        divide2.add(interval);

        BasicDBList subtractList = new BasicDBList();
        subtractList.add(new BasicDBObject("$divide", divide1));
        subtractList.add(new BasicDBObject("$divide", divide2));


        BasicDBObject substract = new BasicDBObject("$subtract", subtractList);

        DBObject totalCount = new BasicDBObject("$sum", 1);

        BasicDBObject g = new BasicDBObject("_id", substract);
        g.append("features_count", totalCount);
        DBObject group = new BasicDBObject("$group", g);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id", 1));

//        logger.info("getAllIntervalFrequencies - (>·_·)>");
        System.out.println(options.toString());

        System.out.println(match.toString());
        System.out.println(group.toString());
        System.out.println(sort.toString());

        long dbTimeStart = System.currentTimeMillis();
        QueryResult output = coll.aggregate("$histogram", Arrays.asList(match, group, sort), options);
        long dbTimeEnd = System.currentTimeMillis();

//        System.out.println(output.getCommand());

        Map<Long, DBObject> ids = new HashMap<>();
        // Create DBObject for intervals with features inside them
        for (DBObject intervalObj : (List<DBObject>) output.getResult()) {
            Long _id = Math.round((Double) intervalObj.get("_id"));//is double

            DBObject intervalVisited = ids.get(_id);
            if (intervalVisited == null) {
                intervalObj.put("_id", _id);
                intervalObj.put("start", getChunkStart(_id.intValue(), interval));
                intervalObj.put("end", getChunkEnd(_id.intValue(), interval));
                intervalObj.put("chromosome", region.getChromosome());
                intervalObj.put("features_count", Math.log((int) intervalObj.get("features_count")));
                ids.put(_id, intervalObj);
            } else {
                Double sum = (Double) intervalVisited.get("features_count") + Math.log((int) intervalObj.get("features_count"));
                intervalVisited.put("features_count", sum.intValue());
            }
        }

        // Create DBObject for intervals without features inside them
        BasicDBList resultList = new BasicDBList();
        int firstChunkId = getChunkId(region.getStart(), interval);
        int lastChunkId = getChunkId(region.getEnd(), interval);
        DBObject intervalObj;
        for (int chunkId = firstChunkId; chunkId <= lastChunkId; chunkId++) {
            intervalObj = ids.get((long) chunkId);
            if (intervalObj == null) {
                intervalObj = new BasicDBObject();
                intervalObj.put("_id", chunkId);
                intervalObj.put("start", getChunkStart(chunkId, interval));
                intervalObj.put("end", getChunkEnd(chunkId, interval));
                intervalObj.put("chromosome", region.getChromosome());
                intervalObj.put("features_count", 0);
            }
            resultList.add(intervalObj);
        }

        QueryResult queryResult = new QueryResult(region.toString(), ((Long) (dbTimeEnd - dbTimeStart)).intValue(),
                resultList.size(), resultList.size(), null, null, resultList);

        return queryResult;
    }


    @Override
    public QueryResult getAllVariantsByGene(String geneName, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        QueryBuilder qb = QueryBuilder.start("_at.gn").all(Arrays.asList(geneName));
        parseQueryOptions(options, qb);
        return coll.find(qb.get(), options, variantConverter);
    }

    @Override
    public QueryResult getMostAffectedGenes(int numGenes, QueryOptions options) {
        return getGenesRanking(numGenes, -1, options);
    }

    @Override
    public QueryResult getLeastAffectedGenes(int numGenes, QueryOptions options) {
        return getGenesRanking(numGenes, 1, options);
    }

    private QueryResult getGenesRanking(int numGenes, int order, QueryOptions options) {
        // db.variants.aggregate( { $project : { genes : "$_at.gn"} },
        //                        { $unwind : "$genes"},
        //                        { $group : { _id : "$genes", count: { $sum : 1 } }},
        //                        { $sort : { "count" : -1 }},
        //                        { $limit : 10 } )
        MongoDBCollection coll = db.getCollection(collectionName);

        QueryBuilder qb = QueryBuilder.start();
        parseQueryOptions(options, qb);

        DBObject match = new BasicDBObject("$match", qb.get());
        DBObject project = new BasicDBObject("$project", new BasicDBObject("genes", "$_at.gn"));
        DBObject unwind = new BasicDBObject("$unwind", "$genes");
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$genes").append("count", new BasicDBObject("$sum", 1)));
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("count", order)); // 1 = ascending, -1 = descending
        DBObject limit = new BasicDBObject("$limit", numGenes);

        return coll.aggregate("$effects.geneName", Arrays.asList(match, project, unwind, group, sort, limit), options);
    }


    @Override
    public QueryResult getTopConsequenceTypes(int numConsequenceTypes, QueryOptions options) {
        return getConsequenceTypesRanking(numConsequenceTypes, -1, options);
    }

    @Override
    public QueryResult getBottomConsequenceTypes(int numConsequenceTypes, QueryOptions options) {
        return getConsequenceTypesRanking(numConsequenceTypes, 1, options);
    }

    private QueryResult getConsequenceTypesRanking(int numConsequenceTypes, int order, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        QueryBuilder qb = QueryBuilder.start();
        parseQueryOptions(options, qb);

        DBObject match = new BasicDBObject("$match", qb.get());
        DBObject project = new BasicDBObject("$project", new BasicDBObject("so", "$_at.ct"));
        DBObject unwind = new BasicDBObject("$unwind", "$so");
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$so").append("count", new BasicDBObject("$sum", 1)));
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("count", order)); // 1 = ascending, -1 = descending
        DBObject limit = new BasicDBObject("$limit", numConsequenceTypes);

        return coll.aggregate("$effects.so", Arrays.asList(match, project, unwind, group, sort, limit), options);
    }


    @Override
    public QueryResult getVariantById(String id, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        BasicDBObject query = new BasicDBObject(DBObjectToVariantConverter.ID_FIELD, id);
        return coll.find(query, options, variantConverter);
    }

    @Override
    public List<QueryResult> getVariantsByIdList(List<String> ids, QueryOptions options) {
        List<QueryResult> allResults = new LinkedList<>();
        for (String r : ids) {
            QueryResult queryResult = getVariantById(r, options);
            allResults.add(queryResult);
        }
        return allResults;
    }

    @Override
    public boolean close() {
        mongoManager.close(db.getDatabaseName());
        return true;
    }


    private QueryBuilder parseQueryOptions(QueryOptions options, QueryBuilder builder) {
        if (options != null) {
            if (options.containsKey("region") && !options.getString("region").isEmpty()) {
                getRegionFilter(Region.parseRegion(options.getString("region")), builder);
            }

            if (options.containsKey("type") && !options.getString("type").isEmpty()) {
                getVariantTypeFilter(options.getString("type"), builder);
            }

            if (options.containsKey("reference") && !options.getString("reference").isEmpty()) {
                getReferenceFilter(options.getString("reference"), builder);
            }

            if (options.containsKey("alternate") && !options.getString("alternate").isEmpty()) {
                getAlternateFilter(options.getString("alternate"), builder);
            }

            if (options.containsKey("effect") && !options.getList("effect").isEmpty() && !options.getListAs("effect", String.class).get(0).isEmpty()) {
                getEffectFilter(options.getListAs("effect", String.class), builder);
            }

            if (options.containsKey("studies") && !options.getList("studies").isEmpty() && !options.getListAs("studies", String.class).get(0).isEmpty()) {
                System.out.println("# studies = " + options.getList("studies").size());
                getStudyFilter(options.getListAs("studies", String.class), builder);
            }

            if (options.containsKey("files") && !options.getList("files").isEmpty() && !options.getListAs("files", String.class).get(0).isEmpty()) {
                System.out.println("# files = " + options.getList("files").size());
                getFileFilter(options.getListAs("files", String.class), builder);
            }

            if (options.containsKey("maf") && options.containsKey("opMaf")
                    && options.getFloat("maf") >= 0 && !options.getString("opMaf").isEmpty()) {
                getMafFilter(options.getFloat("maf"), ComparisonOperator.fromString(options.getString("opMaf")), builder);
            }

            if (options.containsKey("missingAlleles") && options.containsKey("opMissingAlleles")
                    && options.getInt("missingAlleles") >= 0 && !options.getString("opMissingAlleles").isEmpty()) {
                getMissingAllelesFilter(options.getInt("missingAlleles"), ComparisonOperator.fromString(options.getString("opMissingAlleles")), builder);
            }

            if (options.containsKey("missingGenotypes") && options.containsKey("opMissingGenotypes")
                    && options.getInt("missingGenotypes") >= 0 && !options.getString("opMissingGenotypes").isEmpty()) {
                getMissingGenotypesFilter(options.getInt("missingGenotypes"), ComparisonOperator.fromString(options.getString("opMissingGenotypes")), builder);
            }

        }

        return builder;
    }

    private QueryBuilder getRegionFilter(Region region, QueryBuilder builder) {
        List<String> chunkIds = getChunkIds(region);
        builder.and("_at.chunkIds").in(chunkIds);
        builder.and(DBObjectToVariantConverter.END_FIELD).greaterThanEquals(region.getStart());
        builder.and(DBObjectToVariantConverter.START_FIELD).lessThanEquals(region.getEnd());
        return builder;
    }

    private QueryBuilder getRegionFilter(List<Region> regions, QueryBuilder builder) {
        DBObject[] objects = new DBObject[regions.size()];

        int i = 0;
        for (Region region : regions) {
            List<String> chunkIds = getChunkIds(region);
            DBObject regionObject = new BasicDBObject("_at.chunkIds", new BasicDBObject("$in", chunkIds))
                    .append(DBObjectToVariantConverter.END_FIELD, new BasicDBObject("$gte", region.getStart()))
                    .append(DBObjectToVariantConverter.START_FIELD, new BasicDBObject("$lte", region.getEnd()));
            objects[i] = regionObject;
            i++;
        }
        builder.or(objects);
        return builder;
    }

    private QueryBuilder getReferenceFilter(String reference, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.REFERENCE_FIELD).is(reference);
    }

    private QueryBuilder getAlternateFilter(String alternate, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.ALTERNATE_FIELD).is(alternate);
    }

    private QueryBuilder getVariantTypeFilter(String type, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.TYPE_FIELD).is(type.toUpperCase());
    }

    private QueryBuilder getEffectFilter(List<String> effects, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.EFFECTS_FIELD + "." + DBObjectToVariantConverter.SOTERM_FIELD).in(effects);
    }

    private QueryBuilder getStudyFilter(List<String> studies, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STUDYID_FIELD).in(studies);
    }

    private QueryBuilder getFileFilter(List<String> files, QueryBuilder builder) {
        return builder.and(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceConverter.FILEID_FIELD).in(files);
    }

    private QueryBuilder getMafFilter(float maf, ComparisonOperator op, QueryBuilder builder) {
        return op.apply(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STATS_FIELD
                + "." + DBObjectToVariantStatsConverter.MAF_FIELD, maf, builder);
    }

    private QueryBuilder getMissingAllelesFilter(int missingAlleles, ComparisonOperator op, QueryBuilder builder) {
        return op.apply(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STATS_FIELD
                + "." + DBObjectToVariantStatsConverter.MISSALLELE_FIELD, missingAlleles, builder);
    }

    private QueryBuilder getMissingGenotypesFilter(int missingGenotypes, ComparisonOperator op, QueryBuilder builder) {
        return op.apply(DBObjectToVariantConverter.FILES_FIELD + "." + DBObjectToVariantSourceEntryConverter.STATS_FIELD
                + "." + DBObjectToVariantStatsConverter.MISSGENOTYPE_FIELD, missingGenotypes, builder);
    }


    /* *******************
     * Auxiliary methods *
     * *******************/

    private List<String> getChunkIds(Region region) {
        List<String> chunkIds = new LinkedList<>();

        int chunkSize = (region.getEnd() - region.getStart() > VariantMongoWriter.CHUNK_SIZE_BIG) ?
                VariantMongoWriter.CHUNK_SIZE_BIG : VariantMongoWriter.CHUNK_SIZE_SMALL;
        int ks = chunkSize / 1000;
        int chunkStart = region.getStart() / chunkSize;
        int chunkEnd = region.getEnd() / chunkSize;

        for (int i = chunkStart; i <= chunkEnd; i++) {
            String chunkId = region.getChromosome() + "_" + i + "_" + ks + "k";
            chunkIds.add(chunkId);
        }

        return chunkIds;
    }

    private int getChunkId(int position, int chunksize) {
        return position / chunksize;
    }

    private int getChunkStart(int id, int chunksize) {
        return (id == 0) ? 1 : id * chunksize;
    }

    private int getChunkEnd(int id, int chunksize) {
        return (id * chunksize) + chunksize - 1;
    }

    public List<QueryResult> getAllVariantsByRegionListAndFileIds(List<Region> regions, List<String> fileIds, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);

        List<QueryResult> allResults = new LinkedList<>();
        QueryBuilder qb = QueryBuilder.start();

        getRegionFilter(regions, qb);

        qb.and("files.fid").in(fileIds);
        parseQueryOptions(options, qb);

        QueryResult queryResult = coll.find(qb.get(), options, variantConverter);
        allResults.add(queryResult);

        return allResults;

    }

      /* *******************
     *  Auxiliary types  *
     * *******************/

    private enum ComparisonOperator {
        LT("<") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).lessThan(value);
            }
        },

        LTE("<=") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).lessThanEquals(value);
            }
        },

        GT(">") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).greaterThan(value);
            }
        },

        GTE(">=") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).greaterThanEquals(value);
            }
        },

        EQ("=") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).is(value);
            }
        },

        NEQ("=/=") {
            @Override
            QueryBuilder apply(String key, Object value, QueryBuilder builder) {
                return builder.and(key).notEquals(value);
            }
        };

        private final String symbol;

        private ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        abstract QueryBuilder apply(String key, Object value, QueryBuilder builder);

        // Returns Operation for string, or null if string is invalid
        private static final Map<String, ComparisonOperator> stringToEnum = new HashMap<>();

        static { // Initialize map from constant name to enum constant
            for (ComparisonOperator op : values()) {
                stringToEnum.put(op.toString(), op);
            }
        }

        public static ComparisonOperator fromString(String symbol) {
            return stringToEnum.get(symbol);
        }

    }

}
