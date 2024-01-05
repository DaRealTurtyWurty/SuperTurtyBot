package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;

public class PollCommand extends CoreCommand {
    private static final Map<Integer, String> NUMBER_EMOTE_MAP = new HashMap<>();

    static {
        NUMBER_EMOTE_MAP.put(0, "0️⃣");
        NUMBER_EMOTE_MAP.put(1, "1️⃣");
        NUMBER_EMOTE_MAP.put(2, "2️⃣");
        NUMBER_EMOTE_MAP.put(3, "3️⃣");
        NUMBER_EMOTE_MAP.put(4, "4️⃣");
        NUMBER_EMOTE_MAP.put(5, "5️⃣");
        NUMBER_EMOTE_MAP.put(6, "6️⃣");
        NUMBER_EMOTE_MAP.put(7, "7️⃣");
        NUMBER_EMOTE_MAP.put(8, "8️⃣");
        NUMBER_EMOTE_MAP.put(9, "9️⃣");
        NUMBER_EMOTE_MAP.put(10, "🔟");
    }

    public PollCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "question", "The question that you want to ask", true),
            new OptionData(OptionType.STRING, "option1", "Option 1", true),
            new OptionData(OptionType.STRING, "option2", "Option 2", true),
            new OptionData(OptionType.STRING, "option3", "Option 3", false),
            new OptionData(OptionType.STRING, "option4", "Option 4", false),
            new OptionData(OptionType.STRING, "option5", "Option 5", false),
            new OptionData(OptionType.STRING, "option6", "Option 6", false),
            new OptionData(OptionType.STRING, "option7", "Option 7", false),
            new OptionData(OptionType.STRING, "option8", "Option 8", false),
            new OptionData(OptionType.STRING, "option9", "Option 9", false),
            new OptionData(OptionType.STRING, "option10", "Option 10", false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Creates a poll";
    }

    @Override
    public String getHowToUse() {
        return "/poll [question] [option1] [option2]\n/poll [question] [option1] [option2] [option3]\n/poll [question] [option1] [option2] [option3] [option4-10]";
    }

    @Override
    public String getName() {
        return "poll";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String question = event.getOption("question", null, OptionMapping::getAsString);
        if (question == null) {
            event.deferReply(true).setContent("❌ You must provide a question to ask!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final String[] options = new String[10];
        options[0] = event.getOption("option1", null, OptionMapping::getAsString);
        options[1] = event.getOption("option2", null, OptionMapping::getAsString);
        if (options[0] == null || options[1] == null) {
            event.deferReply(true).setContent("❌ You must provide at least 2 options!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final List<String> extraOptions = new ArrayList<>();
        final String option3 = event.getOption("option3", "", OptionMapping::getAsString);
        final String option4 = event.getOption("option4", "", OptionMapping::getAsString);
        final String option5 = event.getOption("option5", "", OptionMapping::getAsString);
        final String option6 = event.getOption("option6", "", OptionMapping::getAsString);
        final String option7 = event.getOption("option7", "", OptionMapping::getAsString);
        final String option8 = event.getOption("option8", "", OptionMapping::getAsString);
        final String option9 = event.getOption("option9", "", OptionMapping::getAsString);
        final String option10 = event.getOption("option10", "", OptionMapping::getAsString);
        Collections.addAll(extraOptions, option3, option4, option5, option6, option7, option8, option9, option10);

        int optionIndex = 2;
        for (final var option : extraOptions) {
            if (!option.trim().isBlank()) {
                options[optionIndex++] = option;
            }
        }

        final Pair<EmbedBuilder, List<String>> embedAndOptions = createEmbed(event.getUser(), question, options);
        event.getChannel().sendMessageEmbeds(embedAndOptions.getKey().build()).queue(msg -> {
            embedAndOptions.getValue().forEach(emote -> msg.addReaction(Emoji.fromUnicode(emote)).queue());

            event.deferReply(true).setContent("Successfully created poll!").mentionRepliedUser(false).queue();
        });
    }

    private static Pair<EmbedBuilder, List<String>> createEmbed(User author, String question, String... options) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.CYAN);
        embed.setFooter(author.getName(), author.getEffectiveAvatarUrl());
        embed.setTitle(question);

        final List<String> emotes = new ArrayList<>();
        int optionIndex = 1;
        for (final String option : options) {
            if (option == null || option.isBlank()) {
                continue;
            }

            final String emote = NUMBER_EMOTE_MAP.get(optionIndex++);
            embed.appendDescription(emote + " " + option + "\n");
            emotes.add(emote);
        }

        return Pair.of(embed, emotes);
    }
}
