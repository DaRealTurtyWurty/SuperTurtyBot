package io.github.darealturtywurty.superturtybot.modules.idlerpg;

public enum Finding {
    NOTHING(Bruhnuments.UNDEFINED), CAVE(Bruhnuments.UNKNOWN), DUNGEON(Bruhnuments.UNKNOWN),
    CHEST(Bruhnuments.POSITIVE), VILLAGE(Bruhnuments.POSITIVE), POND(Bruhnuments.UNKNOWN), TRAP(Bruhnuments.NEGATIVE),
    ENEMY(Bruhnuments.NEGATIVE), FOOD(Bruhnuments.POSITIVE);

    private final Bruhnuments bruhnuments;
    private final String foundMessage;

    Finding(Bruhnuments bruhnuments, String foundMessage) {
        this.bruhnuments = bruhnuments;
        this.foundMessage = foundMessage;
    }
    
    public Bruhnuments getBruhnuments() {
        return this.bruhnuments;
    }

    public String getFoundMessage() {
        return this.foundMessage;
    }
}