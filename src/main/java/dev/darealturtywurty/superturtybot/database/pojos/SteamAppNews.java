package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class SteamAppNews {
    private String author;
    private String contents;
    private long date;
    private String feedlabel;
    private String feedname;
    private String gid;
    private boolean externalUrl;
    private String title;
    private String url;
    private Map<String, Object> additionalProperties;

    public SteamAppNews() {
        this("", "", -1, "", "", "", false, "", "", new HashMap<>());
    }
}
