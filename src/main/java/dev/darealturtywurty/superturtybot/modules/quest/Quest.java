package dev.darealturtywurty.superturtybot.modules.quest;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

@Data
public class Quest<C, S> implements Registerable {
    private String id;
    private final String displayName;
    private final String description;

    private final C config;
    private final QuestRule<C, S> rule;

    private final QuestReward reward;
    private final BiPredicate<Guild, Member> isAvailableFor;
    private final Set<String> conflictGroups = new HashSet<>();

    public Quest(String id, String displayName, String description, C config, QuestRule<C, S> rule,
                 QuestReward reward, BiPredicate<Guild, Member> isAvailableFor) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.config = config;
        this.rule = rule;
        this.reward = reward;
        this.isAvailableFor = isAvailableFor;
    }

    public Quest<C, S> addConflictGroups(String... groups) {
        Collections.addAll(this.conflictGroups, groups);
        return this;
    }

    public boolean conflictsWith(Quest<?, ?> other) {
        return !Collections.disjoint(this.conflictGroups, other.conflictGroups);
    }

    @Override
    public Registerable setName(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return this.id;
    }

    public boolean isAvailableFor(Guild guild, Member member) {
        return this.isAvailableFor.test(guild, member);
    }

    public QuestStatus evaluate(List<QuestEvent> events) {
        S state = this.rule.createState();
        for (QuestEvent event : events) {
            this.rule.apply(config, state, event);
        }

        return rule.status(config, state);
    }
}
