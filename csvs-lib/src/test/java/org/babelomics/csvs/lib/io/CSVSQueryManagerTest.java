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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    static String dbName = "gpvs-query-manager-test-db_2";


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


        URL url_fileRegions = CSVSQueryManagerTest.class.getClassLoader().getResource("xgen-exome-research-panel-targets.processed.bed");
        File file_regions = new File(url_fileRegions.toURI());


        URL url_4 = CSVSQueryManagerTest.class.getClassLoader().getResource("GPrueba.csv");
        File file_4 = new File(url_4.toURI());
        CSVSUtil.loadVariants(file_4.toPath(), 49, 1, datastore, null, "Person", true, "XY");

/*
        URL url_1= CSVSQueryManagerTest.class.getClassLoader().getResource("d23_t1.csv");
        File file_1 = new File(url_1.toURI());
        //CSVSUtil.loadVariants(file_d1_t1.toPath(), 23, 1, datastore);
        CSVSUtil.loadVariants(file_1.toPath(), 23, 1, datastore, file_regions.toPath(), "Nombre Apellido1 Apellido2", true, "XX");

        URL url_2 = CSVSQueryManagerTest.class.getClassLoader().getResource("d23_t1-2.csv");
        File file_2 = new File(url_2.toURI());
        CSVSUtil.loadVariants(file_2.toPath(), 23, 1, datastore, file_regions.toPath(), "Nombre Apellido1 Apellido2", true, "XX");


        // Recalculate panel
        List<Integer> diseases = new ArrayList<>();
        diseases.add(23);
        List<Integer> technologies = new ArrayList<>();
        technologies.add(1);
        CSVSUtil.recalculate(diseases, technologies, "xgen-exome-research-panel-targets.processed.bed", datastore);


        URL url_3 = CSVSQueryManagerTest.class.getClassLoader().getResource("d23_t1-3.csv");
        File file_3 = new File(url_3.toURI());
        CSVSUtil.loadVariants(file_3.toPath(), 23, 1, datastore, null, "Person", true, "XY");
*/
        //CSVSUtil.unloadVariants(file_1.toPath(), 23, 1, datastore);
        //CSVSUtil.unloadVariants(file_3.toPath(), 23, 1, datastore);
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
    /*

    @Test
    public void checkDiseaseGroupSize() {

        DiseaseGroup d1 = qm.getDiseaseById(1);
        assertEquals(d1.getSamples(), 7);
        assertEquals(d1.getVariants(), 3);

        DiseaseGroup d2 = qm.getDiseaseById(2);
        assertEquals(d2.getSamples(), 14);
        assertEquals(d2.getVariants(), 6);

        Technology t1 = qm.getTechnologyById(1);
        assertEquals(t1.getSamples(), 8);
        assertEquals(t1.getVariants(), 6);

    }

    @Test
    public void testGetAllDiseaseGroups() throws Exception {

        List<DiseaseGroup> list = qm.getAllDiseaseGroups();

        assertEquals(list.size(), 26);

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
    */

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
/*
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

        Map<Region, List<SaturationElement>> res = qm.getSaturation(regions, diseases, tech);
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
        assertEquals(list1.size(), 7);

        List<Variant> list2 = Lists.newArrayList(qm.getAllVariants(new ArrayList<>(), new ArrayList<>(), 0, 3, count));
        assertEquals(list2.size(), 3);

    }
*/
    @Test
    public void testGetChunkId() throws Exception {

    }


}