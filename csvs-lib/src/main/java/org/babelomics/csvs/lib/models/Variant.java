package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babelomics.csvs.lib.io.CSVSVariantCountsMongoWriter;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */

@Entity(noClassnameStored = true)
@Indexes({
        @Index(name = "index_variant_chr_pos_ref_alt", value = "c,p,r,a", unique = true),
        @Index(name = "index_variant_chIds", value = "_at.chIds"),
        @Index(name = "d_dgid", value = "d.dgid"),
        @Index(name = "rs", value = "rs"),
})
public class Variant {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("c")
    private String chromosome;
    @Property("p")
    private int position;
    @Property("r")
    private String reference;
    @Property("a")
    private String alternate;
    @Property("i")
    private String ids;


    @Property("_at")
    private Map<String, Object> attr;

    //    @JsonIgnore
    @Embedded("d")
    private List<DiseaseCount> diseases;

    //    @JsonIgnore
    @Embedded("dspanel")
    private List<DiseaseSum> diseasesSamplePanel;

    @Transient
    private DiseaseCount stats;

    @Property("an")
    private Map<String, Object> annots;

    @Property("rs")
    private String rs;

    public Variant() {
        this.attr = new HashMap<>();
        this.annots = new HashMap<>();
        this.diseases = new ArrayList<>();
        this.stats = new DiseaseCount();
    }

    public Variant(String chromosome, int position, String reference, String alternate) {
        this();
        this.chromosome = chromosome;
        this.position = position;
        this.reference = reference;
        this.alternate = alternate;
    }

    public Variant(String variant) {
        this();
        if (variant != null && !variant.isEmpty()) {
            String[] fields = variant.split("[:-]", -1);
            this.chromosome = fields[0];
            this.position = Integer.parseInt(fields[1]);
            this.reference = fields[2];
            this.alternate = fields[3];
        }
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAlternate() {
        return alternate;
    }

    public void setAlternate(String alternate) {
        this.alternate = alternate;
    }

    public List<DiseaseCount> getDiseases() {
        return diseases;
    }

    public void setDiseases(List<DiseaseCount> diseases) {
        this.diseases = diseases;
    }

    public ObjectId getId() {
        return id;
    }

    public Map<String, Object> getAnnots() {
        return annots;
    }

    public void setAnnots(Map<String, Object> annots) {
        this.annots = annots;
    }

    public DiseaseCount getStats() {
        return stats;
    }

    public void setStats(DiseaseCount stats) {
        this.stats = stats;
    }

    public void addGenotypesToDiseaseAndTechnology(DiseaseGroup diseaseId, Technology technology, int gt00, int gt01, int gt11, int gtmissing) {
        DiseaseCount dc = new DiseaseCount(diseaseId, technology, gt00, gt01, gt11, gtmissing);
        this.diseases.add(dc);
    }

    public void addDiseaseCount(DiseaseCount dc) {
        this.diseases.add(dc);
    }

    public void deleteDiseaseCount(DiseaseCount dc) {
        this.diseases.remove(dc);
    }

    public List<DiseaseSum> getDiseasesSamplePanel() {
        return diseasesSamplePanel;
    }

    public String getRs() {
        return rs;
    }

    public void setRs(String rs) {
        this.rs = rs;
    }

    private DiseaseCount searchDC(DiseaseCount diseaseCountSearch){
        if (this.diseases != null) {

        List<DiseaseCount> filtered = this.diseases.stream()
                .filter(dc -> dc.getDiseaseGroupId() == diseaseCountSearch.getDiseaseGroupId() && dc.getTechnologyId() == diseaseCountSearch.getTechnologyId()).collect(Collectors.toList());

        if( filtered.size() > 0)
            return filtered.get(0);
        }
         return null;
    }

    private DiseaseSum searchDS(DiseaseSum diseaseSumSearch){
        if (this.diseasesSamplePanel != null) {

            List<DiseaseSum> filtered = this.diseasesSamplePanel.stream()
                    .filter(ds -> ds.getDiseaseGroupId() == diseaseSumSearch.getDiseaseGroupId() && ds.getTechnologyId() == diseaseSumSearch.getTechnologyId()).collect(Collectors.toList());

            if( filtered.size() > 0)
                return filtered.get(0);
        }
         return null;
    }


    private void addDiseasesSamplePanel(DiseaseSum ds){
        if (this.diseasesSamplePanel == null)
            this.diseasesSamplePanel = new ArrayList<>();

        if (ds.getSumSampleRegions() == 0)
            this.diseasesSamplePanel.remove(ds);
        else
            this.diseasesSamplePanel.add(ds);

    }

    /**
     * Set a disease the num of samples in the region. If samples == 0 delete if exists.
     * @param ds
     */
    public void setSumSampleRegion(DiseaseSum ds) {
        if (this.diseasesSamplePanel == null)
            this.diseasesSamplePanel = new ArrayList<>();

        if (ds.getSumSampleRegions() == 0) {
            if (!this.diseasesSamplePanel.isEmpty()) {
                DiseaseSum diseaseSum = searchDS(ds);
                if (diseaseSum != null) {
                    System.out.println("Delete" + diseaseSum);
                    this.diseasesSamplePanel.remove(diseaseSum);
                }
            }
        } else {
            DiseaseSum diseaseSum = searchDS(ds);
            if (diseaseSum != null)
                this.diseasesSamplePanel.replaceAll(elto -> elto.getDiseaseGroupId() == ds.getDiseaseGroupId() && elto.getTechnologyId() == ds.getTechnologyId() ? setAndReturn(elto, ds.getSumSampleRegions()) : elto);
            else
                this.diseasesSamplePanel.add(ds);
        }

    }

    public DiseaseSum setAndReturn (DiseaseSum ds, int sum){
        ds.setSumSampleRegions(sum);
        return ds;
    }


    @Deprecated
    public DiseaseCount getDiseaseCount(DiseaseGroup dg) {
        for (DiseaseCount dc : this.diseases) {
            if (dc.getDiseaseGroup().getGroupId() == dg.getGroupId()) {
                return dc;
            }
        }
        return null;
    }

    public DiseaseCount getDiseaseCount(DiseaseGroup dg, Technology t) {
        for (DiseaseCount dc : this.diseases) {
            if (dc.getDiseaseGroup().getGroupId() == dg.getGroupId() && dc.getTechnology().getTechnologyId() == t.getTechnologyId()) {
                return dc;
            }
        }
        return null;
    }

    @PrePersist
    private void prePresist() {

        String chunkSmall = this.getChromosome() + "_" + this.getPosition() / CSVSVariantCountsMongoWriter.CHUNK_SIZE_SMALL + "_" + CSVSVariantCountsMongoWriter.CHUNK_SIZE_SMALL / 1000 + "k";
        String chunkBig = this.getChromosome() + "_" + this.getPosition() / CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG + "_" + CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG / 1000 + "k";
        List<String> chunks = Arrays.asList(chunkSmall, chunkBig);

        this.attr.put("chIds", chunks);

    }

    @Override
    public String toString() {

        List<String> disIds = new ArrayList<>();

        if (this.diseases != null) {
            for (DiseaseCount dg : this.diseases) {
                disIds.add(dg.getDiseaseGroup().getGroupId() + "_" + dg.getTechnology().getTechnologyId());
            }
        }

        return "Variant{" +
                "chromosome='" + chromosome + '\'' +
                ", position=" + position +
                ", reference='" + reference + '\'' +
                ", alternate='" + alternate + '\'' +
//                ", attr=" + attr +
                ", diseases=" + disIds +
//                ", annots=" + annots +
                '}';
    }

}
