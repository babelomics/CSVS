package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.*;
import org.babelomics.csvs.lib.ws.QueryResponse;
import org.opencb.biodata.models.feature.Region;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Path("/pharmaVariants")
@Api(value = "pharmaVariants", description = "Pharmagenomic variants")
@Produces(MediaType.APPLICATION_JSON)
public class PharmaVariantsWSServer extends CSVSWSServer {

    public PharmaVariantsWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);
    }


    private void initString(String names, List<String> nameList) {
        if (names.length() > 0){
            String[] namesSplits = StringUtils.split(names, ",");
            for (String n: namesSplits){
                nameList.add(n);
            }
        }
    }

    private void init( String regions, String variants,  List<Region> regionList, List<Variant> variantList) {
        int regionsSize = 0;

        if (regions.length() > 0) {
            String[] regSplits = StringUtils.split(regions,",");
            for (String s : regSplits) {
                Region r = Region.parseRegion(s);
                regionList.add(r);
                regionsSize += r.getEnd() - r.getStart();
            }
        }

        if (variants.length() > 0) {
            String[] variantsSplits = StringUtils.split(variants,"&");
            for (String v : variantsSplits) {
                variantList.add(new Variant(v));
            }
        }

    }



    @GET
    @Path("/annotation")
    @Produces("application/json")
    @ApiOperation(value = "Get pharmacogenomic annotation variants by region")
    public Response getPharmaVariantsAnnotationByRegionList(@ApiParam(value = "regions") @QueryParam("regions") @DefaultValue("") String regions,
                                        @ApiParam(value = "variants") @QueryParam("variants") @DefaultValue("") String variants,
                                        @ApiParam(value = "names") @QueryParam("names") @DefaultValue("") String names,
                                        @ApiParam(value = "rs") @QueryParam("rs") @DefaultValue("") String rs,
                                        @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                        @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                        @ApiParam(value = "skipCount") @QueryParam("skipCount") @DefaultValue("false") boolean skipCount,
                                        @ApiParam(value = "csv") @QueryParam("csv") @DefaultValue("false") boolean csv
    ) {

        List<Region> regionList = new ArrayList<>();
        List<Variant> variantList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        List<String> rsList = new ArrayList<>();

        init(regions, variants, regionList,variantList);
        initString(names, nameList);
        initString(rs, rsList);

        MutableLong count = new MutableLong(-1);

        if (csv) {
            skip = 0;
            limit = 200;
        }
        //long start = System.currentTimeMillis();

        Iterable<AnnotationPharmaAllele> variantsPharma = null;

        try {
            variantsPharma = qm.getPharmaVariantsAnnotationByRegionList(regionList, variantList, nameList, rsList, skip, limit, skipCount, count);
        } catch (Exception e1){
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(variantsPharma);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("regions", regionList);

        if (!csv) {
            qr.addQueryOption("limit", limit);
            qr.addQueryOption("skip", skip);
        } else {
            qr.addQueryOption("csv", csv);
        }

        return createOkResponse(qr);
    }

}
