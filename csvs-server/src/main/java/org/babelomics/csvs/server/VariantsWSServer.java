package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.Variant;
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

        System.out.println("diseases = " + diseases);
        System.out.println("technologies = " + technologies);

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
        Iterable<Variant> variants = qm.getVariantsByRegionList(regionList, diseaseList, technologyList, skip, limit, skipCount, count);
        long end = System.currentTimeMillis();

        System.out.println("(end-start) = " + (end - start));

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

        System.out.println("diseaseList = " + diseaseList);
        System.out.println("technologyList = " + technologyList);

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


}
