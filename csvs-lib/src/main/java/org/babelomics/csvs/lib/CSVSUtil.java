package org.babelomics.csvs.lib;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mongodb.*;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.*;
import org.babelomics.csvs.lib.annot.CellBaseAnnotator;
import org.babelomics.csvs.lib.config.CSVSConfiguration;
import org.babelomics.csvs.lib.io.*;
import org.babelomics.csvs.lib.models.*;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


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


    public static void populateDiseases(Datastore datastore, CSVSConfiguration configuration) {
        List<DiseaseGroup> diseaseGroups = configuration.getDiseasesGroups();
        int numLoader = 0;
        // 1-22 reserved for icd10
        if (diseaseGroups != null && !diseaseGroups.isEmpty())
            for (DiseaseGroup dg : diseaseGroups) {
                try {
                    datastore.save(dg);
                    numLoader ++;
                } catch (DuplicateKeyException e) {
                    System.err.println("Duplicated Disease Group: " + dg);
                }
            }
        System.out.println(numLoader + " diseaseGroups loaders!");
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

    public static void populateTechnologies(Datastore datastore, CSVSConfiguration configuration) {
        List<Technology> technologies = configuration.getTechnologies();

        int numLoader = 0;
        if (technologies != null && !technologies.isEmpty())
            for (Technology t : technologies) {
                try {
                    datastore.save(t);
                    numLoader ++;
                } catch (DuplicateKeyException e) {
                    System.err.println("Duplicated Technology: " + t);
                }
            }
        System.out.println(numLoader + " technologies loaders!");
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
            System.exit(0);
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
            System.exit(1);
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
            recalculateUnload(fileDelete, datastore);
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

    /**
     * Recalculate examples of a variant studied in a region after unload.
     * @param file
     * @param datastore
     */
    public static void recalculateUnload (File file, Datastore datastore) {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);

        System.out.println("All variants = "  + datastore.createQuery(Variant.class).countAll());
        System.out.println("INI  =" +  new Date());

        Panel p =  datastore.createQuery(Panel.class).field("_id").equal(file.getIdPanel()).get();
        // Get regions
        // List<Region> regionList =  datastore.createQuery(Region.class).field("pid").equal(file.getIdPanel()).asList();
        if (p != null) {
            long num_regions =  datastore.createQuery(Region.class).field("pid").equal(p.getId()).countAll();
            int limit = 1000;
            long pages = num_regions/limit;
            long numVAr = 0;
            for ( int skip = 0 ; skip <= pages; skip++) {
                Iterator<Region> regions_iterator = datastore.createQuery(Region.class).field("pid").equal(p.getId()).offset(skip*limit).limit(limit).iterator();
                System.out.println("Start REGIONS");
                List<Region> regions = Lists.newArrayList(regions_iterator);
                Query<Variant> query = datastore.createQuery(Variant.class);
                query.or(criteriaSearchVariant(regions, datastore));
                System.out.println("Variants banch in regions = " + skip + ": " +  query.countAll());
                if (query.countAll() > 0) {
                    // Calculate Sample Count
                    int varModify = 0;
                    DiseaseGroup dg = file.getDisease();
                    Technology tg = file.getTechnology();
                    int d = dg.getGroupId();
                    int t = tg.getTechnologyId();

                    List<Integer> diseaseId = new ArrayList<>();
                    diseaseId.add(d);
                    List<Integer> technologyId= new ArrayList<>();
                    technologyId.add(t);

                    Iterator<Variant> it = query.batchSize(100).iterator();
                    while (it.hasNext()) {
                        varModify++;
                        List<Variant> batch = new ArrayList<>();

                        for (int i = 0; i < 100 && it.hasNext(); i++) {
                            Variant v = it.next();

                            // Get sum all file
                            boolean withRegions = false;
                            int sumSampleRegion =  qm.initialCalculateSampleCount(v,d, t, datastore);
                            if(sumSampleRegion  > 0)
                                withRegions = true;

                            v.decSumSampleRegion(file.getSamples(), dg, tg, withRegions);

                            batch.add(v);
                            System.out.println("VARIANTE: " + v.getChromosome() + ":" + v.getPosition() );
                        }

                        datastore.save(batch);
                        System.out.println(varModify);
                        batch.clear();
                        System.out.println("Total: " +  numVAr + "      " +  new Date());
                    }

                    System.out.println("END  =" +  new Date());
                }
            }
        }
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
     * Calculate examples of a variant studied in a region.
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

        // Basch regions  regions.addAll(datastore.createQuery(Region.class).field("pid").equal(p.getId()).asList());
        if (p != null) {
            long num_regions =  datastore.createQuery(Region.class).field("pid").equal(p.getId()).countAll();
            int limit = 1000;
            long pages = num_regions/limit;
            long numVAr = 0;
            for ( int skip = 0 ; skip <= pages; skip++) {
                System.out.println("Skip: " + skip);
                Iterator<Region> regions_iterator = datastore.createQuery(Region.class).field("pid").equal(p.getId()).offset(skip*limit).limit(limit).iterator();
                Query<Variant> query = datastore.createQuery(Variant.class);
                numVAr = recalc_regions(numVAr, qm, diseases, technologies, query, regions_iterator, datastore);
            }
        }
    }

    /**
     * Recalculate only a group of regions
     * @param numVAr
     * @param diseases
     * @param technologies
     * @param query
     * @param regions_iterator
     * @param datastore
     * @return
     */
    private static long recalc_regions(long numVAr, CSVSQueryManager qm, List<Integer> diseases , List<Integer> technologies, Query<Variant> query , Iterator<Region> regions_iterator, Datastore datastore){
        System.out.println("Start REGIONS");
        List<Region> regions = Lists.newArrayList(regions_iterator);

        query.or(criteriaSearchVariant(regions, datastore));
        System.out.println("Variants banch in regions = " + query.countAll());
        if (query.countAll() > 0) {

            // Calculate Sample Count
            Iterator<Variant> it = query.batchSize(100).iterator();
            System.out.println("Start");
            while (it.hasNext()) {
                List<Variant> batch = new ArrayList<>();

                for (int i = 0; i < 100 && it.hasNext(); i++) {
                    Variant v = it.next();
                    numVAr++;
                    for (int d : diseases) {
                        for (int t : technologies) {
                            int sumSampleRegion = qm.initialCalculateSampleCount(v, d, t, datastore);
                            v.setSumSampleRegion(new DiseaseSum(d, t, sumSampleRegion));
                            System.out.println("VARIANTE: " + v.getChromosome() + ":" + v.getPosition() + "  Samples: " + sumSampleRegion);
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
        return numVAr;
    }

    /**
     * Calculate examples of a variant for a new file
     * @param diseaseGroupId
     * @param technologyId
     * @param datastore
     */
    public static void recalculate(Path variantsPath, Integer diseaseGroupId, Integer technologyId, Datastore datastore) throws IOException, NoSuchAlgorithmException {
        CSVSQueryManager qm = new CSVSQueryManager(datastore);

        // Search files with panel
        Query<File> queryFile = datastore.createQuery(File.class);
        List<File> files = queryFile.field("dgid").equal(diseaseGroupId).field("tid").equal(technologyId).field("pid").exists().asList();

        if(files.size() > 0){
            String sha256 = calculateSHA256(variantsPath);

            File fDb = datastore.createQuery(File.class).field("sum").equal(sha256).get();

            if (fDb != null) {
                // Search variant in the file
                Query<FileVariant> querFileVariant = datastore.createQuery(FileVariant.class);
                List<FileVariant> fvquery = querFileVariant.field("fid").equal(fDb.getId()).asList();
                System.out.println("All variants file = " + fvquery.size());
                List<ObjectId> variantObjId = new ArrayList<>();
                for (FileVariant fv : fvquery) {
                    variantObjId.add(fv.getIdVariant());
                }

                // Calculate    List<Region> regions = new ArrayList<>();
                List<ObjectId> panelObjectIds = new ArrayList<>();
                for (File f : files) {
                    panelObjectIds.add(f.getIdPanel());
                }

                if (panelObjectIds != null && panelObjectIds.size() > 0) {
                    List<Integer> diseases = new ArrayList<>();
                    List<Integer> technologies = new ArrayList<>();
                    diseases.add(diseaseGroupId);
                    technologies.add(technologyId);
                    long num_regions = datastore.createQuery(Region.class).field("pid").in(panelObjectIds).countAll();
                    int limit = 1000;
                    long pages = num_regions / limit;
                    long numVAr = 0;
                    for (int skip = 0; skip <= pages; skip++) {
                        System.out.println("Skip: " + skip);
                        Iterator<Region> regions_iterator = datastore.createQuery(Region.class).field("pid").in(panelObjectIds).offset(skip * limit).limit(limit).iterator();
                        Query<Variant> query = datastore.createQuery(Variant.class);
                        query.field("_id").in(variantObjId);
                        numVAr = recalc_regions(numVAr, qm, diseases, technologies, query, regions_iterator, datastore);
                    }
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
