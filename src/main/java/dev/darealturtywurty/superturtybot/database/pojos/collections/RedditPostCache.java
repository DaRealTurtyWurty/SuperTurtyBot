package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedditPostCache {
    private String subreddit;
    private String guid;
    private long createdAt;
}
