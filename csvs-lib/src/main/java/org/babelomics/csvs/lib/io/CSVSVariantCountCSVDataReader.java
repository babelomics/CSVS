package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Technology;
import org.babelomics.csvs.lib.models.Variant;
import org.babelomics.csvs.lib.models.Panel;
import org.opencb.commons.io.DataReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSVariantCountCSVDataReader implements DataReader<Variant> {

    protected String filePath;
    protected BufferedReader reader;
    private DiseaseGroup diseaseGroup;
    private Technology technology;
    protected Panel panel;
    private boolean checkPanel = true;

    public CSVSVariantCountCSVDataReader(String filePath, DiseaseGroup dg, Technology t) {
        this.filePath = filePath;
        this.diseaseGroup = dg;
        this.technology = t;
    }

    public CSVSVariantCountCSVDataReader(String filePath, Panel p) {
        this.filePath = filePath;
        this.panel = p;
    }

    public CSVSVariantCountCSVDataReader(String filePath, DiseaseGroup dg, Technology t, Panel p ) {
        this.filePath = filePath;
        this.diseaseGroup = dg;
        this.technology = t;
        this.panel = p;
    }

    public CSVSVariantCountCSVDataReader(String filePath, DiseaseGroup dg, Technology t, Panel p, boolean checkPanel) {
        this.filePath = filePath;
        this.diseaseGroup = dg;
        this.technology = t;
        this.panel = p;
        this.checkPanel = checkPanel;
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
    public List<Variant> read() {

        List<Variant> variants;

        String line;
        try {
            variants = new ArrayList<>();
            while ((line = this.reader.readLine()) != null) {

                if (!line.trim().equals("")) {

                    String[] splits = line.split("\t");
		   
                    // Replace CHR
                    String c = splits[0].toUpperCase().replace("CHR","");

                    Variant v = new Variant(c, Integer.parseInt(splits[1]), splits[2], splits[3]);
                    if (!splits[4].isEmpty() && !splits[4].equals(".")) {
                        v.setIds(splits[4]);
                    }

                    // Add variants if it is in list de regions of file
                    if (panel != null  && checkPanel)
			            if( !panel.contains(v)) {
	                        System.out.println("Variant not in list regions: " + line);
	                        continue;
                        }

                    v.addGenotypesToDiseaseAndTechnology(this.diseaseGroup, this.technology, Integer.parseInt(splits[5]), Integer.parseInt(splits[6]), Integer.parseInt(splits[7]), Integer.parseInt(splits[8]));
                    variants.add(v);
                    return variants;
                }

            }
            return variants;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Variant> read(int batchSize) {
        List<Variant> listRecords = new ArrayList<>(batchSize);

        int i = 0;
        List<Variant> variants;
        while ((i < batchSize) && (variants = this.read()) != null && variants.size() > 0) {
            listRecords.addAll(variants);
            i += variants.size();
        }

        return listRecords;
    }
}
