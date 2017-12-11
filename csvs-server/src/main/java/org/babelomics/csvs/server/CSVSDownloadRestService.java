package org.babelomics.csvs.server;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class CSVSDownloadRestService {

    private static final long serialVersionUID = 1L;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;

    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    static String DOWNLOAD_PATH;
    static String EXTENSION_DEFAULT = ".csv";
    static String PROPERTIES = "csvs.properties";
    static String URL_MAIL_DEFAULT;
    static String SENT = "sent"; // Value received from send mail

    static {

        InputStream is = CSVSWSServer.class.getClassLoader().getResourceAsStream(PROPERTIES);
        properties = new Properties();

        try {
            properties.load(is);

        } catch (IOException e) {
            System.out.println("Error loading properties" + e.getMessage());
            e.printStackTrace();
        }

        jsonObjectMapper = new ObjectMapper();
        jsonObjectWriter = jsonObjectMapper.writer();

        DOWNLOAD_PATH = properties.getProperty("CSVS.DOWNLOAD_PATH", "");
        URL_MAIL_DEFAULT = properties.getProperty("CSVS.URL_MAIL_DEFAULT", "http://localhost:8081");
    }


    @POST
    @Path("/download/{disease}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadREST(@DefaultValue("") @PathParam("disease") String disease,
                                 @DefaultValue("") @FormParam("subject") String subject,
                                 @DefaultValue("") @FormParam("text") String text,
                                 @DefaultValue("") @FormParam("html") String html,
                                 @DefaultValue("") @FormParam("from") String from,
                                 @DefaultValue("") @FormParam("to") String to,
                                 @DefaultValue("") @FormParam("host") String host,
                                 @DefaultValue("") @FormParam("port") String port,
                                 @DefaultValue("") @FormParam("secure") String secure,
                                 @DefaultValue("") @FormParam("debug") String debug,
                                 @DefaultValue("") @FormParam("ignoreTLS") String ignoreTLS,
                                 @DefaultValue("") @FormParam("user") String user,
                                 @DefaultValue("") @FormParam("pass") String pass
    ) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("subject", subject);
            map.put("text", text);
            map.put("html", html);
            map.put("from", from);
            map.put("to", to);
            map.put("host", host);
            map.put("port", port);
            map.put("secure", secure);
            map.put("debug", debug);
            map.put("ignoreTLS", ignoreTLS);
            map.put("user", user);
            map.put("pass", pass);

            String parmas = CSVSDownloadRestService.mapToString(map);
            logger.info("CSVS: Parameters send: " + parmas);

            if (disease != null) {
                if (sendMail(parmas)) {

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
    @Path("/download/{disease}/{params}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadRESTService(@PathParam("disease") String disease, @PathParam("params") String params) throws FileNotFoundException {

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
     * Method to send mail
     *
     * @param urlParameters
     * @return
     */
    private boolean sendMail(String urlParameters) {

        try {
            System.out.print("CSVS: " + URL_MAIL_DEFAULT);

            URL myURL = new URL(URL_MAIL_DEFAULT);
            logger.info("CSVS: URL " + myURL);

            System.out.print("Post parameters : " + urlParameters);
            logger.info("CSVS: URL " + myURL);

            // Connect
            HttpURLConnection con = (HttpURLConnection) myURL.openConnection();
            con.setDoOutput(true);

            // Send
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
            logger.info("CSVS: outputStreamWriter : " + outputStreamWriter);
            outputStreamWriter.write(urlParameters);
            logger.info("CSVS: urlParameters : " + urlParameters);
            outputStreamWriter.flush();


            // Response
            int responseCode = con.getResponseCode();
            logger.info("CSVS: Response Code : " + responseCode);

            // Read file
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String result = response.toString();
            logger.info("CSVS: result : " + result);

            // Parse info to json
            HashMap<String, Object> json = new ObjectMapper().readValue(result, HashMap.class);
            logger.info("CSVS: DATA" + json.get("data"));

            if (!json.isEmpty() && SENT.equals(json.get("data")))
                return true;
            else
                return false;
        } // end try
        catch (IOException e) {
            System.err.println("Error: " + e);
            logger.error("CSVS: " + e);
        }
        return false;
    }


    /**
     * Parse map to string
     *
     * @param map
     * @return
     */
    private static String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();
    }

}
