package org.babelomics.csvs.lib.io;

import org.babelomics.csvs.lib.models.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.opencb.commons.io.DataWriter;

import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSVariantCountsMongoWriter implements DataWriter<Variant> {

    private DiseaseGroup diseaseGroup;
    private Technology technology;
    private Datastore datastore;
    private File file;
    private int samples;
    private int variantsT;
    private int variantsD;

    public static final int CHUNK_SIZE_SMALL = 1000;
    public static final int CHUNK_SIZE_BIG = 10000;

    public CSVSVariantCountsMongoWriter(DiseaseGroup diseaseGroup, Technology t, File f, Datastore datastore) {
        this.diseaseGroup = diseaseGroup;
        this.technology = t;
        this.file = f;
        this.datastore = datastore;
        this.samples = 0;
        this.variantsD = 0;
        this.variantsT = 0;

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

        this.file.setSamples(this.samples);

        this.diseaseGroup.incSamples(this.samples);
        this.diseaseGroup.incVariants(this.variantsD);

        this.technology.incSamples(this.samples);
        this.technology.incVariants(this.variantsT);
        this.datastore.save(this.diseaseGroup);
        this.datastore.save(this.technology);


        return true;
    }

    @Override
    public boolean write(Variant elem) {

        Query<Variant> query = datastore.createQuery(Variant.class);

        this.samples = (this.samples == 0) ? elem.getDiseaseCount(this.diseaseGroup, this.technology).getTotalGts() : this.samples;

        Variant v = query.field("chromosome").equal(elem.getChromosome())
                .field("position").equal(elem.getPosition()).
                        field("reference").equal(elem.getReference()).
                        field("alternate").equal(elem.getAlternate())
                .get();

        if (v == null) {
            // Add file
            elem.addFile(this.file);
            this.datastore.save(elem);
            this.variantsD++;
            this.variantsT++;
        } else {
            // Add file
            v.addFile(this.file);

            if (v.getIds() == null || v.getIds().isEmpty()) {
                v.setIds(elem.getIds());
            }

            boolean b = false;
            boolean d = false, t = false;
            for (DiseaseCount dc : v.getDiseases()) {
                if (!d && dc.getDiseaseGroup().getGroupId() == this.diseaseGroup.getGroupId()) {
                    d = true;
                }
                if (!t && dc.getTechnology().getTechnologyId() == this.technology.getTechnologyId()) {
                    t = true;
                }

                if (dc.getDiseaseGroup().getGroupId() == this.diseaseGroup.getGroupId() && dc.getTechnology().getTechnologyId() == this.technology.getTechnologyId()) {
                    DiseaseCount aux = elem.getDiseaseCount(this.diseaseGroup, this.technology);
                    dc.incGt00(aux.getGt00());
                    dc.incGt01(aux.getGt01());
                    dc.incGt11(aux.getGt11());
                    dc.incGtMissing(aux.getGtmissing());

                    this.datastore.save(v);
                    b = true;
                }
            }

            if (!d) {
                this.variantsD++;
            }
            if (!t) {
                this.variantsT++;
            }

            if (!b) {
                DiseaseCount aux = elem.getDiseaseCount(this.diseaseGroup, this.technology);
                DiseaseCount newDc = new DiseaseCount(aux.getDiseaseGroup(), aux.getTechnology(), aux.getGt00(), aux.getGt01(), aux.getGt11(), aux.getGtmissing());
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
