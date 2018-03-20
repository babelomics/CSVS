package org.babelomics.csvs.app.cli;

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
    private final CommandAnnotFile annotFile;
    private final CommandRecalculate recalculate;


    public OptionsParser() {
        jcommander = new JCommander();
        jcommander.addCommand(load = new CommandLoad());
        jcommander.addCommand(unload = new CommandUnload());
        jcommander.addCommand(calculateCounts = new CommandCount());
        jcommander.addCommand(query = new CommandQuery());
        jcommander.addCommand(setup = new CommandSetup());
        jcommander.addCommand(annot = new CommandAnnot());
        jcommander.addCommand(annotFile = new CommandAnnotFile());
        jcommander.addCommand(recalculate = new CommandRecalculate());
    }

    interface Command {
    }

    @Parameters(commandNames = {"load"}, commandDescription = "Loads an already generated data model into a backend")
    class CommandLoad implements Command {

        @Parameter(names = {"-i", "--input"}, description = "Prefix of files to save in the selected backend", required = true, arity = 1)
        String input;

        @Parameter(names = {"-d", "--diseaseId"}, description = "Disease group Id", required = true, arity = 1)
        int disease;

        @Parameter(names = {"-t", "--technologyId"}, description = "Technology Id", required = true, arity = 1)
        int technology;

        @Parameter(names = {"-p","--panelFile"}, description = "PanelFile with list of regions")
        String panelFile="";

        @Parameter(names = {"-r", "--recalculate"}, description = "If panel recalculate all variants", arity = 1)
        boolean recalculate=true;

        @Parameter(names = {"-pr", "--personReference"}, description = "Name person reference", arity = 1)
        String personReference="";

        @Parameter(names = {"--filter"}, description = "Filter file: remove variants when not in the panel", arity = 0)
        boolean filter=false;

        @Parameter(names = {"-c", "--checkPanel"}, description = "Check  variants in the panel and format file", arity = 1)
        boolean checkPanel=true;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";
    }

    @Parameters(commandNames = {"unload"}, commandDescription = "Unloads variants from the DB")
    class CommandUnload implements Command {

        @Parameter(names = {"-i", "--input"}, description = "DESC", required = true, arity = 1)
        String input;

        @Parameter(names = {"-d", "--diseaseId"}, description = "Disease group Id", required = true, arity = 1)
        int disease;

        @Parameter(names = {"-t", "--technologyId"}, description = "Technology Id", required = true, arity = 1)
        int technology;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";
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

        @Parameter(names = {"--technologies"}, description = "List all technologies", arity = 0)
        boolean technologies = false;

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

        @Parameter(names = {"--technologyId"}, description = "TechnologyId")
        List<Integer> technologyId = new ArrayList<>();

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
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";


    }

    @Parameters(commandNames = {"setup"}, commandDescription = "Setup Database")
    class CommandSetup implements Command {

        @Parameter(names = {"--populate-diseases"}, description = "Populate diseases", arity = 0)
        boolean populateDiseases;

        @Parameter(names = {"--populate-technologies"}, description = "Populate technologies", arity = 0)
        boolean populateTechnologies;

        @Parameter(names = {"--new-disease"}, description = "New disease", arity = 1)
        String newDisease;

        @Parameter(names = {"--new-technology"}, description = "New technology", arity = 1)
        String newTechnology;

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";
        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";


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
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";

    }

    @Parameters(commandNames = {"annot-file"}, commandDescription = "Annot VCF file")
    class CommandAnnotFile implements Command {

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";

        @Parameter(names = {"--diseaseId"}, description = "DiseaseId")
        List<Integer> diseaseId = new ArrayList<>();

        @Parameter(names = {"--technologyId"}, description = "TechnologyId")
        List<Integer> technologyId = new ArrayList<>();

        @Parameter(names = {"--input"}, description = "Input file", arity = 1)
        String input = "";

        @Parameter(names = {"--outfile"}, description = "Output file", arity = 1)
        String outfile = "output.vcf";

        @Parameter(names = {"--outdir"}, description = "Output dir", arity = 1)
        String outdir = "./";


    }

    @Parameters(commandNames = {"recalculate"}, commandDescription = "Calculate examples of a variant studied in a region")
    class CommandRecalculate implements Command {

        @Parameter(names = {"--diseaseId"}, description = "DiseaseId")
        List<Integer> diseaseId = new ArrayList<>();

        @Parameter(names = {"--technologyId"}, description = "TechnologyId")
        List<Integer> technologyId = new ArrayList<>();

        @Parameter(names = {"--panelName"}, description = "Panel name to recalculate", required = true, arity = 1)
        String panelName = "";

        @Parameter(names = {"--host"}, description = "DB host", arity = 1)
        String host = "localhost";

        @Parameter(names = {"--user"}, description = "DB User", arity = 1)
        String user = "";
        @Parameter(names = {"--pass"}, description = "DB Pass", arity = 1)
        String pass = "";
        @Parameter(names = {"--dbName"}, description = "DB Name", arity = 1)
        String dbName = "csvs";
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

    CommandAnnotFile getAnnotFileCommand() {
        return annotFile;
    }

    CommandRecalculate getRecalculateCommand() { return recalculate; }
}
