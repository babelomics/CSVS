package org.babelomics.csvs.app.cli;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.CSVSUtil;
import org.babelomics.csvs.lib.io.CSVSQueryManager;
import org.babelomics.csvs.lib.models.DiseaseCount;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Technology;
import org.babelomics.csvs.lib.models.Variant;
import org.mongodb.morphia.Datastore;
import org.opencb.biodata.models.feature.Region;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.cellbase.core.common.core.Gene;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSMain {


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
                case "recalculate":
                    command = parser.getRecalculateCommand();
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

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);


            if (c.populateDiseases) {
                CSVSUtil.populateDiseases(datastore);
            }

            if (c.populateTechnologies) {
                CSVSUtil.populateTechnologies(datastore);
            }


            if (c.newDisease != null && c.newDisease.length() > 0) {
                CSVSUtil.addNewDisease(datastore, c.newDisease);
            }

        } else if (command instanceof OptionsParser.CommandLoad) {
            OptionsParser.CommandLoad c = (OptionsParser.CommandLoad) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;
            int technologyId = c.technology;

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            Path panelFile = null;
            if (!c.panelFile.isEmpty())
                panelFile = Paths.get(c.panelFile);

            String personReference = c.personReference;

            CSVSUtil.loadVariants(inputFile, diseaseGroupId, technologyId, datastore, panelFile, personReference);

            if(panelFile != null && c.recalculate) {
                List<Integer>  diseases = new ArrayList<>();
                diseases.add(diseaseGroupId);
                List<Integer> technologies = new ArrayList<>();
                technologies.add(technologyId);
		
                CSVSUtil.recalculate(diseases, technologies, c.panelFile ,datastore);
            }

        } else if (command instanceof OptionsParser.CommandUnload) {
            OptionsParser.CommandUnload c = (OptionsParser.CommandUnload) command;

            Path inputFile = Paths.get(c.input);
            int diseaseGroupId = c.disease;
            int technologyId = c.technology;

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            CSVSUtil.unloadVariants(inputFile, diseaseGroupId, technologyId, datastore);
        } else if (command instanceof OptionsParser.CommandCount) {
            OptionsParser.CommandCount c = (OptionsParser.CommandCount) command;

            Path input = Paths.get(c.input);
            Path output = Paths.get(c.output);

            CSVSUtil.compressVariants(input, output);
        } else if (command instanceof OptionsParser.CommandAnnot) {

            OptionsParser.CommandAnnot c = (OptionsParser.CommandAnnot) command;

            boolean ct = c.ct;
            boolean remove = c.remove;
            boolean override = c.override;
            boolean gene = c.gene;

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            CSVSUtil.annot(ct, remove, override, gene, datastore);

        } else if (command instanceof OptionsParser.CommandQuery) {
            OptionsParser.CommandQuery c = (OptionsParser.CommandQuery) command;

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            CSVSQueryManager qm = new CSVSQueryManager(datastore);


            if (c.diseases) {

                System.out.println("\n\nList of Groups of Diseases\n==========================\n");

                List<DiseaseGroup> query = qm.getAllDiseaseGroups();
                System.out.println("Id\tName\tSamples\tVariants");
                for (DiseaseGroup dg : query) {
                    System.out.println(dg.getGroupId() + "\t" + dg.getName() + "\t" + dg.getSamples() + "\t" + dg.getVariants());
                }

            } else if (c.technologies) {
                System.out.println("\n\nList of Technologies\n==========================\n");

                List<Technology> query = qm.getAllTechnologies();

                for (Technology technology : query) {
                    System.out.println(technology.getTechnologyId() + "\t" + technology.getName() + "\t" + technology.getSamples() + "\t" + technology.getVariants());
                }

            } else if (c.regionLIst.size() > 0 || c.geneList.size() > 0) {

                List<Integer> diseaseId = c.diseaseId;
                List<Integer> technologyId = c.technologyId;
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

                Iterable<Variant> query = qm.getVariantsByRegionList(regionList, diseaseId, technologyId, c.skip, c.limit, false, count);

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
                List<Integer> technologyId = c.technologyId;

                MutableLong count = new MutableLong(-1);
                Iterable<Variant> query = qm.getAllVariants(diseaseId, technologyId, c.skip, c.limit, count);

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
            String output = c.outdir + "/" + c.outfile;
            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            CSVSUtil.annotFile(input, output, c.diseaseId, c.technologyId, datastore);

        } else if (command instanceof OptionsParser.CommandRecalculate) {
            OptionsParser.CommandRecalculate c = (OptionsParser.CommandRecalculate) command;

            Datastore datastore = CSVSUtil.getDatastore(c.host, c.user, c.pass, c.dbName);

            CSVSUtil.recalculate( c.diseaseId, c.technologyId, c.panelName, datastore);

        } else {
            System.err.println("Comand not found");
        }

    }

}
