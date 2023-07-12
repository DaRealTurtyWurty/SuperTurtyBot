package dev.darealturtywurty.superturtybot.core.api.request;

import java.util.ArrayList;
import java.util.List;

public class RegionExcludeRequestData {
    private final boolean excludeCountries;
    private final boolean excludeTerritories;
    private final boolean excludeIslands;
    private final boolean excludeMainland;

    private RegionExcludeRequestData(boolean excludeCountries, boolean excludeTerritories, boolean excludeIslands, boolean excludeMainland) {
        this.excludeCountries = excludeCountries;
        this.excludeTerritories = excludeTerritories;
        this.excludeIslands = excludeIslands;
        this.excludeMainland = excludeMainland;
    }

    public boolean isExcludeCountries() {
        return excludeCountries;
    }

    public boolean isExcludeTerritories() {
        return excludeTerritories;
    }

    public boolean isExcludeIslands() {
        return excludeIslands;
    }

    public boolean isExcludeMainland() {
        return excludeMainland;
    }

    public boolean hasExclusions() {
        return excludeCountries || excludeTerritories || excludeIslands || excludeMainland;
    }

    public List<String> getExclusions() {
        List<String> exclusions = new ArrayList<>();

        if (excludeCountries)
            exclusions.add("countries");
        if (excludeTerritories)
            exclusions.add("territories");
        if (excludeIslands)
            exclusions.add("islands");
        if (excludeMainland)
            exclusions.add("mainland");

        return exclusions;
    }

    public static class Builder {
        private boolean excludeCountries;
        private boolean excludeTerritories;
        private boolean excludeIslands;
        private boolean excludeMainland;

        public Builder excludeCountries() {
            this.excludeCountries = true;
            return this;
        }

        public Builder excludeTerritories() {
            this.excludeTerritories = true;
            return this;
        }

        public Builder excludeIslands() {
            this.excludeIslands = true;
            return this;
        }

        public Builder excludeMainland() {
            this.excludeMainland = true;
            return this;
        }

        public RegionExcludeRequestData build() {
            if (this.excludeCountries && this.excludeTerritories && this.excludeIslands && this.excludeMainland)
                throw new IllegalStateException("Cannot exclude all regions!");

            return new RegionExcludeRequestData(excludeCountries, excludeTerritories, excludeIslands, excludeMainland);
        }
    }

    public static RegionExcludeRequestData empty() {
        return new RegionExcludeRequestData(false, false, false, false);
    }
}
