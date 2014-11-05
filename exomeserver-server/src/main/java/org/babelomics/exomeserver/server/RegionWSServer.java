package org.babelomics.exomeserver.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.variant.mongodb.VariantMongoDBAdaptor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Path("/region")
@Api(value = "region", description = "Region")
@Produces(MediaType.APPLICATION_JSON)
public class RegionWSServer extends ExomeServerWSServer {

    private VariantMongoDBAdaptor variantMongoDbAdaptor;


    public RegionWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException, IllegalOpenCGACredentialsException {
        super(version, uriInfo, hsr);

        variantMongoDbAdaptor = new VariantMongoDBAdaptor(credentials);
    }

    @GET
    @Path("/{regions}/variants")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(@ApiParam(value = "regions") @PathParam("regions") String regionId) {

        queryOptions.put("merge", true);

        // Parse the provided regions. The total size of all regions together 
        // can't excede 1 million positions
        int regionsSize = 0;
        List<Region> regions = new ArrayList<>();
        for (String s : regionId.split(",")) {
            Region r = Region.parseRegion(s);
            regions.add(r);
            regionsSize += r.getEnd() - r.getStart();
        }

        if (regionsSize <= 1000000) {
            List<QueryResult> allVariantsByRegionList = variantMongoDbAdaptor.getAllVariantsByRegionList(regions, queryOptions);
            return createOkResponse(allVariantsByRegionList);
        } else {
            return createErrorResponse("The total size of all regions provided can't exceed 1 million positions. "
                    + "If you want to browse a larger number of positions, please provide the parameter 'histogram=true'");
        }
    }

    @OPTIONS
    @Path("/{region}/variants")
    public Response getVariantsByRegion() {
        return createOkResponse("");
    }
}
