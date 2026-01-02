package dev.darealturtywurty.superturtybot.core.api.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Region {
    private double population;
    private String name;
    private String cca3;
    private String cca2;
    private String region;
    private double landAreaKm;
    private double densityMi;

    private String flag;
    private String outline;

    private List<String> aliases;

    public Region(double population, String name, String cca3, String cca2, String region, double landAreaKm, double densityMi, String flag, String outline, List<String> aliases) {
        this.population = population;
        this.name = name;
        this.cca3 = cca3;
        this.cca2 = cca2;
        this.region = region;
        this.landAreaKm = landAreaKm;
        this.densityMi = densityMi;
        this.flag = flag;
        this.outline = outline;
        this.aliases = aliases;
    }

    public Region() {
        this(0, "", "", "", "", 0, 0, "", "", new ArrayList<>());
    }
}
