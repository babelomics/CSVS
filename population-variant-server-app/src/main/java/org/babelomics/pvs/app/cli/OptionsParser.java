package org.babelomics.pvs.app.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class OptionsParser {

    private final JCommander jcommander;

    private final CommandLoad load;
    private final CommandUnload unload;
    private final CommandCount calculateCounts;
    private final CommandQuery query;
    private final CommandSetup setup;
    private final CommandAnnot annot;


    public OptionsParser() {
        jcommander = new JCommander();
        jcommander.addCommand(load = new CommandLoad());
        jcommander.addCommand(unload = new CommandUnload());
        jcommander.addCommand(calculateCounts = new CommandCount());
        jcommander.addCommand(query = new CommandQuery());
        jcommander.addCommand(setup = new CommandSetup());
        jcommander.addCommand(annot = new CommandAnnot());

    }

    interface Command {
    }

    @Parameters(commandNames = {"load"}, commandDescription = "Loads an already generated data model into a backend")
    class CommandLoad implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Prefix of files to save in the selected backend", required = true, arity = 1)
        String input;

        @Parameter(names = {"-d", "--diseaseId"}, description = "Disease group Id", required = true, arity = 1)
        int disease;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
    }

    @Parameters(commandNames = {"unload"}, commandDescription = "Unloads variants from the DB")
    class CommandUnload implements Command {

        @Parameter(names = {"-i", "--input"}, description = "DESC", required = true, arity = 1)
        String input;

        @Parameter(names = {"-d", "--diseaseId"}, description = "Disease group Id", required = true, arity = 1)
        int disease;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
    }

    @Parameters(commandNames = {"count"}, commandDescription = "Calculate genotype counts")
    class CommandCount implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Input File", required = true, arity = 1)
        String input;

        @Parameter(names = {"-o", "--output"}, description = "Output File", required = true, arity = 1)
        String output;


    }

    @Parameters(commandNames = {"query"}, commandDescription = "Query")
    class CommandQuery implements Command {

        @Parameter(names = {"--diseases"}, description = "List all disease groups", arity = 0)
        boolean diseases = false;

        @Parameter(names = {"--all"}, description = "List all variants", arity = 0)
        boolean all = false;

        @Parameter(names = {"--csv"}, description = "Export to CSV", arity = 0)
        boolean csv;

        @Parameter(names = {"--outfile"}, description = "Output file", arity = 1)
        String outfile = "query.csv";

        @Parameter(names = {"--regions"}, description = "Comma-separated list of regions")
        List<String> regionLIst = new ArrayList<>();

        @Parameter(names = {"--genes"}, description = "Comma-separated list of genes")
        List<String> geneList = new ArrayList<>();

        @Parameter(names = {"--diseaseId"}, description = "DiseaseId")
        List<Integer> diseaseId = new ArrayList<>();

        @Parameter(names = {"--skip"}, description = "Skip")
        Integer skip = null;

        @Parameter(names = {"--limit"}, description = "Limit")
        Integer limit = null;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";


    }

    @Parameters(commandNames = {"setup"}, commandDescription = "Setup Database")
    class CommandSetup implements Command {

        @Parameter(names = {"--populate-diseases"}, description = "Populate diseases", arity = 0)
        boolean populateDiseases;

        @Parameter(names = {"--new-disease"}, description = "New Diseases", arity = 1)
        String newDisease;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";
        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";


    }

    @Parameters(commandNames = {"annot"}, commandDescription = "Annot Variants")
    class CommandAnnot implements Command {

        @Parameter(names = {"--ct"}, description = "Annot consequence Type", arity = 0)
        boolean ct;

        @Parameter(names = {"--gene"}, description = "Annot Gene", arity = 0)
        boolean gene;

        @Parameter(names = {"--remove"}, description = "Remove selected annotations", arity = 0)
        boolean remove;

        @Parameter(names = {"--override"}, description = "Oerride selected annotations", arity = 0)
        boolean override;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";

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

    CommandLoad getLoadCommand() {
        return load;
    }

    CommandUnload getUnloadCommand() {
        return unload;
    }

    CommandCount getCalculateCuntsCommand() {
        return calculateCounts;
    }

    CommandSetup getSetupCommand() {
        return setup;
    }

    CommandQuery getQueryCommand() {
        return query;
    }

    CommandAnnot getAnnotCommand() {
        return annot;
    }

}
