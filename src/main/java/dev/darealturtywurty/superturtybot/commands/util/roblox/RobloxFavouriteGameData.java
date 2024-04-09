package dev.darealturtywurty.superturtybot.commands.util.roblox;

import lombok.Data;

@Data
public class RobloxFavouriteGameData {
    private long id;
    private String name;
    private String description;
    private Creator creator;
    private RootPlace rootPlace;
    private String created;
    private String updated;
    private int placeVisits;

    @Data
    public static class Creator {
        private long id;
        private String group;
    }

    @Data
    public static class RootPlace {
        private long id;
        private String type;
    }
}