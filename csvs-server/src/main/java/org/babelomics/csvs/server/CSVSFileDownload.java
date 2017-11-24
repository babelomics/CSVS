package org.babelomics.csvs.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to download a file
 *
 * @author grg
 */
@WebServlet(name="download", urlPatterns = "/download/*")
public class CSVSFileDownload extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;

    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    static String downloadPath;
    static String EXTENSION_DEFAULT = ".csv";
    static String PROPERTIES = "csvs.properties";

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

        downloadPath = properties.getProperty("CSVS.DOWNLOAD_PATH", "");
    }

    /**
     * Method download file from POST
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        downloadReport(request,response);
    }

    /**
     * Method download file from GET
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        downloadReport(request,response);
    }

    /**
     * Methon download File
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
     private void downloadReport(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String filename = null;
        String extension = EXTENSION_DEFAULT;

        // Get option download
        String p = request.getPathInfo();
        if (p != null) {
            filename = p.substring(p.lastIndexOf("/") + 1, p.length());
            logger.debug("CSVS: getPathInfo() " + filename);
        }

        System.out.println(downloadPath + filename + extension);
        File data = new File(downloadPath + filename + extension);
        logger.debug("CSVS: File " + downloadPath + filename + extension);
        PrintWriter out = response.getWriter();
        // Check exist file
        if (!data.exists())
        {
            // System.out.println("File doesn't exist");
            logger.info("CSVS: File doesn't exist (" + filename + extension + ")");
            response.setHeader("Content-type", "application/json;");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "x-requested-with, content-type");

            out.println("{\"error\":\"File doesn't exist.\"}");
        } else {
            // Donwload file
            logger.debug("CSVS: Download...");
            //response.setContentType("APPLICATION/OCTET-STREAM");
            response.setHeader("Content-Transfer-Encoding","binary");
            response.setHeader("Content-Type","binary/octet-stream");

            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + extension + "\"");
            response.setHeader("Content-Description", "File Transfer");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "x-requested-with, content-type");

            FileInputStream fl = new FileInputStream(downloadPath + filename + extension);

            int i;
            while ((i = fl.read()) != -1) {
                out.write(i);
            }
            fl.close();
            out.close();

            logger.debug("CSVS: Download done! ");
        }
    }
}