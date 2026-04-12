package dev.darealturtywurty.superturtybot.dashboard.service.chat_revival;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ChatRevivalSettingsRequest {
    private boolean chatRevivalEnabled;
    private String chatRevivalChannelId;
    private int chatRevivalTime;
    private List<String> chatRevivalTypes = new ArrayList<>();
    private boolean chatRevivalAllowNsfw;
}
