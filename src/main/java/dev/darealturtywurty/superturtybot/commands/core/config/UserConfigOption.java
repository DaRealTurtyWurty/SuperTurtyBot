package dev.darealturtywurty.superturtybot.commands.core.config;

import com.google.common.base.CaseFormat;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.Checks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class UserConfigOption implements Registerable {
    private String name;
    @Getter
    private final DataType dataType;
    private final BiConsumer<UserConfig, String> serializer;
    private final BiPredicate<SlashCommandInteractionEvent, String> validator;
    @Getter
    private final Function<UserConfig, Object> valueFromConfig;
    @Getter
    private final Function<String, List<Pair<String, String>>> autoComplete;

    private UserConfigOption(Builder builder) {
        this.dataType = builder.dataType;
        this.serializer = builder.serializer;
        this.validator = builder.validator;
        this.valueFromConfig = builder.valueFromConfig;
        this.autoComplete = builder.autoComplete;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getRichName() {
        return WordUtils.capitalize(this.name.replace("_", " "));
    }

    public String getSaveName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name);
    }

    public void serialize(UserConfig config, String value) {
        this.serializer.accept(config, value);
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    public boolean validate(SlashCommandInteractionEvent event, String value) {
        return this.validator.test(event, value);
    }
    
    public static class Builder {
        private DataType dataType = DataType.STRING;
        private BiConsumer<UserConfig, String> serializer = (config, str) -> {
        };
        private BiPredicate<SlashCommandInteractionEvent, String> validator = (dataType, str) -> true;
        private Function<UserConfig, Object> valueFromConfig = config -> null;
        private Function<String, List<Pair<String, String>>> autoComplete = str -> new ArrayList<>();
        
        public UserConfigOption build() {
            return new UserConfigOption(this);
        }
        
        public Builder dataType(@NotNull DataType dataType) {
            Checks.notNull(dataType, "dataType");
            this.dataType = dataType;
            return this;
        }
        
        public Builder serializer(@NotNull BiConsumer<UserConfig, String> serializer) {
            Checks.notNull(serializer, "serializer");
            this.serializer = serializer;
            return this;
        }
        
        public Builder validator(@NotNull BiPredicate<SlashCommandInteractionEvent, String> validator) {
            Checks.notNull(validator, "validator");
            this.validator = validator;
            return this;
        }
        
        public Builder valueFromConfig(@NotNull Function<UserConfig, Object> valueFromConfig) {
            Checks.notNull(valueFromConfig, "valueFromConfig");
            this.valueFromConfig = valueFromConfig;
            return this;
        }

        public Builder autoComplete(@NotNull Function<String, List<Pair<String, String>>> autoComplete) {
            Checks.notNull(autoComplete, "autoComplete");
            this.autoComplete = autoComplete;
            return this;
        }

        public Builder enumAutoComplete(@NotNull Enum<?>[] values) {
            Checks.notNull(values, "values");
            this.autoComplete = str -> {
                List<Pair<String, String>> options = new ArrayList<>();
                for (Enum<?> value : values) {
                    String name = value.name().toLowerCase();
                    if (!name.startsWith(str.toLowerCase()))
                        continue;

                    options.add(Pair.of(name.substring(0, 1).toUpperCase() + name.substring(1), name.toUpperCase()));
                }

                return options;
            };
            return this;
        }

        public Builder autoCompleteColors() {
            this.autoComplete = str -> {
                List<Pair<String, String>> completions = new ArrayList<>();
                for (Map.Entry<String, String> color : UserConfig.COLOR_HEX_MAP.entrySet()) {
                    completions.add(Pair.of(color.getKey(), color.getValue()));
                }

                return completions;
            };
            return this;
        }
    }

    public enum DataType {
        INTEGER(str -> Ints.tryParse(str) != null),
        DOUBLE(str -> Doubles.tryParse(str) != null),
        FLOAT(str -> Floats.tryParse(str) != null),
        BOOLEAN(str -> "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)),
        STRING(str -> true),
        LONG(str -> Longs.tryParse(str) != null),
        COLOR(str -> {
            try {
                Color.decode(str);
                return true;
            } catch (final NumberFormatException exception) {
                return false;
            }
        });

        public final Function<String, Boolean> validator;

        DataType(Function<String, Boolean> validator) {
            this.validator = validator;
        }
    }
}
