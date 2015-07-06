package org.babelomics.pvs.app.cli;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import org.babelomics.pvs.lib.io.PVSRunner;
import org.babelomics.pvs.lib.io.PVSVariantCountCSVDataReader;
import org.babelomics.pvs.lib.io.PVSVariantCountsCSVDataWriter;
import org.babelomics.pvs.lib.io.PVSVariantCountsMongoWriter;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.VariantStudy;
import org.opencb.biodata.tools.variant.tasks.VariantRunner;
import org.opencb.biodata.tools.variant.tasks.VariantStatsTask;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSMain {

    public static final String SEPARATOR = "#-#";

    public static void main(String[] args) throws IOException {
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
                // OLD
                case "transform-variants":
                    command = parser.getTransformCommand();
                    break;
                case "load-variants":
                    command = parser.getLoadCommand();
                    break;
                case "compress-variants":
                    command = parser.getCompressComand();
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
            System.out.println("SETUP");

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


            for (DiseaseGroup dg : diseaseGroups) {
                try {
                    datastore.save(dg);
                } catch (DuplicateKeyException e) {
                    System.err.println("Duplicated Disase Group: " + dg);
                }
            }


        } else if (command instanceof OptionsParser.CommandTransformVariants) {
            OptionsParser.CommandTransformVariants c = (OptionsParser.CommandTransformVariants) command;

            Path file = Paths.get(c.file);
            Path outdir = Paths.get(c.outdir);

            String disease = Joiner.on("_").join(c.disease).toUpperCase();
            String study = Joiner.on("_").join(c.study).toUpperCase();
            String phenotype = c.phenotype.toString();

            String fileId = study + SEPARATOR + disease + SEPARATOR + phenotype;

            String paper = c.paper == null ? "" : c.paper;
            String description = c.description == null ? "" : c.description;

            VariantStudy.StudyType studyType = VariantStudy.StudyType.CASE_CONTROL;
            VariantSource.Aggregation aggregated = c.aggregated;
            VariantSource source = new VariantSource(file.getFileName().toString(), fileId, study, study, studyType, aggregated);

            source.addMetadata("disease", disease);
            source.addMetadata("phenotype", phenotype);
            source.addMetadata("paper", paper);
            source.addMetadata("desc", description);
            source.addMetadata("sta", Boolean.toString(c.staticStudy));
            source.addMetadata("cov", String.valueOf(c.coverage));
            source.addMetadata("tech", c.technology);

            transformVariants(source, file, outdir);
        } else if (command instanceof OptionsParser.CommandLoadVariants) {
            OptionsParser.CommandLoadVariants c = (OptionsParser.CommandLoadVariants) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;

            VariantSource source = new VariantSource(inputFile.getFileName().toString(), null, null, null, VariantStudy.StudyType.CASE_CONTROL, VariantSource.Aggregation.NONE);

            loadVariants(inputFile, diseaseGroupId, datastore);
        } else if (command instanceof OptionsParser.CommandCompressVariants) {
            OptionsParser.CommandCompressVariants c = (OptionsParser.CommandCompressVariants) command;

            Path input = Paths.get(c.input);
            Path output = Paths.get(c.output);

            compressVariants(input, output);
        }

    }

    private static void compressVariants(Path input, Path output) throws IOException {

        VariantSource source = new VariantSource("file", "file", "file", "file");
        VariantReader reader = new VariantVcfReader(source, input.toAbsolutePath().toString());
        VariantWriter writer = new PVSVariantCountsCSVDataWriter(reader, output.toAbsolutePath().toString());


        List<Task<Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        taskList.add(new VariantStatsTask(reader, source));
        writers.add(writer);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("Compressing variants...");
        variantRunner.run();
        System.out.println("Variants compressed!");


    }

    private static void loadVariants(Path variantsPath, int diseaseGroupId, Datastore datastore) throws IOException {

        Query<DiseaseGroup> query = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
        DiseaseGroup dg = query.get();


        DataReader<org.babelomics.pvs.lib.models.Variant> reader = new PVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg);

        List<Task<org.babelomics.pvs.lib.models.Variant>> taskList = new SortedList<>();
        List<DataWriter<org.babelomics.pvs.lib.models.Variant>> writers = new ArrayList<>();
        DataWriter<org.babelomics.pvs.lib.models.Variant> writer = new PVSVariantCountsMongoWriter(dg, datastore);

        writers.add(writer);

        Runner<org.babelomics.pvs.lib.models.Variant> pvsRunner = new PVSRunner(reader, writers, taskList, 100);

        System.out.println("Loading variants...");
        pvsRunner.run();
        System.out.println("Variants loaded!");

    }

    private static void transformVariants(VariantSource source, Path file, Path outdir) throws IOException {

//        VariantReader reader = null;
//
//        switch (source.getAggregation()) {
//            case NONE:
//                reader = new VariantVcfReader(source, file.toAbsolutePath().toString());
//                break;
//            case BASIC:
//                reader = new VariantVcfReader(source, file.toAbsolutePath().toString(), new VariantAggregatedVcfFactory());
//                break;
//            case EVS:
//                reader = new VariantVcfReader(source, file.toAbsolutePath().toString(), new VariantVcfEVSFactory());
//                break;
//            default:
//                reader = new VariantVcfReader(source, file.toAbsolutePath().toString());
//        }
//
//        List<Task<Variant>> taskList = new SortedList<>();
//        List<VariantWriter> writers = new ArrayList<>();
//
//        VariantWriter jsonWriter = new PVSJsonWriter(source, outdir);
//
//
//        taskList.add(new PVSVariantStatsTask(reader, source));
//        jsonWriter.includeStats(true);
//        jsonWriter.includeEffect(false);
//        jsonWriter.includeSamples(false);
//
//        writers.add(jsonWriter);
//
//        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);
//
//        System.out.println("Indexing variants...");
//        variantRunner.run();
//        System.out.println("Variants indexed!");

    }
}
