package org.babelomics.exomeserver.lib.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantFactory;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.VariantVcfFactory;
import org.opencb.biodata.models.variant.stats.VariantStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExomeServerVariantUpdateStatsTaskTest {

    private VariantSource source = new VariantSource("filename.vcf", "fileId", "studyId", "studyName");
    private VariantFactory factory = new VariantVcfFactory();


    private Variant dbVariant;
    private Variant newVariant;
    private VariantStats dbStats;
    private VariantStats newStats;

    @Before
    public void setUp() throws Exception {
        List<String> sampleNames = Arrays.asList("NA001", "NA002", "NA003", "NA004");
        source.setSamples(sampleNames);


    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void test1() throws Exception {

        String line1 = "1\t14653\t.\tC\tT\t87.77\tPASS\t.\tGT:AD:DP:GQ:PL\t0/0:.:25:.:.\t1/1:9,3:12:63:63,0,279\t0/1:3,6:9:76:169,0,76\t0/0:.:23:.:.";
        String line2 = "1\t14653\t.\tC\tT\t87.77\tPASS\t.\tGT:AD:DP:GQ:PL\t0/0:.:25:.:.\t1/1:9,3:12:63:63,0,279\t0/1:3,6:9:76:169,0,76\t0/1:.:23:.:.";

        dbVariant = factory.create(source, line1).get(0);
        newVariant = factory.create(source, line2).get(0);
        List<Variant> list = new ArrayList<>();
        list.add(dbVariant);
        list.add(newVariant);
        VariantStats.calculateStatsForVariantsList(list, null);

        dbStats = dbVariant.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();
        newStats = newVariant.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();

        System.out.println("DB MAF = " + dbStats.getMaf());
        System.out.println("DB MAF All = " + dbStats.getMafAllele());
        System.out.println("DB Gen. Count = " + dbStats.getGenotypesCount());

        assertEquals(dbStats.getMaf(), 0.375, 0);
        assertEquals(dbStats.getMafAllele(), "T");


        ExomeServerVariantUpdateStatsTask task = new ExomeServerVariantUpdateStatsTask(source);

        task.updateStats(newStats, dbStats);

        System.out.println("=========================================");

        System.out.println("NEW MAF = " + newStats.getMaf());
        System.out.println("NEW MAF All = " + newStats.getMafAllele());
        System.out.println("NEW Gen. Count = " + newStats.getGenotypesCount());


        System.out.println("==========================================");
        System.out.println("DB MAF = " + dbStats.getMaf());
        System.out.println("DB MAF All = " + dbStats.getMafAllele());
        System.out.println("DB Gen. Count = " + dbStats.getGenotypesCount());

        assertEquals(dbStats.getMaf(), 0.4375, 0);
        assertEquals(dbStats.getMafAllele(), "T");


    }

    @Test
    public void test2() {
        String line1 = "1\t14653\t.\tC\tT\t87.77\tPASS\t.\tGT:AD:DP:GQ:PL\t0/0:.:25:.:.\t1/1:9,3:12:63:63,0,279\t0/1:3,6:9:76:169,0,76\t0/0:.:23:.:.";
        String line2 = "1\t14653\t.\tC\tT\t87.77\tPASS\t.\tGT:AD:DP:GQ:PL\t1/1:.:25:.:.\t1/1:9,3:12:63:63,0,279\t1/1:3,6:9:76:169,0,76\t0/1:.:23:.:.";

        dbVariant = factory.create(source, line1).get(0);
        newVariant = factory.create(source, line2).get(0);
        List<Variant> list = new ArrayList<>();
        list.add(dbVariant);
        list.add(newVariant);
        VariantStats.calculateStatsForVariantsList(list, null);

        dbStats = dbVariant.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();
        newStats = newVariant.getSourceEntry(source.getFileId(), source.getStudyId()).getStats();

        System.out.println("DB MAF = " + dbStats.getMaf());
        System.out.println("DB MAF All = " + dbStats.getMafAllele());
        System.out.println("DB Gen. Count = " + dbStats.getGenotypesCount());

        assertEquals(dbStats.getMaf(), 0.375, 0);
        assertEquals(dbStats.getMafAllele(), "T");


        ExomeServerVariantUpdateStatsTask task = new ExomeServerVariantUpdateStatsTask(source);

        task.updateStats(newStats, dbStats);

        System.out.println("=========================================");

        System.out.println("NEW MAF = " + newStats.getMaf());
        System.out.println("NEW MAF All = " + newStats.getMafAllele());
        System.out.println("NEW Gen. Count = " + newStats.getGenotypesCount());


        System.out.println("==========================================");
        System.out.println("DB MAF = " + dbStats.getMaf());
        System.out.println("DB MAF All = " + dbStats.getMafAllele());
        System.out.println("DB Gen. Count = " + dbStats.getGenotypesCount());

        assertEquals(dbStats.getMaf(), 0.375, 0);
        assertEquals(dbStats.getMafAllele(), "C");

    }


}