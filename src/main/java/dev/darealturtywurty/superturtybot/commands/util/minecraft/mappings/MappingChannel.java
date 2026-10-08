package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import java.util.Arrays;

public enum MappingChannel {
    MOJMAP("mojmap", "Mojmap"),
    MCP("mcp", "MCP"),
    SRG("srg", "SRG"),
    YARN("yarn", "Yarn"),
    INTERMEDIARY("intermediary", "Intermediary"),
    OBFUSCATED("official", "Obfuscated");

    private final String namespace;
    private final String displayName;

    MappingChannel(String namespace, String displayName) {
        this.namespace = namespace;
        this.displayName = displayName;
    }

    public static MappingChannel fromName(String name) {
        return Arrays.stream(values())
            .filter(channel -> channel.name().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Choose Mojmap, MCP, SRG, Yarn, Intermediary, or obfuscated."));
    }

    public String namespace() {
        return this.namespace;
    }

    public String displayName() {
        return this.displayName;
    }
}
