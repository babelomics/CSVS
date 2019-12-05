package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.ParRegions;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.variant.VariantFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.opencb.biodata.formats.io.FileFormatException;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.vcf4.*;
import org.opencb.biodata.models.variant.VariantVcfFactory;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantFactory;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.exceptions.NotAVariantException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gema Roldán
 */
public class CSVSVariantVcfReader implements VariantReader {

    private Vcf4 vcf4;
    private BufferedReader reader;
    private Path path;

    private String filePath;

    private VariantSource source;
    private VariantFactory factory;

    private boolean replaceAF;
    private String chromGender;
    private List<ParRegions> parRegions;

    public CSVSVariantVcfReader(VariantSource source, String filePath, boolean replaceAF, String chromGender, List<ParRegions> parRegions) {
        this(source, filePath, new VariantVcfFactory(), replaceAF, chromGender, parRegions);
    }

    public CSVSVariantVcfReader(VariantSource source, String filePath, VariantFactory factory, boolean replaceAF, String chromGender, List<ParRegions> parRegions) {
        this.source = source;
        this.filePath = filePath;
        this.factory = factory;
        this.replaceAF = replaceAF;
        this.chromGender = chromGender;
        this.parRegions = parRegions;
    }

    @Override
    public boolean open() {
        try {
            path = Paths.get(filePath);
            Files.exists(path);

            vcf4 = new Vcf4();
            if (path.toFile().getName().endsWith(".gz")) {
                this.reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))));
            } else {
                this.reader = Files.newBufferedReader(path, Charset.defaultCharset());
            }

        }
        catch (IOException  ex) {
            Logger.getLogger(VariantVcfReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }


        return true;
    }

    @Override
    public boolean pre() {
        try {
            processHeader();

            // Copy all the read metadata to the VariantSource object
            // TODO May it be that Vcf4 wasn't necessary anymore?
            source.addMetadata("fileformat", vcf4.getFileFormat());
            source.addMetadata("INFO", vcf4.getInfo().values());
            source.addMetadata("FILTER", vcf4.getFilter().values());
            source.addMetadata("FORMAT", vcf4.getFormat().values());
            for (Map.Entry<String, String> otherMeta : vcf4.getMetaInformation().entrySet()) {
                source.addMetadata(otherMeta.getKey(), otherMeta.getValue());
            }
            source.setSamples(vcf4.getSampleNames());
        } catch (IOException | FileFormatException ex) {
            Logger.getLogger(VariantVcfReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean close() {
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(VariantVcfReader.class.getName()).log(Level.SEVERE, null, ex);
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
        String line;
        try {
            while ((line = reader.readLine()) != null && (line.trim().equals("") || line.startsWith("#"))) ;

            Boolean isReference=true;
            List<Variant> variants = null;
            // Look for a non reference position (alternative != '.')
            while (line != null && isReference) {
                try {
                    if((line.toLowerCase()).startsWith("chr"))
                        line=line.substring(3, line.length());

                    if(line.startsWith("M\t"))
                        line=line.replace("M\t", "MT\t");

                    String[] fields = line.split("\t");
                    if (fields.length > 4) {
                        String alternate = fields[4];
                        // Ignore "." or CNV
                        if (!".".equals(alternate) && !alternate.startsWith("<")) {
                            boolean cParRegions = "XY".equals(chromGender) && ("X".equals(fields[0]) || "Y".equals(fields[0]));

                            String[] alternateAlleles = alternate.split(",");
                            // Replace AF if line have more than one allele (only when AF is not "Allele Frecuency in the .vcf)
                            //   if (alternateAlleles.length > 1) {
                            if (replaceAF && line.contains(";AF=")) {
                                System.out.println("  Replaced: \"AF=[0-9]*\" --> \"\" ");
                                System.out.println("   Before:  " + line);
                                Pattern pattern = Pattern.compile(";AF=[0-9.]*;");
                                Matcher m = pattern.matcher(line);
                                line = m.replaceFirst(";");
                                System.out.println("   After:   " + line);
                            }

                            //Create new a line by alternate alleles
                            variants = splitLines(fields, alternateAlleles, chromGender, cParRegions);
                           //    variants = factory.create(source, cParRegions ? checkParRegions(fields) : line);

                            isReference = false;
                        }else{
                            throw new NotAVariantException("Alternate '.' " + line);
                        }

                    } else
                        throw new NotAVariantException("Fields length < 4" + line);
                } catch (NotAVariantException e) {  // This line represents a reference position (alternative = '.')
                    line = reader.readLine();
                }
            }
            return variants;

        } catch (IOException ex) {
            Logger.getLogger(VariantVcfReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Create new a line by alternate alleles.
     * @return
     */
    private List<Variant> splitLines(String[] fields, String[] alternateAlleles, String chromGender, boolean cParRegions) {
        List<Variant> variants = null;
        if (fields.length < 9) {
            throw new NotAVariantException("Not enough fields provided (min 9)");
        }

        // If not GT, use generic function
        if (!"GT".equals((fields[8].split(":")[0] )))
            return factory.create(source, cParRegions ? checkParRegions(fields) : String.join("\t", fields));

        // Ignore
        if ("XX".equals(chromGender) && "Y".equals(fields[0]))
            throw new NotAVariantException("Chromosome Y is not valid in Female)");

        // Create new lines
        List<String> newLines = new ArrayList<>();

        // Get data GT
        String[] fieldGT = fields[9].split(":");
        String[] GTTemp = fieldGT[0].split("[/\\|]");
        String[] GT = new String[2];
        if (GTTemp.length < 2) {
            switch (GTTemp[0]){
                case "0": case "1":
                    if ("MT".equals(fields[0])) {
                        GT[0] = GTTemp[0];
                        GT[1] = GTTemp[0];
                        break;
                    }

                    // MALE
                    if ("XY".equals(chromGender) && "X".equals(fields[0]) || ("Y".equals(fields[0]))) {
                        GT[0] = GTTemp[0];
                        break;
                    }

                    // FEMALE
                    if("XX".equals(chromGender) && (("X".equals(fields[0]) || "MT".equals(fields[0])))) {
                        GT[0] = GTTemp[0];
                        GT[1] = GTTemp[0];
                        break;
                    }

                    GT[0] = ".";
                    GT[1] = ".";
                    break;

                case ".":
                    // replace by ./.
                    GT[0] = ".";
                    GT[1] = ".";
                    break;
                default:
                    // Trate biodata
                    return factory.create(source, cParRegions ? checkParRegions(fields) : String.join("\t", fields));
            }
            if (!".".equals(GT[0]) && !".".equals(GT[1]))
                System.out.println("--> " + String.join("\t", fields) + "  --> " + String.join(":", GT));
        } else {
            GT[0] = GTTemp[0];
            GT[1] = GTTemp[1];
        }

        // Case GT in "0/0","0/1","1/0","1/1","0|0","0|1","1|0","1|1" get only first
        List<String> options = new ArrayList<>(Arrays.asList("0/0", "0/1", "1/0", "1/1"));
        if (options.contains(String.join("/", GT)) || GT[1] == null) {
            fields[4] = alternateAlleles[0];
            if (!cParRegions)
                fieldGT[0] = GT[0] + "/" + GT[1];
            fields[9] = String.join(":", fieldGT);
            newLines.add(cParRegions ? checkParRegions(fields) : String.join("\t", fields));
        } else {
            // Divide
            for (int i = 0; i < alternateAlleles.length; i++) {
                // Alternative
                fields[4] = alternateAlleles[i];

                // Generate field[9] (GT)
                if (GT[0].equals(GT[1])) {
                    // Replace GT por 1/1
                    if (("" + (i + 1) + "").equals(GT[0]))
                        fieldGT[0] = "1/1";
                    else
                        fieldGT[0] = "./.";
                } else {
                    if ("0".equals(GT[0]) || "0".equals(GT[1])) {
                        // File alte = "0/1"
                        if ("0".equals(GT[0]) && ("" + (i + 1) + "").equals(GT[1]) || "0".equals(GT[1]) && ("" + (i + 1) + "").equals(GT[0]))
                            fieldGT[0] = "0/1";
                        else
                            fieldGT[0] = "./.";
                    } else {
                        // All "./."
                        fieldGT[0] = "./.";
                    }
                }


                fields[9] = String.join(":", fieldGT);
                //System.out.println("--> Divide " + String.join("\t", fields) + "  -->  GT Origin:  " + String.join(":", GT) + "    ---> Final:" +String.join(":", fields[9])+  " --> altr: " +  String.join(",", alternateAlleles));
                newLines.add(cParRegions ? checkParRegions(fields) : String.join("\t", fields));
            }
        }

        for (String newLine: newLines) {
           // System.out.println(newLines);
            if (variants == null)
                variants = new ArrayList<>();

            variants.addAll(factory.create(source, newLine));
        }

        return variants;
    }

    /**
     * Chek is male and chrom = X or chrom = Y and pos in Par Regions
     * @param fields
     * @return
     */
    private String checkParRegions(String[] fields) {
        Integer pos = Integer.parseInt(fields[1]);
        String[] fieldGT = fields[9].split(":");
        ParRegions parRegion = parRegions.stream().filter(pr ->
                    "X".equals(fields[0]) ?
                        fields[0].equals(pr.getX().getChromosome()) && pr.getX().getStart() <= pos && pos <= pr.getX().getEnd() :
                        fields[0].equals(pr.getY().getChromosome()) && pr.getY().getStart() <= pos && pos <= pr.getY().getEnd()
        ).findFirst().orElse(null);

        if(parRegion != null) {
            if ("0".equals(fieldGT[0]) || "1".equals(fieldGT[0])) {

                System.out.println("------> ParRegions: search" + String.join("\t", fields));

                // find chromosome Y
                BufferedReader searchReader = null; //new BufferedReader(reader);
                String searchLine = "";
                try {
                    if (path.toFile().getName().endsWith(".gz")) {
                        searchReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))));
                    } else {
                        searchReader = Files.newBufferedReader(path, Charset.defaultCharset());
                    }

                    boolean find = false;
                    while ((searchLine = searchReader.readLine()) != null && (searchLine.trim().equals("") || searchLine.startsWith("#"))) ;

                    while (searchLine !=  null && !find) {
                        if((searchLine.toLowerCase()).startsWith("chr"))
                            searchLine=searchLine.substring(3, searchLine.length());
                        if((searchLine.startsWith("X") || searchLine.startsWith("Y"))) {
                            String[] fieldsSearch = searchLine.split("\t");
                            if (("X".equals(fields[0]) && "Y".equals(fieldsSearch[0]) && Integer.parseInt(fieldsSearch[1]) == Integer.parseInt(fields[1]) + parRegion.getDiference()) ||
                                    ("Y".equals(fields[0]) && "X".equals(fieldsSearch[0]) && Integer.parseInt(fieldsSearch[1]) == Integer.parseInt(fields[1]) - parRegion.getDiference())) {
                                fieldGT[0] = fieldGT[0] + "/" + fieldsSearch[9].split(":")[0];
                                find = true;
                            }
                        }
                        searchLine = searchReader.readLine();
                    }
                    if (!find)
                        fieldGT[0] = "0/1";

                    searchReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<String> options = new ArrayList<>(Arrays.asList("0", "1"));
            if (options.contains(fieldGT[0]))
                fieldGT[0]=fieldGT[0] + "/" + fieldGT[0];

            options = new ArrayList<>(Arrays.asList("0/0", "1/1", "0|0", "1|1"));
            if (!options.contains(fieldGT[0]))
                fieldGT[0] = "./.";
        }

        fields[9] = String.join(":", fieldGT);
        String newLine = String.join("\t", fields);
        //System.out.println("checkParRegions: " + newLine);

        return newLine;
    }


    @Override
    public List<Variant> read(int batchSize) {
        List<Variant> listRecords = new ArrayList<>(batchSize);

        int i = 0;
        List<Variant> variants;
        while ((i < batchSize) && (variants = this.read()) != null) {
            listRecords.addAll(variants);
            i += variants.size();
        }

        return listRecords;
    }

    @Override
    public List<String> getSampleNames() {
        return this.vcf4.getSampleNames();
    }

    @Override
    public String getHeader() {
        StringBuilder header = new StringBuilder();
        header.append("##fileformat=").append(vcf4.getFileFormat()).append("\n");

        Iterator<String> iter = vcf4.getMetaInformation().keySet().iterator();
        String headerKey;
        while (iter.hasNext()) {
            headerKey = iter.next();
            header.append("##").append(headerKey).append("=").append(vcf4.getMetaInformation().get(headerKey)).append("\n");
        }

        for (VcfAlternateHeader vcfAlternate : vcf4.getAlternate().values()) {
            header.append(vcfAlternate.toString()).append("\n");
        }

        for (VcfFilterHeader vcfFilter : vcf4.getFilter().values()) {
            header.append(vcfFilter.toString()).append("\n");
        }

        for (VcfInfoHeader vcfInfo : vcf4.getInfo().values()) {
            header.append(vcfInfo.toString()).append("\n");
        }

        for (VcfFormatHeader vcfFormat : vcf4.getFormat().values()) {
            header.append(vcfFormat.toString()).append("\n");
        }

        header.append("#").append(Joiner.on("\t").join(vcf4.getHeaderLine())).append("\n");

        return header.toString();
    }

    private void processHeader() throws IOException, FileFormatException {
        BufferedReader localBufferedReader;

        if (Files.probeContentType(path).contains("gzip") || path.toString().endsWith(".gz")) {
            localBufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))));
        } else {
            localBufferedReader = new BufferedReader(new FileReader(path.toFile()));
        }

        boolean header = false;
        String line;

        while ((line = localBufferedReader.readLine()) != null && line.startsWith("#")) {
            if (line.startsWith("##fileformat")) {
                if (line.split("=").length > 1) {
                    vcf4.setFileFormat(line.split("=")[1].trim());
                } else {
                    throw new FileFormatException("");
                }

            } else if (line.startsWith("##INFO")) {
                VcfInfoHeader vcfInfo = new VcfInfoHeader(line);
                vcf4.getInfo().put(vcfInfo.getId(), vcfInfo);

            } else if (line.startsWith("##FILTER")) {
                VcfFilterHeader vcfFilter = new VcfFilterHeader(line);
                vcf4.getFilter().put(vcfFilter.getId(), vcfFilter);

            } else if (line.startsWith("##FORMAT")) {
                VcfFormatHeader vcfFormat = new VcfFormatHeader(line);
                vcf4.getFormat().put(vcfFormat.getId(), vcfFormat);

            } else if (line.startsWith("#CHROM")) {
//               List<String>  headerLine = StringUtils.toList(line.replace("#", ""), "\t");
                List<String> headerLine = Splitter.on("\t").splitToList(line.replace("#", ""));
                vcf4.setHeaderLine(headerLine);
                header = true;

            } else {
                String[] fields = line.replace("#", "").split("=", 2);
                vcf4.getMetaInformation().put(fields[0], fields[1]);
            }
        }

        if (!header) {
            System.err.println("VCF Header must be provided.");
//            System.exit(-1);
        }

        localBufferedReader.close();
    }
}