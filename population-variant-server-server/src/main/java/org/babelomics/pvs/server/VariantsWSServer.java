package org.babelomics.pvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.pvs.lib.models.Variant;
import org.babelomics.pvs.lib.ws.QueryResponse;
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
public class VariantsWSServer extends PVSWSServer {

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
                                        @ApiParam(value = "studies") @QueryParam("studies") String studies,
                                        @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
                                        @ApiParam(value = "phenotypes") @QueryParam("phenotypes") String phenotypes,
                                        @ApiParam(value = "csv") @QueryParam("csv") @DefaultValue("false") boolean csv
    ) {


        List<Region> regionList = new ArrayList<>();
        List<Integer> diseaseList = new ArrayList<>();
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

        MutableLong count = new MutableLong(-1);

        if (csv) {
            skip = 0;
            limit = 200;
        }

        Iterable<Variant> variants = qm.getVariantsByRegionList(regionList, diseaseList, skip, limit, count);

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

}
