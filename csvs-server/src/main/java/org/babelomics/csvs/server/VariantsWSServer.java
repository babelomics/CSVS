package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.*;
import org.babelomics.csvs.lib.ws.QueryResponse;
import org.opencb.biodata.models.feature.Region;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;

@Path("/variants")
@Api(value = "variants", description = "Variants")
@Produces(MediaType.APPLICATION_JSON)
public class VariantsWSServer extends CSVSWSServer {

    public VariantsWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);
    }

    @GET
    @Path("/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(@ApiParam(value = "regions") @QueryParam("regions") @DefaultValue("") String regions,
                                        @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                        @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                        @ApiParam(value = "skipCount") @QueryParam("skipCount") @DefaultValue("false") boolean skipCount,
                                        @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
                                        @ApiParam(value = "technologies") @QueryParam("technologies") @DefaultValue("") String technologies,
                                        @ApiParam(value = "csv") @QueryParam("csv") @DefaultValue("false") boolean csv
    ) {


        List<Region> regionList = new ArrayList<>();
        List<Integer> diseaseList = new ArrayList<>();
        List<Integer> technologyList = new ArrayList<>();

        //System.out.println("diseases = " + diseases);
        //System.out.println("technologies = " + technologies);

        int regionsSize = 0;


        if (regions.length() > 0) {
            String[] regSplits = regions.split(",");
            for (String s : regSplits) {
                Region r = Region.parseRegion(s);
                regionList.add(r);
                regionsSize += r.getEnd() - r.getStart();
            }
        }

        if (diseases.length() > 0) {
            String[] disSplits = diseases.split(",");
            for (String d : disSplits) {
                diseaseList.add(Integer.valueOf(d));

            }
        }


        if (technologies.length() > 0) {
            String[] techSplit = technologies.split(",");
            for (String t : techSplit) {
                technologyList.add(Integer.valueOf(t));
            }
        }

        MutableLong count = new MutableLong(-1);

        if (csv) {
            skip = 0;
            limit = 200;
        }

        long start = System.currentTimeMillis();

        Iterable<Variant> variants = null;

        try {
            variants = qm.getVariantsByRegionList(regionList, diseaseList, technologyList, skip, limit, skipCount, count);
        } catch (Exception e1){
            return  createErrorResponse(e1);
        }

        long end = System.currentTimeMillis();

        //System.out.println("(end-start) = " + (end - start));

        QueryResponse qr = createQueryResponse(variants);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("regions", regionList);
        if (diseases.length() > 0) {
            qr.addQueryOption("diseases", diseases);
        }

        if (!csv) {
            qr.addQueryOption("limit", limit);
            qr.addQueryOption("skip", skip);
        } else {
            qr.addQueryOption("csv", csv);
        }


        return createOkResponse(qr);
    }

    @GET
    @Path("/{variants}/get")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariants(@ApiParam(value = "variants") @PathParam("variants") @DefaultValue("") String variants,
                                @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
                                @ApiParam(value = "technologies") @QueryParam("technologies") @DefaultValue("") String technologies

    ) {

        List<Variant> variantList = new ArrayList<>();
        List<Integer> diseaseList = new ArrayList<>();
        List<Integer> technologyList = new ArrayList<>();


        if (variants.length() > 0) {
            String[] varSplit = variants.split(",");
            for (String s : varSplit) {
                Variant variant = new Variant(s);
                variantList.add(variant);
            }
        }

        if (diseases.length() > 0) {
            String[] disSplits = diseases.split(",");
            for (String d : disSplits) {
                diseaseList.add(Integer.valueOf(d));
            }
        }

        if (technologies.length() > 0) {
            String[] techSplit = technologies.split(",");
            for (String t : techSplit) {
                technologyList.add(Integer.valueOf(t));
            }
        }

        //System.out.println("diseaseList = " + diseaseList);
        //System.out.println("technologyList = " + technologyList);

        List<Variant> variantRes = qm.getVariants(variantList, diseaseList, technologyList);

        QueryResponse qr = createQueryResponse(variantRes);
        qr.setNumTotalResults(variantRes.size());

        qr.addQueryOption("variants", variants);
        if (diseases.length() > 0) {
            qr.addQueryOption("diseases", diseases);
        }

        qr.addQueryOption("limit", limit);
        qr.addQueryOption("skip", skip);

        return createOkResponse(qr);
    }

    @POST
    @Path("/{variant}/contact")
    @Produces("application/json")
    @ApiOperation(value = "Contact request")
    public Response getVariants(@ApiParam(value = "variant") @PathParam("variant") String variant,
                                @ApiParam(value = "name", required = true) @NotNull @FormParam("name") String name,
                                @ApiParam(value = "institution", required = true) @NotNull @FormParam("institution") String institution,
                                @ApiParam(value = "email", required = true) @NotNull @FormParam("email") String email,
                                @ApiParam(value = "reason", required = true) @NotNull @FormParam("reason") String reason,
                                @ApiParam(value = "regionsSearch") @FormParam("regionsSearch") @DefaultValue("") String regionsSearch,
                                @ApiParam(value = "geneSearch") @FormParam("geneSearch") @DefaultValue("") String geneSearch,
                                @ApiParam(value = "diseasesSearch") @FormParam("diseasesSearch") @DefaultValue("") String diseasesSearch,
                                @ApiParam(value = "technologiesSearch") @FormParam("technologiesSearch") @DefaultValue("") String technologiesSearch
    ) {
        Map<String, Object> contact = new HashMap<String, Object>();
        contact.put("variant",variant);
        contact.put("name",name);
        contact.put("institution", institution);
        contact.put("email", email);
        contact.put("reason", reason);

        List<DiseaseGroup> d = qm.getAllDiseaseGroups();
        List<Technology> t = qm.getAllTechnologies();
        Map<String, Object> search = new HashMap<String, Object>();
        search.put("regionsSearch", regionsSearch);
        search.put("geneSearch", geneSearch);
        search.put("diseasesSearch", diseasesSearch);
        search.put("technologiesSearch", technologiesSearch);
        contact.put("search", search);

        List<File> f = qm.getInfoFile(variant);

        if (!f.isEmpty() ) {
            contact.put("file", f);

            // Add diseases group
            Map<Integer, String> diseasesGroup = new HashMap<Integer, String>();
            if (d != null){
                d.forEach(item->diseasesGroup.put(item.getGroupId(), item.getName()));
            }
            contact.put("diseasesGroup", diseasesGroup);

            // Add technology
            Map<Integer, String> technologies = new HashMap<Integer, String>();
            if (t != null) {
                t.forEach(item -> technologies.put(item.getTechnologyId(), item.getName()));
            }
            contact.put("technologies", technologies);

            String urlParameters = preSendMail(contact);
            if(!sendMail(urlParameters))
                return createErrorResponse("PROBLEM_SERVER");
       } else {
            return createErrorResponse("No exist reporter for " + variant);
        }

        QueryResponse qr = createQueryResponse(contact);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);
    }


    @GET
    @Path("/metadata")
    @Produces("application/json")
    @ApiOperation(value = "Contact request")
    public Response getMetadata() {
        List<Metadata> metadata = qm.getMetadata();
        List<Metadata> metadataSummary = new ArrayList<>();

        // Remove extra information
        for(Metadata m : metadata){
            Metadata newMetadata = new Metadata(m.getVersion(), m.getDate(), m.getIndividuals());
            metadataSummary.add(newMetadata);
        }


        QueryResponse qr = createQueryResponse(metadataSummary);
        qr.setNumResults(metadataSummary.size());
        qr.setNumTotalResults(metadataSummary.size());

        return createOkResponse(qr);
    }

    /**
     * Method to preparate params to send mail.
     *
     * @param contact
     * @return
     */
    private String preSendMail(Map<String, Object> contact) {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(configMail);
        map.put(CSVSWSServer.FROM, configMail.get(CSVSWSServer.TO));

        StringBuffer text = new StringBuffer();
        text.append("CSVS contact: ");
        text.append(" Request information ");
        text.append(contact.get("variant"));

        map.put(CSVSWSServer.SUBJECT, text.toString());

        text = new StringBuffer();
        text.append("- Contact request: \n");
        text.append("\n   * Variant: ");
        text.append(contact.get("variant"));
        text.append("\n   * Name: ");
        text.append(contact.get("name"));
        text.append("\n   * Institution: ");
        text.append(contact.get("institution"));
        text.append("\n   * Email: ");
        text.append(contact.get("email"));
        text.append("\n   * Petition: ");
        text.append(contact.get("reason"));

        java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("MMM dd, yyyy");

        text.append("\n\n- Summary variant information (variant reporters): ");
        List<String> summaryPR = new ArrayList<String>();

        StringBuffer textDetail = new StringBuffer();
        textDetail.append("\n\n- Variant information detailed:\n");
        List<File> files = (List<File>) contact.get("file");
        Map<Integer, String> diseasesGroup = (HashMap<Integer, String>) contact.get("diseasesGroup");
        Map<Integer, String> technologies = (HashMap<Integer, String>) contact.get("technologies");

        if (files != null) {
            for(File f : files ) {
                textDetail.append("\n   * File name: ");
                textDetail.append(f.getNameFile());
                textDetail.append("\n     Variant reporter: ");
                textDetail.append(f.getPersonReference());
                if( ! summaryPR.contains(f.getPersonReference()))
                    summaryPR.add(f.getPersonReference());
                textDetail.append("\n     Disease group: ");
                textDetail.append(diseasesGroup.get(f.getDiseaseGroupId()));
                textDetail.append("\n     Technology: ");
                textDetail.append(technologies.get(f.getTechnologyId()));
                textDetail.append("\n     Create date in CSVS: ");
                textDetail.append(DATE_FORMAT.format(f.getDate()));
                textDetail.append("\n");
            }
        }

        text.append(String.join(", ", summaryPR));
        text.append(textDetail);

        text.append("\n\n- Filters selected when submitting request:\n");
        Map<String, Object> search = (Map<String, Object>) contact.get("search");
        text.append("\n   * Region: ");
        text.append(search.get("regionsSearch"));
        text.append("\n   * Gene: ");
        text.append(search.get("geneSearch"));
        text.append("\n   * Subpopulations: ");
        text.append( this.getDescriptionDetail((String)search.get("diseasesSearch"),diseasesGroup));
        text.append("\n   * Technologies: ");
        text.append( this.getDescriptionDetail((String) search.get("technologiesSearch"),technologies));

        map.put(CSVSWSServer.TEXT, text.toString());
        map.put(CSVSWSServer.HTML, "<pre>" + text + "<pre>");

        return mapToString(map);
    }

    /**
     * Get description disease o technology
     * @param dt  Disease o Technology
     * @param data Map with diseases o technologies
     * @return
     */
    private StringBuffer getDescriptionDetail(String dt, Map data){
        StringBuffer textDetail = new StringBuffer();

        if ( dt!= null) {

            if("".equals(dt)) {
                textDetail.append("All");
            }
            else {
                for (String elto : dt.toString().split(",")) {
                    textDetail.append("\n        ");
                    try {
                        textDetail.append(data.get(Integer.parseInt(elto)));
                    } catch (Exception e){
                        textDetail.append(elto);
                    }
                }
            }
        }

        return textDetail;
    }

}
