package org.babelomics.pvs.app.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class OptionsParser {

    private final JCommander jcommander;

    private final CommandLoadVariants load;
    private final CommandCalculateCounts calculateCounts;
    private final CommandQuery query;
    private final CommandSetup setup;


    public OptionsParser() {
        jcommander = new JCommander();
        jcommander.addCommand(load = new CommandLoadVariants());
        jcommander.addCommand(calculateCounts = new CommandCalculateCounts());
        jcommander.addCommand(query = new CommandQuery());
        jcommander.addCommand(setup = new CommandSetup());

    }

    interface Command {
    }

    @Parameters(commandNames = {"load-variants"}, commandDescription = "Loads an already generated data model into a backend")
    class CommandLoadVariants implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Prefix of files to save in the selected backend", required = true, arity = 1)
        String input;

        @Parameter(names = {"-d", "--diseaseId"}, description = "Disease group Id", required = true, arity = 1)
        int disease;
    }

    @Parameters(commandNames = {"calculate-counts"}, commandDescription = "Calculate genotype counts")
    class CommandCalculateCounts implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Input File", required = true, arity = 1)
        String input;

        @Parameter(names = {"-o", "--output"}, description = "Output File", required = true, arity = 1)
        String output;

    }

    @Parameters(commandNames = {"query"}, commandDescription = "Query")
    class CommandQuery implements Command {

        @Parameter(names = {"--diseases"}, description = "List all disease groups", arity = 0)
        boolean diseases;


    }

    @Parameters(commandNames = {"setup"}, commandDescription = "Setup Database")
    class CommandSetup implements Command {


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

    CommandCalculateCounts getCalculateCuntsCommand() {
        return calculateCounts;
    }

    CommandSetup getSetupCommand() {
        return setup;
    }

    CommandQuery getQueryCommand() {
        return query;
    }

}
