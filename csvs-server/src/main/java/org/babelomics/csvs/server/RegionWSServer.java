package org.babelomics.csvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.babelomics.csvs.lib.models.IntervalFrequency;
import org.babelomics.csvs.lib.models.SaturationElement;
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
import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
@Path("/regions")
@Api(value = "regions", description = "Regions")
@Produces(MediaType.APPLICATION_JSON)
public class RegionWSServer extends CSVSWSServer {
    public RegionWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException {
        super(version, uriInfo, httpServletRequest);
    }

    @GET
    @Path("/{regions}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(
            @ApiParam(value = "regions") @PathParam("regions") String regions,
            @ApiParam(value = "histogram") @QueryParam("histogram") @DefaultValue("false") boolean histogram,
            @ApiParam(value = "histogramLogarithm") @QueryParam("histogramLogarithm") @DefaultValue("false") boolean histogramLogarithm,
            @ApiParam(value = "histogramMax") @QueryParam("histogramMax") @DefaultValue("500") int histogramMax,
            @ApiParam(value = "interval") @QueryParam("interval") @DefaultValue("280") int interval
    ) {
        String errorAuthentication = checkAuthentication();
        if (!errorAuthentication.isEmpty())
            return createErrorResponse(errorAuthentication);

        List<Region> regionList = Region.parseRegions(regions);
        if (histogram) {

            List<List<IntervalFrequency>> res = qm.getAllIntervalFrequencies(regionList, histogramLogarithm, histogramMax, interval);
            QueryResponse qr = createQueryResponse(res);
            qr.setNumTotalResults(res.size());

            qr.addQueryOption("regions", regionList);
            qr.addQueryOption("histogram", histogram);
            qr.addQueryOption("histogramLogarithm", histogramLogarithm);
            qr.addQueryOption("histogramMax", histogramMax);
            qr.addQueryOption("interval", interval);

            return createOkResponse(qr);
        } else {

            List<List<Variant>> res = qm.getVariantsByRegionList(regionList);
            QueryResponse qr = createQueryResponse(res);
            qr.setNumTotalResults(res.size());

            qr.addQueryOption("regions", regionList);

            return createOkResponse(qr);
        }
    }

    @GET
    @Path("/{regions}/saturation")
    @Produces("application/json")
    @ApiOperation(value = "Get Saturation per gene")
    public Response getVariantsByRegion(@ApiParam(value = "regions") @PathParam("regions") @DefaultValue("") String regions,
                                        @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                        @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                        @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
                                        @ApiParam(value = "technologies") @QueryParam("technologies") @DefaultValue("") String technologies
    ) {

        String errorAuthentication = checkAuthentication();
        if (!errorAuthentication.isEmpty())
            return createErrorResponse(errorAuthentication);

        List<Region> regionList = new ArrayList<>();
        List<Integer> diseaseList = new ArrayList<>();
        List<Integer> technologyList = new ArrayList<>();

        if (regions.length() > 0) {
            String[] regSplits = regions.split(",");
            for (String s : regSplits) {
                Region r = Region.parseRegion(s);
                regionList.add(r);
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

        Map<Region, List<SaturationElement>> res = qm.getSaturationOrderIncrement(regionList, diseaseList, technologyList);

        QueryResponse qr = createQueryResponse(res);
        qr.setNumTotalResults(res.size());

        return createOkResponse(qr);
    }

}
