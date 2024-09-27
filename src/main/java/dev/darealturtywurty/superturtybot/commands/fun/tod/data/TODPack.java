package dev.darealturtywurty.superturtybot.commands.fun.tod.data;

import dev.darealturtywurty.superturtybot.commands.fun.tod.TruthOrDareCommand;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TODPack {
    public static final TODPack DEFAULT = new TODPack("Default", "The default truth or dare pack.");

    static {
        DEFAULT.truths.addAll(TruthOrDareCommand.DEFAULT_TRUTHS);
        DEFAULT.dares.addAll(TruthOrDareCommand.DEFAULT_DARES);
    }

    private final List<String> truths = new ArrayList<>();
    private final List<String> dares = new ArrayList<>();
    private String name;
    private String description;

    public TODPack(String name, String description) {
        this.name = name;
        this.description = description;
    }
}