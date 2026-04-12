package dev.darealturtywurty.superturtybot.dashboard.service.economy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EconomySettingsRequest {
    private String economyCurrency;
    private boolean economyEnabled;
    private boolean donateEnabled;
    private String defaultEconomyBalance;
    private float incomeTax;
}
