package dev.darealturtywurty.superturtybot.modules.collectable;

public enum CollectablePresentation {
    EMOJI("emoji"),
    IMAGE("image");

    private final String id;

    CollectablePresentation(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}
