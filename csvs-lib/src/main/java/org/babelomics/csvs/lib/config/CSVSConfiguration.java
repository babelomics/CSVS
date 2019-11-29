package org.babelomics.csvs.lib.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Technology;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class CSVSConfiguration {

    private List<DiseaseGroup> diseasesGroups;
    private List<Technology> technologies;


    public CSVSConfiguration() {
    }

    public CSVSConfiguration(List<DiseaseGroup> diseasesGroups, List<Technology> technologies) {
        this.diseasesGroups = diseasesGroups;
        this.technologies = technologies;
    }

    public static CSVSConfiguration load(InputStream configurationInputStream) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();

        CSVSConfiguration prioCNVsConfiguration =null;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

        try {
            prioCNVsConfiguration = jsonMapper.readValue(configurationInputStream, CSVSConfiguration.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prioCNVsConfiguration;
    }

    public static CSVSConfiguration load(String nameFile) throws IOException {
        InputStream io = CSVSConfiguration.class.getClassLoader().getResourceAsStream(nameFile);
        return load(io);
    }

    public List<DiseaseGroup> getDiseasesGroups() {
        return diseasesGroups;
    }

    public void setDiseasesGroups(List<DiseaseGroup> diseasesGroups) {
        this.diseasesGroups = diseasesGroups;
    }

    public List<Technology> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<Technology> technologies) {
        this.technologies = technologies;
    }
}
