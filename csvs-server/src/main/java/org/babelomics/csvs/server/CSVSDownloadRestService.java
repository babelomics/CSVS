package org.babelomics.csvs.server;

import java.io.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/download")
@Api(value = "Downloads", description = "Downloads file")
public class CSVSDownloadRestService extends CSVSWSServer {
    public CSVSDownloadRestService(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);
    }

    static String EXTENSION_DEFAULT = ".csv";
    static String SENT = "sent"; // Value received from send mail

    @POST
    @Path("/{disease}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Download file")
    public Response downloadREST(@ApiParam(value = "disease") @DefaultValue("") @PathParam("disease") String disease,
                                 @ApiParam(value = "subject") @DefaultValue("") @FormParam("subject") String subject,
                                 @ApiParam(value = "text") @DefaultValue("") @FormParam("text") String text,
                                 @ApiParam(value = "html") @DefaultValue("") @FormParam("html") String html,
                                 @ApiParam(value = "from") @DefaultValue("") @FormParam("from") String from
    ) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("subject", subject);
            map.put("text", text);
            map.put("html", html);
            map.put("from", from);

            if (disease != null) {
                String urlParameters = preSendMail(map,  "add");
                logger.info("CSVS: Parameters send: " + urlParameters);
                if (sendMail(urlParameters)) {

                    logger.info("CSVS: Download file: " + DOWNLOAD_PATH + disease + EXTENSION_DEFAULT);

                    // Send url
                    if ("".equals(DOWNLOAD_PATH)) {
                        return Response.ok()
                                .status(200).entity("{\"data\":{\"url\":\"" + disease + EXTENSION_DEFAULT + "\"}}")
                                .header("Access-Control-Allow-Origin", "*")
                                .build();
                    } else {
                        // Send file
                        File file = new File(DOWNLOAD_PATH + disease + EXTENSION_DEFAULT);
                        if (file.exists()) {
                            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                                    .status(200)
                                    .header("Access-Control-Allow-Origin", "*")
                                    .header("Access-Control-Allow-Credentials", false)
                                    .header("Access-Control-Max-Age", "86400") // 24 hours
                                    .header("Access-Control-Allow-Headers", "X-Requested-With, X-HTTP-Method-Override, Content-Type, Accept,  Authorization, Access-Control-Allow-Origin")
                                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                                    .build();
                        } else {
                            return Response.status(200).entity("{\"data\":{\"error\":\"File not exist\"}}")
                                    .header("Access-Control-Allow-Origin", "*")
                                    .build();
                        }
                    }
                } else {
                    return Response.status(200)
                            .header("Access-Control-Allow-Origin", "*")
                            .entity("{\"data\":{\"error\": \"Problem with download\"}}").build();
                }
            } else {
                return Response.status(200)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity("{\"data\":{\"error\":'Disease not exist\"}}").build();
            }
        } // end try
        catch (Exception e) {
            System.err.println("Error: " + e);
            logger.error("CSVS: " + e);
            return Response.status(200).entity("{\"data\":{\"error\": \"Problem with download\"}}").build();
        }
    }


    @GET
    @Path("/{disease}/{params}")
    @ApiOperation(value = "Download file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadRESTService(@ApiParam(value = "disease") @PathParam("disease") String disease,
                                        @ApiParam(value = "params") @PathParam("params") String params) throws FileNotFoundException {

        if (disease != null) {

            if (sendMail(params)) {

                logger.info("CSVS: Download file: " + DOWNLOAD_PATH + disease + EXTENSION_DEFAULT);

                // Send url
                if ("".equals(DOWNLOAD_PATH)) {
                    return Response.ok()
                            .status(200).entity("{\"data\":{\"url\":\"" + disease + EXTENSION_DEFAULT + "\"}}")
                            .header("Access-Control-Allow-Origin", "*")
                            .build();
                } else {
                    File file = new File(DOWNLOAD_PATH + disease + EXTENSION_DEFAULT);

                    if (file.exists()) {
                        logger.info("File" + file.getName());

                        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                                .header("Access-Control-Allow-Origin", "*")
                                .header("Access-Control-Allow-Headers", "x-requested-with, content-type")
                                .header("Access-Control-Allow-Credentials", false)
                                .build();
                    } else {
                        return Response.status(200).entity("{\"data\":{\"error\":\"File not exist\"}}").build();
                    }
                }

            } else {
                return Response.status(200).entity("{\"data\":{\"error\":\"Problem with download\"}}").build();
            }
        } else {
            return Response.status(200).entity("{\"data\":{\"error\":\"Disease not exist\"}}").build();

        }
    }


    /**
     * Method to preparate params to send mail.
     *
     * @param map params to send mail
     * @return
     */
    private String preSendMail(Map<String, String> map, String action) {
        map.putAll(configMail);
        map.put(CSVSWSServer.FROM, configMail.get(CSVSWSServer.TO));

        return mapToString(map);
    }
}