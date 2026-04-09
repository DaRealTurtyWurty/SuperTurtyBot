package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StickyRoles {
    private long guild;
    private long user;
    private List<Long> roles = new ArrayList<>();
    private long savedAt;
}
