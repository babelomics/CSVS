package org.babelomics.csvs.server;

import com.wordnik.swagger.annotations.Api;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
@Path("/studies")
@Api(value = "studies", description = "Study")
@Produces(MediaType.APPLICATION_JSON)
public class StudyWSServer{
//public class StudyWSServer extends PVSWSServer {

//    private PVSStudyDBAdaptor studyMongoDbAdaptor;
//    private VariantSourceDBAdaptor variantSourceDbAdaptor;
//
//    public StudyWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
//            throws IOException, IllegalOpenCGACredentialsException, NamingException {
//        super(version, uriInfo, hsr);
//        studyMongoDbAdaptor = new PVSStudyMongoDBAdaptor(credentials);
//        variantSourceDbAdaptor = new PVSVariantSourceMongoDBAdaptor(credentials);
//    }
//
//    @GET
//    @Path("/list")
//    @Produces("application/json")
//    @ApiOperation(value = "Get all Studies")
//    public Response getStudies() {
//        return createOkResponse(studyMongoDbAdaptor.getAllStudies(queryOptions));
//    }
//
//
//    @GET
//    @Path("/diseases")
//    @Produces("application/json")
//    @ApiOperation(value = "Get all Diseases")
//    public Response getDiseases() {
//        return createOkResponse(studyMongoDbAdaptor.listDiseases());
//    }
//
//    @GET
//    @Path("/{study}/files")
//    @Produces("application/json")
//    @ApiOperation(value = "Get Files from a Study")
//    public Response getFilesByStudy(@ApiParam(value = "study") @PathParam("study") String study) {
//        QueryResult idQueryResult = studyMongoDbAdaptor.findStudyNameOrStudyId(study, queryOptions);
//        if (idQueryResult.getNumResults() == 0) {
//            QueryResult queryResult = new QueryResult();
//            queryResult.setErrorMsg("Study identifier not found");
//            return createOkResponse(queryResult);
//        }
//
//        BasicDBObject id = (BasicDBObject) idQueryResult.getResult().get(0);
//        QueryResult finalResult = variantSourceDbAdaptor.getAllSourcesByStudyId(id.getString(PVSDBObjectToVariantSourceConverter.STUDYID_FIELD), queryOptions);
//        finalResult.setDbTime(finalResult.getDbTime() + idQueryResult.getDbTime());
//        return createOkResponse(finalResult);
//    }
//
//    @GET
//    @Path("/{study}/view")
//    @Produces("application/json")
//    @ApiOperation(value = "Get Study Info")
//    public Response getStudy(@ApiParam(value = "study") @PathParam("study") String study) {
//        QueryResult idQueryResult = studyMongoDbAdaptor.findStudyNameOrStudyId(study, queryOptions);
//        if (idQueryResult.getNumResults() == 0) {
//            QueryResult queryResult = new QueryResult();
//            queryResult.setErrorMsg("Study identifier not found");
//            return createOkResponse(queryResult);
//        }
//
//        BasicDBObject id = (BasicDBObject) idQueryResult.getResult().get(0);
//        QueryResult finalResult = studyMongoDbAdaptor.getStudyById(id.getString(PVSDBObjectToVariantSourceConverter.STUDYID_FIELD), queryOptions);
//        finalResult.setDbTime(finalResult.getDbTime() + idQueryResult.getDbTime());
//        return createOkResponse(finalResult);
//    }
//
}
