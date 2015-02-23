package org.babelomics.pvs.app.cli;

import com.beust.jcommander.ParameterException;
import org.babelomics.pvs.lib.io.PVSJsonWriter;
import org.babelomics.pvs.lib.io.PVSVariantCompressedVcfDataWriter;
import org.babelomics.pvs.lib.io.PVSVariantJsonReader;
import org.babelomics.pvs.lib.io.PVSVariantMongoWriter;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.formats.variant.vcf4.io.VariantVcfReader;
import org.opencb.biodata.models.variant.*;
import org.opencb.commons.containers.list.SortedList;
import org.opencb.commons.run.Task;
import org.opencb.opencga.lib.auth.MongoCredentials;
import org.opencb.opencga.lib.auth.OpenCGACredentials;
import org.opencb.variant.lib.runners.VariantRunner;
import org.opencb.variant.lib.runners.tasks.VariantStatsTask;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSMain {

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
                case "transform-variants":
                    command = parser.getTransformCommand();
                    break;
                case "load-variants":
                    command = parser.getLoadCommand();
                    break;
                case "compress-variants":
                    command = parser.getCompressComand();
                    break;
//                case "add-variants":
//                    command = parser.getAddCommand();
//                    break;
                default:
                    System.out.println("Command not implemented!!");
                    System.exit(1);
            }
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.err.println(parser.usage());
            System.exit(1);
        }

        if (command instanceof OptionsParser.CommandTransformVariants) {
            OptionsParser.CommandTransformVariants c = (OptionsParser.CommandTransformVariants) command;

            Path file = Paths.get(c.file);
            Path outdir = Paths.get(c.outdir);
            String fileId = c.study.toUpperCase() + "_" + c.disease.toUpperCase() + "_" + c.phenotype.toUpperCase();
            String paper = c.paper == null ? "" : c.paper;
            String description = c.description == null ? "" : c.description;

            VariantStudy.StudyType studyType = VariantStudy.StudyType.CASE_CONTROL;
            VariantSource.Aggregation aggregated = c.aggregated;
            VariantSource source = new VariantSource(file.getFileName().toString(), fileId, c.study, c.study, studyType, aggregated);

            source.addMetadata("disease", c.disease);
            source.addMetadata("phenotype", c.phenotype);
            source.addMetadata("paper", paper);
            source.addMetadata("desc", description);
            source.addMetadata("sta", Boolean.toString(c.staticStudy));
            source.addMetadata("cov", String.valueOf(c.coverage));

            transformVariants(source, file, outdir);
        } else if (command instanceof OptionsParser.CommandLoadVariants) {
            OptionsParser.CommandLoadVariants c = (OptionsParser.CommandLoadVariants) command;


            Path variantsPath = Paths.get(c.input + ".variants.json.gz");
            Path filePath = Paths.get(c.input + ".file.json.gz");
            Path credentials = Paths.get(c.credentials);

            VariantStudy.StudyType st = VariantStudy.StudyType.CASE_CONTROL;

            VariantSource source = new VariantSource(variantsPath.getFileName().toString(), null, null, null, st, VariantSource.Aggregation.NONE);

            loadVariants(source, variantsPath, filePath, credentials);
        } else if (command instanceof OptionsParser.CommandCompressVariants) {
            OptionsParser.CommandCompressVariants c = (OptionsParser.CommandCompressVariants) command;

            Path input = Paths.get(c.input);
            Path output = Paths.get(c.output);

            compressVariants(input, output);
        }
