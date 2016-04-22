package org.babelomics.csvs.lib.io;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.babelomics.csvs.lib.CSVSUtil;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Technology;
import org.junit.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSQueryManagerTest {
    static Datastore datastore;
    static MongoClient mongoclient;
    static CSVSQueryManager qm;
    static int port = 12345;
    static MongodExecutable mongodExecutable;


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
        mongoclient = new MongoClient("localhost", port);
//        mongoclient.dropDatabase("csvs-query-manager-test-db");
        datastore = morphia.createDatastore(mongoclient, "csvs-query-manager-test-db");
        datastore.ensureIndexes();

        qm = new CSVSQueryManager(datastore);
        CSVSUtil.populateDiseases(datastore);
        CSVSUtil.populateTechnologies(datastore);

        URL url_d1_t1 = CSVSQueryManagerTest.class.getClassLoader().getResource("d1_t1.csv");
        File file_d1_t1 = new File(url_d1_t1.toURI());
        CSVSUtil.loadVariants(file_d1_t1.toPath(), 1, 1, datastore);

        URL url_d1_t2 = CSVSQueryManagerTest.class.getClassLoader().getResource("d1_t2.csv");
        File file_d1_t2 = new File(url_d1_t2.toURI());
        CSVSUtil.loadVariants(file_d1_t2.toPath(), 1, 2, datastore);

        URL url_d2_t1 = CSVSQueryManagerTest.class.getClassLoader().getResource("d2_t1.csv");
        File file_d2_t1 = new File(url_d2_t1.toURI());
        CSVSUtil.loadVariants(file_d2_t1.toPath(), 2, 1, datastore);
    }

    @AfterClass
    public static void closeDB() {
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
        assertEquals(d2.getSamples(), 4);
        assertEquals(d2.getVariants(), 6);

        Technology t1 = qm.getTechnologyById(1);
        assertEquals(t1.getSamples(), 8);
        assertEquals(t1.getVariants(), 6);

    }

    @Test
    public void testGetAllDiseaseGroups() throws Exception {

        List<DiseaseGroup> list = qm.getAllDiseaseGroups();

    }

    @Test
    public void testGetAllTechnologies() throws Exception {

        List<Technology> list = qm.getAllTechnologies();

    }

    @Test
    public void testGetAllDiseaseGroupsOrderedBySample() throws Exception {


    }

    @Test
    public void testGetMaxDiseaseId() throws Exception {

    }

    @Test
    public void testGetMaxTechnologyId() throws Exception {

    }

    @Test
    public void testGetVariantsByRegionList() throws Exception {

    }

    @Test
    public void testGetVariant() throws Exception {

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

    }

    @Test
    public void testGetSaturation() throws Exception {

    }

    @Test
    public void testGetAllVariants() throws Exception {

    }

    @Test
    public void testGetChunkId() throws Exception {

    }
}