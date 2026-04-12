package dev.darealturtywurty.superturtybot.dashboard.service.birthday;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BirthdaySettingsRequest {
    private String birthdayChannelId;
    private boolean announceBirthdays;
}
