package dev.darealturtywurty.superturtybot.dashboard.service.counting;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CountingChannelUpsertRequest {
    private String channelId;
    private String mode;
    private Integer maxCountingSuccession;
}
