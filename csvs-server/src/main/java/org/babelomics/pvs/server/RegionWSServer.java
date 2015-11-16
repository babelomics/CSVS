package org.babelomics.pvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.babelomics.csvs.lib.models.IntervalFrequency;
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
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
@Path("/regions")
@Api(value = "regions", description = "Regions")
@Produces(MediaType.APPLICATION_JSON)
public class RegionWSServer extends PVSWSServer {
    public RegionWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException {
        super(version, uriInfo, httpServletRequest);
    }
    @GET
    @Path("/{regionsParam}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(
            @ApiParam(value = "regionsParam") @PathParam("regionsParam") String regions,
            @ApiParam(value = "histogram") @QueryParam("histogram") @DefaultValue("false") boolean histogram,
            @ApiParam(value = "histogramLogarithm") @QueryParam("histogramLogarithm") @DefaultValue("false") boolean histogramLogarithm,
            @ApiParam(value = "histogramMax") @QueryParam("histogramMax") @DefaultValue("500") int histogramMax,
            @ApiParam(value = "interval") @QueryParam("interval") @DefaultValue("280") int interval
    ) {

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
}
