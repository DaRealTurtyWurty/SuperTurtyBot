package dev.darealturtywurty.superturtybot.commands.fun.tod.data;

public record TODResult(String question, TODType type) {
    public static String calculateId(TODPack pack, String question) {
        return String.valueOf(pack.hashCode() + question.hashCode());
    }
}
