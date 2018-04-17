package org.babelomics.csvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.babelomics.csvs.lib.models.Technology;
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
@Path("/technologies")
@Api(value = "technologies", description = "Technologies")
@Produces(MediaType.APPLICATION_JSON)
public class TechnologiesWSServer extends CSVSWSServer {
    public TechnologiesWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);

    }

    @GET
    @Path("/list")
    @Produces("application/json")
    @ApiOperation(value = "List technologies")
    public Response getAllDiseases() {

        List<Technology> res = qm.getAllTechnologies();

        System.out.println("res = " + res);

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);

    }
}
