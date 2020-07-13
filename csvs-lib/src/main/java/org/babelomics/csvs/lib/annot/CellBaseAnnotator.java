package org.babelomics.csvs.lib.annot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.babelomics.csvs.lib.models.Variant;
import org.opencb.biodata.models.variant.annotation.ConsequenceType;
import org.opencb.biodata.models.variant.annotation.VariantAnnotation;
import org.opencb.biodata.models.variation.GenomicVariant;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CellBaseAnnotator {

    private CellBaseClient cellBaseClient;
    private boolean override;
    private boolean remove;
    private boolean ct;
    //private boolean gene;
    private boolean hgvs;
    private URI uri;
    private URI uriHgvs;
    private String version;

    private final static String CT_TAG = "ct";
    private final static String HGVSP_TAG = "hgvsp";
    private final static String HGVSC_TAG = "hgvsc";
    private final static String HGVS_C_P_TAG = "ann_c_p";
    private final static String HOST = "http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest";
    private final static String VERSION = "v3";
    private final static String SPECIES = "hsapiens";
    private final static String ASSEMBLY = "grch37";

    public CellBaseAnnotator(String host, String version) {
        this.cellBaseClient = null;
        this.override = false;
        this.remove = false;
        this.ct = false;
        //this.gene = false;
        this.hgvs = false;

        this.version = version != null ? version : VERSION;
        try {
            this.uri = new URI(host != null? host : HOST);
            cellBaseClient = new CellBaseClient(this.uri, this.version, SPECIES);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println("Invalid URL: " + host + " / " + HOST);
        }

    }

    public URI getUriHgvs() {
        return uriHgvs;
    }

    public void setUriHgvs(URI uriHgvs) {
        this.uriHgvs = uriHgvs;
    }
    public void setUriHgvs(String hostHgvs) {
        try {
            this.uriHgvs = new URI(hostHgvs != null ? hostHgvs : "");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println("Invalid URL: " + hostHgvs);

        }
    }
    public void annot(List<Variant> variantList) throws IOException {

        //List<GenomicVariant> genomicVariantList = new ArrayList<>();
        List<String> genomicVariantList = new ArrayList<>();

        for (Variant variant : variantList) {
            genomicVariantList.add(variant.getChromosome() + ":" + variant.getPosition() + ":" +
                    (variant.getReference().isEmpty() ? "-" : variant.getReference()) + ":" +
                    (variant.getAlternate().isEmpty() ? "-" : variant.getAlternate())
            );
        }

        // GET HGVS,
        List<HgvsAnnotation> variantAnnotationHGVSList = null;
        if (this.isHgvs()) {
            variantAnnotationHGVSList = getVariantAnnotationsHGVSURL(genomicVariantList);
        }
        //List<HgvsAnnotation> variantAnnotationHGVSList = getVariantAnnotationsHGVSURL(genomicVariantList);

        // GET GENES , CONSECUENCES TYPE
        List<VariantAnnotation> variantAnnotationList = null;
        if (this.isCt()) {
            variantAnnotationList = getVariantAnnotationsURL(genomicVariantList);
        }

        int index = 0;

        for (Variant variant : variantList) {
            if (variantAnnotationList != null) {
                VariantAnnotation v = variantAnnotationList.stream().filter(va ->
                        variant.getChromosome().equals(va.getChromosome()) &&
                                variant.getPosition() == va.getStart() &&
                                variant.getReference().equals(va.getReferenceAllele()) &&
                                variant.getAlternate().equals(va.getAlternateAllele())
                ).findAny().orElse(null);

                if (v != null) {
                    if (this.isCt()) {
                        Set<String> cts = new HashSet<>();

                        if (v.getConsequenceTypes().size() > 0) {
                            for (ConsequenceType ct : v.getConsequenceTypes()) {
                                if (ct.getSoTerms() != null) {
                                    for (ConsequenceType.ConsequenceTypeEntry cte : ct.getSoTerms()) {
                                        cts.add(cte.getSoName());
                                    }
                                }
                            }
                        }

                        if (cts.size() > 0) {
                            variant.getAnnots().put(CT_TAG, cts);
                        }
                    }

                }
            }

            List<HgvsAnnotation> listHgvsV = variantAnnotationHGVSList != null ? ((ArrayList) ((List<HgvsAnnotation>) variantAnnotationHGVSList.get(index))) : null;

            if (listHgvsV != null) {
                Set<String> listPC = new HashSet();
                List dataHgvsp = new ArrayList(), dataHgvsc = new ArrayList();
                listHgvsV.forEach(lHgvs -> {

                    if (lHgvs.getHgvsp() != null) {
                        dataHgvsp.addAll(lHgvs.getHgvsp());
                        lHgvs.getHgvsp().forEach(x ->
                                {
                                    if (x.split(":").length > 1)
                                        listPC.add(x.split(":")[1]);
                                }
                        );
                    }
                    if (lHgvs.getHgvsc() != null) {
                        dataHgvsc.addAll(lHgvs.getHgvsc());
                        lHgvs.getHgvsc().forEach(x ->
                                {
                                    if (x.split(":").length > 1)
                                        listPC.add(x.split(":")[1]);
                                }
                        );
                    }

                });
                if (dataHgvsp.size() > 0 ) {
                    variant.getAnnots().put(HGVSP_TAG, dataHgvsp);
                }
                if (dataHgvsc.size() > 0 ) {
                    variant.getAnnots().put(HGVSC_TAG, dataHgvsc);
                }

                if (listPC.size() > 0 ) {
                    variant.getAnnots().put(HGVS_C_P_TAG, listPC);
                }

            }
            index++;

        }
    }

    //private List<VariantAnnotation> getVariantAnnotationsURL(List<GenomicVariant> genomicVariantList) throws IOException {
    private List<VariantAnnotation> getVariantAnnotationsURL(List<String> genomicVariantList) throws IOException {
        List<VariantAnnotation> variantAnnotationList = null;
        QueryResponse<QueryResult<Object>> queryResponse = null;


        URL obj = new URL( uri+ "/" +this.version +"/"+SPECIES+"/genomic/variant/"+String.join(",", genomicVariantList)+
        //URL obj = new URL( uri+ "/" +this.version +"/"+SPECIES+"/genomic/variant/"+String.join(",", genomicVariantStringList)+
                "/annotation?assembly="+ASSEMBLY+"&include=consequenceType");
                //"/annotation?assembly="+ASSEMBLY+"&include=consequenceType&limit=-1&skip=-1&skipCount=false&count=false&Output%20format=pb%20(Not%20implemented%20yet)&normalize=false&phased=false&useCache=false&imprecise=true&svExtraPadding=0&cnvExtraPadding=0");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer responseBuffer = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseBuffer.append(inputLine);
            }
            in.close();

            ObjectMapper objectMapper = new ObjectMapper();

            try {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                queryResponse = objectMapper.readValue((String) responseBuffer.toString(), new TypeReference<QueryResponse<QueryResult<Object>> >(){});
            } catch (IOException ex) {

            } finally {

            }
        } else {
            System.out.println("GET request not worked " + obj.getPath());
        }
        if (queryResponse != null) {
            Collection<QueryResult<Object>> response = queryResponse.getResponse();

            QueryResult<Object>[] queryResults = response.toArray(new QueryResult[1]);
            variantAnnotationList = new ArrayList<>(genomicVariantList.size());
            //variantAnnotationList = new ArrayList<>(genomicVariantStringList.size());
            for (QueryResult<Object> queryResult : queryResults) {
                if(queryResult.getResult() != null){

                    ArrayList<LinkedHashMap> variantResults = (ArrayList<LinkedHashMap>) ((QueryResult) queryResult).getResult();
                    if (variantResults != null) {
                        for (LinkedHashMap variantElem : variantResults) {
                            VariantAnnotation v = new VariantAnnotation();

                            String chrom = (String) variantElem.get("chromosome");
                            if (chrom != null) {
                                v.setChromosome(chrom);
                            }
                            Integer start = (Integer) variantElem.get("start");
                            if (start != null) {
                                v.setStart(start);
                            }
                            String reference = (String) variantElem.get("reference");
                            if (reference != null) {
                                v.setReferenceAllele(reference);
                            }
                            String alternate = (String) variantElem.get("alternate");
                            if (alternate != null) {
                                v.setAlternateAllele(alternate);
                            }

                            ArrayList<LinkedHashMap>  consequenceTypes = (ArrayList<LinkedHashMap>) variantElem.get("consequenceTypes");
                            if (variantResults != null) {
                                List<ConsequenceType> listCt = new ArrayList<>();
                                for (LinkedHashMap consequenceTypeElem : consequenceTypes) {
                                    ConsequenceType ct = new ConsequenceType();

                                    ArrayList<LinkedHashMap>   sequenceOntologyTerms = (ArrayList<LinkedHashMap>) consequenceTypeElem.get("sequenceOntologyTerms");
                                    if (sequenceOntologyTerms != null) {
                                        List<ConsequenceType.ConsequenceTypeEntry> soNew = new ArrayList();
                                        ConsequenceType.ConsequenceTypeEntry cte = new ConsequenceType.ConsequenceTypeEntry();
                                        for (LinkedHashMap so : sequenceOntologyTerms) {
                                            cte.setSoName((String) so.get("name"));
                                            cte.setSoAccession((String) so.get("accession"));
                                            soNew.add(cte);
                                        }
                                        ct.setSoTerms(soNew);
                                    }
                                    listCt.add(ct);
                                }
                                v.setConsequenceTypes(listCt);
                            }
                            variantAnnotationList.add(v);
                        }
                    }
                }
            }
        }
        return variantAnnotationList;
    }


    private List<HgvsAnnotation> getVariantAnnotationsHGVSURL(List<String> genomicVariantList) throws IOException {
        List<HgvsAnnotation> variantAnnotationList = null;
        List<HgvsAnnotation> queryResponse = null;

        // POST
        URL obj = new URL( uriHgvs + "/variants/hgvs");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("accept", "*/*");
        // Add body
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        String json = new ObjectMapper().writeValueAsString(genomicVariantList);
        //String json = new ObjectMapper().writeValueAsString(genomicVariantStringList);
        wr.write(json);
        wr.flush();

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer responseBuffer = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseBuffer.append(inputLine);
            }
            in.close();

            ObjectMapper objectMapper = new ObjectMapper();

            try {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                //queryResponse = objectMapper.readValue((String) responseBuffer.toString(), new TypeReference<QueryResponse<QueryResult<Object>> >(){});
                queryResponse = objectMapper.readValue((String) responseBuffer.toString(),new TypeReference<List<List<HgvsAnnotation>>>(){});
            } catch (IOException ex) {

            } finally {

            }
        } else {
            System.out.println("POST request not worked " + obj.getPath());
        }


        if (queryResponse != null) {
            variantAnnotationList = queryResponse;
        }
        return variantAnnotationList;
    }

    private List<VariantAnnotation> getVariantAnnotationsREST(List<GenomicVariant> genomicVariantList) throws IOException {
        QueryResponse<QueryResult<VariantAnnotation>> queryResponse;
        List<String> genomicVariantStringList = new ArrayList<>(genomicVariantList.size());
        for (GenomicVariant genomicVariant : genomicVariantList) {
            genomicVariantStringList.add(genomicVariant.toString());
        }

        QueryOptions queryOptions= new QueryOptions();
        queryOptions.add("include", "consequenceType");
        boolean queryError = false;
        try {

            queryResponse = cellBaseClient.get(
                    CellBaseClient.Category.genomic,
                    CellBaseClient.SubCategory.variant,
                    genomicVariantStringList,
                    CellBaseClient.Resource.annotation,
                    queryOptions);


            if (queryResponse == null) {
                queryError = true;
            }
        } catch (JsonProcessingException e) {
            queryError = true;
            queryResponse = null;
        }

        if (queryResponse != null && queryResponse.getResponse().size() != genomicVariantList.size()) {
            queryError = true;
        }

        if (queryError) {

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


    public boolean isHgvs() {
        return hgvs;
    }

    public void setHgvs(boolean hgvs) {
        this.hgvs = hgvs;
    }
}