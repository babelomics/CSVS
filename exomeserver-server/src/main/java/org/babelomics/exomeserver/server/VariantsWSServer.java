package org.babelomics.exomeserver.server;


import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.babelomics.exomeserver.lib.mongodb.dbAdaptor.ExomeServerStudyMongoDBAdaptor;
import org.babelomics.exomeserver.lib.mongodb.dbAdaptor.ExomeServerVariantMongoDBAdaptor;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.ArchivedVariantFile;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Path("/variants")
@Api(value = "variants", description = "Variants")
@Produces(MediaType.APPLICATION_JSON)
public class VariantsWSServer extends ExomeServerWSServer {

    private ExomeServerVariantMongoDBAdaptor variantMongoDbAdaptor;
    private ExomeServerStudyMongoDBAdaptor studyMongoDBAdaptor;


    public VariantsWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException, IllegalOpenCGACredentialsException {
        super(version, uriInfo, hsr);

        variantMongoDbAdaptor = new ExomeServerVariantMongoDBAdaptor(credentials);
        studyMongoDBAdaptor = new ExomeServerStudyMongoDBAdaptor(credentials);
    }

    @GET
    @Path("/{regions}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get Variants By Region")
    public Response getVariantsByRegion(@ApiParam(value = "regions") @PathParam("regions") String regionId,
                                        @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
                                        @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
                                        @ApiParam(value = "studies") @QueryParam("studies") String studies,
                                        @ApiParam(value = "diseases") @QueryParam("diseases") String diseases,
                                        @ApiParam(value = "phenotypes") @QueryParam("phenotypes") String phenotypes
    ) {

        queryOptions.put("merge", true);
        queryOptions.put("limit", limit);
        queryOptions.put("skip", skip);


        // Parse the provided regions. The total size of all regions together 
        // can't excede 1 million positions
        int regionsSize = 0;
        List<Region> regions = new ArrayList<>();
        for (String s : regionId.split(",")) {
            Region r = Region.parseRegion(s);
            regions.add(r);
            regionsSize += r.getEnd() - r.getStart();
        }

        List<StudyElement> studyElements = new ArrayList<>();
        QueryOptions qo = new QueryOptions();


        QueryResult<BasicDBObject> allStudies = studyMongoDBAdaptor.getAllFileId(qo);


        for (BasicDBObject study : allStudies.getResult()) {
            String fid = study.getString("fid");
            studyElements.add(new StudyElement(fid));
        }

        List<StudyElement> finalStudyElements = new ArrayList<>();

        if (studies != null) {
            String[] studiesList = studies.trim().split(",");
            for (StudyElement fid : studyElements) {
                if (checkFileIdStudies(fid, studiesList)) {
                    finalStudyElements.add(fid);
                }
            }
        }

        if (diseases != null) {
            String[] diseasesList = diseases.trim().split(",");

            if (finalStudyElements.isEmpty()) {
                for (StudyElement fid : studyElements) {
                    if (checkFileIdDiseases(fid, diseasesList)) {
                        finalStudyElements.add(fid);
                    }
                }
            } else {
                Iterator<StudyElement> it = finalStudyElements.listIterator();
                while (it.hasNext()) {
                    StudyElement fid = it.next();
                    if (!checkFileIdDiseases(fid, diseasesList)) {
                        it.remove();
                    }
                }
            }
        }

        if (phenotypes != null) {
            String[] phenotypesList = phenotypes.trim().split(",");
            if (finalStudyElements.isEmpty()) {
                for (StudyElement fid : studyElements) {
                    if (checkFileIdPhenotypes(fid, phenotypesList)) {
                        finalStudyElements.add(fid);
                    }
                }
            } else {
                Iterator<StudyElement> it = finalStudyElements.listIterator();
                while (it.hasNext()) {
                    StudyElement fid = it.next();
                    if (!checkFileIdPhenotypes(fid, phenotypesList)) {
                        it.remove();
                    }
                }
            }
        }

        List<String> aux = new ArrayList<>(finalStudyElements.size());
        for (StudyElement se : finalStudyElements) {
            aux.add(se.toString());
        }

        // if (regionsSize <= 1000000) {
//        List<QueryResult> allVariantsByRegionList = variantMongoDbAdaptor.getAllVariantsByRegionList(regions, queryOptions);
        List<QueryResult> allVariantsByRegionList = variantMongoDbAdaptor.getAllVariantsByRegionListAndFileIds(regions, aux, queryOptions);


        removeStudies(allVariantsByRegionList, finalStudyElements);
        transformVariants(allVariantsByRegionList);


        return createOkResponse(allVariantsByRegionList);
//        } else {
//            return createErrorResponse("The total size of all regions provided can't exceed 1 million positions. "
//                    + "If you want to browse a larger number of positions, please provide the parameter 'histogram=true'");
//        }
    }

