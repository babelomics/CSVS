package org.babelomics.csvs.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class CSVSFileDownload extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;

    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    static String downloadPath;
    static String type = ".csv";

    static {

        InputStream is = CSVSWSServer.class.getClassLoader().getResourceAsStream("csvs.properties");
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


    protected void doOptions(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
    }


    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        downloadReport(request,response);
    }


    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        downloadReport(request,response);
    }


    private void downloadReport(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String filename = null;

        // Get option download
        String p = request.getPathInfo();
        if (p != null) {
            filename = p.substring(p.lastIndexOf("/") + 1, p.length());
            logger.debug("CSVS: getPathInfo() " + filename);
        }

        System.out.println(downloadPath + filename + type);
        File data = new File(downloadPath + filename + type);
        logger.debug("CSVS: File " + downloadPath + filename + type);
        PrintWriter out = response.getWriter();
        // Check exist file
        if (!data.exists())
        {
            // System.out.println("File doesn't exist");
            logger.info("CSVS: File doesn't exist (" + filename + type + ")");
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

            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + type + "\"");
            response.setHeader("Content-Description", "File Transfer");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "x-requested-with, content-type");

            FileInputStream fl = new FileInputStream(downloadPath + filename + type);

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


/*

    static byte[] buffer = new byte[1024];


    public static void download(final URL url) {
        FileOutputStream fileOutputStream = null;
        String filepath = "/var/www/latest/download/";
        InputStream inputStream = null;
        int len = 0;
        int off = 0;
        //this.url = url;
        try {
            // Establish connection
            URLConnection urlConnection = url.openConnection();

            //System.out.println('CSVS: Type file: ' + urlConnection.getContentType());

            // Get file
            String p = url.getFile();
            String filename = p.substring(p.lastIndexOf("/") + 1, p.length());
            System.out.println("CSVS: Name file: " + filename);
            fileOutputStream = new FileOutputStream(filename);
            inputStream = new FileInputStream(filepath + filename);
            System.out.println("CSVS: InputStream " + inputStream.toString());


            System.out.println("CSVS: Download...");
            // Read file server and write local file
            while ((len = inputStream.read(buffer)) >= 0) { // buffer temporal read
                fileOutputStream.write(buffer, off, len);
                fileOutputStream.flush();
            }
            System.out.println("CSVS: Download done: "); //+location+this);


            /*
            // Lectura de la foto de la web y escritura en fichero local
            byte[] array = new byte[1000]; // buffer temporal de lectura.
            int leido = is.read(array);
            while (leido > 0) {
                fos.write(array, 0, leido);
                leido = is.read(array);
            }

            // cierre de conexion y fichero.
            is.close();
            fos.close();


        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException ioException2) {
                ioException2.printStackTrace();
            }
        }
    }
}
*/
