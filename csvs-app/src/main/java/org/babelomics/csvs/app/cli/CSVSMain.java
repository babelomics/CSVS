package org.babelomics.csvs.app.cli;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.*;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.annot.CellBaseAnnotator;
import org.babelomics.csvs.lib.io.*;
import org.babelomics.csvs.lib.models.DiseaseCount;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.File;
import org.babelomics.csvs.lib.models.Variant;
import org.babelomics.csvs.lib.stats.CSVSVariantStatsTask;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.tools.variant.tasks.VariantRunner;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.cellbase.core.common.core.Gene;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSMain {


    private static Datastore getDatastore(String host, String user, String pass) {

        Datastore datastore;

        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.pvs.lib.models");

        MongoClient mongoClient;
        if (user == "" && pass == "") {
            mongoClient = new MongoClient(host);
        } else {
            MongoCredential credential = MongoCredential.createCredential(user, "pvs", pass.toCharArray());
            mongoClient = new MongoClient(new ServerAddress(host), Arrays.asList(credential));
        }

        datastore = morphia.createDatastore(mongoClient, "pvs");
        datastore.ensureIndexes();
        return datastore;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        OptionsParser parser = new OptionsParser();

        // If no arguments are provided, or -h/--help is the first argument, the usage is shown
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            System.out.println(parser.usage());
            return;
        }

        OptionsParser.Command command = null;

        try {
            switch (parser.parse(args)) {
                case "setup":
                    command = parser.getSetupCommand();
                    break;
                case "load":
                    command = parser.getLoadCommand();
                    break;
                case "unload":
                    command = parser.getUnloadCommand();
                    break;
                case "count":
                    command = parser.getCalculateCuntsCommand();
                    break;
                case "annot":
                    command = parser.getAnnotCommand();
                    break;

                case "query":
                    command = parser.getQueryCommand();
                    break;
                case "annot-file":
                    command = parser.getAnnotFileCommand();
                    break;

                default:
                    System.out.println("Command not implemented!!");
                    System.exit(1);
            }
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.err.println(parser.usage());
            System.exit(1);
        }


        if (command instanceof OptionsParser.CommandSetup) {
            OptionsParser.CommandSetup c = (OptionsParser.CommandSetup) command;

            Datastore datastore = getDatastore(c.host, c.user, c.pass);


            if (c.populateDiseases) {
                List<DiseaseGroup> diseaseGroups = new ArrayList<>();

                diseaseGroups.add(new DiseaseGroup(1, "I Certain infectious and parasitic diseases"));
                diseaseGroups.add(new DiseaseGroup(2, "II Neoplasms"));
                diseaseGroups.add(new DiseaseGroup(3, "III Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism"));
                diseaseGroups.add(new DiseaseGroup(4, "IV Endocrine, nutritional and metabolic diseases"));
                diseaseGroups.add(new DiseaseGroup(5, "V Mental and behavioural disorders"));
                diseaseGroups.add(new DiseaseGroup(6, "VI Diseases of the nervous system"));
                diseaseGroups.add(new DiseaseGroup(7, "VII Diseases of the eye and adnexa"));
                diseaseGroups.add(new DiseaseGroup(8, "VIII Diseases of the ear and mastoid process"));
                diseaseGroups.add(new DiseaseGroup(9, "IX Diseases of the circulatory system"));
                diseaseGroups.add(new DiseaseGroup(10, "X Diseases of the respiratory system"));
                diseaseGroups.add(new DiseaseGroup(11, "XI Diseases of the digestive system"));
                diseaseGroups.add(new DiseaseGroup(12, "XII Diseases of the skin and subcutaneous tissue"));
                diseaseGroups.add(new DiseaseGroup(13, "XIII Diseases of the musculoskeletal system and connective tissue"));
                diseaseGroups.add(new DiseaseGroup(14, "XIV Diseases of the genitourinary system"));
                diseaseGroups.add(new DiseaseGroup(15, "XV Pregnancy, childbirth and the puerperium"));
                diseaseGroups.add(new DiseaseGroup(16, "XVI Certain conditions originating in the perinatal period"));
                diseaseGroups.add(new DiseaseGroup(17, "XVII Congenital malformations, deformations and chromosomal abnormalities"));
                diseaseGroups.add(new DiseaseGroup(18, "XVIII Symptoms, signs and abnormal clinical and laboratory findings, not elsewhere classified"));

                for (DiseaseGroup dg : diseaseGroups) {
                    try {
                        datastore.save(dg);
                    } catch (DuplicateKeyException e) {
                        System.err.println("Duplicated Disase Group: " + dg);
                    }
                }
            }

            if (c.newDisease != null && c.newDisease.length() > 0) {
                CSVSQueryManager qm = new CSVSQueryManager(datastore);

                int newId = qm.getMaxDiseaseId();
                if (newId != -1) {
                    newId++;
                    DiseaseGroup dg = new DiseaseGroup(newId, c.newDisease);
                    datastore.save(dg);
                }
            }

        } else if (command instanceof OptionsParser.CommandLoad) {
            OptionsParser.CommandLoad c = (OptionsParser.CommandLoad) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;

            Datastore datastore = getDatastore(c.host, c.user, c.pass);

            loadVariants(inputFile, diseaseGroupId, datastore);
        } else if (command instanceof OptionsParser.CommandUnload) {
            OptionsParser.CommandUnload c = (OptionsParser.CommandUnload) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;

            Datastore datastore = getDatastore(c.host, c.user, c.pass);

            unloadVariants(inputFile, diseaseGroupId, datastore);
        } else if (command instanceof OptionsParser.CommandCount) {
            OptionsParser.CommandCount c = (OptionsParser.CommandCount) command;

            Path input = Paths.get(c.input);
            Path output = Paths.get(c.output);

            compressVariants(input, output);
        } else if (command instanceof OptionsParser.CommandAnnot) {

            OptionsParser.CommandAnnot c = (OptionsParser.CommandAnnot) command;

            Datastore datastore = getDatastore(c.host, c.user, c.pass);

            CellBaseAnnotator cba = new CellBaseAnnotator();

            cba.setCt(c.ct);
            cba.setRemove(c.remove);
            cba.setOverride(c.override);
            cba.setGene(c.gene);


            Iterator<Variant> it = datastore.createQuery(Variant.class).batchSize(10).iterator();


            while (it.hasNext()) {
                List<Variant> batch = new ArrayList<>();

                for (int i = 0; i < 10 && it.hasNext(); i++) {
                    batch.add(it.next());
                }

                cba.annot(batch);
                datastore.save(batch);
                batch.clear();
            }


        } else if (command instanceof OptionsParser.CommandQuery) {
            OptionsParser.CommandQuery c = (OptionsParser.CommandQuery) command;

            Datastore datastore = getDatastore(c.host, c.user, c.pass);

            CSVSQueryManager qm = new CSVSQueryManager(datastore);


            if (c.diseases) {

                System.out.println("\n\nList of Groups of Diseases\n==========================\n");

                List<DiseaseGroup> query = qm.getAllDiseaseGroups();

                for (DiseaseGroup dg : query) {
                    System.out.println(dg.getGroupId() + "\t" + dg.getName() + "\t" + dg.getSamples());
                }

            } else if (c.regionLIst.size() > 0 || c.geneList.size() > 0) {

                List<Integer> diseaseId = c.diseaseId;
                PrintWriter pw = null;

                List<Region> regionList = new ArrayList<>();

                for (String region : c.regionLIst) {
                    Region r = new Region(region);
                    regionList.add(r);
                }

                if (c.geneList.size() > 0) {
                    URI cellbaseUri = new URI("http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest");
                    CellBaseClient cbc = new CellBaseClient(cellbaseUri, "v3", "hsapiens");
                    String id = Joiner.on(",").join(c.geneList).toUpperCase();
                    QueryOptions qo = new QueryOptions();
                    qo.add("include", "chromosome,start,end");

                    QueryResponse<QueryResult<Gene>> info = cbc.getInfo(CellBaseClient.Category.feature, CellBaseClient.SubCategory.gene, id, qo);

                    for (QueryResult<Gene> qr : info.getResponse()) {
                        for (Gene gene : qr.getResult()) {
                            regionList.add(new Region(gene.getChromosome(), gene.getStart(), gene.getEnd()));
                        }
                    }
                }

                MutableLong count = new MutableLong(-1);

                Iterable<Variant> query = qm.getVariantsByRegionList(regionList, diseaseId, c.skip, c.limit, count);

                if (!c.csv) {
                    System.out.println("chr\tpos\tref\talt\t0/0\t0/1\t1/1\t./.\trefFreq\taltFreq\tMAF");
                } else {
                    pw = new PrintWriter(c.outfile);
                    pw.append("chr\tpos\tref\talt\t0/0\t0/1\t1/1\t./.\trefFreq\taltFreq\tMAF").append("\n");
                }

                for (Variant v : query) {

                    String ref = (v.getReference() == null || v.getReference().isEmpty()) ? "." : v.getReference();
                    String alt = (v.getAlternate() == null || v.getAlternate().isEmpty()) ? "." : v.getAlternate();

                    StringBuilder sb = new StringBuilder();
                    sb.append(v.getChromosome()).append("\t");
                    sb.append(v.getPosition()).append("\t");
                    sb.append(ref).append("\t");
                    sb.append(alt).append("\t");

                    DiseaseCount dc = v.getStats();

                    sb.append(dc.getGt00()).append("\t");
                    sb.append(dc.getGt01()).append("\t");
                    sb.append(dc.getGt11()).append("\t");
                    sb.append(dc.getGtmissing()).append("\t");
                    sb.append(dc.getRefFreq()).append("\t");
                    sb.append(dc.getAltFreq()).append("\t");
                    sb.append(dc.getMaf()).append("\t");

                    if (!c.csv) {
                        System.out.println(sb.toString());
                    } else {
                        pw.append(sb.toString()).append("\n");
                    }
                }

                if (c.csv) {
                    pw.close();
                }
            } else if (c.all) {

                PrintWriter pw = null;
                List<Integer> diseaseId = c.diseaseId;

                MutableLong count = new MutableLong(-1);
                Iterable<Variant> query = qm.getAllVariants(diseaseId, c.skip, c.limit, count);

                if (!c.csv) {
                    System.out.println("chr\tpos\tref\talt\tid\t0/0\t0/1\t1/1\t./.\trefFreq\taltFreq\tMAF");
                } else {
                    pw = new PrintWriter(c.outfile);
                    pw.append("chr\tpos\tref\talt\tid\t0/0\t0/1\t1/1\t./.\trefFreq\taltFreq\tMAF").append("\n");
                }

                for (Variant v : query) {

                    String ref = (v.getReference() == null || v.getReference().isEmpty()) ? "." : v.getReference();
                    String alt = (v.getAlternate() == null || v.getAlternate().isEmpty()) ? "." : v.getAlternate();
                    String id = (v.getIds() == null) ? "." : v.getIds();

                    StringBuilder sb = new StringBuilder();
                    sb.append(v.getChromosome()).append("\t");
                    sb.append(v.getPosition()).append("\t");
                    sb.append(ref).append("\t");
                    sb.append(alt).append("\t");
                    sb.append(id).append("\t");

                    DiseaseCount dc = v.getStats();

                    sb.append(dc.getGt00()).append("\t");
                    sb.append(dc.getGt01()).append("\t");
                    sb.append(dc.getGt11()).append("\t");
                    sb.append(dc.getGtmissing()).append("\t");
                    sb.append(dc.getRefFreq()).append("\t");
                    sb.append(dc.getAltFreq()).append("\t");
                    sb.append(dc.getMaf()).append("\t");

                    if (!c.csv) {
                        System.out.println(sb.toString());
                    } else {
                        pw.append(sb.toString()).append("\n");
                    }
                }

                if (c.csv) {
                    pw.close();
                }

            }


        } else if (command instanceof OptionsParser.CommandAnnotFile) {
            OptionsParser.CommandAnnotFile c = (OptionsParser.CommandAnnotFile) command;
            String input = c.input;
            String output = c.outdir + c.outfile;
            List<Integer> diseases = (c.diseaseId != null && c.diseaseId.size() > 0) ? c.diseaseId : null;
            Datastore datastore = getDatastore(c.host, c.user, c.pass);

            CSVSQueryManager qm = new CSVSQueryManager(datastore);

            VCFFileReader variantReader = new VCFFileReader(new java.io.File(input), false);
            VCFHeader fileHeader = variantReader.getFileHeader();

            final VariantContextWriterBuilder builder = new VariantContextWriterBuilder().clearOptions();
            builder.setOption(Options.ALLOW_MISSING_FIELDS_IN_HEADER);


            VCFInfoHeaderLine csvs_maf = new VCFInfoHeaderLine("CSVS_MAF", VCFHeaderLineCount.A, VCFHeaderLineType.Float, "MAF from CSVS, for each ALT allele, in the same order as listed");
            VCFInfoHeaderLine csvs_dis = new VCFInfoHeaderLine("CSVS_DIS", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Selected diseases from CSVS");

            fileHeader.addMetaDataLine(csvs_maf);
            fileHeader.addMetaDataLine(csvs_dis);

            final VariantContextWriter vcfWriter = builder.setOutputFile(new java.io.File(output)).build();
            vcfWriter.writeHeader(fileHeader);


            for (VariantContext context : variantReader) {
                String chr = context.getContig();
                int pos = context.getStart();
                String ref = context.getReference().getBaseString();

                List<String> mafs = new ArrayList<>();
                boolean maf = false;


                for (Allele altAllele : context.getAlternateAlleles()) {
                    String alt = altAllele.getBaseString();
                    Variant dbVariant = qm.getVariant(chr, pos, ref, alt, diseases);
                    if (dbVariant != null) {
                        DiseaseCount dc = dbVariant.getStats();
                        mafs.add(String.valueOf(dc.getMaf()));
                        maf = true;
                    } else {
                        mafs.add(".");
                    }
                }
                if (maf && mafs.size() == context.getAlternateAlleles().size()) {
                    context.getCommonInfo().putAttribute("CSVS_MAF", Joiner.on(",").join(mafs));
                    context.getCommonInfo().putAttribute("CSVS_DIS", (diseases == null || diseases.size() == 0) ? "ALL" : Joiner.on(",").join(diseases));
                }


                vcfWriter.add(context);
            }

            variantReader.close();
            vcfWriter.close();


        } else {
            System.err.println("Comand not found");
        }

    }

    private static void compressVariants(Path input, Path output) throws IOException {

        VariantSource source = new VariantSource("file", "file", "file", "file");
        VariantReader reader = new VariantVcfReader(source, input.toAbsolutePath().toString());
        VariantWriter writer = new CSVSVariantCountsCSVDataWriter(output.toAbsolutePath().toString());

        List<Task<org.opencb.biodata.models.variant.Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        taskList.add(new CSVSVariantStatsTask(reader, source));
        writers.add(writer);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("Compressing variants...");
        variantRunner.run();
        System.out.println("Variants compressed!");

    }

    private static void loadVariants(Path variantsPath, int diseaseGroupId, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        String sha256 = calculateSHA256(variantsPath);

        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        if (fDb == null) {

            Query<DiseaseGroup> query = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
            DiseaseGroup dg = query.get();

            DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg);

            List<Task<Variant>> taskList = new SortedList<>();
            List<DataWriter<Variant>> writers = new ArrayList<>();
            DataWriter<Variant> writer = new CSVSVariantCountsMongoWriter(dg, datastore);

            writers.add(writer);

            Runner<Variant> pvsRunner = new CSVSRunner(reader, writers, taskList, 100);

            System.out.println("Loading variants...");
            pvsRunner.run();
            System.out.println("Variants loaded!");

            File f = new File(sha256);

            try {
                datastore.save(f);
            } catch (DuplicateKeyException ignored) {

            }
        } else {
            System.out.println("File is already in the database");
            System.exit(0);
        }
    }

    private static String calculateSHA256(Path variantsPath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(variantsPath.toAbsolutePath().toString());

        byte[] dataBytes = new byte[1024];

        int nread;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }

        byte[] mdbytes = md.digest();

        StringBuffer sb = new StringBuffer();
        for (byte mdbyte : mdbytes) {
            sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private static void unloadVariants(Path variantsPath, int diseaseGroupId, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        int samples = 0;
        Query<DiseaseGroup> queryDG = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
        DiseaseGroup dg = queryDG.get();

        DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg);

        reader.open();
        reader.pre();

        List<Variant> batch;

        batch = reader.read(1);

        while (!batch.isEmpty()) {

            for (Variant elem : batch) {

                Variant v = datastore.createQuery(Variant.class).field("chromosome").equal(elem.getChromosome())
                        .field("position").equal(elem.getPosition()).
                                field("reference").equal(elem.getReference()).
                                field("alternate").equal(elem.getAlternate())
                        .get();

                if (v != null) {

                    DiseaseCount vDc = v.getDiseaseCount(dg);
                    DiseaseCount elemDC = elem.getDiseaseCount(dg);
                    if (vDc != null) {

                        if (samples == 0) {
                            samples = elemDC.getTotalGts();
                        }

                        vDc.decGt00(elemDC.getGt00());
                        vDc.decGt01(elemDC.getGt01());
                        vDc.decGt11(elemDC.getGt11());
                        vDc.decGtMissing(elemDC.getGtmissing());

                        if (vDc.getTotalGts() <= 0) {
                            v.deleteDiseaseCount(vDc);

                            if (v.getDiseases().size() == 0) {
                                datastore.delete(Variant.class, v.getId());
                            } else {
                                datastore.save(v);
                            }
                        } else {
                            datastore.save(v);
                        }
                    }

                }

            }
            batch.clear();
            batch = reader.read();
        }

        reader.post();
        reader.close();


        String sha256 = calculateSHA256(variantsPath);

        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        datastore.delete(File.class, fDb.getId());

        DiseaseGroup dgDB = datastore.get(DiseaseGroup.class, dg.getId());

        dg.decSamples(samples);

        datastore.save(dg);

    }
}
