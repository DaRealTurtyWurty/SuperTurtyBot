package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version.Download;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version.VersionPackage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MojmapMappings {
    private final Map<String, ClassMapping> classMappings = new HashMap<>();

    public static @Nullable MojmapMappings getMojmapMappings(VersionPackage versionPackage) {
        MojmapMappings clientMappings = getClientMappings(versionPackage);
        MojmapMappings serverMappings = getServerMappings(versionPackage);
        return merge(clientMappings, serverMappings);
    }

    public static MojmapMappings getClientMappings(VersionPackage versionPackage) {
        Download clientMappings = versionPackage.downloads().clientMappings();
        if (clientMappings == null)
            return null;

        Path path = Path.of("versions", versionPackage.id());
        if(Files.notExists(path.resolve("mojmap-client.txt"))) {
            clientMappings.downloadToPath(path, "mojmap-client.txt");
        }

        var mappings = new MojmapMappings();
        mappings.parse(path.resolve("mojmap-client.txt"));
        return mappings;
    }

    public static MojmapMappings getServerMappings(VersionPackage versionPackage) {
        Download serverMappings = versionPackage.downloads().serverMappings();
        if (serverMappings == null)
            return null;

        Path path = Path.of("versions", versionPackage.id());
        if(Files.notExists(path.resolve("mojmap-server.txt"))) {
            serverMappings.downloadToPath(path, "mojmap-server.txt");
        }

        var mappings = new MojmapMappings();
        mappings.parse(path.resolve("mojmap-server.txt"));
        return mappings;
    }

    public static MojmapMappings merge(@Nullable MojmapMappings clientMappings, @Nullable MojmapMappings serverMappings) {
        if (clientMappings == null && serverMappings == null)
            return null;

        if (clientMappings == null)
            return serverMappings;

        if (serverMappings == null)
            return clientMappings;

        var merged = new MojmapMappings();
        merged.classMappings.putAll(clientMappings.classMappings);
        merged.classMappings.putAll(serverMappings.classMappings);
        return merged;
    }

    public void parse(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            parse(lines.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file, e);
        }
    }

    public void parse(String[] lines) {
        ClassMapping currentClass = null;

        for (String line : lines) {
            if(line.isBlank() || line.trim().startsWith("#")) {
                continue; // Skip empty lines and comments
            }

            if(!line.startsWith(" ")) { // Class mapping
                String[] parts = line.trim().split(" -> ");
                if(parts.length != 2)
                    continue;

                String mappedName = parts[0].trim();
                String obfuscatedName = parts[1].replace(":", "").trim();
                var classMapping = new ClassMapping(mappedName, obfuscatedName);
                classMappings.put(obfuscatedName, classMapping);
                currentClass = classMapping;
            } else if (currentClass != null) { // Field or method mapping
                String memberLine = line.trim();
                if(memberLine.contains(":")) {
                    parseMethodMapping(memberLine, currentClass);
                } else {
                    parseFieldMapping(memberLine, currentClass);
                }
            }
        }
    }

    private void parseFieldMapping(String line, ClassMapping classMapping) {
        String[] parts = line.split(" -> ");
        if(parts.length != 2)
            return;

        String typeAndName = parts[0].trim();
        String[] typeAndNameParts = typeAndName.split(" ");
        if(typeAndNameParts.length != 2)
            return;
        String type = typeAndNameParts[0].trim();
        String name = typeAndNameParts[1].trim();
        String mappedName = parts[1].trim();
        classMapping.addFieldMapping(new FieldMapping(name, mappedName, type));
    }

    private void parseMethodMapping(String line, ClassMapping classMapping) {
        String[] parts = line.split(" -> ");
        if(parts.length != 2)
            return;

        String[] lineColumnAndMethod = parts[0].trim().split(":");
        if(lineColumnAndMethod.length != 3)
            return;

        String lineNumberStr = lineColumnAndMethod[0].trim();
        String columnNumberStr = lineColumnAndMethod[1].trim();

        String methodAndParams = lineColumnAndMethod[2].trim();
        int parenthesisIndex = methodAndParams.indexOf('(');
        if(parenthesisIndex == -1)
            return;

        String beforeParenthesis = methodAndParams.substring(0, parenthesisIndex);
        String[] methodAndReturnType = beforeParenthesis.split(" ");
        if(methodAndReturnType.length != 2)
            return;

        String returnType = methodAndReturnType[0].trim();
        String methodName = methodAndReturnType[1].trim();

        String paramTypesStr = methodAndParams.substring(parenthesisIndex + 1, methodAndParams.length() - 1);
        String[] paramTypesSplit = paramTypesStr.split(",");
        List<String> paramTypes = new ArrayList<>();
        for (String paramType : paramTypesSplit) {
            paramTypes.add(paramType.trim());
        }

        String obfuscatedName = parts[1].trim();
        int lineNumber = Integer.parseInt(lineNumberStr);
        int columnNumber = Integer.parseInt(columnNumberStr);
        classMapping.addMethodMapping(new MethodMapping(methodName, paramTypes, returnType, obfuscatedName, lineNumber, columnNumber));
    }

    public ClassMapping findClassMapping(String mapping) {
        return classMappings.values().stream()
                .filter(classMapping -> classMapping.getMappedName().equals(mapping))
                .findFirst()
                .orElse(null);
    }

    public List<MethodMapping> findMethodMappings(String mapping) {
        List<MethodMapping> methodMappings = new ArrayList<>();
        for (ClassMapping classMapping : classMappings.values()) {
            List<MethodMapping> classMethodMappings = classMapping.findMethodMappings(mapping);
            if (classMethodMappings != null) {
                methodMappings.addAll(classMethodMappings);
            }
        }

        return methodMappings;
    }

    public List<FieldMapping> findFieldMappings(String mapping) {
        List<FieldMapping> fieldMappings = new ArrayList<>();
        for (ClassMapping classMapping : classMappings.values()) {
            FieldMapping classFieldMapping = classMapping.findFieldMapping(mapping);
            if (classFieldMapping != null) {
                fieldMappings.add(classFieldMapping);
            }
        }

        return fieldMappings;
    }
}
