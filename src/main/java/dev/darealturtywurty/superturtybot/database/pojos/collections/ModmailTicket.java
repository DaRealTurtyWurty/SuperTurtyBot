package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModmailTicket {
    private long guild;
    private long user;
    private long channel;
    private long category;
    private long ticketNumber;
    private boolean open;
    private String source;
    private String openerMessage;
    private long openedAt;
    private long closedAt;
    private long closedBy;
    private String closeReason;
    private int transcriptChunkCount;
    private int transcriptMessageCount;

    public ModmailTicket(long guild, long user, long channel, long category, long ticketNumber, String source, String openerMessage) {
        this.guild = guild;
        this.user = user;
        this.channel = channel;
        this.category = category;
        this.ticketNumber = ticketNumber;
        this.open = true;
        this.source = source;
        this.openerMessage = openerMessage;
        this.openedAt = System.currentTimeMillis();
        this.closedAt = 0L;
        this.closedBy = 0L;
        this.closeReason = "";
        this.transcriptChunkCount = 0;
        this.transcriptMessageCount = 0;
    }
}
