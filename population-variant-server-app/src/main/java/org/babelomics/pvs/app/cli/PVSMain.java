package org.babelomics.pvs.app.cli;

import com.beust.jcommander.ParameterException;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.pvs.lib.annot.CellBaseAnnotator;
import org.babelomics.pvs.lib.io.*;
import org.babelomics.pvs.lib.models.DiseaseCount;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.babelomics.pvs.lib.models.File;
import org.babelomics.pvs.lib.models.Variant;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.tools.variant.tasks.VariantRunner;
import org.opencb.biodata.tools.variant.tasks.VariantStatsTask;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSMain {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
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

                default:
                    System.out.println("Command not implemented!!");
                    System.exit(1);
            }
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.err.println(parser.usage());
            System.exit(1);
        }

        final Morphia morphia = new Morphia();

        morphia.mapPackage("org.babelomics.pvs.lib.models");

        final Datastore datastore = morphia.createDatastore(new MongoClient(), "pvs");
        datastore.ensureIndexes();


        if (command instanceof OptionsParser.CommandSetup) {
            OptionsParser.CommandSetup c = (OptionsParser.CommandSetup) command;

            if (c.populateDiseases) {
                List<DiseaseGroup> diseaseGroups = new ArrayList<>();

                diseaseGroups.add(new DiseaseGroup(1, "Certain infectious and parasitic diseases"));
                diseaseGroups.add(new DiseaseGroup(2, "Neoplasms"));
                diseaseGroups.add(new DiseaseGroup(3, "Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism"));
                diseaseGroups.add(new DiseaseGroup(4, "Endocrine, nutritional and metabolic diseases"));
                diseaseGroups.add(new DiseaseGroup(5, "Mental and behavioural disorders"));
                diseaseGroups.add(new DiseaseGroup(6, "Diseases of the nervous system"));
                diseaseGroups.add(new DiseaseGroup(7, "Diseases of the eye and adnexa"));
                diseaseGroups.add(new DiseaseGroup(8, "Diseases of the ear and mastoid process"));
                diseaseGroups.add(new DiseaseGroup(9, "Diseases of the circulatory system"));
                diseaseGroups.add(new DiseaseGroup(10, "Diseases of the respiratory system"));
                diseaseGroups.add(new DiseaseGroup(11, "Diseases of the digestive system"));
                diseaseGroups.add(new DiseaseGroup(12, "Diseases of the skin and subcutaneous tissue"));
                diseaseGroups.add(new DiseaseGroup(13, "Diseases of the musculoskeletal system and connective tissue"));
                diseaseGroups.add(new DiseaseGroup(14, "Diseases of the genitourinary system"));
                diseaseGroups.add(new DiseaseGroup(15, "Pregnancy, childbirth and the puerperium"));
                diseaseGroups.add(new DiseaseGroup(16, "Certain conditions originating in the perinatal period"));
                diseaseGroups.add(new DiseaseGroup(17, "Congenital malformations, deformations and chromosomal abnormalities"));
                diseaseGroups.add(new DiseaseGroup(18, "Symptoms, signs and abnormal clinical and laboratory findings, not elsewhere classified"));
                diseaseGroups.add(new DiseaseGroup(19, "Unknown"));

                for (DiseaseGroup dg : diseaseGroups) {
                    try {
                        datastore.save(dg);
                    } catch (DuplicateKeyException e) {
                        System.err.println("Duplicated Disase Group: " + dg);
                    }
                }
            }

            if (c.newDisease != null && c.newDisease.length() > 0) {
                PVSQueryManager qm = new PVSQueryManager("pvs");

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

            loadVariants(inputFile, diseaseGroupId, datastore);
        } else if (command instanceof OptionsParser.CommandUnload) {
            OptionsParser.CommandUnload c = (OptionsParser.CommandUnload) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;

            unloadVariants(inputFile, diseaseGroupId, datastore);
        } else if (command instanceof OptionsParser.CommandCount) {
            OptionsParser.CommandCount c = (OptionsParser.CommandCount) command;

            Path input = Paths.get(c.input);
            Path output = Paths.get(c.output);

            compressVariants(input, output);
        } else if (command instanceof OptionsParser.CommandAnnot) {

            OptionsParser.CommandAnnot c = (OptionsParser.CommandAnnot) command;

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

            PVSQueryManager qm = new PVSQueryManager(datastore);


            if (c.diseases) {

                System.out.println("\n\nList of Groups of Diseases\n==========================\n");

                List<DiseaseGroup> query = qm.getAllDiseaseGroups();

                for (DiseaseGroup dg : query) {
                    System.out.println(dg.getGroupId() + "\t" + dg.getName());
                }

            } else if (c.regionLIst.size() > 0) {

                List<Integer> diseaseId = c.diseaseId;

                Pattern p = Pattern.compile("(\\w+):(\\d+)-(\\d+)");
                List<Region> regionList = new ArrayList<>();

                for (String region : c.regionLIst) {
                    Matcher m = p.matcher(region);

                    if (m.matches()) {
                        String chr = m.group(1);
                        int start = Integer.parseInt(m.group(2));
                        int end = Integer.parseInt(m.group(3));

                        Region r = new Region(chr, start, end);
                        regionList.add(r);

                    } else {
                        System.out.println("no: " + region);
                    }

                }

                long start = System.currentTimeMillis();
                MutableLong count = new MutableLong(-1);

                Iterable<Variant> query = qm.getVariantsByRegionList(regionList, diseaseId, c.skip, c.limit, count);
                for (Variant v : query) {
                    System.out.println("v = " + v);
                }

                long end = System.currentTimeMillis();
                System.out.println(end - start);
                System.out.println("count: " + count);

            }


        } else {
            System.err.println("Comand not found");
        }

    }

    private static void compressVariants(Path input, Path output) throws IOException {

        VariantSource source = new VariantSource("file", "file", "file", "file");
        VariantReader reader = new VariantVcfReader(source, input.toAbsolutePath().toString());
        VariantWriter writer = new PVSVariantCountsCSVDataWriter(output.toAbsolutePath().toString());

        List<Task<org.opencb.biodata.models.variant.Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        taskList.add(new VariantStatsTask(reader, source));
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

            DataReader<Variant> reader = new PVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg);

            List<Task<Variant>> taskList = new SortedList<>();
            List<DataWriter<Variant>> writers = new ArrayList<>();
            DataWriter<Variant> writer = new PVSVariantCountsMongoWriter(dg, datastore);

            writers.add(writer);

            Runner<Variant> pvsRunner = new PVSRunner(reader, writers, taskList, 100);

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

        DataReader<Variant> reader = new PVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg);

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
