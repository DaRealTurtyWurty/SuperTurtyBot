package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModmailTranscriptEntry {
    private long messageId;
    private long authorId;
    private String authorTag;
    private boolean bot;
    private String content;
    private List<String> attachments = new ArrayList<>();
    private List<String> embeds = new ArrayList<>();
    private List<String> stickers = new ArrayList<>();
    private long createdAt;
    private long editedAt;
}
