package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SteamAppNews {
    private String author;
    private String contents;
    private long date;
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
    
    public SteamAppNews(String author, String contents, long date, String feedlabel, String feedname, String gid,
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
    
    public void setDate(long date) {
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
