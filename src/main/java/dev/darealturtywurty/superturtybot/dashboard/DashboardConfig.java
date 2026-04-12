package dev.darealturtywurty.superturtybot.dashboard;

import dev.darealturtywurty.superturtybot.Environment;

import java.util.Arrays;
import java.util.List;

public record DashboardConfig(
        boolean enabled,
        String host,
        int port,
        String publicUrl,
        String apiKey,
        List<String> allowedOrigins
) {
    private static final int DEFAULT_PORT = 7070;
    private static final String DEFAULT_HOST = "0.0.0.0";

    public static DashboardConfig fromEnvironment() {
        Environment environment = Environment.INSTANCE;

        List<String> allowedOrigins = environment.dashboardAllowedOrigins()
                .stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();

        if (allowedOrigins.isEmpty() && environment.isDevelopment()) {
            allowedOrigins = List.of(
                    "http://localhost:3003",
                    "http://127.0.0.1:3003"
            );
        }

        String publicUrl = environment.dashboardPublicUrl()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);

        String apiKey = environment.dashboardApiKey()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);

        return new DashboardConfig(
                environment.dashboardEnabled().orElse(false),
                environment.dashboardHost().orElse(DEFAULT_HOST),
                environment.dashboardPort().orElse(DEFAULT_PORT),
                publicUrl,
                apiKey,
                allowedOrigins
        );
    }

    public boolean hasApiKey() {
        return this.apiKey != null && !this.apiKey.isBlank();
    }

    public boolean isOriginAllowed(String origin) {
        if (origin == null || origin.isBlank())
            return false;

        return this.allowedOrigins.stream().anyMatch(origin::equalsIgnoreCase);
    }

    public String bindAddress() {
        return this.host + ":" + this.port;
    }
}
