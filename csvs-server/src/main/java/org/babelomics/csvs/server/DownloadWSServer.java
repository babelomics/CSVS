package org.babelomics.csvs.server;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.babelomics.csvs.lib.token.CSVSToken;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/download")
@Api(value = "download", description = "Download files")
public class DownloadWSServer extends CSVSWSServer {
    protected String sessionId;

    static String EXTENSION_DEFAULT = "";
    static String SID = "sid";
    static String SENT = "sent"; // Value received from send mail

    public DownloadWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest,
                            @Context HttpHeaders httpHeaders) throws IOException {

        super(version, uriInfo, httpServletRequest);
        try {
            verifyHeaders(httpHeaders, uriInfo.getQueryParameters().getFirst("sid"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @DefaultValue("")
    @QueryParam("sid")
    @ApiParam("Session id")
    protected String dummySessionId;

    @GET
    @Path("/file/{disease}")
    @ApiOperation(value = "Download files")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadRESTService(@PathParam("disease") String disease) throws FileNotFoundException {

        if (disease != null) {
            CSVSToken csvsToken = new CSVSToken(SECRET_KEY);
            Claims claims = null;
            String msj = "";
            try {
                claims = csvsToken.decodeJWT(sessionId);
            } catch (Exception e){
                logger.error("CSVS: Download file: " + DOWNLOAD_PATH + disease + EXTENSION_DEFAULT + " " + e.toString());
                msj = e.toString();
            }

            msj = msj + checkDownloadDisease(claims, disease);
            if (claims != null && msj.isEmpty()) {
                String text = "Token jwt: " + this.sessionId + "\n"+
                        "ID: " + claims.getId()+ "\n"+
                        "Subject: " + claims.getSubject()+ "\n"+
                        "Issuer: " + claims.getIssuer()+ "\n"+
                        "Audidence: " + claims.getAudience()+ "\n"+
                        "Expiration: " + claims.getExpiration()+ "\n"+
                        "Name: " + claims.get(CSVSToken.NAME)+ "\n"+
                        "Subpopulations: " + claims.get(CSVSToken.SUBPOPULATIONS)+ "\n";

                Map<String, String> map = new HashMap<String, String>();
                map.put("subject", "Download file:" + disease);
                map.put("text", text);
                map.put("html", text);
                map.putAll(configMail);
                map.put(CSVSWSServer.FROM, configMail.get(CSVSWSServer.TO));

                String params = mapToString(map);
                logger.info("CSVS: Parameters send: " + params);

                if (!sendMail(params))
                    logger.error("No email send.");

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
            }else{
                return Response.status(200).entity("{\"data\":{\"error\":\" " + msj + " \"}}").build();
            }
        } else {
            return Response.status(200).entity("{\"data\":{\"error\":\"Disease not exist\"}}").build();
        }
    }

    /**
     * Check data token and send msj if not valid.
     * @param claims
     * @return
     */
    private String checkDownloadDisease(Claims claims, String disease ) {
        String msj = "";
        if (claims != null) {
            List subpopulations = (List) claims.get(CSVSToken.SUBPOPULATIONS);
            if (!subpopulations.contains(disease))
                msj = "Access to supopulation " + disease + " not allowed";
        } else
            msj = "Token no valid";

        return msj;
    }


    /**
     * Get sid
     * @param httpHeaders
     * @param sid
     * @throws Exception
     */
    private void verifyHeaders(HttpHeaders httpHeaders, String sid) throws Exception {

        List<String> authorization = httpHeaders.getRequestHeader("Authorization");

        if (authorization != null && authorization.get(0).length() > 7) {
            String token = authorization.get(0);
            if (!token.startsWith("Bearer ")) {
                throw new Exception("Authorization header must start with Bearer JWToken");
            }
            this.sessionId = token.substring("Bearer".length()).trim();
        }

        if (StringUtils.isEmpty(this.sessionId)) {
            this.sessionId = sid;
        }
    }

}