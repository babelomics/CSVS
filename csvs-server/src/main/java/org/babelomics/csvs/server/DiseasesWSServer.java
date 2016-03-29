package org.babelomics.csvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.ws.QueryResponse;

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

@Path("/diseases")
@Api(value = "Diseases", description = "Diseases")
@Produces(MediaType.APPLICATION_JSON)
public class DiseasesWSServer extends CSVWSServer {
    public DiseasesWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);

    }

    @GET
    @Path("/list")
    @Produces("application/json")
    @ApiOperation(value = "List diseases")
    public Response getAllDiseases() {

        List<DiseaseGroup> res = qm.getAllDiseaseGroups();

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);

    }
}
