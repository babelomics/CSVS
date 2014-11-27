package org.babelomics.exomeserver.lib.mongodb.dbAdaptor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.opencb.datastore.core.ComplexTypeConverter;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.datastore.mongodb.MongoDBCollection;
import org.opencb.datastore.mongodb.MongoDBConfiguration;
import org.opencb.datastore.mongodb.MongoDataStore;
import org.opencb.datastore.mongodb.MongoDataStoreManager;
import org.opencb.opencga.lib.auth.MongoCredentials;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class ExomeServerStudyMongoDBAdaptor implements ExomeServerStudyDBAdaptor {
    private final MongoDataStoreManager mongoManager;
    private final MongoDataStore db;

    public ExomeServerStudyMongoDBAdaptor(MongoCredentials credentials) throws UnknownHostException {
        this.mongoManager = new MongoDataStoreManager(credentials.getMongoHost(), credentials.getMongoPort());
        MongoDBConfiguration mongoDBConfiguration = MongoDBConfiguration.builder().add("username", credentials.getUsername()).add("password", credentials.getPassword() != null ? new String(credentials.getPassword()) : null).build();
        this.db = this.mongoManager.get(credentials.getMongoDbName(), mongoDBConfiguration);
    }

    public QueryResult listStudies() {
        MongoDBCollection coll = this.db.getCollection("files");
        BasicDBObject project1 = new BasicDBObject("$project", (new BasicDBObject("_id", Integer.valueOf(0)))
                .append("sid", Integer.valueOf(1))
                .append("sname", Integer.valueOf(1)));
        BasicDBObject group = new BasicDBObject("$group", new BasicDBObject("_id", (new BasicDBObject("studyId", "$sid")).append("studyName", "$sname")));
        BasicDBObject project2 = new BasicDBObject("$project", (new BasicDBObject("studyId", "$_id.studyId")).append("studyName", "$_id.studyName").append("_id", Integer.valueOf(0)));
        return coll.aggregate("$studyList", Arrays.asList(new DBObject[]{project1, group, project2}), (QueryOptions) null);
    }

    public QueryResult getAllStudies(QueryOptions options) {
        MongoDBCollection coll = this.db.getCollection("files");
        QueryBuilder qb = QueryBuilder.start();
        DBObject returnFields = new BasicDBObject("_id", Integer.valueOf(0));
        return coll.find(qb.get(), options, null, returnFields);
    }

    public QueryResult getAllFileId(QueryOptions options) {
        MongoDBCollection coll = this.db.getCollection("files");
        QueryBuilder qb = QueryBuilder.start();
        DBObject returnFields = new BasicDBObject("_id", Integer.valueOf(0)).
                append("fid", Integer.valueOf(1)).append("meta.sta", Integer.valueOf(1));
        return coll.find(qb.get(), options, null, returnFields);
    }

    public QueryResult findStudyNameOrStudyId(String study, QueryOptions options) {
        MongoDBCollection coll = this.db.getCollection("files");
        QueryBuilder qb = QueryBuilder.start();
        qb.or(new DBObject[]{new BasicDBObject("sname", study), new BasicDBObject("sid", study)});
        BasicDBObject returnFields = (new BasicDBObject("sid", Integer.valueOf(1))).append("_id", Integer.valueOf(0));
        options.add("limit", Integer.valueOf(1));
        return coll.find(qb.get(), options, (ComplexTypeConverter) null, returnFields);
    }

    public QueryResult getStudyById(String studyId, QueryOptions options) {
        MongoDBCollection coll = this.db.getCollection("files");
        QueryBuilder qb = QueryBuilder.start();
        this.getStudyIdFilter(studyId, qb);
        BasicDBObject match = new BasicDBObject("$match", qb.get());
        BasicDBObject project = new BasicDBObject("$project", (new BasicDBObject("_id", Integer.valueOf(0))).append("sid", Integer.valueOf(1)).append("sname", Integer.valueOf(1)));
        BasicDBObject group = new BasicDBObject("$group", (new BasicDBObject("_id", (new BasicDBObject("studyId", "$sid")).append("studyName", "$sname"))).append("numFiles", new BasicDBObject("$sum", Integer.valueOf(1))));
        QueryResult aggregationResult = coll.aggregate("$studyInfo", Arrays.asList(new DBObject[]{match, project, group}), options);
        List results = aggregationResult.getResult();
        DBObject dbo = (DBObject) results.iterator().next();
        DBObject dboId = (DBObject) dbo.get("_id");
        BasicDBObject outputDbo = (new BasicDBObject("studyId", dboId.get("studyId"))).append("studyName", dboId.get("studyName")).append("numFiles", dbo.get("numFiles"));
        QueryResult transformedResult = new QueryResult(aggregationResult.getId(), aggregationResult.getDbTime(), aggregationResult.getNumResults(), aggregationResult.getNumTotalResults(), aggregationResult.getWarningMsg(), aggregationResult.getErrorMsg(), Arrays.asList(new DBObject[]{outputDbo}));
        return transformedResult;
    }

    public boolean close() {
        this.mongoManager.close(this.db.getDatabaseName());
        return true;
    }

    private QueryBuilder getStudyFilter(String name, QueryBuilder builder) {
        return builder.and("sname").is(name);
    }

    private QueryBuilder getStudyIdFilter(String id, QueryBuilder builder) {
        return builder.and("sid").is(id);
    }

    @Override
    public QueryResult listDiseases() {
        MongoDBCollection coll = this.db.getCollection("files");
        return coll.distinct("meta.dis", null);
    }
}
