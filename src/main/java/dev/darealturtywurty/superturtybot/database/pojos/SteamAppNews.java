package dev.darealturtywurty.superturtybot.database.pojos;

import java.util.HashMap;
import java.util.Map;

public class SteamAppNews {
    private String author;
    private String contents;
    private int date;
    private String feedlabel;
    private String feedname;
    private String gid;
    private boolean isExternalUrl;
    private String title;
    private String url;
    private Map<String, Object> additionalProperties;
    
    public SteamAppNews() {
        this("", "", -1, "", "", "", false, "", "", new HashMap<>());
    }
    
    public SteamAppNews(String author, String contents, int date, String feedlabel, String feedname, String gid,
        boolean isExternalUrl, String title, String url, Map<String, Object> additionalProperties) {
        this.author = author;
        this.contents = contents;
        this.date = date;
        this.feedlabel = feedlabel;
        this.feedname = feedname;
        this.gid = gid;
        this.isExternalUrl = isExternalUrl;
        this.title = title;
        this.url = url;
        this.additionalProperties = additionalProperties;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }
    
    public String getAuthor() {
        return this.author;
    }
    
    public String getContents() {
        return this.contents;
    }
    
    public int getDate() {
        return this.date;
    }
    
    public String getFeedlabel() {
        return this.feedlabel;
    }
    
    public String getFeedname() {
        return this.feedname;
    }
    
    public String getGid() {
        return this.gid;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public boolean isExternalUrl() {
        return this.isExternalUrl;
    }
    
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public void setContents(String contents) {
        this.contents = contents;
    }
    
    public void setDate(int date) {
        this.date = date;
    }
    
    public void setExternalUrl(boolean isExternalUrl) {
        this.isExternalUrl = isExternalUrl;
    }
    
    public void setFeedlabel(String feedlabel) {
        this.feedlabel = feedlabel;
    }
    
    public void setFeedname(String feedname) {
        this.feedname = feedname;
    }
    
    public void setGid(String gid) {
        this.gid = gid;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
