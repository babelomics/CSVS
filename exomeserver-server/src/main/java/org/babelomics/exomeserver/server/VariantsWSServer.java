package org.babelomics.exomeserver.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.babelomics.exomeserver.lib.mongodb.dbAdaptor.ExomeServerVariantMongoDBAdaptor;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.ArchivedVariantFile;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;

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


@Path("/variants")
@Api(value = "variants", description = "Variants")
@Produces(MediaType.APPLICATION_JSON)
public class VariantsWSServer extends ExomeServerWSServer {

    private ExomeServerVariantMongoDBAdaptor variantMongoDbAdaptor;


    public VariantsWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException, IllegalOpenCGACredentialsException {
        super(version, uriInfo, hsr);

        variantMongoDbAdaptor = new ExomeServerVariantMongoDBAdaptor(credentials);
    }

    @GET
    @Path("/{regions}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(@ApiParam(value = "regions") @PathParam("regions") String regionId,
                                        @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                        @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip) {

        queryOptions.put("merge", true);
        queryOptions.put("limit", limit);
        queryOptions.put("skip", skip);

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

            transformVariants(allVariantsByRegionList);


            return createOkResponse(allVariantsByRegionList);
        } else {
            return createErrorResponse("The total size of all regions provided can't exceed 1 million positions. "
                    + "If you want to browse a larger number of positions, please provide the parameter 'histogram=true'");
        }
    }

    private void transformVariants(List<QueryResult> allVariantsByRegionList) {


        for (QueryResult qr : allVariantsByRegionList) {
            List<Variant> variantList = qr.getResult();
            List<Variant> newVariantList = new ArrayList<>(variantList.size());

            for (Variant v : variantList) {
                combineFiles(v.getFiles());
            }


//            qr.setResult(newVariantList);

        }
    }

    private void combineFiles(Map<String, ArchivedVariantFile> files) {
        ArchivedVariantFile newAVF = new ArchivedVariantFile("MAF", "MAF");
        VariantStats stats = new VariantStats();
        newAVF.setStats(stats);

        for (Map.Entry<String, ArchivedVariantFile> entry : files.entrySet()) {
            ArchivedVariantFile avf = entry.getValue();
            for (Map.Entry<Genotype, Integer> o : avf.getStats().getGenotypesCount().entrySet()) {
                stats.addGenotype(o.getKey(), o.getValue());
            }
//            files.remove(entry.getKey());
        }

        files.clear(); // TODO aaleman: clear all but static studies.

        files.put("MAF", newAVF);
    }


    @OPTIONS
    @Path("/{region}/variants")
    public Response getVariantsByRegion() {
        return createOkResponse("");
    }


}
