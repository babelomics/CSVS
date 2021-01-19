package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import io.jsonwebtoken.Claims;
import org.babelomics.csvs.lib.CSVSUtil;
import org.babelomics.csvs.lib.models.Opinion;
import org.babelomics.csvs.lib.models.Pathology;
import org.babelomics.csvs.lib.models.Variant;
import org.babelomics.csvs.lib.token.CSVSToken;
import org.babelomics.csvs.lib.ws.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.*;

/**
 * @author Gema Roldán Gonzalez <gema.roldan@juntadeandalucia.es>
 */
@Path("/pathologies")
@Api(value = "pathologies", description = "Pathologies from variants")
@Produces(MediaType.APPLICATION_JSON)
public class PathologiesWSServer extends CSVSWSServer {
    static int TOKEN_DAYS = 30;
    static String TOKEN_ISSUER ="CSVS";
    static String TOKEN_AUDIENCE = "csvs.clinbioinfosspa.es";


    public PathologiesWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo,
                               @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException {
        super(version, uriInfo, httpServletRequest);
        try {
            verifyHeaders(httpHeaders, uriInfo.getQueryParameters().getFirst("sid"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }



    @GET
    @Path("/{variants}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Opinion By Region")
    public Response getVariantsPathopedia(@ApiParam(value = "variants") @PathParam("variants") String variants) {

        long start = System.currentTimeMillis();
        List<Variant> listVariants = new ArrayList<>();
        String[] splits = variants.split("&");
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
                                   @ApiParam(value = "states") @QueryParam("states") @DefaultValue("") String states,
                                   @ApiParam(value = "clinSignificance") @QueryParam("clinSignificance")  List<String> clinSignificance
    ) {

        List<Integer> statesList = new ArrayList<>();

        if (states.length() > 0) {
            String[] stateSplit = states.split(",");
            for (String s : stateSplit) {
                statesList.add(Integer.valueOf(s));
            }
        }

        List<Opinion> res = qm.getAllOpinion(new Variant(variant), statesList, sort, limit, skip, clinSignificance) ;

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
            Map infoOpinion = new HashMap();
            Variant resVariant = res.getVariant();
            infoOpinion.put("variant", resVariant.getChromosome() + ":" + resVariant.getPosition()+ ":"+resVariant.getReference()+">"+resVariant.getAlternate());
            infoOpinion.put("name", res.getName());
            infoOpinion.put("institution", res.getInstitution());
            infoOpinion.put("evidence", res.getEvidence());
            infoOpinion.put("clinicalSignificance", res.getTypeDesc());
            infoOpinion.put("state", res.getStateDesc());

            Map aditionalClaims = new HashMap();

            aditionalClaims.put("Info", infoOpinion);

            String resultToken = CSVSUtil.generateToken(TOKEN_ISSUER, TOKEN_AUDIENCE, TOKEN_ISSUER, TOKEN_DAYS, aditionalClaims, SECRET_KEY);


            String urlParameters = preSendMail(res,  "add", resultToken);
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

        CSVSToken csvsToken = new CSVSToken(SECRET_KEY);
        Claims claims = null;
        String msj = "";
        try {
            claims = csvsToken.decodeJWT(sessionId);

            // check variant token equal idOpinion
            if (claims.containsKey("Info")) {
                Map aditionalClaims = (Map) claims.get("Info");
                if (aditionalClaims.containsKey("variant")) {
                    Variant oVariant = o.getVariant();
                    String oVariantString = oVariant.getChromosome() + ":" + oVariant.getPosition()+ ":"+oVariant.getReference()+">"+oVariant.getAlternate();
                    if (!oVariantString.equals(aditionalClaims.get("variant")))
                        msj = "Variant no equal to token";
                } else {
                    msj = "token no contain Variant";
                }
            } else
                msj = "Token no contains Info";

        } catch (Exception e){
            logger.error("CSVS: Update pathologies " + e.toString());
            msj = "Claims no valid";
        }

        if (o != null && "".equals(msj)) {
            res= qm.saveOpinion(o, state);
            String urlParameters = preSendMail(res, "update", sessionId);
            sendMail(urlParameters);
        }

        if(!"".equals(msj)){
            return createErrorResponse(msj);
        } else {
            QueryResponse qr = createQueryResponse(res);
            qr.setNumResults(qr.getNumTotalResults());

            return createOkResponse(qr);
        }
    }

    /**
     * Method to preparate params to send mail.
     *
     * @param opinion
     * @return
     */
    private String preSendMail(Opinion opinion, String action, String sid) {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(configMail);
        map.put(CSVSWSServer.FROM, configMail.get(CSVSWSServer.TO));

        StringBuffer text = new StringBuffer();
        text.append("CSVS Pathopedia: ");
        text.append(opinion.getVariant().pretty());

        if ("update".equals(action))
            text.append(" -> Change state opinion");
        else
            text.append(" -> New opinion");

        map.put(CSVSWSServer.SUBJECT, text.toString());

        text = new StringBuffer();
        text.append("- Review pathopedia: \n");
        text.append("\n   Variant: ");
        text.append(opinion.getVariant().pretty());
        text.append("\n   Name: ");
        text.append(opinion.getName());
        text.append("\n   Institution: ");
        text.append(opinion.getInstitution());
        text.append("\n   Evidence: ");
        text.append(opinion.getEvidence());
        text.append("\n   Type: ");
        text.append(opinion.getTypeDesc());
        text.append("\n   State: ");
        text.append(opinion.getStateDesc());

        text.append("\n\n- Select what action you want to do:");

        if(opinion.getState() != Opinion.PENDING) {
            text.append("\n\n  GO TO PENDING TO PUBLIC: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.PENDING);
            text.append("/update");
            text.append("?sid=");
            text.append(sid);
        }

        if(opinion.getState() != Opinion.PUBLISHED) {
            text.append("\n\n  GO TO PUBLISH: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.PUBLISHED);
            text.append("/update");
            text.append("?sid=");
            text.append(sid);
        }

        if(opinion.getState() != Opinion.REJECTED) {
            text.append("\n\n GO TO REJECT: ");
            text.append(uriInfo.getBaseUri());
            text.append("pathologies/");
            text.append(opinion.getId());
            text.append("/");
            text.append(Opinion.REJECTED);
            text.append("/update");
            text.append("?sid=");
            text.append(sid);
        }
        map.put(CSVSWSServer.TEXT, text.toString());
        map.put(CSVSWSServer.HTML, "<pre>" + text + "<pre>");

        return mapToString(map);
    }
}