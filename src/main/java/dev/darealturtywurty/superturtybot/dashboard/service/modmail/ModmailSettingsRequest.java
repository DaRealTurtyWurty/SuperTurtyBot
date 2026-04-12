package dev.darealturtywurty.superturtybot.dashboard.service.modmail;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ModmailSettingsRequest {
    private List<String> moderatorRoleIds = new ArrayList<>();
    private String ticketCreatedMessage = "";
}
