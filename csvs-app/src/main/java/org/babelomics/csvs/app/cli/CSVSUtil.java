package org.babelomics.csvs.app.cli;

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
import org.babelomics.csvs.lib.annot.CellBaseAnnotator;
import org.babelomics.csvs.lib.io.*;
import org.babelomics.csvs.lib.models.*;
import org.babelomics.csvs.lib.stats.CSVSVariantStatsTask;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.tools.variant.tasks.VariantRunner;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSUtil {

    public static Datastore getDatastore(String host, String user, String pass, String dbName) {

        Datastore datastore;

        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.csvs.lib.models");

        MongoClient mongoClient;
        if (user == "" && pass == "") {
            mongoClient = new MongoClient(host);
        } else {
            MongoCredential credential = MongoCredential.createCredential(user, dbName, pass.toCharArray());
            mongoClient = new MongoClient(new ServerAddress(host), Arrays.asList(credential));
        }

        datastore = morphia.createDatastore(mongoClient, dbName);
        datastore.ensureIndexes();
        return datastore;
    }


    public static void populateDiseases(Datastore datastore) {
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
                System.err.println("Duplicated Disease Group: " + dg);
            }
        }
    }

    public static void addNewDisease(Datastore datastore, String disease) {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);

        int newId = qm.getMaxDiseaseId();
        if (newId != -1) {
            newId++;
            DiseaseGroup dg = new DiseaseGroup(newId, disease);
            datastore.save(dg);
        }
    }

    public static void populateTechnologies(Datastore datastore) {
        List<Technology> technologies = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            technologies.add(new Technology(i + 1, "Technology_" + (i + 1)));
        }

        for (Technology t : technologies) {
            try {
                datastore.save(t);
            } catch (DuplicateKeyException e) {
                System.err.println("Duplicated Technology: " + t);
            }
        }
    }

    public static void loadVariants(Path variantsPath, int diseaseGroupId, int technologyId, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        String sha256 = calculateSHA256(variantsPath);

        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        if (true || fDb == null) {

            Query<DiseaseGroup> queryDiseaseGroup = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
            DiseaseGroup dg = queryDiseaseGroup.get();

            Query<Technology> queryTechnology = datastore.createQuery(Technology.class).field("technologyId").equal(technologyId);
            Technology t = queryTechnology.get();

            System.out.println("dg = " + dg);
            System.out.println("t = " + t);

            DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg, t);

            List<Task<Variant>> taskList = new SortedList<>();
            List<DataWriter<Variant>> writers = new ArrayList<>();
            DataWriter<Variant> writer = new CSVSVariantCountsMongoWriter(dg, t, datastore);

            writers.add(writer);

            Runner<Variant> pvsRunner = new CSVSRunner(reader, writers, taskList, 100);

            System.out.println("Loading variants...");
            pvsRunner.run();
            System.out.println("Variants loaded!");

            File f = new File(sha256, dg, t);

            try {
                datastore.save(f);
            } catch (DuplicateKeyException ignored) {

            }
        } else {
            System.out.println("File is already in the database");
            System.exit(0);
        }
    }

    public static void unloadVariants(Path variantsPath, int diseaseGroupId, int technologyId, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        int samples = 0;
        int variants = 0;


        String sha256 = calculateSHA256(variantsPath);
        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        if (fDb == null) {
            System.out.println("File is not in the database");
            System.exit(1);
        }

        Query<DiseaseGroup> queryDiseaseGroup = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
        DiseaseGroup dg = queryDiseaseGroup.get();

        Query<Technology> queryTechnology = datastore.createQuery(Technology.class).field("technologyId").equal(technologyId);
        Technology t = queryTechnology.get();

        DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg, t);

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
                    variants++;

                    DiseaseCount vDc = v.getDiseaseCount(dg, t);
                    DiseaseCount elemDC = elem.getDiseaseCount(dg, t);
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

        datastore.delete(File.class, fDb.getId());
        dg.decSamples(samples);
        dg.decVariants(variants);

        t.decSamples(samples);
        t.decVariants(variants);
        datastore.save(dg);
    }

    public static void annotFile(String input, String output, List<Integer> diseases, Datastore datastore) {
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
            chr = removePrefix(chr);
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
    }

    public static void compressVariants(Path input, Path output) throws IOException {

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


    private static String removePrefix(String chr) {
        String res = chr.replace("chrom", "").replace("chrm", "").replace("chr", "").replace("ch", "");
        return res;
    }

    public static void annot(boolean ct, boolean remove, boolean override, boolean gene, Datastore datastore) throws IOException {
        CellBaseAnnotator cba = new CellBaseAnnotator();

        cba.setCt(ct);
        cba.setRemove(remove);
        cba.setOverride(override);
        cba.setGene(gene);


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
    }


}
