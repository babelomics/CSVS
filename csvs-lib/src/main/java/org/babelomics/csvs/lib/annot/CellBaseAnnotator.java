package org.babelomics.csvs.lib.annot;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babelomics.csvs.lib.models.Variant;
import org.opencb.biodata.models.variant.annotation.ConsequenceType;
import org.opencb.biodata.models.variant.annotation.VariantAnnotation;
import org.opencb.biodata.models.variation.GenomicVariant;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CellBaseAnnotator {

    private CellBaseClient cellBaseClient;
    private boolean override;
    private boolean remove;
    private boolean ct;
    private boolean gene;


    private final static String CT_TAG = "ct";
    private final static String GENE_TAG = "g";
    private final static String HOST = "http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest";
    private final static String VERSION = "v3";
    private final static String SPECIES = "hsapiens";

    public CellBaseAnnotator(String host, String version) {
        this.cellBaseClient = null;
        this.override = false;
        this.remove = false;
        this.ct = false;
        this.gene = false;


        try {
            URI uri = new URI(host != null? host : HOST);
            cellBaseClient = new CellBaseClient(uri, version != null ? version : VERSION, SPECIES);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println("Invalid URL: " + HOST);
        }

    }

    public void annot(List<Variant> variantList) throws IOException {

        if (this.isRemove()) {
            for (Variant v : variantList) {
                this.remove(v);
            }
        } else {

            List<GenomicVariant> genomicVariantList = new ArrayList<>();

            for (Variant variant : variantList) {
                GenomicVariant genomicVariant = new GenomicVariant(variant.getChromosome(), variant.getPosition(),
                        variant.getReference().isEmpty() ? "-" : variant.getReference(),
                        variant.getAlternate().isEmpty() ? "-" : variant.getAlternate());
                genomicVariantList.add(genomicVariant);
            }

            List<VariantAnnotation> variantAnnotationList = getVariantAnnotationsREST(genomicVariantList);

            if (variantAnnotationList.size() == variantList.size()) {

                for (int i = 0; i < variantAnnotationList.size(); i++) {
                    Variant variant = variantList.get(i);
                    VariantAnnotation va = variantAnnotationList.get(i);

                    if (this.isCt() && (!variant.getAnnots().containsKey(CT_TAG) || this.isOverride())) {
                        Set<String> cts = new HashSet<>();

                        for (ConsequenceType ct : va.getConsequenceTypes()) {
                            for (ConsequenceType.ConsequenceTypeEntry cte : ct.getSoTerms()) {
                                cts.add(cte.getSoName());
                            }
                        }

                        if (cts.size() > 0) {
                            variant.getAnnots().put(CT_TAG, cts);
                        }
                    }

                    if (this.isGene() && (!variant.getAnnots().containsKey(GENE_TAG) || this.isOverride())) {
                        Set<String> genes = new HashSet<>();
                        for (ConsequenceType ct : va.getConsequenceTypes()) {
                            String geneName = ct.getGeneName();
                            if (geneName != null && geneName != "") {
                                genes.add(geneName);
                            }
                        }

                        if (genes.size() > 0) {
                            variant.getAnnots().put(GENE_TAG, genes);
                        }
                    }
                }
            }
        }

    }

    public void remove(Variant v) {
        if (this.isCt())
            v.getAnnots().remove(CT_TAG);
        if (this.isGene())
            v.getAnnots().remove(GENE_TAG);
    }

    private List<VariantAnnotation> getVariantAnnotationsREST(List<GenomicVariant> genomicVariantList) throws IOException {
        QueryResponse<QueryResult<VariantAnnotation>> queryResponse;
        List<String> genomicVariantStringList = new ArrayList<>(genomicVariantList.size());
        for (GenomicVariant genomicVariant : genomicVariantList) {
            genomicVariantStringList.add(genomicVariant.toString());
        }

        boolean queryError = false;
        try {
            queryResponse = cellBaseClient.get(
                    CellBaseClient.Category.genomic,
                    CellBaseClient.SubCategory.variant,
                    genomicVariantStringList,
                    CellBaseClient.Resource.fullAnnotation,
                    null);
            if (queryResponse == null) {
//                logger.warn("CellBase REST fail. Returned null. {}", cellBaseClient.getLastQuery());
                queryError = true;
            }
        } catch (JsonProcessingException e) {
//            logger.warn("CellBase REST fail. Error parsing " + cellBaseClient.getLastQuery(), e);
            queryError = true;
            queryResponse = null;
        }

        if (queryResponse != null && queryResponse.getResponse().size() != genomicVariantList.size()) {
//            logger.warn("QueryResult size (" + queryResponse.getResponse().size() + ") != genomicVariantList size (" + genomicVariantList.size() + ").");
            //throw new IOException("QueryResult size != " + genomicVariantList.size() + ". " + queryResponse);
            queryError = true;
        }

        if (queryError) {
//            logger.warn("CellBase REST error. {}", cellBaseClient.getLastQuery());

            if (genomicVariantList.size() == 1) {
//                logger.error("CellBase REST error. Skipping variant. {}", genomicVariantList.get(0));
                return Collections.emptyList();
            }

            List<VariantAnnotation> variantAnnotationList = new LinkedList<>();
            List<GenomicVariant> genomicVariants1 = genomicVariantList.subList(0, genomicVariantList.size() / 2);
            if (!genomicVariants1.isEmpty()) {
                variantAnnotationList.addAll(getVariantAnnotationsREST(genomicVariants1));
            }
            List<GenomicVariant> genomicVariants2 = genomicVariantList.subList(genomicVariantList.size() / 2, genomicVariantList.size());
            if (!genomicVariants2.isEmpty()) {
                variantAnnotationList.addAll(getVariantAnnotationsREST(genomicVariants2));
            }
            return variantAnnotationList;
        }

        Collection<QueryResult<VariantAnnotation>> response = queryResponse.getResponse();

        QueryResult<VariantAnnotation>[] queryResults = response.toArray(new QueryResult[1]);
        List<VariantAnnotation> variantAnnotationList = new ArrayList<>(genomicVariantList.size());
        for (QueryResult<VariantAnnotation> queryResult : queryResults) {
            variantAnnotationList.addAll(queryResult.getResult());
        }
        return variantAnnotationList;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public boolean isCt() {
        return ct;
    }

    public void setCt(boolean ct) {
        this.ct = ct;
    }

    public boolean isGene() {
        return gene;
    }

    public void setGene(boolean gene) {
        this.gene = gene;
    }
}
