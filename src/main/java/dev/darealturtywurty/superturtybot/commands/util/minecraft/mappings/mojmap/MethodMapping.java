package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap;

import java.util.List;

public record MethodMapping(String mappedName, List<String> paramTypes, String returnType, String obfuscatedName, int line, int column) {
}
