package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingChannel;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingData;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingData.Result;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingService;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MinecraftVersions;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TranslateCommand extends SubcommandCommand {
    public TranslateCommand() {
        super("translate", "Translate a Minecraft class, method, or field between mapping channels");
        addOption(OptionType.STRING, "version", "The Minecraft version", true, true);
        addOption(MappingCommandUtils.channelOption("from", "The mapping channel to translate from"));
        addOption(MappingCommandUtils.channelOption("to", "The mapping channel to translate to"));
        addOption(OptionType.STRING, "mapping", "Class or member name, e.g. Minecraft or Minecraft#getInstance", true,
            true);
        addOption(new OptionData(OptionType.STRING, "side", "The game side (defaults to client)")
            .addChoice("Client", "client").addChoice("Server", "server"));
        addOption(OptionType.BOOLEAN, "ignore_classes", "Exclude classes");
        addOption(OptionType.BOOLEAN, "ignore_methods", "Exclude methods");
        addOption(OptionType.BOOLEAN, "ignore_fields", "Exclude fields");
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!"mappings".equals(event.getName()) || !getName().equals(event.getSubcommandName()))
            return;

        String value = event.getFocusedOption().getValue();
        CompletableFuture<List<String>> choices;
        if ("version".equals(event.getFocusedOption().getName())) {
            choices = CompletableFuture.supplyAsync(() -> MinecraftVersions.getPistonVersions().stream()
                .filter(version -> version.toLowerCase(Locale.ROOT).startsWith(value.toLowerCase(Locale.ROOT)))
                .limit(25)
                .toList(), MappingService.EXECUTOR);
        } else if ("mapping".equals(event.getFocusedOption().getName())) {
            String version = event.getOption("version", "", OptionMapping::getAsString);
            String channel = event.getOption("from", "", OptionMapping::getAsString);
            if (version.isBlank() || channel.isBlank()) {
                event.replyChoices().queue();
                return;
            }

            try {
                MappingChannel from = MappingChannel.fromName(channel);
                String side = event.getOption("side", "client", OptionMapping::getAsString);
                boolean classes = !event.getOption("ignore_classes", false, OptionMapping::getAsBoolean);
                boolean methods = !event.getOption("ignore_methods", false, OptionMapping::getAsBoolean);
                boolean fields = !event.getOption("ignore_fields", false, OptionMapping::getAsBoolean);
                choices = MappingService.get(version, side, from, from)
                    .thenApplyAsync(data -> data.suggest(value, from, classes, methods, fields),
                        MappingService.EXECUTOR);
            } catch (IllegalArgumentException exception) {
                event.replyChoices().queue();
                return;
            }
        } else {
            event.replyChoices().queue();
            return;
        }

        choices.completeOnTimeout(List.of(), 1500, TimeUnit.MILLISECONDS)
            .exceptionally(exception -> List.of())
            .thenAccept(results -> event.replyChoiceStrings(results).queue());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String version = event.getOption("version", "", OptionMapping::getAsString).trim();
        String query = event.getOption("mapping", "", OptionMapping::getAsString).trim();
        if (version.isEmpty() || query.isEmpty()) {
            reply(event, "❌ Please provide a Minecraft version and a mapping to look up.", false, true);
            return;
        }

        MappingChannel from;
        MappingChannel to;
        try {
            from = MappingChannel.fromName(event.getOption("from", "", OptionMapping::getAsString));
            to = MappingChannel.fromName(event.getOption("to", "", OptionMapping::getAsString));
            if (from == to)
                throw new IllegalArgumentException("Choose two different mapping channels.");
        } catch (IllegalArgumentException exception) {
            reply(event, "❌ " + exception.getMessage(), false, true);
            return;
        }

        boolean classes = !event.getOption("ignore_classes", false, OptionMapping::getAsBoolean);
        boolean methods = !event.getOption("ignore_methods", false, OptionMapping::getAsBoolean);
        boolean fields = !event.getOption("ignore_fields", false, OptionMapping::getAsBoolean);
        if (!classes && !methods && !fields) {
            reply(event, "❌ Include at least one of classes, methods, or fields.", false, true);
            return;
        }

        String side = event.getOption("side", "client", OptionMapping::getAsString);
        event.deferReply().queue(hook -> MappingService.get(version, side, from, to)
            .whenCompleteAsync((data, error) -> {
                if (error != null) {
                    MappingCommandUtils.reportError(hook, error);
                    return;
                }

                try {
                    List<Result> results = data.search(query, from, classes, methods, fields);
                    if (!results.isEmpty() && results.getFirst().rank() == 0) {
                        results = results.stream().filter(result -> result.rank() == 0).toList();
                    }

                    sendResults(hook, data, results, from, to);
                } catch (Exception exception) {
                    MappingCommandUtils.reportError(hook, exception);
                }
            }, MappingService.EXECUTOR));
    }

    private void sendResults(
        InteractionHook hook,
        MappingData data,
        List<Result> results,
        MappingChannel from,
        MappingChannel to
    ) {
        if (results.isEmpty()) {
            hook.editOriginal("❌ No mappings found. Try a simple class name or Class#member.").queue();
            return;
        }

        int source = data.namespace(from);
        int target = data.namespace(to);
        var output = new StringBuilder();
        int count = 0;
        int missing = 0;
        for (Result result : results) {
            if (!result.hasName(target)) {
                missing++;
                continue;
            }

            if (count++ >= 1000)
                continue;

            output.append(result.format(source));
            output.append("\n  -> ").append(result.format(target));

            output.append("\n\n");
        }

        if (count == 0) {
            hook.editOriginal("❌ Matches were found, but they have no names in " + to.displayName() + ".").queue();
            return;
        }

        String header = "Minecraft " + data.version() + " (" + data.side() + ") · " + from.displayName()
            + " → " + to.displayName() + " · " + count + " result(s)";
        for (var build : data.builds().entrySet()) {
            header += "\n" + build.getKey().displayName() + " build: " + build.getValue();
        }
        if (missing > 0) {
            header += "\n" + missing + " match(es) have no name in " + to.displayName() + ".";
        }
        if (count > 1000) {
            header += "\nShowing the first 1,000 results. Use a class or full method signature to narrow the search.";
        }

        MappingCommandUtils.sendResponse(hook, header, output.toString());
    }
}
