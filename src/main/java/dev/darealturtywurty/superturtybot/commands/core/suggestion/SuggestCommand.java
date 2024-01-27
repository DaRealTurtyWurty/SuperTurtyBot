package dev.darealturtywurty.superturtybot.commands.core.suggestion;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SuggestCommand extends CoreCommand {
    public SuggestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("add", "Adds a suggestion to the suggestion channel").addOptions(
                        new OptionData(OptionType.STRING, "suggestion", "The thing that you want to suggest", true),
                        new OptionData(OptionType.STRING, "media_url", "A media URL that you would like to add to your suggestion", false)
                ),
                new SubcommandData("approve", "Approves a suggestion").addOptions(
                        new OptionData(OptionType.INTEGER, "suggestion_number", "The number of the suggestion you want to approve", true),
                        new OptionData(OptionType.STRING, "reason", "The reason for approving the suggestion", false)
                ),
                new SubcommandData("deny", "Denies a suggestion").addOptions(
                        new OptionData(OptionType.INTEGER, "suggestion_number", "The number of the suggestion you want to deny", true),
                        new OptionData(OptionType.STRING, "reason", "The reason for denying the suggestion", false)
                ),
                new SubcommandData("consider", "Considers a suggestion").addOptions(
                        new OptionData(OptionType.INTEGER, "suggestion_number", "The number of the suggestion you want to consider", true),
                        new OptionData(OptionType.STRING, "reason", "The reason for considering the suggestion", false)
                ),
                new SubcommandData("delete", "Deletes a suggestion").addOptions(
                        new OptionData(OptionType.INTEGER, "suggestion_number", "The number of the suggestion you want to delete", true),
                        new OptionData(OptionType.STRING, "reason", "The reason for deleting the suggestion", false)
                )
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Allows you to suggest something (e.g. a bot feature/improvement) to the server! Also allows you to approve, deny, consider, and delete suggestions!";
    }
    
    @Override
    public String getHowToUse() {
        return """
                /suggest add <suggestion> [media_url]
                /suggest approve <suggestion_number> [reason]
                /suggest deny <suggestion_number> [reason]
                /suggest consider <suggestion_number> [reason]
                /suggest delete <suggestion_number> [reason]""";
    }
    
    @Override
    public String getName() {
        return "suggest";
    }
    
    @Override
    public String getRichName() {
        return "Suggest";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }
        
        final TextChannel suggestionChannel = SuggestionManager.getSuggestionChannel(event);
        if (suggestionChannel == null)
            return;

        String subcommand = event.getSubcommandName();
        if(subcommand == null || subcommand.isBlank()) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        if(!"add".equalsIgnoreCase(subcommand)) {
            // check permission
            if(!event.getMember().hasPermission(suggestionChannel, Permission.MANAGE_CHANNEL)) {
                reply(event, "❌ You do not have permission to use this command!", false, true);
                return;
            }
        }

        switch (subcommand) {
            case "add" -> runAddSuggestion(event, suggestionChannel);
            case "approve" -> runApproveSuggestion(event, suggestionChannel);
            case "deny" -> runDenySuggestion(event, suggestionChannel);
            case "consider" -> runConsiderSuggestion(event, suggestionChannel);
            case "delete" -> runDeleteSuggestion(event, suggestionChannel);
            default -> reply(event, "❌ You must specify a valid subcommand!", false, true);
        }
    }

    private void runAddSuggestion(SlashCommandInteractionEvent event, TextChannel suggestionChannel) {
        final String suggestionStr = event.getOption("suggestion", null, OptionMapping::getAsString);
        if (suggestionStr == null || suggestionStr.isBlank()) {
            reply(event, "You must provide something to suggest!", false, true);
            return;
        }

        event.deferReply(false).mentionRepliedUser(false).queue();
        final String mediaURL = event.getOption("media_url", null, OptionMapping::getAsString);
        final CompletableFuture<Suggestion> suggestion = SuggestionManager.addSuggestion(suggestionChannel,
                event.getGuild(), event.getMember(), suggestionStr, mediaURL);
        suggestion.thenAccept(sug -> {
            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setColor(sug != null ? Color.GREEN : Color.RED);
            embed.setTitle(
                    sug != null ? "✅ Suggestion successfully added!" : "❌ There was an issue adding this suggestion!",
                    sug != null
                            ? "https://discord.com/channels/" + event.getGuild().getIdLong() + "/"
                            + suggestionChannel.getIdLong() + "/" + sug.getMessage()
                            : "");
            embed.setFooter("Created by: " + event.getUser().getName(), event.getMember().getEffectiveAvatarUrl());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void runApproveSuggestion(SlashCommandInteractionEvent event, TextChannel suggestionChannel) {
        final int suggestionNumber = event.getOption("suggestion_number", 0, OptionMapping::getAsInt);
        final String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);

        final CompletableFuture<Suggestion> suggestion = SuggestionManager.respondSuggestion(event.getGuild(),
                suggestionChannel, event.getMember(), suggestionNumber, reason, SuggestionResponse.Type.APPROVED);
        if(suggestion == null) {
            reply(event, "❌ You must supply a valid suggestion number!", false, true);
            return;
        }

        suggestion.thenAccept(sug -> {
            if (sug == null) {
                reply(event, "❌ You must supply a valid suggestion number!", false, true);
                return;
            }

            User user = event.getJDA().getUserById(sug.getUser());
            if (user == null) {
                reply(event, "❌ There was an issue retrieving the user who suggested this!", false, true);
                return;
            }

            user.openPrivateChannel().queue(channel -> {
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.GREEN);
                embed.setTitle("✅" + event.getUser().getName() + " has approved your suggestion!",
                        "https://discord.com/channels/" + event.getGuild().getIdLong() + "/" + suggestionChannel.getIdLong()
                                + "/" + sug.getMessage());
                embed.setDescription(reason);
                embed.setFooter(event.getUser().getName(), event.getMember().getEffectiveAvatarUrl());

                channel.sendMessageEmbeds(embed.build()).queue();
            }, throwable -> {});

            reply(event, "✅ Successfully approved suggestion #" + suggestionNumber + "!");
        });
    }

    private void runDenySuggestion(SlashCommandInteractionEvent event, TextChannel suggestionChannel) {
        int suggestionNumber = event.getOption("suggestion_number", 0, OptionMapping::getAsInt);
        String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);

        CompletableFuture<Suggestion> suggestion = SuggestionManager.respondSuggestion(event.getGuild(),
                suggestionChannel, event.getMember(), suggestionNumber, reason, SuggestionResponse.Type.DENIED);
        if(suggestion == null) {
            reply(event, "❌ You must supply a valid suggestion number!", false, true);
            return;
        }

        suggestion.thenAccept(sug -> {
            if (sug == null) {
                reply(event, "❌ You must supply a valid suggestion number!", false, true);
                return;
            }

            User user = event.getJDA().getUserById(sug.getUser());
            if (user == null) {
                reply(event, "❌ There was an issue retrieving the user who suggested this!", false, true);
                return;
            }

            user.openPrivateChannel().queue(channel -> {
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.RED);
                embed.setTitle("❌" + event.getUser().getName() + " has denied your suggestion!",
                        "https://discord.com/channels/" + event.getGuild().getIdLong() + "/" + suggestionChannel.getIdLong()
                                + "/" + sug.getMessage());
                embed.setDescription(reason);
                embed.setFooter(event.getUser().getName(), event.getMember().getEffectiveAvatarUrl());

                channel.sendMessageEmbeds(embed.build()).queue();
            }, throwable -> {});

            reply(event, "✅ Successfully denied suggestion #" + suggestionNumber + "!");
        });
    }

    private void runConsiderSuggestion(SlashCommandInteractionEvent event, TextChannel suggestionChannel) {
        int suggestionNumber = event.getOption("suggestion_number", 0, OptionMapping::getAsInt);
        String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);

        CompletableFuture<Suggestion> suggestion = SuggestionManager.respondSuggestion(event.getGuild(),
                suggestionChannel, event.getMember(), suggestionNumber, reason, SuggestionResponse.Type.CONSIDERED);

        if(suggestion == null) {
            reply(event, "❌ You must supply a valid suggestion number!", false, true);
            return;
        }

        suggestion.thenAccept(sug -> {
            if (sug == null) {
                reply(event, "❌ You must supply a valid suggestion number!", false, true);
                return;
            }

            User user = event.getJDA().getUserById(sug.getUser());
            if (user == null) {
                reply(event, "❌ There was an issue retrieving the user who suggested this!", false, true);
                return;
            }

            user.openPrivateChannel().queue(channel -> {
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.YELLOW);
                embed.setTitle("⚠" + event.getUser().getName() + " has considered your suggestion!",
                        "https://discord.com/channels/" + event.getGuild().getIdLong() + "/" + suggestionChannel.getIdLong()
                                + "/" + sug.getMessage());
                embed.setDescription(reason);
                embed.setFooter(event.getUser().getName(), event.getMember().getEffectiveAvatarUrl());

                channel.sendMessageEmbeds(embed.build()).queue();
            }, throwable -> {});

            reply(event, "✅ Successfully considered suggestion #" + suggestionNumber + "!");
        });
    }

    private void runDeleteSuggestion(SlashCommandInteractionEvent event, TextChannel suggestionChannel) {
        int suggestionNumber = event.getOption("suggestion_number", 0, OptionMapping::getAsInt);

        CompletableFuture<Suggestion> suggestion = SuggestionManager.deleteSuggestion(event.getGuild(),
                suggestionChannel, event.getMember(), suggestionNumber);

        suggestion.thenAccept(sug -> {
            if(sug == null) {
                reply(event, "❌ You must supply a valid suggestion number!", false, true);
                return;
            }

            reply(event, "✅ Successfully deleted suggestion #" + suggestionNumber + "!");
        });
    }
}
