package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.ModmailTranscriptEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModmailTranscriptChunk {
    private long guild;
    private long ticketChannel;
    private int chunkIndex;
    private List<ModmailTranscriptEntry> entries = new ArrayList<>();
}
