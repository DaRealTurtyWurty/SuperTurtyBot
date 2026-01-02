package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class ClassMapping {
    private final String mappedName;
    private final String obfuscatedName;
    private final List<FieldMapping> fieldMappings = new ArrayList<>();
    private final List<MethodMapping> methodMappings = new ArrayList<>();

    public ClassMapping(String mappedName, String obfuscatedName) {
        this.mappedName = mappedName;
        this.obfuscatedName = obfuscatedName;
    }

    @Override
    public String toString() {
        return "ClassMapping{" +
                "mappedName='" + mappedName + '\'' +
                ", obfuscatedName='" + obfuscatedName + '\'' +
                ", fieldMappings=" + fieldMappings +
                ", methodMappings=" + methodMappings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClassMapping that = (ClassMapping) o;
        return Objects.equals(mappedName, that.mappedName) && Objects.equals(obfuscatedName, that.obfuscatedName) && Objects.equals(fieldMappings, that.fieldMappings) && Objects.equals(methodMappings, that.methodMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappedName, obfuscatedName, fieldMappings, methodMappings);
    }

    public void addFieldMapping(FieldMapping fieldMapping) {
        fieldMappings.add(fieldMapping);
    }

    public String getFieldMapping(String mappedFieldName) {
        return fieldMappings.stream()
                .filter(fieldMapping -> fieldMapping.mappedName().equals(mappedFieldName))
                .map(FieldMapping::obfuscatedName)
                .findFirst()
                .orElse(null);
    }

    public void addMethodMapping(MethodMapping methodMapping) {
        methodMappings.add(methodMapping);
    }

    public List<MethodMapping> getMethodMapping(String mappedMethodName) {
        List<MethodMapping> matchingMethods = new ArrayList<>();
        for (MethodMapping methodMapping : methodMappings) {
            if (methodMapping.mappedName().equals(mappedMethodName)) {
                matchingMethods.add(methodMapping);
            }
        }

        return matchingMethods;
    }

    public List<MethodMapping> findMethodMappings(String mapping) {
        List<MethodMapping> matchingMethods = new ArrayList<>();
        for (MethodMapping methodMapping : methodMappings) {
            if (methodMapping.mappedName().equals(mapping)) {
                matchingMethods.add(methodMapping);
            }
        }

        return matchingMethods;
    }

    public FieldMapping findFieldMapping(String mapping) {
        for (FieldMapping fieldMapping : fieldMappings) {
            if (fieldMapping.mappedName().equals(mapping)) {
                return fieldMapping;
            }
        }

        return null;
    }
}