    private void removeStudies(List<QueryResult> allVariantsByRegionList, List<StudyElement> finalStudyElements) {

        List<String> ids = new ArrayList<>(finalStudyElements.size());
        for (StudyElement se : finalStudyElements) {
            ids.add(se.getStudy() + "_" + se.toString());
        }

        for (QueryResult qr : allVariantsByRegionList) {
            List<Variant> variantList = qr.getResult();

            for (Variant v : variantList) {
                Map<String, ArchivedVariantFile> files = v.getFiles();

                for (Iterator<Map.Entry<String, ArchivedVariantFile>> it = files.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ArchivedVariantFile> entry = it.next();

                    if (!ids.contains(entry.getKey())) {
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean checkFileIdStudies(StudyElement fid, String[] studiesList) {
        for (String study : studiesList) {
            if (fid.getStudy().equalsIgnoreCase(study))
                return true;
        }
        return false;
    }

    private boolean checkFileIdDiseases(StudyElement fid, String[] diseasesList) {
        for (String disease : diseasesList) {
            if (fid.getDisease().equalsIgnoreCase(disease))
                return true;
        }
        return false;
    }

    private boolean checkFileIdPhenotypes(StudyElement fid, String[] phenotypesList) {
        for (String phenotype : phenotypesList) {
            if (fid.getPhenotype().equalsIgnoreCase(phenotype))
                return true;
        }
        return false;
    }

    private void transformVariants(List<QueryResult> allVariantsByRegionList) {


        for (QueryResult qr : allVariantsByRegionList) {
            List<Variant> variantList = qr.getResult();
            List<Variant> newVariantList = new ArrayList<>(variantList.size());

            for (Variant v : variantList) {
                combineFiles(v.getFiles());
            }


//            qr.setResult(newVariantList);

        }
    }

    private void combineFiles(Map<String, ArchivedVariantFile> files) {
        ArchivedVariantFile newAVF = new ArchivedVariantFile("MAF", "MAF");
        VariantStats stats = new VariantStats();
        newAVF.setStats(stats);

        for (Map.Entry<String, ArchivedVariantFile> entry : files.entrySet()) {
            ArchivedVariantFile avf = entry.getValue();
            for (Map.Entry<Genotype, Integer> o : avf.getStats().getGenotypesCount().entrySet()) {
                stats.addGenotype(o.getKey(), o.getValue());
            }
        }

        files.clear(); // TODO aaleman: clear all but static studies.

//        recalculeMAF(stats);

        files.put("MAF", newAVF);
    }

    private void recalculeMAF(VariantStats stats) {

    }


    @OPTIONS
    @Path("/{region}/variants")
    public Response getVariantsByRegion() {
        return createOkResponse("");
    }

    private class StudyElement {
        private String study;
        private String disease;
        private String phenotype;

        public StudyElement(String fid) {
            String[] aux = fid.split("_");
            study = aux[0];
            disease = aux[1];
            phenotype = aux[2];
        }

        public String getStudy() {
            return study;
        }

        public String getDisease() {
            return disease;
        }

        public String getPhenotype() {
            return phenotype;
        }

        @Override
        public String toString() {
            return this.study + "_" + this.disease + "_" + this.phenotype;
        }
    }
}
