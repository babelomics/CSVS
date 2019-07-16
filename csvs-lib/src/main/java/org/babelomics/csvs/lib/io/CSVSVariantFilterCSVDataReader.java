package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Panel;
import org.babelomics.csvs.lib.models.Region;
import org.babelomics.csvs.lib.models.Variant;
import org.opencb.commons.io.DataReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
public class CSVSVariantFilterCSVDataReader extends CSVSVariantCountCSVDataReader {
    protected PrintWriter printer;


    public CSVSVariantFilterCSVDataReader(String filePath,  Panel p, List<Region> regions) {
        super(filePath, p, regions);
    }


    @Override
    public boolean pre() {

        try {
            printer.append(reader.readLine()).append("\n");
            return true;
        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public boolean open() {
        Path path = Paths.get(this.filePath);

        try {

            this.reader = Files.newBufferedReader(path, Charset.defaultCharset());
            printer = new PrintWriter("Filter_" + path.getFileName() );

        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            System.err.println("ERROR: opening inputFile");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean close() {
        try {
            this.reader.close();
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<Variant> read() {

        List<Variant> variants;

        String line;
        try {
            variants = new ArrayList<>();
            while ((line = this.reader.readLine()) != null) {

                if (!line.trim().equals("") &&  !line.startsWith("#")) {

                    String[] splits = line.split("\t");
		   
                    // Replace CHR
                    String c = splits[0].toUpperCase().replace("CHR","");
                    if ("M".equals(c))
                        c="MT";

                    Variant v = new Variant(c, Integer.parseInt(splits[1]), splits[2], splits[3]);
                    if (!splits[4].isEmpty() && !splits[4].equals(".")) {
                        v.setIds(splits[4]);
                    }

                    // Add variants if it is in list de regions of file
                    if (panel != null  &&  !panel.contains(v, regions)) {
                        System.out.println("Variant not in list regions: " + line);
                        continue;
                    }

                    printer.append(line.replace(splits[0]+"\t",c+"\t")).append("\n");

                }

            }
            return variants;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
