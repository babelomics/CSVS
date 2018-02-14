package org.babelomics.csvs.lib.io;

import org.opencb.commons.io.DataReader;
import org.babelomics.csvs.lib.models.Region;
import org.babelomics.csvs.lib.models.Panel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
public class CSVSRegionsCSVDataReader implements DataReader<Region> {

    private String filePath;
    private Panel panel;
    private BufferedReader reader;

    public CSVSRegionsCSVDataReader(String filePath, Panel panel) {
        this.filePath = filePath;
        this.panel = panel;
    }

    @Override
    public boolean open() {

        Path path = Paths.get(this.filePath);
        try {
            this.reader = Files.newBufferedReader(path, Charset.defaultCharset());
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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean pre() {

        try {
            reader.readLine();
        } catch (IOException e) {

            return false;
        }

        return true;
    }

    @Override
    public boolean post() {
        return true;
    }

    @Override
    public List<Region> read() {

        List<Region> regions;

        String line;
        try {
            regions = new ArrayList<>();
            while ((line = this.reader.readLine()) != null) {

                if (!line.trim().equals("")) {

                    String[] splits = line.split("\t");

                    String c = splits[0].toUpperCase().replace("CHR","");

                    Region r = new Region(c, Integer.parseInt(splits[1]), Integer.parseInt(splits[2]), panel);

                    regions.add(r);
                    return regions;
                }

            }
            return regions;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Region> read(int batchSize) {
        List<Region> listRecords = new ArrayList<>(batchSize);

        int i = 0;
        List<Region> regions;
        while ((i < batchSize) && (regions = this.read()) != null && regions.size() > 0) {
            listRecords.addAll(regions);
            i += regions.size();
        }

        return listRecords;
    }
}
