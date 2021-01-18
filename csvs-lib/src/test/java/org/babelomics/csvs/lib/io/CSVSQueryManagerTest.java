package org.babelomics.csvs.lib.io;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.CSVSUtil;
import org.babelomics.csvs.lib.config.CSVSConfiguration;
import org.babelomics.csvs.lib.models.*;
import org.junit.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.opencb.biodata.models.feature.Region;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSQueryManagerTest {
    static Datastore datastore;
    static MongoClient mongoclient;
    static CSVSQueryManager qm;
    static int port = 12345;
    static MongodExecutable mongodExecutable;
    static String dbName = "csvs-query-manager-test-db2";


    @BeforeClass
    public static void setDB() throws URISyntaxException, IOException, NoSuchAlgorithmException {

        MongodStarter starter = MongodStarter.getDefaultInstance().getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = starter.prepare(mongodConfig);
        MongodProcess mongod = mongodExecutable.start();


        Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.csvs.lib.models");
        mongoclient = new MongoClient("localhost", 27017);
        mongoclient.dropDatabase(dbName);

        datastore = morphia.createDatastore(mongoclient, dbName);
        datastore.ensureIndexes();

        qm = new CSVSQueryManager(datastore);
        new CSVSConfiguration();
        CSVSConfiguration configuration = CSVSConfiguration.load("configuration.json");
        CSVSUtil.populateDiseases(datastore, configuration);
        CSVSUtil.populateTechnologies(datastore, configuration);

        // Load files
        URL url_d1_t1 = CSVSQueryManagerTest.class.getClassLoader().getResource("d1_t1.csv");
        File file_d1_t1 = new File(url_d1_t1.toURI());
        CSVSUtil.loadVariants(file_d1_t1.toPath(), 1, 1, datastore);


        URL url_d2_t1 = CSVSQueryManagerTest.class.getClassLoader().getResource("d2_t1.csv");
        File file_d2_t1 = new File(url_d2_t1.toURI());
        CSVSUtil.loadVariants(file_d2_t1.toPath(), 2, 1, datastore);

        // Load panel
        URL url_d2_t2 = CSVSQueryManagerTest.class.getClassLoader().getResource("d2_t2.csv");
        File file_d2_t2 = new File(url_d2_t2.toURI());
        URL url_fileRegions = CSVSQueryManagerTest.class.getClassLoader().getResource("TruSight_One_v1.1.bed");
        File file_regions = new File(url_fileRegions.toURI());
        CSVSUtil.loadVariants(file_d2_t2.toPath(), 2, 2, datastore, file_regions.toPath(), "Nombre Apellido1 Apellido2", true, "XX");

        // Recalculate panel
        List<Integer> diseases = new ArrayList<>();
        diseases.add(2);
        List<Integer> technologies = new ArrayList<>();
        technologies.add(2);
        CSVSUtil.recalculate(diseases, technologies, "TruSight_One_v1.1.bed", datastore);

        // Load panel
        URL url_d1_t2 = CSVSQueryManagerTest.class.getClassLoader().getResource("d1_t2.csv");
        File file_d1_t2 = new File(url_d1_t2.toURI());
        CSVSUtil.loadVariants(file_d1_t2.toPath(), 1, 2, datastore, file_regions.toPath(), "Nombre Apellido1 Apellido2", true, "XY");

        // Recalculate panel
        diseases = new ArrayList<>();
        diseases.add(1);
        CSVSUtil.recalculate(diseases, technologies, "TruSight_One_v1.1.bed",  datastore);

        // Unload panel
        CSVSUtil.unloadVariants(file_d1_t2.toPath(), 1, 2,  datastore);


        // Load file
        CSVSUtil.loadVariants(file_d1_t2.toPath(), 1, 2, datastore);
        diseases = new ArrayList<>();
        diseases.add(1);

        // Recalculate all
        CSVSUtil.recalculate(diseases, technologies,  datastore);



        // Panel with Gender XX and XY
        // Load files
        URL url_d3_t1_XX = CSVSQueryManagerTest.class.getClassLoader().getResource("d3_t1_XX.csv");
        File file_d3_t1_XX = new File(url_d3_t1_XX.toURI());
        URL url_fileRegionsGender = CSVSQueryManagerTest.class.getClassLoader().getResource("Gender.bed");
        File file_regionsGender = new File(url_fileRegionsGender.toURI());
        CSVSUtil.loadVariants(file_d3_t1_XX.toPath(), 3, 1, datastore, file_regionsGender.toPath(), "Panel with gender XX", true, "XX");
        URL url_d3_t1_XY = CSVSQueryManagerTest.class.getClassLoader().getResource("d3_t1_XY.csv");
        File file_d3_t1_XY = new File(url_d3_t1_XY.toURI());
        CSVSUtil.loadVariants(file_d3_t1_XY.toPath(), 3, 1, datastore, file_regionsGender.toPath(), "Panel with gender XY", true, "XY");


        url_d3_t1_XY = CSVSQueryManagerTest.class.getClassLoader().getResource("d3_t1_XY_2.csv");
        file_d3_t1_XY = new File(url_d3_t1_XY.toURI());
        url_fileRegionsGender = CSVSQueryManagerTest.class.getClassLoader().getResource("TruSight_One_v1.1.bed");
        file_regionsGender = new File(url_fileRegionsGender.toURI());
        CSVSUtil.loadVariants(file_d3_t1_XY.toPath(), 3, 1, datastore, file_regionsGender.toPath(), "Panel with gender XY 2", true, "XY");


        // Recalculate all
        diseases = new ArrayList<>();
        diseases.add(3);
        technologies = new ArrayList<>();
        technologies.add(1);
        CSVSUtil.recalculate(diseases, technologies,  datastore);
    }

    @AfterClass
    public static void closeDB() {
//        mongoclient.dropDatabase(dbName);
        mongodExecutable.stop();
    }


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void checkDiseaseGroupSize() {

        DiseaseGroup d1 = qm.getDiseaseById(1);
        assertEquals(d1.getSamples(), 7);
        assertEquals(d1.getVariants(), 3);

        DiseaseGroup d2 = qm.getDiseaseById(2);
        assertEquals(d2.getSamples(), 14);
        assertEquals(d2.getVariants(), 6);

        Technology t1 = qm.getTechnologyById(1);
        //assertEquals(t1.getSamples(), 8);
        //assertEquals(t1.getVariants(), 6);
        // add Gender test
        assertEquals(t1.getSamples(), 11);
        assertEquals(t1.getVariants(), 14);

    }

    @Test
    public void testGetAllDiseaseGroups() throws Exception {

        List<DiseaseGroup> list = qm.getAllDiseaseGroups();

        assertEquals(list.size(), 48);

    }

    @Test
    public void testGetAllTechnologies() throws Exception {

        List<Technology> list = qm.getAllTechnologies();
        assertEquals(list.size(), 5);

    }

    @Test
    public void testGetAllDiseaseGroupsOrderedBySample() throws Exception {

        List<DiseaseGroup> list = qm.getAllDiseaseGroupsOrderedBySample();

        DiseaseGroup dg = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            assertTrue(dg.getSamples() >= list.get(i).getSamples());
            dg = list.get(i);
        }
    }

    @Test
    public void testGetMaxDiseaseId() throws Exception {

        int maxId = qm.getMaxDiseaseId();

        for (DiseaseGroup dg : qm.getAllDiseaseGroups()) {
            assertTrue(maxId >= dg.getGroupId());
        }

    }

    @Test
    public void testGetMaxTechnologyId() throws Exception {

        int maxId = qm.getMaxTechnologyId();

        for (Technology dg : qm.getAllTechnologies()) {
            assertTrue(maxId >= dg.getTechnologyId());
        }

    }

    @Test
    public void testGetVariantsByRegionList() throws Exception {

        Region r = new Region("1", 1, 1);
        List<Region> list = new ArrayList<>();
        list.add(r);

        List<Integer> d1 = Arrays.asList(1);
        List<Integer> d2 = Arrays.asList(2);
        List<Integer> t1 = Arrays.asList(1);
        List<Integer> t2 = Arrays.asList(2);


        List<Variant> variants = Lists.newArrayList(qm.getVariantsByRegionList(list, d1, t1, 0, 1, false, new MutableLong(-1)));

        assertEquals(variants.size(), 1);

        Variant v = variants.get(0);

        DiseaseCount dc1 = v.getStats();
        System.out.println("CSVS (testGetVariantsByRegionList): dc1 = " + dc1);
        assertEquals(dc1.getGt00(), 2);
        assertEquals(dc1.getGt01(), 2);
        assertEquals(dc1.getGt11(), 0);
        assertEquals(dc1.getGtmissing(), 0);


        variants = Lists.newArrayList(qm.getVariantsByRegionList(list, new ArrayList<>(), t1, 0, 1, false, new MutableLong(-1)));

        assertEquals(variants.size(), 1);

        v = variants.get(0);

        DiseaseCount dc2 = v.getStats();
        System.out.println("\"CSVS (testGetVariantsByRegionList): dc2 =" + dc2);
        assertEquals(dc2.getGt00(), 4);
        assertEquals(dc2.getGt01(), 4);
        assertEquals(dc2.getGt11(), 0);
        assertEquals(dc2.getGtmissing(), 0);

    }


    @Test
    public void testGetVariantsByRegionListGender() throws Exception {


        List<Region> list = new ArrayList<>();
        list.add(new Region("X", 1, 10));
        list.add(new Region("Y", 1, 10));

        List<Integer> d3= Arrays.asList(3);
        List<Integer> t1 = Arrays.asList(1);


        List<Variant> variants = Lists.newArrayList(qm.getVariantsByRegionList(list, d3, t1, 0, 100, false, new MutableLong(-1)));

        //assertEquals(variants.size(), 1);
        System.out.println("CSVS (testGetVariantsByRegionListGender): variants.size() = " + variants.size());
        for(Variant v: variants) {
            DiseaseCount dc1 = v.getStats();
            System.out.println("CSVS (testGetVariantsByRegionListGender): dc1 = " + dc1 + "  Var:" + v.getChromosome() + ":" + v.getPosition()+ ":" + v.getReference() + ":" + v.getAlternate());
        }
       // assertEquals(dc1.getGt00(), 2);
        //assertEquals(dc1.getGt01(), 2);
        //assertEquals(dc1.getGt11(), 0);
        //assertEquals(dc1.getGtmissing(), 0);
    }

    @Test
    public void testGetVariant() throws Exception {
        List<Integer> d1 = Arrays.asList(1);
        List<Integer> d2 = Arrays.asList(2);
        List<Integer> t1 = Arrays.asList(1);
        List<Integer> t2 = Arrays.asList(2);

        Variant v1 = qm.getVariant("1", 1, "A", "C", new ArrayList<>(), new ArrayList<>());

        DiseaseCount dc1 = v1.getStats();
        System.out.println("CSVS (testGetVariant): dc1 = " + dc1);
        assertEquals(dc1.getGt00(), 7);
        assertEquals(dc1.getGt01(), 4);
        assertEquals(dc1.getGt11(), 0);
        assertEquals(dc1.getGtmissing(), 0);


        Variant v2 = qm.getVariant("1", 1, "A", "C", d1, new ArrayList<>());

        DiseaseCount dc2 = v2.getStats();
        System.out.println("CSVS (testGetVariant): dc2 = " + dc2);
        assertEquals(dc2.getGt00(), 5);
        assertEquals(dc2.getGt01(), 2);
        assertEquals(dc2.getGt11(), 0);
        assertEquals(dc2.getGtmissing(), 0);


        Variant v3 = qm.getVariant("1", 1, "A", "C", d2, t1);
        DiseaseCount dc3 = v3.getStats();
        System.out.println("CSVS (testGetVariant): dc3 = " + dc3);
        assertEquals(dc3.getGt00(), 2);
        assertEquals(dc3.getGt01(), 2);
        assertEquals(dc3.getGt11(), 0);
        assertEquals(dc3.getGtmissing(), 0);

        Variant v4 = qm.getVariant("1", 3, "A", "C", d1, t1);
        assertNull(v4);

        Variant v5 = qm.getVariant("1", 2, "A", "C", new ArrayList<>(), t1);
        DiseaseCount dc5 = v5.getStats();
        System.out.println("CSVS (testGetVariant): dc5 = " + dc5);
        assertEquals(dc5.getGt00(), 1);
        assertEquals(dc5.getGt01(), 4);
        assertEquals(dc5.getGt11(), 2);
        assertEquals(dc5.getGtmissing(), 1);

        Variant v6 = qm.getVariant("1", 2, "A", "C", d2, t2);
        assertNull(v6);

    }

    @Test
    public void testGetVariant1() throws Exception {

    }

    @Test
    public void testGetVariants() throws Exception {

    }

    @Test
    public void testGetAllIntervalFrequencies() throws Exception {

    }

    @Test
    public void testGetIntervalFrequencies() throws Exception {

    }

    @Test
    public void testGetVariantsByRegionList1() throws Exception {

        Region r = new Region("1", 1, 2);
        List<Region> list = new ArrayList<>();
        list.add(r);

        List<List<Variant>> variants = qm.getVariantsByRegionList(list);

        assertEquals(variants.size(), 1);

        List<Variant> variantList = variants.get(0);

        assertEquals(variantList.size(), 2);

        Variant v1 = variantList.get(0);
        Variant v2 = variantList.get(1);

        assertEquals(v1.getPosition(), 1);
        assertEquals(v1.getChromosome(), "1");
        assertEquals(v1.getReference(), "A");
        assertEquals(v1.getAlternate(), "C");

        assertEquals(v2.getChromosome(), "1");
        assertEquals(v2.getPosition(), 2);
        assertEquals(v2.getReference(), "A");
        assertEquals(v2.getAlternate(), "C");


    }

    @Test
    public void testGetSaturation() throws Exception {

        List<Region> regions = new ArrayList<>();
        regions.add(new Region("1:1-50"));
        List<Integer> diseases = new ArrayList<>();
//        diseases.add(1);
        diseases.add(2);

        List<Integer> tech = new ArrayList<>();
        tech.add(1);
//        tech.add(2);
//

        Map<Region, List<SaturationElement>> res = qm.getSaturationOrderIncrement(regions, diseases, tech);
//        System.out.println(res);

        for (Map.Entry<Region, List<SaturationElement>> entry : res.entrySet()) {
            System.out.println(entry.getKey());
            List<SaturationElement> value = entry.getValue();
            for (SaturationElement se : value) {
                System.out.println("\t" + se);
            }
        }

    }

    @Test
    public void testGetAllVariants() throws Exception {

        MutableLong count = new MutableLong(-1);
        List<Variant> list1 = Lists.newArrayList(qm.getAllVariants(new ArrayList<>(), new ArrayList<>(), 0, 100, count));
        assertEquals(list1.size(), 15);

        List<Variant> list2 = Lists.newArrayList(qm.getAllVariants(new ArrayList<>(), new ArrayList<>(), 0, 3, count));
        assertEquals(list2.size(), 3);

    }

    @Test
    public void testGetChunkId() throws Exception {

    }


    @Test
    public void testsaveOpinion() throws Exception {
        Variant v1 = qm.getVariant("1", 1, "A", "C", new ArrayList<>(), new ArrayList<>());
        Opinion op1 = new Opinion();
        op1.setVariant(v1);
        op1.setName("Name person");
        op1.setInstitution("Name institucion");
        op1.setEvidence("Evidence");
        op1.setType(Opinion.LIKELY_PATHOGENIC);
        op1.setCreated(new Date());

        op1 = qm.saveOpinion(op1,Opinion.PENDING);
        assertEquals(op1.getState(), 0);

        Variant v2 = qm.getVariant("1", 1, "A", "C", new ArrayList<>(), new ArrayList<>());
        Opinion op2 = new Opinion();
        op2.setVariant(v2);
        op2.setName("Name person");
        op2.setInstitution("Name institucion");
        op2.setEvidence("Evidence");
        op2.setType(Opinion.PATHOGENIC);
        op2.setCreated(new Date());
        qm.saveOpinion(op2, Opinion.PENDING);
        assertEquals(op2.getState(), 0);

        qm.saveOpinion(op1, Opinion.PUBLISHED);
        assertEquals(op1.getState(), 1);

        Opinion op3 = new Opinion();
        op3.setVariant(v1);
        op3.setName("Name person");
        op3.setInstitution("Name institucion");
        op3.setEvidence("Evidence");
        op3.setType(Opinion.BENING);
        op3.setCreated(new Date());
        qm.saveOpinion(op3, Opinion.REJECTED);
        assertEquals(op3.getState(), 2);
        qm.saveOpinion(op3, Opinion.PUBLISHED);

        Opinion op4 = new Opinion("Name person","Name institucion","Evidence",Opinion.LIKELY_PATHOGENIC);
        op4.setVariant(v1);
        qm.saveOpinion(op4, Opinion.PUBLISHED);
    }

    @Test
    public void testGetVariantsPathopedia() throws Exception{

        List<Variant> variants = new ArrayList<>();
        variants.add(new Variant("1",1,"A","C"));
        variants.add(new Variant("1",2,"A","C"));
        variants.add(new Variant("1",3,"A","C"));
        variants.add(new Variant("1",4,"G","A"));
        variants.add(new Variant("1",5,"C","T"));

        List<Integer> states = new ArrayList<>();
        states.add(Opinion.PUBLISHED);

        List<Pathology> pathologies = qm.getVariantsPathopedia(variants, states);
        System.out.println("List pathologies =  " + pathologies.size());

        if (pathologies != null){
            for(Pathology p: pathologies){
                System.out.println(p);
            }
        }
    }

    @Test
    public void testGetAllOpinion()throws Exception {

        Variant v1 = new Variant("1",1,"A","C");
        List<Integer> states = new ArrayList<>();
        states.add(Opinion.PUBLISHED);


        List<Opinion> listOpinions = qm.getAllOpinion(v1, states, "c", null, null, null);
        if (listOpinions != null){
            for(Opinion op: listOpinions){
                System.out.println(op);
            }
        }

    }
}