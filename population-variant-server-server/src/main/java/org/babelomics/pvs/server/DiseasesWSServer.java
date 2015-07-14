package org.babelomics.pvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.babelomics.pvs.lib.models.DiseaseGroup;

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
public class DiseasesWSServer extends PVSWSServer {
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

        return createOkResponse(res);
//        return createErrorResponse("The total size of all regions provided can't exceed 1 million positions. ");

    }
}
