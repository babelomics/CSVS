package org.babelomics.pvs.lib.io;

import org.babelomics.pvs.lib.models.DiseaseCount;
import org.babelomics.pvs.lib.models.DiseaseGroup;
import org.babelomics.pvs.lib.models.Variant;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencb.commons.io.DataWriter;

import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSVariantCountsMongoWriter implements DataWriter<Variant> {

    private DiseaseGroup diseaseGroup;
    private Datastore datastore;

    public PVSVariantCountsMongoWriter(DiseaseGroup diseaseGroup, Datastore datastore) {
        this.diseaseGroup = diseaseGroup;
        this.datastore = datastore;
    }

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public boolean close() {

        return true;
    }

    @Override
    public boolean pre() {


        return true;
    }

    @Override
    public boolean post() {
        return true;
    }

    @Override
    public boolean write(Variant elem) {
        Query<Variant> query = datastore.createQuery(Variant.class);

        Variant v = query.field("chromosome").equal(elem.getChromosome())
                .field("position").equal(elem.getPosition()).
                        field("reference").equal(elem.getReference()).
                        field("alternate").equal(elem.getAlternate())
                .get();

        if (v == null) {
            this.datastore.save(elem);
        } else {

            boolean b = false;
            for (DiseaseCount dc : v.getDiseases()) {
                if (dc.getDiseaseGroup().getGroupId() == this.diseaseGroup.getGroupId()) {
                    DiseaseCount aux = elem.getDiseaseCount(this.diseaseGroup);
                    dc.incGt00(aux.getGt00());
                    dc.incGt01(aux.getGt01());
                    dc.incGt11(aux.getGt11());
                    dc.incGtMissing(aux.getGtmissing());
                    this.datastore.save(v);
                    b = true;
                }
            }

            if (!b) {
                DiseaseCount aux = elem.getDiseaseCount(this.diseaseGroup);
                DiseaseCount newDc = new DiseaseCount(aux.getDiseaseGroup(), aux.getGt00(), aux.getGt01(), aux.getGt11(), aux.getGtmissing());
                v.addDiseaseCount(newDc);
                this.datastore.save(v);
            }

        }

        return true;
    }

    @Override
    public boolean write(List<Variant> batch) {

        for (Variant v : batch) {
            this.write(v);
        }
        return true;
    }
}
