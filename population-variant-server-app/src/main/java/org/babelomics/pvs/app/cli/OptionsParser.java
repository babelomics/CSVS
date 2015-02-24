package org.babelomics.pvs.app.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.opencb.biodata.models.variant.VariantSource;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class OptionsParser {

    private final JCommander jcommander;

    private final CommandTransformVariants transform;
    private final CommandLoadVariants load;
    private final CommandAddVariants add;
    private final CommandCompressVariants compress;


    public OptionsParser() {
        jcommander = new JCommander();
        jcommander.addCommand(transform = new CommandTransformVariants());
        jcommander.addCommand(load = new CommandLoadVariants());
        jcommander.addCommand(add = new CommandAddVariants());
        jcommander.addCommand(compress = new CommandCompressVariants());

    }

    interface Command {
    }

    @Parameters(commandNames = {"transform-variants"}, commandDescription = "Generates a data model from an input file")
    class CommandTransformVariants implements Command {

        @Parameter(names = {"-i", "--input"}, description = "File to transform into the OpenCGA data model", required = true, arity = 1)
        String file;

        @Parameter(names = {"-s", "--study"}, description = "Full name of the study where the file is classified", required = true, arity = 1)
        String study;

        @Parameter(names = {"-o", "--outdir"}, description = "Directory where output files will be saved", arity = 1)
        String outdir;

        @Parameter(names = {"-d", "--disease"}, description = "Disease", arity = 1, required = true)
        String disease;

        @Parameter(names = {"-p", "--phenotype"}, description = "Phenotype", arity = 1, required = true)
        String phenotype;

        @Parameter(names = {"--paper"}, description = "Paper", arity = 1)
        String paper;

        @Parameter(names = {"--description"}, description = "Description", arity = 1)
        String description;

        @Parameter(names = {"--static"}, description = "Static study, its MAF will not be combined", arity = 0)
        boolean staticStudy;

        @Parameter(names = {"-c, --coverage"}, description = "Coverage", arity = 1)
        int coverage = -1;

        @Parameter(names = {"--aggregated"}, description = "Aggregated VCF File: basic or EVS (optional)", arity = 1)
        VariantSource.Aggregation aggregated = VariantSource.Aggregation.NONE;


    }

    @Parameters(commandNames = {"load-variants"}, commandDescription = "Loads an already generated data model into a backend")
    class CommandLoadVariants implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Prefix of files to save in the selected backend", required = true, arity = 1)
        String input;

        @Parameter(names = {"-c", "--credentials"}, description = "Path to the file where the backend credentials are stored", required = true, arity = 1)
        String credentials;

    }

    @Parameters(commandNames = {"add-variants"}, commandDescription = "Add an already generated data model in an existing Study into a backend")
    class CommandAddVariants implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Prefix of files to save in the selected backend", required = true, arity = 1)
        String input;

        @Parameter(names = {"-c", "--credentials"}, description = "Path to the file where the backend credentials are stored", required = true, arity = 1)
        String credentials;

    }

    @Parameters(commandNames = {"compress-variants"}, commandDescription = "Compress Variants")
    class CommandCompressVariants implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Input File", required = true, arity = 1)
        String input;

        @Parameter(names = {"-o", "--output"}, description = "Output File", arity = 1)
        String output;

    }


    String parse(String[] args) throws ParameterException {
        jcommander.parse(args);
        return jcommander.getParsedCommand();
    }

    String usage() {
        StringBuilder builder = new StringBuilder();
        jcommander.usage(builder);
        return builder.toString();
    }

    CommandLoadVariants getLoadCommand() {
        return load;
    }

    CommandTransformVariants getTransformCommand() {
        return transform;
    }

    CommandAddVariants getAddCommand() {
        return add;
    }

    CommandCompressVariants getCompressComand() {
        return compress;
    }

}
