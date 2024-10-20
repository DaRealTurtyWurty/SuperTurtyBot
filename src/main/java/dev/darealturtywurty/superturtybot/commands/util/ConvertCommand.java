package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConvertCommand extends CoreCommand {
    private static final Map<Measurement, List<Unit>> UNITS = new HashMap<>();

    static {
        List<Unit> lengths = new ArrayList<>();
        var centimeters = new BaseUnit(
                "centimeters",
                "Centimeters",
                "cm");
        var length = new Measurement("length", "Length", centimeters);
        lengths.add(centimeters);
        lengths.add(new Unit(
                "millimeters",
                "Millimeters",
                "mm",
                (measurement, value) -> value * 0.1,
                value -> value / 0.1));
        lengths.add(new Unit(
                "micrometers",
                "Micrometers",
                "µm",
                (measurement, value) -> value * 0.0001,
                value -> value / 0.0001));
        lengths.add(new Unit(
                "nanometers",
                "Nanometers",
                "nm",
                (measurement, value) -> value * 1e-7,
                value -> value / 1e-7));
        lengths.add(new Unit(
                "meters",
                "Meters",
                "m",
                (measurement, value) -> value * 100,
                value -> value / 100));
        lengths.add(new Unit(
                "kilometers",
                "Kilometers",
                "km",
                (measurement, value) -> value * 100_000,
                value -> value / 100_000));
        lengths.add(new Unit(
                "inches",
                "Inches",
                "in",
                (measurement, value) -> value * 2.54,
                value -> value / 2.54));
        lengths.add(new Unit(
                "feet",
                "Feet",
                "ft",
                (measurement, value) -> value * 30.48,
                value -> value / 30.48));
        lengths.add(new Unit(
                "yards",
                "Yards",
                "yd",
                (measurement, value) -> value * 91.44,
                value -> value / 91.44));
        lengths.add(new Unit(
                "miles",
                "Miles",
                "mi",
                (measurement, value) -> value * 160_934.4,
                value -> value / 160_934.4));
        lengths.add(new Unit(
                "nauticalmiles",
                "Nautical Miles",
                "nmi",
                (measurement, value) -> value * 185_200,
                value -> value / 185_200));
        lengths.add(new Unit(
                "lightyears",
                "Light Years",
                "ly",
                (measurement, value) -> value * 9.461e+17,
                value -> value / 9.461e+17));

        UNITS.put(length, lengths);

        var celsius = new BaseUnit(
                "celsius",
                "Celsius",
                "°C");
        var temperature = new Measurement("temperature", "Temperature", celsius);
        List<Unit> temperatures = new ArrayList<>();
        temperatures.add(celsius);
        temperatures.add(new Unit(
                "fahrenheit",
                "Fahrenheit",
                "°F",
                (measurement, value) -> (value * 1.8) + 32,
                value -> (value - 32) / 1.8));
        temperatures.add(new Unit(
                "kelvin",
                "Kelvin",
                "K",
                (measurement, value) -> value + 273.15,
                value -> value - 273.15));

        UNITS.put(temperature, temperatures);

        var liters = new BaseUnit(
                "liters",
                "Liters",
                "L");
        var volume = new Measurement("volume", "Volume", liters);
        List<Unit> volumes = new ArrayList<>();
        volumes.add(liters);
        volumes.add(new Unit(
                "milliliters",
                "Milliliters",
                "mL",
                (measurement, value) -> value * 0.001,
                value -> value / 0.001));
        volumes.add(new Unit(
                "gallons",
                "Gallons",
                "gal",
                (measurement, value) -> value * 4.546,
                value -> value / 4.546));
        volumes.add(new Unit(
                "quarts",
                "Quarts",
                "qt",
                (measurement, value) -> value * 1.136,
                value -> value / 1.136));
        volumes.add(new Unit(
                "pints",
                "Pints",
                "pt",
                (measurement, value) -> value * 1.76,
                value -> value / 1.76));
        volumes.add(new Unit(
                "cups",
                "Cups",
                "c",
                (measurement, value) -> value * 3.52,
                value -> value / 3.52));
        volumes.add(new Unit(
                "fluidounces",
                "Fluid Ounces",
                "fl oz",
                (measurement, value) -> value * 35.195,
                value -> value / 35.195));
        volumes.add(new Unit(
                "tablespoons",
                "Tablespoons",
                "tbsp",
                (measurement, value) -> value * 56.312,
                value -> value / 56.312));
        volumes.add(new Unit(
                "teaspoons",
                "Teaspoons",
                "tsp",
                (measurement, value) -> value * 168.936,
                value -> value / 168.936));
        volumes.add(new Unit(
                "cubicmeters",
                "Cubic Meters",
                "m³",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        volumes.add(new Unit(
                "cubiccentimeters",
                "Cubic Centimeters",
                "cm³",
                (measurement, value) -> value * 0.001,
                value -> value / 0.001));
        volumes.add(new Unit(
                "cubicinches",
                "Cubic Inches",
                "in³",
                (measurement, value) -> value * 61.024,
                value -> value / 61.024));
        volumes.add(new Unit(
                "cubicfeet",
                "Cubic Feet",
                "ft³",
                (measurement, value) -> value * 28.317,
                value -> value / 28.317));
        volumes.add(new Unit(
                "cubicyards",
                "Cubic Yards",
                "yd³",
                (measurement, value) -> value * 764.555,
                value -> value / 764.555));

        UNITS.put(volume, volumes);

        var kilograms = new BaseUnit(
                "kilograms",
                "Kilograms",
                "kg");
        var weight = new Measurement("weight", "Weight", kilograms);
        List<Unit> weights = new ArrayList<>();
        weights.add(kilograms);
        weights.add(new Unit(
                "grams",
                "Grams",
                "g",
                (measurement, value) -> value * 0.001,
                value -> value / 0.001));
        weights.add(new Unit(
                "milligrams",
                "Milligrams",
                "mg",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        weights.add(new Unit(
                "metrictons",
                "Metric Tons",
                "t",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        weights.add(new Unit(
                "imperialtons",
                "Imperial Tons",
                "lt",
                (measurement, value) -> value * 1016.047,
                value -> value / 1016.047));
        weights.add(new Unit(
                "ustons",
                "US Tons",
                "st",
                (measurement, value) -> value * 907.185,
                value -> value / 907.185));
        weights.add(new Unit(
                "pounds",
                "Pounds",
                "lb",
                (measurement, value) -> value * 2.205,
                value -> value / 2.205));
        weights.add(new Unit(
                "ounces",
                "Ounces",
                "oz",
                (measurement, value) -> value * 35.274,
                value -> value / 35.274));

        UNITS.put(weight, weights);

        var meterspersecond = new BaseUnit(
                "meterspersecond",
                "Meters Per Second",
                "m/s");
        var speed = new Measurement("speed", "Speed", meterspersecond);
        List<Unit> speeds = new ArrayList<>();
        speeds.add(meterspersecond);
        speeds.add(new Unit(
                "kilometersperhour",
                "Kilometers Per Hour",
                "km/h",
                (measurement, value) -> value * 3.6,
                value -> value / 3.6));
        speeds.add(new Unit(
                "milesperhour",
                "Miles Per Hour",
                "mph",
                (measurement, value) -> value * 2.237,
                value -> value / 2.237));
        speeds.add(new Unit(
                "knots",
                "Knots",
                "kn",
                (measurement, value) -> value * 1.944,
                value -> value / 1.944));
        speeds.add(new Unit(
                "feetpersecond",
                "Feet Per Second",
                "ft/s",
                (measurement, value) -> value * 3.281,
                value -> value / 3.281));
        speeds.add(new Unit(
                "mach",
                "Mach",
                "M",
                (measurement, value) -> value * 343,
                value -> value / 343));

        UNITS.put(speed, speeds);

        var seconds = new BaseUnit(
                "seconds",
                "Seconds",
                "s");
        var time = new Measurement("time", "Time", seconds);
        List<Unit> times = new ArrayList<>();
        times.add(seconds);
        times.add(new Unit(
                "nanoseconds",
                "Nanoseconds",
                "ns",
                (measurement, value) -> value * 1e+9,
                value -> value / 1e+9));
        times.add(new Unit(
                "microseconds",
                "Microseconds",
                "µs",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        times.add(new Unit(
                "milliseconds",
                "Milliseconds",
                "ms",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        times.add(new Unit(
                "minutes",
                "Minutes",
                "min",
                (measurement, value) -> value * 60,
                value -> value / 60));
        times.add(new Unit(
                "hours",
                "Hours",
                "h",
                (measurement, value) -> value * 3600,
                value -> value / 3600));
        times.add(new Unit(
                "days",
                "Days",
                "d",
                (measurement, value) -> value * 86400,
                value -> value / 86400));
        times.add(new Unit(
                "weeks",
                "Weeks",
                "wk",
                (measurement, value) -> value * 604800,
                value -> value / 604800));
        times.add(new Unit(
                "months",
                "Months",
                "mo",
                (measurement, value) -> value * 2.628e+6,
                value -> value / 2.628e+6));
        times.add(new Unit(
                "years",
                "Years",
                "yr",
                (measurement, value) -> value * 3.154e+7,
                value -> value / 3.154e+7));
        times.add(new Unit(
                "decades",
                "Decades",
                "dec",
                (measurement, value) -> value * 3.154e+8,
                value -> value / 3.154e+8));

        UNITS.put(time, times);

        var squaremeters = new BaseUnit(
                "squaremeters",
                "Square Meters",
                "m²");
        var area = new Measurement("area", "Area", squaremeters);
        List<Unit> areas = new ArrayList<>();
        areas.add(squaremeters);
        areas.add(new Unit(
                "squarekilometers",
                "Square Kilometers",
                "km²",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        areas.add(new Unit(
                "squarefeet",
                "Square Feet",
                "ft²",
                (measurement, value) -> value * 10.764,
                value -> value / 10.764));
        areas.add(new Unit(
                "squareyards",
                "Square Yards",
                "yd²",
                (measurement, value) -> value * 1.196,
                value -> value / 1.196));
        areas.add(new Unit(
                "squaremiles",
                "Square Miles",
                "mi²",
                (measurement, value) -> value * 2.59e+6,
                value -> value / 2.59e+6));
        areas.add(new Unit(
                "squareinches",
                "Square Inches",
                "in²",
                (measurement, value) -> value * 1550,
                value -> value / 1550));
        areas.add(new Unit(
                "acres",
                "Acres",
                "ac",
                (measurement, value) -> value * 4046.856,
                value -> value / 4046.856));
        areas.add(new Unit(
                "hectares",
                "Hectares",
                "ha",
                (measurement, value) -> value * 10000,
                value -> value / 10000));

        UNITS.put(area, areas);

        var pascals = new BaseUnit(
                "pascals",
                "Pascals",
                "Pa");
        var pressure = new Measurement("pressure", "Pressure", pascals);
        List<Unit> pressures = new ArrayList<>();
        pressures.add(pascals);
        pressures.add(new Unit(
                "bars",
                "Bars",
                "bar",
                (measurement, value) -> value * 100_000,
                value -> value / 100_000));
        pressures.add(new Unit(
                "poundspersquareinch",
                "Pounds Per Square Inch",
                "psi",
                (measurement, value) -> value * 6894.757,
                value -> value / 6894.757));
        pressures.add(new Unit(
                "atmospheres",
                "Atmospheres",
                "atm",
                (measurement, value) -> value * 101_325,
                value -> value / 101_325));
        pressures.add(new Unit(
                "torrs",
                "Torrs",
                "Torr",
                (measurement, value) -> value * 133.322,
                value -> value / 133.322));

        UNITS.put(pressure, pressures);

        var joules = new BaseUnit(
                "joules",
                "Joules",
                "J");
        var energy = new Measurement("energy", "Energy", joules);
        List<Unit> energies = new ArrayList<>();
        energies.add(joules);
        energies.add(new Unit(
                "kilojoules",
                "Kilojoules",
                "kJ",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        energies.add(new Unit(
                "gramcalories",
                "Gram Calories",
                "cal",
                (measurement, value) -> value * 4.184,
                value -> value / 4.184));
        energies.add(new Unit(
                "kilocalories",
                "Kilocalories",
                "kcal",
                (measurement, value) -> value * 4184,
                value -> value / 4184));
        energies.add(new Unit(
                "watt-hours",
                "Watt Hours",
                "Wh",
                (measurement, value) -> value * 3600,
                value -> value / 3600));
        energies.add(new Unit(
                "kilowatt-hours",
                "Kilowatt Hours",
                "kWh",
                (measurement, value) -> value * 3.6e+6,
                value -> value / 3.6e+6));
        energies.add(new Unit(
                "electronvolts",
                "Electronvolts",
                "eV",
                (measurement, value) -> value * 6.242e+18,
                value -> value / 6.242e+18));
        energies.add(new Unit(
                "britishthermalunit",
                "British Thermal Unit",
                "BTU",
                (measurement, value) -> value * 1055.056,
                value -> value / 1055.056));
        energies.add(new Unit(
                "usthermalunit",
                "US Thermal Unit",
                "BTU",
                (measurement, value) -> value * 1.055e+8,
                value -> value / 1.055e+8));
        energies.add(new Unit(
                "foot-pound",
                "Foot Pound",
                "ft·lb",
                (measurement, value) -> value * 1.356,
                value -> value / 1.356));

        UNITS.put(energy, energies);

        var watts = new BaseUnit(
                "watts",
                "Watts",
                "W");
        var power = new Measurement("power", "Power", watts);
        List<Unit> powers = new ArrayList<>();
        powers.add(watts);
        powers.add(new Unit(
                "kilowatts",
                "Kilowatts",
                "kW",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        powers.add(new Unit(
                "megawatts",
                "Megawatts",
                "MW",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        powers.add(new Unit(
                "gigawatts",
                "Gigawatts",
                "GW",
                (measurement, value) -> value * 1e+9,
                value -> value / 1e+9));
        powers.add(new Unit(
                "terawatts",
                "Terawatts",
                "TW",
                (measurement, value) -> value * 1e+12,
                value -> value / 1e+12));
        powers.add(new Unit(
                "horsepower",
                "Horsepower",
                "hp",
                (measurement, value) -> value * 745.7,
                value -> value / 745.7));
        powers.add(new Unit(
                "metrichorsepower",
                "Metric Horsepower",
                "hp(M)",
                (measurement, value) -> value * 735.498,
                value -> value / 735.498));

        UNITS.put(power, powers);

        var bits = new BaseUnit(
                "bits",
                "Bits",
                "b");
        var data = new Measurement("data", "Data", bits);
        List<Unit> datums = new ArrayList<>();
        datums.add(bits);
        datums.add(new Unit(
                "bytes",
                "Bytes",
                "B",
                (measurement, value) -> value * 8,
                value -> value / 8));
        datums.add(new Unit(
                "kilobits",
                "Kilobits",
                "kb",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        datums.add(new Unit(
                "kilobytes",
                "Kilobytes",
                "kB",
                (measurement, value) -> value * 8000,
                value -> value / 8000));
        datums.add(new Unit(
                "megabits",
                "Megabits",
                "Mb",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        datums.add(new Unit(
                "megabytes",
                "Megabytes",
                "MB",
                (measurement, value) -> value * 8e+6,
                value -> value / 8e+6));
        datums.add(new Unit(
                "gigabits",
                "Gigabits",
                "Gb",
                (measurement, value) -> value * 1e+9,
                value -> value / 1e+9));
        datums.add(new Unit(
                "gigabytes",
                "Gigabytes",
                "GB",
                (measurement, value) -> value * 8e+9,
                value -> value / 8e+9));
        datums.add(new Unit(
                "terabits",
                "Terabits",
                "Tb",
                (measurement, value) -> value * 1e+12,
                value -> value / 1e+12));
        datums.add(new Unit(
                "terabytes",
                "Terabytes",
                "TB",
                (measurement, value) -> value * 8e+12,
                value -> value / 8e+12));
        datums.add(new Unit(
                "petabits",
                "Petabits",
                "Pb",
                (measurement, value) -> value * 1e+15,
                value -> value / 1e+15));
        datums.add(new Unit(
                "petabytes",
                "Petabytes",
                "PB",
                (measurement, value) -> value * 8e+15,
                value -> value / 8e+15));
        datums.add(new Unit(
                "kibibits",
                "Kibibits",
                "Kib",
                (measurement, value) -> value * 1024,
                value -> value / 1024));
        datums.add(new Unit(
                "mebibits",
                "Mebibits",
                "Mib",
                (measurement, value) -> value * 1.049e+6,
                value -> value / 1.049e+6));
        datums.add(new Unit(
                "gibibits",
                "Gibibits",
                "Gib",
                (measurement, value) -> value * 1.074e+9,
                value -> value / 1.074e+9));
        datums.add(new Unit(
                "tebibits",
                "Tebibits",
                "Tib",
                (measurement, value) -> value * 1.1e+12,
                value -> value / 1.1e+12));
        datums.add(new Unit(
                "pebibits",
                "Pebibits",
                "Pib",
                (measurement, value) -> value * 1.126e+15,
                value -> value / 1.126e+15));
        datums.add(new Unit(
                "kibibytes",
                "Kibibytes",
                "KiB",
                (measurement, value) -> value * 8192,
                value -> value / 8192));
        datums.add(new Unit(
                "mebibytes",
                "Mebibytes",
                "MiB",
                (measurement, value) -> value * 8.389e+6,
                value -> value / 8.389e+6));
        datums.add(new Unit(
                "gibibytes",
                "Gibibytes",
                "GiB",
                (measurement, value) -> value * 8.59e+9,
                value -> value / 8.59e+9));
        datums.add(new Unit(
                "tebibytes",
                "Tebibytes",
                "TiB",
                (measurement, value) -> value * 8.796e+12,
                value -> value / 8.796e+12));
        datums.add(new Unit(
                "pebibytes",
                "Pebibytes",
                "PiB",
                (measurement, value) -> value * 9.007e+15,
                value -> value / 9.007e+15));

        UNITS.put(data, datums);

        var hertz = new BaseUnit(
                "hertz",
                "Hertz",
                "Hz");
        var frequency = new Measurement("frequency", "Frequency", hertz);
        List<Unit> frequencies = new ArrayList<>();
        frequencies.add(hertz);
        frequencies.add(new Unit(
                "kilohertz",
                "Kilohertz",
                "kHz",
                (measurement, value) -> value * 1000,
                value -> value / 1000));
        frequencies.add(new Unit(
                "megahertz",
                "Megahertz",
                "MHz",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        frequencies.add(new Unit(
                "gigahertz",
                "Gigahertz",
                "GHz",
                (measurement, value) -> value * 1e+9,
                value -> value / 1e+9));
        frequencies.add(new Unit(
                "terahertz",
                "Terahertz",
                "THz",
                (measurement, value) -> value * 1e+12,
                value -> value / 1e+12));
        frequencies.add(new Unit(
                "petahertz",
                "Petahertz",
                "PHz",
                (measurement, value) -> value * 1e+15,
                value -> value / 1e+15));
        frequencies.add(new Unit(
                "centihertz",
                "Centihertz",
                "cHz",
                (measurement, value) -> value * 0.01,
                value -> value / 0.01));
        frequencies.add(new Unit(
                "millihertz",
                "Millihertz",
                "mHz",
                (measurement, value) -> value * 0.001,
                value -> value / 0.001));
        frequencies.add(new Unit(
                "microhertz",
                "Microhertz",
                "µHz",
                (measurement, value) -> value * 1e+6,
                value -> value / 1e+6));
        frequencies.add(new Unit(
                "nanohertz",
                "Nanohertz",
                "nHz",
                (measurement, value) -> value * 1e+9,
                value -> value / 1e+9));

        UNITS.put(frequency, frequencies);

        var degrees = new BaseUnit(
                "degrees",
                "Degrees",
                "°");
        var angle = new Measurement("angle", "Angle", degrees);
        List<Unit> angles = new ArrayList<>();
        angles.add(degrees);
        angles.add(new Unit(
                "radians",
                "Radians",
                "rad",
                (measurement, value) -> value * 180 / Math.PI,
                value -> value * Math.PI / 180));
        angles.add(new Unit(
                "gradians",
                "Gradians",
                "grad",
                (measurement, value) -> value * 180 / 200,
                value -> value * 200 / 180));
        angles.add(new Unit(
                "arcseconds",
                "Arcseconds",
                "arcsec",
                (measurement, value) -> value * 3600,
                value -> value / 3600));
        angles.add(new Unit(
                "arcminutes",
                "Arcminutes",
                "arcmin",
                (measurement, value) -> value * 60,
                value -> value / 60));
        angles.add(new Unit(
                "milliradians",
                "Milliradians",
                "mrad",
                (measurement, value) -> value * 180 / (Math.PI * 1000),
                value -> value * Math.PI * 1000 / 180));

        UNITS.put(angle, angles);

        var newtonmeters = new BaseUnit(
                "newtonmeters",
                "Newton Meters",
                "N·m");
        var torque = new Measurement("torque", "Torque", newtonmeters);
        List<Unit> torques = new ArrayList<>();
        torques.add(newtonmeters);
        torques.add(new Unit(
                "footpounds",
                "Foot Pounds",
                "ft·lb",
                (measurement, value) -> value * 1.356,
                value -> value / 1.356));

        UNITS.put(torque, torques);

        var lux = new BaseUnit(
                "lux",
                "Lux",
                "lx");
        var illuminance = new Measurement("illuminance", "Illuminance", lux);
        List<Unit> illuminances = new ArrayList<>();
        illuminances.add(lux);
        illuminances.add(new Unit(
                "candelas",
                "Candelas",
                "cd",
                (measurement, value) -> value,
                value -> value));
        illuminances.add(new Unit(
                "footcandles",
                "Footcandles",
                "fc",
                (measurement, value) -> value * 10.764,
                value -> value / 10.764));

        UNITS.put(illuminance, illuminances);
    }

    public record Measurement(String name, String richName, BaseUnit baseUnit) {
    }

    public static class Unit {
        private final String name;
        private final String richName;
        private final String symbol;
        private final BiFunction<Measurement, Double, Double> toBase;
        private final Function<Double, Double> fromBase;

        public Unit(String name, String richName, String symbol, BiFunction<Measurement, Double, Double> toBase, Function<Double, Double> fromBase) {
            this.name = name;
            this.richName = richName;
            this.symbol = symbol;
            this.toBase = toBase;
            this.fromBase = fromBase;
        }

        public double toBase(Measurement measurement, double value) {
            return toBase.apply(measurement, value);
        }

        public double fromBase(double value) {
            return fromBase.apply(value);
        }
    }

    public static class BaseUnit extends Unit {
        public BaseUnit(String name, String richName, String symbol) {
            super(name, richName, symbol, (measurement, value) -> value, value -> value);
        }
    }

    public ConvertCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        List<SubcommandData> subcommands = new ArrayList<>();
        for (Measurement measurement : UNITS.keySet()) {
            SubcommandData subcommand = new SubcommandData(measurement.name, measurement.richName);
            subcommand.addOptions(
                    new OptionData(OptionType.STRING, "from", "The unit to convert from", true, true),
                    new OptionData(OptionType.STRING, "to", "The unit to convert to", true, true),
                    new OptionData(OptionType.NUMBER, "value", "The value to convert", true)
            );

            subcommands.add(subcommand);
        }

        subcommands.addAll(List.of(
                new SubcommandData("list", "Lists all the available units"),
                new SubcommandData("info", "Gets information about a unit").addOptions(
                        new OptionData(OptionType.STRING, "unit", "The unit to get information about", true, true)
                ),
                new SubcommandData("all", "Converts a value from one unit to all the other units").addOptions(
                        new OptionData(OptionType.STRING, "from", "The unit to convert from", true, true),
                        new OptionData(OptionType.NUMBER, "value", "The value to convert", true)
                )
        ));

        return subcommands;
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()) || event.getSubcommandName() == null)
            return;

        String subcommand = event.getSubcommandName();
        AutoCompleteQuery query = event.getFocusedOption();
        if (subcommand.equals("info")) {
            if (query.getName().equals("unit")) {
                replyUnits(event, query);
            }

            return;
        }

        if (subcommand.equals("all")) {
            if (query.getName().equals("from")) {
                replyUnits(event, query);
            }

            return;
        }

        Measurement measurement = UNITS.keySet().stream().filter(m -> m.name.equals(subcommand)).findFirst().orElse(null);
        if (measurement == null) {
            event.replyChoices().queue();
            return;
        }

        if (query.getName().equals("from")) {
            String typed = query.getValue();

            String to = event.getOption("to", null, OptionMapping::getAsString);
            List<String> units = UNITS.get(measurement)
                    .stream()
                    .map(u -> u.name)
                    .filter(u -> u.contains(typed))
                    .filter(u -> !u.equals(to))
                    .sorted((a, b) -> {
                        if (a.startsWith(typed) && !b.startsWith(typed))
                            return -1;
                        if (!a.startsWith(typed) && b.startsWith(typed))
                            return 1;
                        return a.compareTo(b);
                    })
                    .limit(25)
                    .toList();

            event.replyChoiceStrings(units).queue();
            return;
        }

        if (query.getName().equals("to")) {
            String typed = query.getValue();

            String from = event.getOption("from", null, OptionMapping::getAsString);
            List<String> units = UNITS.get(measurement)
                    .stream()
                    .map(u -> u.name)
                    .filter(u -> u.contains(typed))
                    .filter(u -> !u.equals(from))
                    .sorted((a, b) -> {
                        if (a.startsWith(typed) && !b.startsWith(typed))
                            return -1;
                        if (!a.startsWith(typed) && b.startsWith(typed))
                            return 1;
                        return a.compareTo(b);
                    })
                    .limit(25)
                    .toList();

            event.replyChoiceStrings(units).queue();
        }
    }

    private static void replyUnits(@NotNull CommandAutoCompleteInteractionEvent event, AutoCompleteQuery query) {
        String typed = query.getValue();
        List<String> units = UNITS.values()
                .stream()
                .flatMap(List::stream)
                .map(u -> u.name)
                .filter(u -> u.contains(typed))
                .sorted((a, b) -> {
                    if (a.startsWith(typed) && !b.startsWith(typed))
                        return -1;
                    if (!a.startsWith(typed) && b.startsWith(typed))
                        return 1;
                    return a.compareTo(b);
                })
                .limit(25)
                .toList();
        event.replyChoiceStrings(units).queue();
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Converts a value from one unit to another";
    }

    @Override
    public String getName() {
        return "convert";
    }

    @Override
    public String getRichName() {
        return "Convert";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ Please select a subcommand!", true);
            return;
        }

        handleSubcommandNoGroup(event, subcommand);
    }

    private static void handleSubcommandNoGroup(SlashCommandInteractionEvent event, String subcommand) {
        switch (subcommand) {
            case "list" -> {
                var embed = new EmbedBuilder()
                        .setTitle("Units")
                        .setColor(Color.GREEN)
                        .setTimestamp(Instant.now())
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

                for (Measurement measurement : UNITS.keySet()) {
                    StringBuilder builder = new StringBuilder();
                    for (Unit unit : UNITS.get(measurement)) {
                        builder.append(unit.richName).append(" (").append(unit.symbol).append(")\n");
                    }

                    embed.addField(measurement.richName(), builder.toString(), true);
                }

                reply(event, embed, false);
                return;
            }
            case "info" -> {
                String unitStr = event.getOption("unit", null, OptionMapping::getAsString);
                if (unitStr == null) {
                    reply(event, "❌ Please specify a unit!", true);
                    return;
                }

                Unit unit = UNITS.values().stream().flatMap(List::stream).filter(u -> u.name.equalsIgnoreCase(unitStr)).findFirst().orElse(null);
                if (unit == null) {
                    reply(event, "❌ Please specify a valid unit!", true);
                    return;
                }

                reply(event, "**" + unit.richName + "** (" + unit.symbol + ")\n\n" +
                        "Name: " + unit.name + "\n" +
                        "Symbol: " + unit.symbol, false);
                return;
            }
            case "all" -> {
                String fromStr = event.getOption("from", null, OptionMapping::getAsString);
                double value = event.getOption("value", null, OptionMapping::getAsDouble);

                if (fromStr == null) {
                    reply(event, "❌ Please specify the unit!", true);
                    return;
                }

                Unit from = UNITS.values().stream().flatMap(List::stream).filter(u -> u.name.equalsIgnoreCase(fromStr)).findFirst().orElse(null);

                if (from == null) {
                    reply(event, "❌ Please specify a valid unit!", true);
                    return;
                }

                Measurement measurement;
                if (from instanceof BaseUnit) {
                    measurement = UNITS.keySet().stream().filter(m -> m.baseUnit.equals(from)).findFirst().orElse(null);
                } else {
                    measurement = UNITS.keySet().stream().filter(m -> UNITS.get(m).contains(from)).findFirst().orElse(null);
                }

                if (measurement == null) {
                    reply(event, "❌ You must supply a valid measurement unit!", true);
                    return;
                }

                double base = from.toBase(measurement, value);

                var embed = new EmbedBuilder()
                        .setTitle(value + " " + from.richName + " (" + from.symbol + ") converted to all other " + measurement.richName())
                        .setColor(Color.GREEN)
                        .setTimestamp(Instant.now())
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

                for (Unit unit : UNITS.get(measurement).stream().filter(u -> !u.equals(from)).toList()) {
                    double converted = unit.fromBase(base);

                    double rounded = roundToNDecimalPlaces(converted, 3);
                    String formatted;
                    if (rounded == (long) rounded) {
                        formatted = String.format("%d", (long) rounded) + unit.symbol;
                    } else {
                        formatted = rounded + unit.symbol;
                    }

                    embed.addField(unit.richName, formatted, true);
                }

                reply(event, embed, false);
                return;
            }
        }

        Measurement measurement = UNITS.keySet().stream().filter(m -> m.name.equals(subcommand)).findFirst().orElse(null);
        if (measurement != null) {
            String fromStr = event.getOption("from", null, OptionMapping::getAsString);
            String toStr = event.getOption("to", null, OptionMapping::getAsString);
            double value = event.getOption("value", null, OptionMapping::getAsDouble);

            if (fromStr == null || toStr == null) {
                reply(event, "❌ Please specify the units!", true);
                return;
            }

            Unit from = UNITS.get(measurement).stream().filter(u -> u.name.equalsIgnoreCase(fromStr)).findFirst().orElse(null);
            Unit to = UNITS.get(measurement).stream().filter(u -> u.name.equalsIgnoreCase(toStr)).findFirst().orElse(null);

            if (from == null || to == null) {
                reply(event, "❌ Please specify valid units!", true);
                return;
            }

            double base = from.toBase(measurement, value);
            double converted = to.fromBase(base);

            double rounded = roundToNDecimalPlaces(converted, 3);
            String formatted;
            if (rounded == (long) rounded) {
                formatted = String.format("%d", (long) rounded) + to.symbol;
            } else {
                formatted = rounded + to.symbol;
            }

            reply(event, "**" + from.richName + "** (" + from.symbol + ") to **" + to.richName + "** (" + to.symbol + ")\n\n" +
                    "Value: " + value + "\n" +
                    "Converted: " + formatted, false);
            return;
        }

        reply(event, "❌ Please select a valid measurement!", true);
    }

    public static double roundToNDecimalPlaces(double number, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative.");
        }

        BigDecimal bigDecimal = new BigDecimal(Double.toString(number));
        bigDecimal = bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}