//        else if (command instanceof OptionsParser.CommandAddVariants) {
//            OptionsParser.CommandAddVariants c = (OptionsParser.CommandAddVariants) command;
//
//            Path variantsPath = Paths.get(c.input + ".variants.json.gz");
//            Path filePath = Paths.get(c.input + ".file.json.gz");
//            Path credentials = Paths.get(c.credentials);
//
//            VariantStudy.StudyType st = VariantStudy.StudyType.CASE_CONTROL;
//
//            VariantSource source = new VariantSource(variantsPath.getFileName().toString(), null, null, null, st, VariantSource.Aggregation.NONE);
//
//            addVariants(source, variantsPath, filePath, credentials);
//
//        }

    }

    private static void compressVariants(Path input, Path output) throws IOException {

        VariantSource source = new VariantSource("file", "file", "file", "file");
        VariantReader reader = new VariantVcfReader(source, input.toAbsolutePath().toString());

        List<Task<Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        VariantWriter writer = new PVSVariantCompressedVcfDataWriter(reader, output.toAbsolutePath().toString());

        taskList.add(new VariantStatsTask(reader, source));
        writers.add(writer);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("Compressing variants...");
        variantRunner.run();
        System.out.println("Variants compressed!");


    }

//    private static void addVariants(VariantSource source, Path variantsPath, Path filePath, Path credentialsPath) throws IOException {
//        VariantReader reader = new VariantJsonReader(source, variantsPath.toAbsolutePath().toString(), filePath.toAbsolutePath().toString());
//
//
//        List<Task<Variant>> taskList = new SortedList<>();
//        List<VariantWriter> writers = new ArrayList<>();
//
//        Properties properties = new Properties();
//        properties.load(new InputStreamReader(new FileInputStream(credentialsPath.toString())));
//        OpenCGACredentials credentials = new MongoCredentials(properties);
//        VariantWriter mongoWriter = new ExomeServerVariantMongoWriter(source, (MongoCredentials) credentials,
//                properties.getProperty("collection_variants", "variants"),
//                properties.getProperty("collection_files", "files"));
//
//        mongoWriter.includeStats(true);
//        mongoWriter.includeEffect(false);
//        mongoWriter.includeSamples(false);
//
//
//        Task<Variant> updateStats = new ExomeServerVariantUpdateStatsTask((MongoCredentials) credentials, source);
//
//        writers.add(mongoWriter);
//
//        taskList.add(updateStats);
//
//        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);
//
//        System.out.println("Loading variants...");
//        variantRunner.run();
//        System.out.println("Variants loaded!");
//    }

    private static void loadVariants(VariantSource source, Path variantsPath, Path filePath, Path credentialsPath) throws IOException {

        VariantReader reader = new PVSVariantJsonReader(source, variantsPath.toAbsolutePath().toString(), filePath.toAbsolutePath().toString());


        List<Task<Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        Properties properties = new Properties();
        properties.load(new InputStreamReader(new FileInputStream(credentialsPath.toString())));
        OpenCGACredentials credentials = new MongoCredentials(properties);
        VariantWriter mongoWriter = new PVSVariantMongoWriter(source, (MongoCredentials) credentials,
                properties.getProperty("collection_variants", "variants"),
                properties.getProperty("collection_files", "files"));

        mongoWriter.includeStats(true);
        mongoWriter.includeEffect(false);
        mongoWriter.includeSamples(false);

        writers.add(mongoWriter);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("Loading variants...");
        variantRunner.run();
        System.out.println("Variants loaded!");

    }

    private static void transformVariants(VariantSource source, Path file, Path outdir) throws IOException {

        VariantReader reader = null;

        switch (source.getAggregation()) {
            case NONE:
                reader = new VariantVcfReader(source, file.toAbsolutePath().toString());
                break;
            case BASIC:
                reader = new VariantVcfReader(source, file.toAbsolutePath().toString(), new VariantAggregatedVcfFactory());
                break;
            case EVS:
                reader = new VariantVcfReader(source, file.toAbsolutePath().toString(), new VariantVcfEVSFactory());
                break;
            default:
                reader = new VariantVcfReader(source, file.toAbsolutePath().toString());
        }

        List<Task<Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        VariantWriter jsonWriter = new PVSJsonWriter(source, outdir);


        if (source.getAggregation() == VariantSource.Aggregation.NONE) {
            taskList.add(new VariantStatsTask(reader, source));
            jsonWriter.includeStats(true);
        } else {
            jsonWriter.includeStats(false);
        }
        jsonWriter.includeEffect(false);
        jsonWriter.includeSamples(false);

        writers.add(jsonWriter);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("Indexing variants...");
        variantRunner.run();
        System.out.println("Variants indexed!");

    }
}
