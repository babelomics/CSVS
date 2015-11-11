package org.babelomics.pvs.lib.models;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class VariantTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testVariantConstructor() throws Exception {

        Variant v1 = new Variant("1:1:A:C");

        System.out.println("v1 = " + v1);

        assertEquals(v1.getChromosome(), "1");
        assertEquals(v1.getPosition(), 1);
        assertEquals(v1.getReference(), "A");
        assertEquals(v1.getAlternate(), "C");

        Variant v2 = new Variant("1:1::C");
        assertEquals(v2.getChromosome(), "1");
        assertEquals(v2.getPosition(), 1);
        assertEquals(v2.getReference(), "");
        assertEquals(v2.getAlternate(), "C");
        System.out.println("v2 = " + v2);

        Variant v3 = new Variant("1:1:A:");
        assertEquals(v3.getChromosome(), "1");
        assertEquals(v3.getPosition(), 1);
        assertEquals(v3.getReference(), "A");
        assertEquals(v3.getAlternate(), "");
        System.out.println("v3 = " + v3);

        Variant v4 = new Variant("1:1::");
        assertEquals(v4.getChromosome(), "1");
        assertEquals(v4.getPosition(), 1);
        assertEquals(v4.getReference(), "");
        assertEquals(v4.getAlternate(), "");
        System.out.println("v4 = " + v4);


    }

    @Test
    public void testAddGenotypesToDisease() throws Exception {

    }

    @Test
    public void testAddDiseaseCount() throws Exception {

    }

    @Test
    public void testDeleteDiseaseCount() throws Exception {

    }

    @Test
    public void testGetChromosome() throws Exception {

    }

    @Test
    public void testSetChromosome() throws Exception {

    }

    @Test
    public void testGetPosition() throws Exception {

    }

    @Test
    public void testSetPosition() throws Exception {

    }

    @Test
    public void testGetReference() throws Exception {

    }

    @Test
    public void testSetReference() throws Exception {

    }

    @Test
    public void testGetAlternate() throws Exception {

    }

    @Test
    public void testSetAlternate() throws Exception {

    }

    @Test
    public void testGetDiseases() throws Exception {

    }

    @Test
    public void testSetDiseases() throws Exception {

    }

    @Test
    public void testGetDiseaseCount() throws Exception {

    }

    @Test
    public void testGetId() throws Exception {

    }
}