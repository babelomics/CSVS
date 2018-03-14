package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.babelomics.csvs.lib.models.Opinion;
import org.babelomics.csvs.lib.models.Pathology;
import org.babelomics.csvs.lib.models.Variant;
import org.babelomics.csvs.lib.ws.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gema Roldán Gonzalez <gema.roldan@juntadeandalucia.es>
 */
@Path("/pathologies")
@Api(value = "Pathologies", description = "Pathologies from variants")
@Produces(MediaType.APPLICATION_JSON)
public class PathologiesWSServer extends CSVSWSServer {

    public PathologiesWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);
    }

    @GET
    @Path("/{variants}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Opinion By Region")
    public Response getVariantsPathopedia(@ApiParam(value = "variants") @PathParam("variants") String variants) {

        long start = System.currentTimeMillis();
        List<Variant> listVariants = new ArrayList<>();
        String[] splits = variants.split(",");
        for (String v : splits){
            listVariants.add(new Variant(v));
        }
        List<Integer> statesList = new ArrayList<>();
        statesList.add(1);

        List<Pathology> resVariants = qm.getVariantsPathopedia(listVariants, statesList);
        long end = System.currentTimeMillis();

        QueryResponse qr = createQueryResponse(resVariants);
        qr.setNumTotalResults(qr.getNumResults());

        return createOkResponse(qr);
    }

    @GET
    @Path("/{variant}/list")
    @Produces("application/json")
    @ApiOperation(value = "List pathologies classifications")
    public Response getAllOpinions(@ApiParam(value = "variant") @PathParam("variant") String variant,
                                   @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                   @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                   @ApiParam(value = "sort") @QueryParam("sort") @DefaultValue("") String sort,
                                   @ApiParam(value = "states") @QueryParam("states") @DefaultValue("") String states
    ) {

        List<Integer> statesList = new ArrayList<>();

        if (states.length() > 0) {
            String[] stateSplit = states.split(",");
            for (String s : stateSplit) {
                statesList.add(Integer.valueOf(s));
            }
        }

        List<Opinion> res = qm.getAllOpinion(new Variant(variant), statesList, sort, limit, skip) ;

        QueryResponse qr = createQueryResponse(res);
        qr.setNumTotalResults(qr.getNumResults());
        qr.addQueryOption("limit", limit);
        qr.addQueryOption("skip", skip);
        return createOkResponse(qr);
    }


    @POST
    @Path("/{variant}/{type}/add")
    @Produces("application/json")
    @ApiOperation(value = "Add new pathology classification")
    public Response addOpinion(@ApiParam(value = "variant") @PathParam("variant") String variant,
                               @ApiParam(value = "type") @PathParam("type") String type,
                               @ApiParam(value = "name", required = true) @NotNull @FormParam("name") String name,
                               @ApiParam(value = "institution", required = true) @NotNull @FormParam("institution") String institution,
                               @ApiParam(value = "evidence", required = true) @NotNull @FormParam("evidence") String evidence,
                               @DefaultValue("")  @FormParam("from") String from )
    {
        Variant v = qm.getVariant(new Variant(variant), null, null);
        Opinion res = null;
        if (v != null) {
            res = qm.saveOpinion(new Opinion(v, name, institution, evidence, type),Opinion.PENDING);
        }

        if (res != null) {
            String urlParameters = preSendMail(res,  "add");
            sendMail(urlParameters);
        }

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);
    }


    @GET
    @Path("/{opinion}/{state}/update")
    @Produces("application/json")
    @ApiOperation(value = "Add new pathology classification")
    public Response updateOpinion(@ApiParam(value = "opinion") @PathParam("opinion") String idOpinion,
                               @ApiParam(value = "state") @PathParam("state") int state) {

        Opinion o = qm.getOpinion(new ObjectId(idOpinion));
        Opinion res = null;
        if (o != null) {
            res= qm.saveOpinion(o, state);
            String urlParameters = preSendMail(res, "update");
            sendMail(urlParameters);
        }

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);
    }

    /**
     * Method to preparate params to send mail.
     *
     * @param opinion
     * @return
     */
    private String preSendMail(Opinion opinion, String action) {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(configMail);
        map.put(CSVSWSServer.FROM, configMail.get(CSVSWSServer.TO));

        StringBuffer text = new StringBuffer();
        text.append("CSVS Pathopedia: ");
        text.append(opinion.getVariant().pretty());

        if ("update".equals(action))
            text.append(" -> New opinion");
        else
            text.append(" -> Changue state opinion");

        map.put(CSVSWSServer.SUBJECT, text.toString());

        text = new StringBuffer();
        text.append("DATA OPINION: \n");
        text.append("\n Variant: ");
        text.append(opinion.getVariant().pretty());
        text.append("\n Name: ");
        text.append(opinion.getName());
        text.append("\n Institution: ");
        text.append(opinion.getInstitution());
        text.append("\n Evidence: ");
        text.append(opinion.getEvidence());
        text.append("\n Type: ");
        text.append(opinion.getTypeDesc());
        text.append("\n State: ");
        text.append(opinion.getStateDesc());

        text.append("\n\nSelect what action you want to do:");

        if(opinion.getState() != Opinion.PENDING) {
            text.append("\n\n GO TO PENDING TO PUBLIC: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.PENDING);
            text.append("/update");
        }

        if(opinion.getState() != Opinion.PUBLISHED) {
            text.append("\n\n GO TO PUBLISH: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.PUBLISHED);
            text.append("/update");
        }

        if(opinion.getState() != Opinion.REJECTED) {
            text.append("\n\n GO TO REJECT: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.REJECTED);
            text.append("/update");
        }
        map.put(CSVSWSServer.TEXT, text.toString());
        map.put(CSVSWSServer.HTML, "<pre>" + text + "<pre>");

        return mapToString(map);
    }
}