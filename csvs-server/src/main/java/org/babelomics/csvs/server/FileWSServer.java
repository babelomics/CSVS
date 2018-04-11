package org.babelomics.csvs.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.babelomics.csvs.lib.ws.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;


/**
 * @author  Gema Roldán Gonzalez <gema.roldan@juntadeandalucia.es>
 */

@Path("/files")
@Api(value = "files", description = "Files")
@Produces(MediaType.APPLICATION_JSON)
public class FileWSServer extends CSVSWSServer {
    public FileWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);

    }

    @GET
    @Path("/samples")
    @Produces("application/json")
    @ApiOperation(value = "Number of Spanish individuals")
    public Response getSamples() {

        int res = qm.calculateSampleCount();

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);

    }
}
