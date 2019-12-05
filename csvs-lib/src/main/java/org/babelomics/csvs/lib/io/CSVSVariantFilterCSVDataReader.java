package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Panel;
import org.babelomics.csvs.lib.models.Region;
import org.babelomics.csvs.lib.models.Variant;
import org.opencb.biodata.models.variant.VariantFactory;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.VariantSourceEntry;
import org.opencb.biodata.models.variant.VariantVcfFactory;
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

    public CSVSVariantFilterCSVDataReader(String filePath,  Panel p, List<Region> regions, String chromGender) {
        super(filePath, p, regions, chromGender);
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
                    splits[0] = splits[0].toUpperCase().replace("CHR","");
                    if ("M".equals(splits[0])) {
                        splits[0] = "MT";
                    }

                    // Ignore chromosome Y when is a woman
                    if (XX.equals(chromGender) && "Y".equals(splits[0])){
                        System.out.println("Ignore chromosome: " + line);
                        continue;
                    }

                    // Ignore if 0/1 and 1/1 and ./. is 0
                    if ("0".equals(splits[6])  && "0".equals(splits[7]) && "0".equals(splits[8])) {
                        System.out.println("No variant: " + line);
                        continue;
                    }

                    // if more that one alternate
                    String normalizeLine;
                    if (splits[3].contains(",")){
                        String[] listAlternate = splits[3].split(",");
                        // Divide in file
                        for (String alt : listAlternate) {
                            splits[3] = alt;
                            splits[8] = String.valueOf( Integer.parseInt(splits[6])+Integer.parseInt(splits[7])+Integer.parseInt(splits[8]));
                            splits[6] = "0";
                            splits[7] = "0";
                            normalizeLine = normalize(splits);
                            if (checkPanel(normalizeLine))
                                printer.append(normalizeLine).append("\n");
                            else
                                System.out.println("Variant not in list regions: " + line);
                        }
                    } else {
                        normalizeLine = normalize(splits);
                        if (checkPanel(normalizeLine))
                            printer.append(normalizeLine).append("\n");
                        else
                            System.out.println("Variant not in list regions: " + line);
                    }
                }
            }
            return variants;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean checkPanel(String normalizeLine) {
        if (panel != null){
            // inicialize variant
            String[] normalizeSplits = normalizeLine.split("\t");

            Variant v = new Variant(normalizeSplits[0], Integer.parseInt(normalizeSplits[1]), normalizeSplits[2], normalizeSplits[3]);

            // Add variants if it is in list de regions of file
            if (!panel.contains(v, regions))
                return false;
        }

        return true;
    }

    /**
     * Normalize
     * @param splits
     * @return
     */
    private String normalize(String[] splits) {
        String[] splitsTemp = splits.clone();
        VariantVcfFactory factory = new VariantVcfFactory();
        List<String> genericData = new ArrayList();
        genericData.add(splitsTemp[0]);
        genericData.add(splitsTemp[1]);
        genericData.add(".");
        genericData.add(splitsTemp[2]);
        genericData.add(splitsTemp[3]);
        genericData.add(".");
        genericData.add(".");
        genericData.add(".");
        genericData.add("GT");
        genericData.add("1/1");

        VariantSource source = new VariantSource("file", "file", "file", "file");
        List<String> samples = new ArrayList<>();
        samples.add("fictional");
        source.setSamples(samples);
        List<org.opencb.biodata.models.variant.Variant> listVariants = factory.create(source, String.join("\t",genericData));

        if (listVariants.size()>0) {
            splitsTemp[1] = String.valueOf(listVariants.get(0).getStart());
            splitsTemp[2] = String.valueOf(listVariants.get(0).getReference());
            splitsTemp[3] = String.valueOf(listVariants.get(0).getAlternate());
        }

        return  String.join("\t", splitsTemp);
    }
}