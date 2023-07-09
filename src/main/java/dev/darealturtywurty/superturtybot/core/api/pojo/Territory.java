package dev.darealturtywurty.superturtybot.core.api.pojo;

import java.util.Objects;

public class Territory {
    private double population;
    private String name;
    private String cca3;
    private String cca2;
    private String region;
    private double landAreaKm;
    private double densityMi;

    private String flag;
    private String outline;

    public Territory(double population, String name, String cca3, String cca2, String region, double landAreaKm, double densityMi, String flag, String outline) {
        this.population = population;
        this.name = name;
        this.cca3 = cca3;
        this.cca2 = cca2;
        this.region = region;
        this.landAreaKm = landAreaKm;
        this.densityMi = densityMi;
        this.flag = flag;
        this.outline = outline;
    }

    public Territory() {
        this(0, "", "", "", "", 0, 0, "", "");
    }

    public double getPopulation() {
        return population;
    }

    public void setPopulation(double population) {
        this.population = population;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCca3() {
        return cca3;
    }

    public void setCca3(String cca3) {
        this.cca3 = cca3;
    }

    public String getCca2() {
        return cca2;
    }

    public void setCca2(String cca2) {
        this.cca2 = cca2;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public double getLandAreaKm() {
        return landAreaKm;
    }

    public void setLandAreaKm(double landAreaKm) {
        this.landAreaKm = landAreaKm;
    }

    public double getDensityMi() {
        return densityMi;
    }

    public void setDensityMi(double densityMi) {
        this.densityMi = densityMi;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Territory that = (Territory) o;
        return Double.compare(that.population, population) == 0 && Double.compare(that.landAreaKm, landAreaKm) == 0 && Double.compare(that.densityMi, densityMi) == 0 && Objects.equals(name, that.name) && Objects.equals(cca3, that.cca3) && Objects.equals(cca2, that.cca2) && Objects.equals(region, that.region) && Objects.equals(flag, that.flag) && Objects.equals(outline, that.outline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(population, name, cca3, cca2, region, landAreaKm, densityMi, flag, outline);
    }

    @Override
    public String toString() {
        return "CountryData{" +
                "population=" + population +
                ", name='" + name + '\'' +
                ", cca3='" + cca3 + '\'' +
                ", cca2='" + cca2 + '\'' +
                ", region='" + region + '\'' +
                ", landAreaKm=" + landAreaKm +
                ", densityMi=" + densityMi +
                ", flag='" + flag + '\'' +
                ", outline='" + outline + '\'' +
                '}';
    }
}
