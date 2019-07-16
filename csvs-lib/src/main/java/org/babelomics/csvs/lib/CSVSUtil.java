package org.babelomics.csvs.lib;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.Cursor;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.*;
import org.babelomics.csvs.lib.annot.CellBaseAnnotator;
import org.babelomics.csvs.lib.io.*;
import org.babelomics.csvs.lib.models.*;
import org.babelomics.csvs.lib.models.Panel;
import org.babelomics.csvs.lib.models.Region;
import org.babelomics.csvs.lib.stats.CSVSVariantStatsTask;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Criteria;
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

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.System.exit;


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
        diseaseGroups.add(new DiseaseGroup(19, "XIX Injury, poisoning and certain other consequences of external causes"));
        diseaseGroups.add(new DiseaseGroup(20, "XX External causes of morbidity and mortality"));
        diseaseGroups.add(new DiseaseGroup(21, "XXI Factors influencing health status and contact with health services"));
        diseaseGroups.add(new DiseaseGroup(22, "XXII Codes for special purposes"));
        diseaseGroups.add(new DiseaseGroup(23, "MGP (267 healthy controls)"));
        diseaseGroups.add(new DiseaseGroup(24, "IBS (107 Spanish individuals from 1000genomes)"));
        diseaseGroups.add(new DiseaseGroup(25, "V Mental and behavioural disorders(controls)"));
        diseaseGroups.add(new DiseaseGroup(26, "Healthy controls"));

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

        technologies.add(new Technology(1, "Illumina"));
        technologies.add(new Technology(2, "SOLiD"));
        technologies.add(new Technology(3, "Roche 454"));
        technologies.add(new Technology(4, "IonTorrent"));
        technologies.add(new Technology(5, "Nanopore"));

        for (Technology t : technologies) {
            try {
                datastore.save(t);
            } catch (DuplicateKeyException e) {
                System.err.println("Duplicated Technology: " + t);
            }
        }
    }

    /**
     * Method to load regions.
     * @param datastore
     * @param panelFilePath
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
     private static Panel loadPanel(Datastore datastore, Path panelFilePath) throws IOException, NoSuchAlgorithmException {

         String panelSha256 = calculateSHA256(panelFilePath);
         Panel p = datastore.createQuery(Panel.class).field("sum").equal(panelSha256)
                 .field("panelName").equal(panelFilePath.getFileName().toString())
                 .get();

         if (p == null){
             p = new Panel(panelSha256, panelFilePath.getFileName().toString());
             datastore.save(p);
             DataReader<Region> regionReader = new CSVSRegionsCSVDataReader(panelFilePath.toAbsolutePath().toString(), p);
             DataWriter<Region> writerRegions = new CSVSRegionsMongoWriter(p, datastore);
             List<DataWriter<Region>> writersRegions = new ArrayList<>();

             writersRegions.add(writerRegions);

             // Load region
             Runner<Region> pvsRegionRunner = new CSVSRegionsRunner(regionReader, writersRegions,  new SortedList<>(), 100);
             System.out.println("Loading regions...");
             pvsRegionRunner.run();
             System.out.println("Regions loaded!");
         }

         return p;
     }


    public static void loadVariants(Path variantsPath, int diseaseGroupId, int technologyId, Datastore datastore ) throws IOException, NoSuchAlgorithmException {
        loadVariants(variantsPath,diseaseGroupId,technologyId,datastore, null, "", true);
    }


    public static void loadVariants(Path variantsPath, int diseaseGroupId, int technologyId, Datastore datastore,  Path panelFilePath, String personReference, boolean checkPanel ) throws IOException, NoSuchAlgorithmException {
        System.out.println("START: loadVariant " + new Date());
        File f;
        String sha256 = calculateSHA256(variantsPath);

        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        if (fDb == null) {

            Query<DiseaseGroup> queryDiseaseGroup = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
            DiseaseGroup dg = queryDiseaseGroup.get();

            Query<Technology> queryTechnology = datastore.createQuery(Technology.class).field("technologyId").equal(technologyId);
            Technology t = queryTechnology.get();

            f = new File(sha256, dg, t);
            f.setNameFile(variantsPath.getFileName().toString());
            if (!personReference.isEmpty())
                f.setPersonReference(personReference);
            f.setDate(new Date());

            // Read panelFile with regions
            Panel p = null;
            List <Region> regions = new ArrayList<>();
            if (panelFilePath != null) {
                p = loadPanel(datastore, panelFilePath);
                f.setIdPanel( (ObjectId) p.getId());
                regions = datastore.createQuery(Region.class).field("pid").equal((ObjectId) p.getId()).asList();
            }

            try {
                // Save file and panel
                datastore.save(f);
                System.out.println("Save File: " + f.getNameFile());
            } catch (DuplicateKeyException ignored) {

            }

            // Read variants
            DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg, t, (checkPanel == true)? p: null, checkPanel, regions);

            List<Task<Variant>> taskList = new SortedList<>();
            List<DataWriter<Variant>> writers = new ArrayList<>();
            DataWriter<Variant> writer = new CSVSVariantCountsMongoWriter(dg, t, f, datastore);

            writers.add(writer);

            Runner<Variant> pvsRunner = new CSVSRunner(reader, writers, taskList, 100);

            System.out.println("Loading variants...");
            pvsRunner.run();
            System.out.println("Variants loaded!");

            try {
                // Update samples
                datastore.save(f);
                System.out.println("Update File " + f.getNameFile() +  " Samples: " +f.getSamples());
            } catch (DuplicateKeyException ignored) {

            }

        } else {
            System.out.println("File is already in the database.  (Name: " + fDb.getNameFile() + " Sum: " +fDb.getSum() + ")");
            exit(0);
        }
         System.out.println("END: loadVariant " + new Date());
    }

    public static void unloadVariants(Path variantsPath, int diseaseGroupId, int technologyId, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        int samples = 0;
        int variants = 0;

        String sha256 = calculateSHA256(variantsPath);
        File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

        if (fDb == null) {
            System.out.println("File is not in the database");
            exit(1);
        }

        Query<DiseaseGroup> queryDiseaseGroup = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(diseaseGroupId);
        DiseaseGroup dg = queryDiseaseGroup.get();

        Query<Technology> queryTechnology = datastore.createQuery(Technology.class).field("technologyId").equal(technologyId);
        Technology t = queryTechnology.get();

        Panel p =  null;
        if (fDb.getIdPanel() != null) {
           p = datastore.createQuery(Panel.class).field("_id").equal(fDb.getIdPanel()).get();
        }

        DataReader<Variant> reader = new CSVSVariantCountCSVDataReader(variantsPath.toAbsolutePath().toString(), dg, t, p);

        reader.open();
        reader.pre();

        List<Variant> batch;
        batch = reader.read(1);

        int variantsD = 0 , variantsT = 0;

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

                // Ini: Calculate num new variants add in the file
                v =  datastore.createQuery(Variant.class).field("chromosome").equal(elem.getChromosome())
                        .field("position").equal(elem.getPosition()).
                                field("reference").equal(elem.getReference()).
                                field("alternate").equal(elem.getAlternate())
                        .get();

                if (v == null) {
                    variantsD++;
                    variantsT++;
                } else {
                    boolean vd = false, vt = false;
                    for (DiseaseCount dc : v.getDiseases()) {
                        if (!vd && dc.getDiseaseGroup().getGroupId() == diseaseGroupId) {
                            vd = true;
                        }
                        if (!vt && dc.getTechnology().getTechnologyId() == technologyId) {
                            vt = true;
                        }
                    }
                    if (!vd) {
                        variantsD++;
                    }
                    if (!vt) {
                        variantsT++;
                    }
                }
                // End: Calculate num new variants add in the file
            }
            batch.clear();
            batch = reader.read();
        }

        reader.post();
        reader.close();

        // Delete fileVariant
        Query<FileVariant>  queryfv = datastore.createQuery(FileVariant.class);
        queryfv.field("fid").equal(fDb.getId());
        datastore.delete(queryfv);

        if (fDb.getIdPanel() != null) {
            File fileDelete = new File(null, fDb.getDisease(), fDb.getTechnology(), p);
            fileDelete.setSamples(fDb.getSamples());
            datastore.delete(File.class, fDb.getId());

            // Reload sum variants with panels
            List diseases  = new ArrayList();
            diseases.add(diseaseGroupId);
            List tecnologies  = new ArrayList();
            tecnologies.add(technologyId);
            recalculate(diseases, tecnologies, p.getPanelName(), datastore);
            recalculateCheckUnload(diseases, tecnologies, datastore);

            // Delete panel if it is not used
            long numUsed = datastore.createQuery(File.class).field ("pid").equal(fileDelete.getIdPanel()).countAll();
            if (numUsed == 0 ) {
                // Delete Regions
                Query<Region>  queryr = datastore.createQuery(Region.class);
                queryr.field("pid").equal(p.getId());
                datastore.delete(queryr);
                // Delete Panel
                datastore.delete(p);
            }
        } else
            datastore.delete(File.class, fDb.getId());


        // Delete samples
        dg.setSamples(dg.getSamples()-samples);
        t.setSamples(t.getSamples()-samples);

        // Delete news variants
        dg.setVariants(dg.getVariants()-variantsD);
        t.setVariants(t.getVariants()-variantsT);
        datastore.save(dg);
        datastore.save(t);
    }

    /**
     * Method to add criteria to search in a file
     * @param regions
     * @param datastore
     * @return
     */
    private static Criteria[] criteriaSearchVariant(List<Region> regions,  Datastore datastore){
        Criteria[] or = new Criteria[regions.size()];
        int j = 0;
        for (Region r : regions) {
            Query<Variant> auxQuery = datastore.createQuery(Variant.class);
            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));

            or[j++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }
        return or;
    }


    public static void annotFile(String input, String output, List<Integer> diseases, List<Integer> technologies, Datastore datastore) {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);

        boolean all_dis = false;
        boolean all_tech = false;

        if (diseases == null || diseases.isEmpty()) {
            diseases = qm.getAllDiseaseGroupIds();
            all_dis = true;
        }
        if (technologies == null || technologies.isEmpty()) {
            technologies = qm.getAllTechnologieIds();
            all_tech = true;
        }

        VCFFileReader variantReader = new VCFFileReader(new java.io.File(input), false);
        VCFHeader fileHeader = variantReader.getFileHeader();

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder().clearOptions();
        builder.setOption(Options.ALLOW_MISSING_FIELDS_IN_HEADER);


        VCFInfoHeaderLine csvs_maf = new VCFInfoHeaderLine("CSVS_MAF", VCFHeaderLineCount.A, VCFHeaderLineType.Float, "MAF from CSVS, for each ALT allele, in the same order as listed");
        VCFInfoHeaderLine csvs_dis = new VCFInfoHeaderLine("CSVS_DIS", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Selected diseases from CSVS");
        VCFInfoHeaderLine csvs_tech = new VCFInfoHeaderLine("CSVS_TECH", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Selected technologies from CSVS");
        VCFInfoHeaderLine csvs_gc = new VCFInfoHeaderLine("CSVS_GC", VCFHeaderLineCount.A, VCFHeaderLineType.Integer, "Genotype count(0/0,0/1,1/1,./.) for each alt Allele in the same order as listed");

        fileHeader.addMetaDataLine(csvs_maf);
        fileHeader.addMetaDataLine(csvs_dis);
        fileHeader.addMetaDataLine(csvs_tech);
        fileHeader.addMetaDataLine(csvs_gc);


        final VariantContextWriter vcfWriter = builder.setOutputFile(new java.io.File(output)).build();
        vcfWriter.writeHeader(fileHeader);


        for (VariantContext context : variantReader) {
            String chr = context.getContig();
            chr = removePrefix(chr);
            int pos = context.getStart();
            String ref = context.getReference().getBaseString();

            List<String> mafs = new ArrayList<>();
            List<String> counts = new ArrayList<>();
            boolean maf = false;


            for (Allele altAllele : context.getAlternateAlleles()) {
                String alt = altAllele.getBaseString();
                Variant dbVariant = qm.getVariant(chr, pos, ref, alt, diseases, technologies);
                if (dbVariant != null) {
                    DiseaseCount dc = dbVariant.getStats();
                    mafs.add(String.valueOf(dc.getMaf()));
                    counts.add("0/0:" + dc.getGt00() + ",0/1:" + dc.getGt01() + ",1/1:" + dc.getGt11() + ",./.:" + dc.getGtmissing());
                    maf = true;
                } else {
                    mafs.add(".");
                    counts.add(".");
                }
            }
            if (maf && mafs.size() == context.getAlternateAlleles().size()) {
                context.getCommonInfo().putAttribute("CSVS_MAF", Joiner.on(",").join(mafs));
                context.getCommonInfo().putAttribute("CSVS_DIS", (all_dis) ? "ALL" : Joiner.on(",").join(diseases));
                context.getCommonInfo().putAttribute("CSVS_TECH", (all_tech) ? "ALL" : Joiner.on(",").join(technologies));
                context.getCommonInfo().putAttribute("CSVS_GC", Joiner.on(";").join(counts));
            }


            vcfWriter.add(context);
        }

        variantReader.close();
        vcfWriter.close();
    }

    public static void compressVariants(Path input, Path output, boolean replaceAF) throws IOException {

        VariantSource source = new VariantSource("file", "file", "file", "file");
        //VariantReader reader = new VariantVcfReader(source, input.toAbsolutePath().toString());
        VariantReader reader = new CSVSVariantVcfReader(source, input.toAbsolutePath().toString(), replaceAF);
        VariantWriter writer = new CSVSVariantCountsCSVDataWriter(output.toAbsolutePath().toString());


        List<Task<org.opencb.biodata.models.variant.Variant>> taskList = new SortedList<>();
        List<VariantWriter> writers = new ArrayList<>();

        taskList.add(new CSVSVariantStatsTask(reader, source));
        writers.add(writer);

        VariantRunner variantRunner = new VariantRunner(source, reader, null, writers, taskList, 100);

        System.out.println("File: " + input.getFileName());
        System.out.println("Compressing variants...");
        variantRunner.run();
        System.out.println("Variants compressed!");

    }



    /**
     * Calculate examples of a variant for all variants - ALL.
     * @param diseases
     * @param technologies
     * @param datastore
     */
    public static void recalculate(List<Integer> diseases , List<Integer> technologies, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        CSVSQueryManager qm = new CSVSQueryManager(datastore);
        long numVAr = 0;

        if (diseases == null || diseases.isEmpty()) {
            diseases = qm.getAllDiseaseGroupIds();
        }
        if (technologies == null || technologies.isEmpty()) {
            technologies = qm.getAllTechnologieIds();
        }

        // Search files with panel
        Query<File> queryFile = datastore.createQuery(File.class);
        List<File> files = queryFile.field("dgid").in(diseases).field("tid").in(technologies).field("pid").exists().asList();

        // Check if exist panels for disease and technology
        if (files.size() > 0) {
            // All variants
            Query<Variant> queryVariant = datastore.createQuery(Variant.class);

            System.out.println("All variants = " + queryVariant.countAll());
            System.out.println("INI  =" +  new Date());
            Map<String, Map> calculateSampleRegions = qm.calculateSampleRegions(datastore);
            recalc_regions(numVAr, qm, diseases, technologies, queryVariant, calculateSampleRegions, datastore);
        }
    }



    /**
     * Calculate examples of a variant studied in a region (panel) - EXOME.
     * @param diseases List diseased
     * @param technologies List technologies
     * @param panelName Name panel to search regions. If null seach all panel by disease and technologies
     * @param datastore
     */
    public static void recalculate( List<Integer> diseases, List<Integer> technologies, String panelName, Datastore datastore) {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);

        if (diseases == null || diseases.isEmpty()) {
            diseases = qm.getAllDiseaseGroupIds();
        }
        if (technologies == null || technologies.isEmpty()) {
            technologies = qm.getAllTechnologieIds();
        }

        System.out.println("All variants = "  + datastore.createQuery(Variant.class).countAll());
        System.out.println("INI  =" +  new Date());

        // Search file with regions
        Query<Panel> queryFile = datastore.createQuery(Panel.class);
        queryFile.field("panelName").equal(panelName);
        Panel p = queryFile.get();

        // Basch regions: Search only affeted variants
        if (p != null) {
            long num_regions =  datastore.createQuery(Region.class).field("pid").equal(p.getId()).countAll();
            int limit = 1000;
            long pages = num_regions/limit;
            long numVAr = 0;
            Map<String, Map> calculateSampleRegions = qm.calculateSampleRegions(datastore);
            for ( int skip = 0 ; skip <= pages; skip++) {
                System.out.println("Skip Region: " + skip);
                Iterator<Region> regions_iterator = datastore.createQuery(Region.class).field("pid").equal(p.getId()).offset(skip*limit).limit(limit).iterator();
                Query<Variant> queryVariant = datastore.createQuery(Variant.class);
                // if check regions
                if (regions_iterator != null && regions_iterator.hasNext()) {
                    List<Region> regions = Lists.newArrayList(regions_iterator);

                    queryVariant.or(criteriaSearchVariant(regions, datastore));
                    System.out.println("Variants banch in regions = " + queryVariant.countAll());
                    // System.out.println("Variants banch in regions = " + regions.toString());
                }
                numVAr = recalc_regions(numVAr, qm, diseases, technologies, queryVariant, calculateSampleRegions, datastore);
            }
        }
    }


    /**
     * Calculate examples of a variant for a new file - GENOME.
     * @param variantsPath
     * @param diseases
     * @param technologies
     * @param datastore
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void recalculate(Path variantsPath, List<Integer> diseases , List<Integer> technologies, Datastore datastore) throws IOException, NoSuchAlgorithmException {

        CSVSQueryManager qm = new CSVSQueryManager(datastore);
        long numVAr = 0;

        if (diseases == null || diseases.isEmpty()) {
            diseases = qm.getAllDiseaseGroupIds();
        }
        if (technologies == null || technologies.isEmpty()) {
            technologies = qm.getAllTechnologieIds();
        }

        // Search files with panel
        Query<File> queryFile = datastore.createQuery(File.class);
        List<File> files = queryFile.field("dgid").in(diseases).field("tid").in(technologies).field("pid").exists().asList();

        // Check if exist panels for disease and technology
        if (files.size() > 0) {
            File fDb = null;

            // Check exist file
            if (variantsPath != null) {
                String sha256 = calculateSHA256(variantsPath);
                fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();
            }
            if (fDb == null || variantsPath == null) {
                System.out.println("File don't exist");
            } else {
                Map<String, Map> calculateSampleRegions = qm.calculateSampleRegions(datastore);

                // Search variant news load in the file
                int limit = 1000;
                int numVariants = 0;
                List<ObjectId> variantObjId = new ArrayList<>();

                AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();

                List<BasicDBObject> aggList = new ArrayList<>();
                BasicDBObject group = new BasicDBObject().append("_id", "$vid").append("sumFile", new BasicDBObject("$sum",1)).append("itemsFileId", new BasicDBObject("$addToSet","$fid"));
                BasicDBObject match = new BasicDBObject().append("sumFile",1).append("itemsFileId",fDb.getId());
                aggList.add(new BasicDBObject("$group", group));
                aggList.add(new BasicDBObject("$match", match));

                // Get count to batch
                List listCount = ((List) ((ArrayList) aggList).clone());
                BasicDBObject groupCount = new BasicDBObject().append("_id", null).append("listCount", new BasicDBObject("$sum", 1));
                listCount.add(new BasicDBObject("$group", groupCount));

                Cursor listCountResult=  datastore.getCollection(FileVariant.class).aggregate(listCount, options);
                if (listCountResult  != null &&  listCountResult.hasNext()){
                    BasicDBObject obj = (BasicDBObject)  listCountResult.next();
                    numVariants = (int) obj.get("listCount");
                }
                // Batch variants
                long pages = numVariants/limit;
                BasicDBObject groupProject = new BasicDBObject().append("_id", null).append("listVariants", new BasicDBObject("$push", "$_id"));
                for ( int skip = 0 ; skip <= pages; skip++) {
                    List aggListSkip = ((List) ((ArrayList) aggList).clone());
                    aggListSkip.add(new BasicDBObject("$limit",  skip + limit));
                    aggListSkip.add(new BasicDBObject("$skip", skip));
                    aggListSkip.add(new BasicDBObject("$group", groupProject));
                    Cursor fileVariant = datastore.getCollection(FileVariant.class).aggregate(aggListSkip, options);

                    if (fileVariant != null && fileVariant.hasNext()){
                        BasicDBObject oObj = (BasicDBObject) fileVariant.next();
                        variantObjId = (ArrayList) oObj.get("listVariants");
                    }

                    // Get variants file
                    if (!variantObjId.isEmpty()) {
                        Query<Variant> queryVariant = datastore.createQuery(Variant.class).field("_id").in(variantObjId);

                        System.out.println("Skip: " + skip + " Variants file = " + variantObjId.size());
                        numVAr = recalc_regions(numVAr, qm, diseases, technologies, queryVariant, calculateSampleRegions, datastore);
                    }
                }

            }
        }
    }

    /**
     *  Recalculate only a group of regions
     * @param numVAr Counter: num variants procesed
     * @param qm
     * @param diseases
     * @param technologies
     * @param queryVariant
     * @param calculateSampleRegions  Map with num samples in the file (only count files with panels)
     * @param datastore
     * @return
     */
    private static long recalc_regions(long numVAr, CSVSQueryManager qm, List<Integer> diseases , List<Integer> technologies,  Query<Variant> queryVariant, Map<String, Map> calculateSampleRegions,
                                      Datastore datastore){
        System.out.println("Start REGIONS");
        int limit = 1000;

        if (queryVariant.countAll() > 0) {

            List<String> distChrom = datastore.getCollection(Variant.class).distinct("c");

            // Search by chromosome
            if (distChrom.size() > 0) {
                for (String c: distChrom) {
                    Query<Variant> query = queryVariant.cloneQuery();
                    // Variant order chromosome and position
                    Iterator<Variant> it = query.field("c").equal(c).order("p").batchSize(limit).iterator();
                    // Get cursor of start regions ordered and cursor of end regions ordered
                    Iterator itStart = datastore.getCollection(Region.class).find(new BasicDBObject("c",c)).sort(new BasicDBObject("s",1)).iterator();
                    Iterator itEnd = datastore.getCollection(Region.class).find(new BasicDBObject("c",c)).sort(new BasicDBObject("e",1)).iterator();

                    // Initialize
                    Map cursor = new HashMap();
                    BasicDBObject cursorStart = null, cursorEnd = null;
                    if (itStart.hasNext())
                       cursorStart = (BasicDBObject) itStart.next();
                    if(itEnd.hasNext())
                        cursorEnd = (BasicDBObject) itEnd.next();

                    while (it != null && it.hasNext()) {
                        List<Variant> batch = new ArrayList<>();

                        for (int i = 0; i < limit && it.hasNext(); i++) {
                            Variant v = it.next();
                            numVAr++;
                            for (int d : diseases) {
                                for (int t : technologies) {
                                    if (calculateSampleRegions.containsKey(d+"_"+t)) {
                                        // Add
                                        while (cursorStart != null && (int)cursorStart.get("s") <= v.getPosition() ) {
                                            if (calculateSampleRegions.get(d + "_" + t).containsKey(cursorStart.get("pid"))) {
                                                Map reg_pid = new HashMap<>();
                                                reg_pid.put(cursorStart.get("pid"), calculateSampleRegions.get(d + "_" + t).get(cursorStart.get("pid")));
                                                cursor.put(cursorStart.get("_id"), reg_pid);
                                            }
                                            if (itStart.hasNext())
                                                cursorStart = (BasicDBObject) itStart.next();
                                            else
                                                cursorStart = null;
                                        }
                                        // Remove
                                        while (cursorEnd != null && v.getPosition() > (int) cursorEnd.get("e") ) {
                                            if (cursor.containsKey(cursorEnd.get("_id")))
                                                cursor.remove(cursorEnd.get("_id"));
                                            if (itEnd.hasNext())
                                                cursorEnd = (BasicDBObject) itEnd.next();
                                            else
                                                cursorEnd = null;
                                        }

                                        // CalculateSampleCount
                                        int sumSampleRegion = 0;
                                        if (!cursor.isEmpty()) {
                                            // Sum only no overlapping regions
                                            sumSampleRegion = 0;
                                            List panel_procesed = new ArrayList();
                                            for (Object key: cursor.keySet()){
                                                Map<String, Integer> reg_pid = (Map<String, Integer>) cursor.get(key);
                                                Object pid = reg_pid.keySet().stream().findFirst().get();
                                                if (!panel_procesed.contains(pid)){
                                                    sumSampleRegion+=reg_pid.get(pid);
                                                    panel_procesed.add(pid);
                                                }
                                            }
                                        }

                                        v.setSumSampleRegion(new DiseaseSum(d, t, sumSampleRegion));
                                        if (sumSampleRegion != 0){
                                            System.out.println("VARIANTE: " + v.getChromosome() + ":" + v.getPosition() + "  Samples: " + sumSampleRegion);
                                        }
                                    }
                                }
                            }
                            batch.add(v);
                        }

                        datastore.save(batch);
                        batch.clear();
                        System.out.println("Total: " + numVAr + "      " + new Date());
                    }
                    System.out.println("END  =" + new Date());
                }
            }

        }

        return numVAr;
    }

    /**
     * Delete samples from panels unloads
     * @param diseases
     * @param technologies
     * @param datastore
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void recalculateCheckUnload(List<Integer> diseases , List<Integer> technologies, Datastore datastore) throws IOException, NoSuchAlgorithmException {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);
        Map<String, Map> calculateSampleRegions = qm.calculateSampleRegions(datastore);
        for (int d : diseases) {
            for (int t : technologies) {
                if (!calculateSampleRegions.containsKey(d + "_" + t)) {
                    BasicDBObject query = new BasicDBObject().append("dspanel", new BasicDBObject("$exists", true))
                                                              .append("dspanel", new BasicDBObject("$elemMatch", new BasicDBObject().append("dgid", d).append("tid", t)));
                    BasicDBObject pull = new BasicDBObject("$pull", new BasicDBObject("dspanel", new BasicDBObject().append("dgid",d).append("tid", t)));
                    WriteResult update = datastore.getCollection(Variant.class).updateMulti(query, pull);

                    System.out.println("Num variants remove panels d= "  +d + " t= " +  t + "  : " +update.getN());
                }
            }
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


    private static String removePrefix(String chr) {
        String res = chr.replace("chrom", "").replace("chrm", "").replace("chr", "").replace("ch", "");
        return res;
    }

    public static void annot(boolean ct, boolean remove, boolean override, boolean gene, Datastore datastore, String host, String version) throws IOException {
        CellBaseAnnotator cba = new CellBaseAnnotator(host, version);

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

    /**
     * Filter file variant: Remove CHR and check variant in the panel.
     * @param variantsPath
     * @param panelFilePath
     * @param datastore
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void filterFile(Path variantsPath, Path panelFilePath, Datastore datastore  ) throws IOException, NoSuchAlgorithmException {
        if (panelFilePath != null){
            Panel p = loadPanel(datastore, panelFilePath);
            List<Region> regions = new ArrayList<>();
            if(p != null)
                regions = datastore.createQuery(Region.class).field("pid").equal((ObjectId) p.getId()).asList();

            DataReader<Variant> readerFilter = new CSVSVariantFilterCSVDataReader(variantsPath.toAbsolutePath().toString(), p, regions);
            List<DataWriter<Variant>> writersRegions = new ArrayList<>();

            Runner<Variant> pvsRunner = new CSVSRunner(readerFilter, writersRegions,  new SortedList<>(), 100);
            System.out.println("Filter variant...");
            pvsRunner.run();
            System.out.println("Variant Filter!.");
            System.out.println("Generated file: " + "Filter_" + variantsPath.getFileName() );

            // Delete panel and regions if not used
            long numUsed = datastore.createQuery(File.class).field("pid").equal(p.getId()).countAll();
            if (numUsed == 0 ) {
                for (Region r : regions)
                    datastore.delete(r);
                datastore.delete(p);
            }
        }

    }

}
