package org.babelomics.csvs.lib.models;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OpinionTest {


    @Test
    public void testOpinionConstructor() throws Exception {

        Variant v1 = new Variant("1:1:A:C");
        Opinion op1 = new Opinion();
        op1.setVariant(v1);
        op1.setName("Name person");
        op1.setInstitution("Name institucion");
        op1.setEvidence("Evidence");
        op1.setType(Opinion.BENING);


        System.out.println("v1 = " + v1.pretty());
        System.out.println("op1 = " + op1);

        assertEquals(op1.getName(), "Name person");
        assertEquals(op1.getInstitution(), "Name institucion");
        assertEquals(op1.getEvidence(), "Evidence");
        assertEquals(op1.getState(),Opinion.PENDING);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getMapStates() throws Exception {
    }

    @Test
    public void getMapType() throws Exception {
    }

    @Test
    public void getId() throws Exception {
    }

    @Test
    public void setId() throws Exception {
    }

    @Test
    public void getVariant() throws Exception {
    }

    @Test
    public void setVariant() throws Exception {
    }

    @Test
    public void getName() throws Exception {
    }

    @Test
    public void setName() throws Exception {
    }

    @Test
    public void getInstitution() throws Exception {
    }

    @Test
    public void setInstitution() throws Exception {
    }

    @Test
    public void getEvidence() throws Exception {
    }

    @Test
    public void setEvidence() throws Exception {
    }

    @Test
    public void getType() throws Exception {
    }

    @Test
    public void setType() throws Exception {
    }

    @Test
    public void getState() throws Exception {
    }

    @Test
    public void setState() throws Exception {
    }

    @Test
    public void getCreated() throws Exception {
    }

    @Test
    public void getTypeDesc() throws Exception {
    }

    @Test
    public void getStateDesc() throws Exception {
    }

}