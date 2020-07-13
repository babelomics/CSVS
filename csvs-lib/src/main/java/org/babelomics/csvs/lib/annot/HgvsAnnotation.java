package org.babelomics.csvs.lib.annot;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HgvsAnnotation {

    private String geneId;
    private String transcriptId;
    private String transcriptName;
    private String transcriptRefSeq;
    private String translationId;
    private String translationRefSeq;
    private List<String> hgvsc;
    private List<String> hgvsp;
    private boolean transcriptCanonical;

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(String transcriptId) {
        this.transcriptId = transcriptId;
    }

    public String getTranscriptName() {
        return transcriptName;
    }

    public void setTranscriptName(String transcriptName) {
        this.transcriptName = transcriptName;
    }

    public String getTranscriptRefSeq() {
        return transcriptRefSeq;
    }

    public void setTranscriptRefSeq(String transcriptRefSeq) {
        this.transcriptRefSeq = transcriptRefSeq;
    }

    public String getTranslationId() {
        return translationId;
    }

    public void setTranslationId(String translationId) {
        this.translationId = translationId;
    }

    public String getTranslationRefSeq() {
        return translationRefSeq;
    }

    public void setTranslationRefSeq(String translationRefSeq) {
        this.translationRefSeq = translationRefSeq;
    }

    public List<String> getHgvsc() {
        return hgvsc;
    }

    public void setHgvsc(List<String> hgvsc) {
        this.hgvsc = hgvsc;
    }

    public List<String> getHgvsp() {
        return hgvsp;
    }

    public void setHgvsp(List<String> hgvsp) {
        this.hgvsp = hgvsp;
    }

    public boolean isTranscriptCanonical() {
        return transcriptCanonical;
    }

    public void setTranscriptCanonical(boolean transcriptCanonical) {
        this.transcriptCanonical = transcriptCanonical;
    }

}


