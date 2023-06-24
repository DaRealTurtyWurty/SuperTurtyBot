package dev.darealturtywurty.superturtybot.commands.core.config;

import java.awt.Color;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.CaseFormat;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.Checks;

public class GuildConfigOption implements Registerable {
    private String name;
    private final DataType dataType;
    private final BiConsumer<GuildConfig, String> serializer;
    private final BiPredicate<SlashCommandInteractionEvent, String> validator;
    private final Function<GuildConfig, Object> valueFromConfig;
    
    private GuildConfigOption(Builder builder) {
        this.dataType = builder.dataType;
        this.serializer = builder.serializer;
        this.validator = builder.validator;
        this.valueFromConfig = builder.valueFromConfig;
    }

    public DataType getDataType() {
        return this.dataType;
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
    
    public Function<GuildConfig, Object> getValueFromConfig() {
        return this.valueFromConfig;
    }
    
    public void serialize(GuildConfig config, String value) {
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
        private BiConsumer<GuildConfig, String> serializer = (config, str) -> {
        };
        private BiPredicate<SlashCommandInteractionEvent, String> validator = (dataType, str) -> true;
        private Function<GuildConfig, Object> valueFromConfig = config -> null;

        public GuildConfigOption build() {
            return new GuildConfigOption(this);
        }

        public Builder dataType(@NotNull DataType dataType) {
            Checks.notNull(dataType, "dataType");
            this.dataType = dataType;
            return this;
        }

        public Builder serializer(@NotNull BiConsumer<GuildConfig, String> serializer) {
            Checks.notNull(serializer, "serializer");
            this.serializer = serializer;
            return this;
        }

        public Builder validator(@NotNull BiPredicate<SlashCommandInteractionEvent, String> validator) {
            Checks.notNull(validator, "validator");
            this.validator = validator;
            return this;
        }

        public Builder valueFromConfig(@NotNull Function<GuildConfig, Object> valueFromConfig) {
            Checks.notNull(valueFromConfig, "valueFromConfig");
            this.valueFromConfig = valueFromConfig;
            return this;
        }
    }
    
    public enum DataType {
        INTEGER(str -> Ints.tryParse(str) != null), DOUBLE(str -> Doubles.tryParse(str) != null),
        FLOAT(str -> Floats.tryParse(str) != null),
        BOOLEAN(str -> "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)), STRING(str -> true),
        LONG(str -> Longs.tryParse(str) != null), COLOR(str -> {
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
