package dev.darealturtywurty.superturtybot.commands.core.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandPermission {
    private String command;
    private List<Permission> permissions = new ArrayList<>();

    public CommandPermission(String command) {
        this.command = command;
    }

    @Data
    public static class Permission {
        private long role;
        private String permission;

        public Permission(long role, String permission) {
            this.role = role;
            this.permission = permission;
        }
    }
}
