package org.babelomics.pvs.lib.io;

import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.babelomics.pvs.lib.models.Variant;
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
public class PVSVariantCountCSVDataReader implements DataReader<Variant> {

    private Path path;
    private String filePath;
    private BufferedReader reader;
    private DiseaseGroup diseaseGroup;

    public PVSVariantCountCSVDataReader(String filePath, DiseaseGroup dg) {
        this.filePath = filePath;
        this.diseaseGroup = dg;
    }

    @Override
    public boolean open() {

        this.path = Paths.get(this.filePath);
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

                    Variant v = new Variant(splits[0], Integer.parseInt(splits[1]), splits[2], splits[3]);

                    v.addGenotypesToDisease(this.diseaseGroup, Integer.parseInt(splits[5]), Integer.parseInt(splits[6]), Integer.parseInt(splits[7]), Integer.parseInt(splits[8]));

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
