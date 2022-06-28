package io.github.darealturtywurty.superturtybot.commands.fun;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CoinFlipCommand extends CoreCommand {
    public CoinFlipCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "choice", "Whether you are choosing heads or tails", false)
            .addChoice("heads", "heads").addChoice("tails", "tails"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Flips a coin";
    }

    @Override
    public String getName() {
        return "coinflip";
    }

    @Override
    public String getRichName() {
        return "Coin Flip";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final OptionMapping choice = event.getOption("choice");
        if (choice == null) {
            if (ThreadLocalRandom.current().nextInt(1000) == 69) {
                event.deferReply().setContent("It landed on it's side. It was neither heads or tails! 😔")
                    .mentionRepliedUser(false).queue();
            } else {
                event.deferReply()
                    .setContent(
                        "It was: " + (ThreadLocalRandom.current().nextBoolean() ? "Heads 🗣" : "Tails 🐍") + "!")
                    .mentionRepliedUser(false).queue();
            }
        } else {
            String choiceStr = choice.getAsString();
            if (!choiceStr.contains("head") && !choiceStr.contains("tail") && !choiceStr.contains("side")) {
                event.deferReply(true).setContent("You must supply either `heads` or `tails`!")
                    .mentionRepliedUser(false).queue();
                return;
            }

            if (choiceStr.contains("head")) {
                choiceStr = "heads";
            } else if (choiceStr.contains("tail")) {
                choiceStr = "tails";
            } else {
                choiceStr = "side";
            }

            String botChoice = "";
            if (ThreadLocalRandom.current().nextInt(1000) == 69) {
                botChoice = "side";
            } else {
                botChoice = ThreadLocalRandom.current().nextBoolean() ? "heads" : "tails";
            }

            String reply = "";
            if (botChoice.equalsIgnoreCase(choiceStr)) {
                if (choiceStr.contains("head")) {
                    reply = "You were correct! It was Heads 🗣.";
                } else if (choiceStr.contains("tail")) {
                    reply = "You were correct! It was Tails 🐍.";
                } else {
                    reply = "You were correct! It landed on it's side 😲.";
                }
            } else if (botChoice.contains("head")) {
                reply = "You were incorrect! It was Heads 🗣.";
            } else if (botChoice.contains("tail")) {
                reply = "You were incorrect! It was Tails 🐍.";
            } else {
                reply = "You were incorrect! It landed on it's side 😲.";
            }

            event.deferReply().setContent("You chose `" + choiceStr + "`. " + reply).mentionRepliedUser(false).queue();
        }
    }
}
