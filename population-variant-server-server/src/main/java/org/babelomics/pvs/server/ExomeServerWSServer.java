package org.babelomics.pvs.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babelomics.pvs.lib.json.PVSVariantSourceEntryJsonMixin;
import org.babelomics.pvs.lib.json.PVSVariantStatsJsonMixin;
import org.babelomics.pvs.lib.json.VariantAnnotationJsonMixin;
import org.babelomics.pvs.lib.json.VariantJsonMixin;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.VariantSourceEntry;
import org.opencb.biodata.models.variant.effect.VariantAnnotation;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.lib.auth.MongoCredentials;
import org.opencb.opencga.lib.common.Config;
import org.opencb.opencga.storage.variant.json.GenotypeJsonMixin;
import org.opencb.opencga.storage.variant.json.VariantSourceJsonMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

@Path("/")
public class ExomeServerWSServer {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;
    protected static Config config;

    protected String version;
    protected UriInfo uriInfo;
    protected String sessionIp;

    // Common input arguments
    protected MultivaluedMap<String, String> params;
    protected QueryOptions queryOptions;
    protected QueryResponse queryResponse;

    // Common output members
    protected long startTime;
    protected long endTime;

    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    @DefaultValue("json")
    @QueryParam("of")
    protected String outputFormat;

    @DefaultValue("")
    @QueryParam("exclude")
    protected String exclude;

    @DefaultValue("")
    @QueryParam("include")
    protected String include;

    @DefaultValue("true")
    @QueryParam("metadata")
    protected Boolean metadata;

    static {

        InputStream is = ExomeServerWSServer.class.getClassLoader().getResourceAsStream("pvs.properties");
        properties = new Properties();
        try {
            properties.load(is);
            System.out.println("properties = " + properties);
        } catch (IOException e) {
            System.out.println("Error loading properties");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.addMixInAnnotations(VariantSourceEntry.class, PVSVariantSourceEntryJsonMixin.class);
        jsonObjectMapper.addMixInAnnotations(Genotype.class, GenotypeJsonMixin.class);
        jsonObjectMapper.addMixInAnnotations(VariantStats.class, PVSVariantStatsJsonMixin.class);
        jsonObjectMapper.addMixInAnnotations(VariantSource.class, VariantSourceJsonMixin.class);
        jsonObjectMapper.addMixInAnnotations(VariantAnnotation.class, VariantAnnotationJsonMixin.class);
        jsonObjectMapper.addMixInAnnotations(Variant.class, VariantJsonMixin.class);
        jsonObjectWriter = jsonObjectMapper.writer();

    }

    protected MongoCredentials credentials;

    public ExomeServerWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException, IllegalOpenCGACredentialsException {
        this.startTime = System.currentTimeMillis();
        this.version = version;
        this.uriInfo = uriInfo;
        logger.debug(uriInfo.getRequestUri().toString());

        this.queryOptions = new QueryOptions();
        queryOptions.put("exclude", exclude);
        queryOptions.put("include", include);
        queryOptions.put("metadata", metadata);

        this.sessionIp = httpServletRequest.getRemoteAddr();
        credentials = new MongoCredentials(properties.getProperty("DB.HOST"), Integer.parseInt(properties.getProperty("DB.PORT")), properties.getProperty("DB.DATABASE"), properties.getProperty("DB.USER"), properties.getProperty("DB.PASS"));
    }


    protected Response createErrorResponse(Object o) {
        QueryResult<ObjectMap> result = new QueryResult();
        result.setErrorMsg(o.toString());
        return createOkResponse(result);
    }

    protected Response createOkResponse(Object obj) {
        queryResponse = new QueryResponse();
        endTime = System.currentTimeMillis() - startTime;
        queryResponse.setTime(new Long(endTime - startTime).intValue());
        queryResponse.setApiVersion(version);
        queryResponse.setQueryOptions(queryOptions);

        // Guarantee that the QueryResponse object contains a coll of results
        Collection coll;
        if (obj instanceof Collection) {
            coll = (Collection) obj;
        } else {
            coll = new ArrayList();
            coll.add(obj);
        }
        queryResponse.setResponse(coll);

        switch (outputFormat.toLowerCase()) {
            case "json":
                return createJsonResponse(queryResponse);
            case "xml":
//                return createXmlResponse(queryResponse);
            default:
                return buildResponse(Response.ok());
        }


    }

    protected Response createJsonResponse(Object object) {
        try {
            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(object), MediaType.APPLICATION_JSON_TYPE));
        } catch (JsonProcessingException e) {
            return createErrorResponse("Error parsing QueryResponse object:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    //Response methods
    protected Response createOkResponse(Object o1, MediaType o2) {
        return buildResponse(Response.ok(o1, o2));
    }

    protected Response createOkResponse(Object o1, MediaType o2, String fileName) {
        return buildResponse(Response.ok(o1, o2).header("content-disposition", "attachment; filename =" + fileName));
    }

    protected Response buildResponse(ResponseBuilder responseBuilder) {
        return responseBuilder.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Headers", "x-requested-with, content-type").build();
    }
}
