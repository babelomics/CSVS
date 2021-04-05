package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author GRG
 */
@Entity(noClassnameStored = true)
public class LogQuery {
    @JsonIgnore
    @Id
    private ObjectId id;

    //@Indexed(name = "userId", unique = true)
    @Property("userId")
    private String userId;

    @Property("idSession")
    private String idSession;

    @Property("date")
    private Date date;

    @Embedded("region")
    private List<String> region = new ArrayList<>();

    @Embedded("cdnasList")
    private List<String> cdnasList = new ArrayList<>();

    @Embedded("proteinsList")
    private List<String> proteinsList = new ArrayList<>();

    public LogQuery() {
    }

    public LogQuery(ObjectId id, String userId, String idSession, Date date, List<String> region, List<String> cdnasList, List<String> proteinsList) {
        this.id = id;
        this.userId = userId;
        this.idSession = idSession;
        this.date = date;
        this.region = region;
        this.cdnasList = cdnasList;
        this.proteinsList = proteinsList;
    }

    public LogQuery(String userId,String idSession,  Date date, List<String> region, List<String> cdnasList, List<String> proteinsList) {
        this.userId = userId;
        this.idSession = idSession;
        this.date = date;
        this.region = region;
        this.cdnasList = cdnasList;
        this.proteinsList = proteinsList;
    }

    public LogQuery(List<String> region, List<String> cdnasList, List<String> proteinsList) {
        this.region = region;
        this.cdnasList = cdnasList;
        this.proteinsList = proteinsList;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIdSession() {
        return idSession;
    }

    public void setIdSession(String idSession) {
        this.idSession = idSession;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<String> getRegion() {
        return region;
    }

    public void setRegion(List<String> region) {
        this.region = region;
    }

    public List<String> getCdnasList() {
        return cdnasList;
    }

    public void setCdnasList(List<String> cdnasList) {
        this.cdnasList = cdnasList;
    }

    public List<String> getProteinsList() {
        return proteinsList;
    }

    public void setProteinsList(List<String> proteinsList) {
        this.proteinsList = proteinsList;
    }


    @Override
    public String toString() {
        return "Technology{" +
                "userId=" + userId +
                ", date=" + date +
                ", region=" + (region != null ? StringUtils.join(region, ",") : "") +
                ", cdnasList=" + (cdnasList != null ? StringUtils.join(cdnasList, ",") : "") +
                ", proteinsList=" + (proteinsList != null ? StringUtils.join(proteinsList, ",") : "") +
                '}';
    }
}
