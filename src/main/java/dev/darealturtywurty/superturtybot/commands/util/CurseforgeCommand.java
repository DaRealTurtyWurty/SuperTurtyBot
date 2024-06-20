package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import io.github.matyrobbrt.curseforgeapi.CurseForgeAPI;
import io.github.matyrobbrt.curseforgeapi.request.AsyncRequest;
import io.github.matyrobbrt.curseforgeapi.request.Response;
import io.github.matyrobbrt.curseforgeapi.request.helper.AsyncRequestHelper;
import io.github.matyrobbrt.curseforgeapi.request.helper.RequestHelper;
import io.github.matyrobbrt.curseforgeapi.request.query.ModSearchQuery;
import io.github.matyrobbrt.curseforgeapi.schemas.Category;
import io.github.matyrobbrt.curseforgeapi.schemas.file.File;
import io.github.matyrobbrt.curseforgeapi.schemas.game.Game;
import io.github.matyrobbrt.curseforgeapi.schemas.mod.Mod;
import io.github.matyrobbrt.curseforgeapi.schemas.mod.ModAuthor;
import io.github.matyrobbrt.curseforgeapi.util.CurseForgeException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CurseforgeCommand extends CoreCommand {
    private static CurseForgeAPI CURSE_FORGE_API;
    private static final Map<String, Game> GAMES = new HashMap<>();
    private static final Map<Game, Set<Category>> CATEGORIES = new HashMap<>();

    static {
        Environment.INSTANCE.curseforgeKey().ifPresent(apiKey -> {
            try {
                CURSE_FORGE_API = CurseForgeAPI.builder()
                        .apiKey(apiKey)
                        .build();
            } catch (LoginException exception) {
                throw new IllegalStateException("Failed to login to curseforge!", exception);
            }

            RequestHelper helper = CURSE_FORGE_API.getHelper();
            try {
                Response<List<Game>> games = helper.getGames();
                for (Game game : games.get()) {
                    GAMES.put(game.name().toUpperCase(Locale.ROOT), game);
                }
            } catch (CurseForgeException exception) {
                throw new IllegalStateException("Failed to get games from curseforge!", exception);
            }

            for (Game game : GAMES.values()) {
                CATEGORIES.computeIfAbsent(game, key -> {
                    try {
                        return new HashSet<>(helper.getCategories(game.id()).get());
                    } catch (CurseForgeException exception) {
                        throw new IllegalStateException("Failed to get categories from curseforge!", exception);
                    }
                });
            }
        });
    }

    public CurseforgeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "game", "The game to search for", true, true),
                new OptionData(OptionType.STRING, "search", "The search query", true),
                new OptionData(OptionType.STRING, "type", "The type of project to search for", false, true),
                new OptionData(OptionType.STRING, "category", "The category to search for", false, true),
                new OptionData(OptionType.STRING, "game-version", "The game version to search for", false, true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Get stats about a curseforge project";
    }
    
    @Override
    public String getHowToUse() {
        return "/curseforge <game> <search> [type] [category] [game-version]";
    }
    
    @Override
    public String getName() {
        return "curseforge";
    }
    
    @Override
    public String getRichName() {
        return "Curseforge Project";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (Environment.INSTANCE.curseforgeKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.error("CurseForge API key has not been set!");
            return;
        }

        String game = event.getOption("game", null, OptionMapping::getAsString);
        String search = event.getOption("search", null, OptionMapping::getAsString);
        String type = event.getOption("type", null, OptionMapping::getAsString);
        String category = event.getOption("category", null, OptionMapping::getAsString);

        if(game == null || search == null) {
            reply(event, "❌ You must provide a game and search query!", false, true);
            return;
        }

        // Check if game exists
        if(!GAMES.containsKey(game.toUpperCase(Locale.ROOT))) {
            reply(event, "❌ That game does not exist!", false, true);
            return;
        }

        Game gameObj = GAMES.get(game.toUpperCase(Locale.ROOT));

        // Check if type exists
        int classId = -1;
        if(type != null) {
            try {
                classId = Integer.parseInt(type);
            } catch (NumberFormatException exception) {
                reply(event, "❌ That type does not exist!", false, true);
                return;
            }
        }

        // Check if category exists
        Category categoryObj = null;
        if(category != null) {
            try {
                categoryObj = CATEGORIES.get(gameObj)
                        .stream()
                        .filter(cat -> cat.name().equalsIgnoreCase(category))
                        .findFirst()
                        .orElse(null);
            } catch (NoSuchElementException exception) {
                reply(event, "❌ That category does not exist!", false, true);
                return;
            }
        }

        if (categoryObj == null && classId != -1) {
            reply(event, "❌ That type does not exist!", false, true);
            return;
        }

        event.deferReply().queue();

        AsyncRequestHelper helper = CURSE_FORGE_API.getAsyncHelper();
        try {
            ModSearchQuery query = ModSearchQuery.of(gameObj)
                    .sortField(ModSearchQuery.SortField.NAME)
                    .searchFilter(search);
            if(categoryObj != null) {
                query.category(categoryObj);
            }

            if(classId != -1) {
                query.classId(classId);
            }

            AsyncRequest<Response<List<Mod>>> request = helper.searchMods(query);
            request.queue(response -> response.ifPresentOrElse(mods -> {
                if (mods.isEmpty()) {
                    event.getHook().editOriginal("❌ No projects found!").mentionRepliedUser(false).queue();
                    return;
                }

                if(mods.size() == 1) {
                    EmbedBuilder embed = createEmbed(mods.getFirst());
                    event.getHook().editOriginalEmbeds(embed.build()).mentionRepliedUser(false).queue();
                    return;
                }

                var contentsBuilder = new PaginatedEmbed.ContentsBuilder();
                for (Mod mod : mods) {
                    contentsBuilder.field(mod.name(), mod.links().websiteUrl());
                }

                PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contentsBuilder)
                        .title("Search Results for " + search)
                        .footer("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                        .timestamp(Instant.now())
                        .color(Color.BLUE)
                        .authorOnly(event.getUser().getIdLong())
                        .build(event.getJDA());

                embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ No projects found!").mentionRepliedUser(false).queue());

                embed.setOnMessageUpdate(message -> {
                    // create select menu
                    List<LayoutComponent> components = new ArrayList<>(message.getComponents());

                    // get a list of the current page's fields
                    List<Mod> currentMods = mods.subList(embed.getPage() * 10, Math.min(mods.size(), (embed.getPage() + 1) * 10));

                    //noinspection DataFlowIssue
                    StringSelectMenu menu = StringSelectMenu.create("curseforge-%d-%d-%d-%d".formatted(
                                    event.isFromGuild() ? event.getGuild().getIdLong() : 0,
                                    event.getChannel().getIdLong(),
                                    message.getIdLong(),
                                    event.getUser().getIdLong()))
                            .setPlaceholder("Select a Mod")
                            .addOptions(currentMods.stream().map(mod -> SelectOption.of(mod.name(), String.valueOf(mod.id()))).toList())
                            .setRequiredRange(1, 1)
                            .build();

                    components.add(ActionRow.of(menu));
                    message.editMessageComponents(components).queue();
                });

                TurtyBot.EVENT_WAITER.builder(StringSelectInteractionEvent.class)
                        .condition(interactionEvent -> interactionEvent.getComponentId().startsWith("curseforge-"))
                        .success(event1 -> {
                            String componentId = event1.getComponentId();
                            String[] split = componentId.split("-");

                            long guildId = Long.parseLong(split[1]);
                            long channelId = Long.parseLong(split[2]);
                            long messageId = Long.parseLong(split[3]);
                            long userId = Long.parseLong(split[4]);

                            Guild guild = event1.getGuild();
                            if (guildId == 0 && guild != null) return;
                            else if (guildId != 0 && guild != null && guild.getIdLong() != guildId) return;
                            else if(event1.getChannel().getIdLong() != channelId) return;
                            else if(event1.getMessageIdLong() != messageId) return;
                            else if(event1.getUser().getIdLong() != userId) {
                                event1.deferEdit().queue();
                                return;
                            }

                            int value = Integer.parseInt(event1.getSelectedOptions().getFirst().getValue());
                            Mod mod = mods.stream().filter(mod1 -> mod1.id() == value).findFirst().orElse(null);
                            if(mod == null) {
                                event1.deferEdit().queue();
                                return;
                            }

                            EmbedBuilder embed1 = createEmbed(mod);
                            event1.deferEdit().setComponents().setEmbeds(embed1.build()).queue();

                            embed.finish();
                        })
                        .failure(() -> {})
                        .build();
            }, () -> event.getHook().editOriginal("❌ No projects found!").mentionRepliedUser(false).queue()));
        } catch (CurseForgeException exception) {
            event.getHook().editOriginal("❌ Failed to search for projects!").mentionRepliedUser(false).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if(!event.getName().equals(getName())) return;

        String focusedValue = event.getFocusedOption().getValue();
        switch (event.getFocusedOption().getName()) {
            case "game" -> event.replyChoiceStrings(
                    GAMES.keySet()
                            .stream()
                            .filter(game -> containsIgnoreCase(game, focusedValue))
                            .limit(25)
                            .toList()
                ).queue(unused -> {}, throwable -> {});

            case "type" -> {
                Game typeGame = GAMES.get(event.getOption("game", null, OptionMapping::getAsString).toUpperCase(Locale.ROOT));
                if (typeGame == null) {
                    event.replyChoices(List.of()).queue(unused -> {}, throwable -> {});
                    return;
                }

                Set<Category> typeCategories = CATEGORIES.get(typeGame);
                if (typeCategories == null || typeCategories.isEmpty()) {
                    event.replyChoices(List.of()).queue(unused -> {}, throwable -> {});
                    return;
                }

                event.replyChoices(
                        typeCategories.stream()
                                .filter(Category::isClass)
                                .filter(category -> containsIgnoreCase(category.name(), focusedValue))
                                .limit(25)
                                .map(category -> new Command.Choice(category.name(), category.id()))
                                .toList()
                        ).queue(unused -> {}, throwable -> {});
            }

            case "category" -> {
                Game categoryGame = GAMES.get(event.getOption("game", null, OptionMapping::getAsString).toUpperCase(Locale.ROOT));
                if (categoryGame == null) {
                    event.replyChoices(List.of()).queue(unused -> {}, throwable -> {});
                    return;
                }

                Set<Category> categoryCategories = CATEGORIES.get(categoryGame);
                if (categoryCategories == null || categoryCategories.isEmpty()) {
                    event.replyChoices(List.of()).queue(unused -> {}, throwable -> {});
                    return;
                }

                event.replyChoiceStrings(
                        categoryCategories.stream()
                                .map(Category::name)
                                .filter(name -> containsIgnoreCase(name, focusedValue))
                                .limit(25)
                                .toList()
                ).queue(unused -> {}, throwable -> {});
            }
            default -> event.replyChoices(List.of()).queue(unused -> {}, throwable -> {});
        }
    }

    private EmbedBuilder createEmbed(Mod mod) {
        var embed = new EmbedBuilder();
        embed.setTitle(mod.name());
        embed.setThumbnail(mod.logo().url());
        embed.setDescription(mod.summary());
        embed.setUrl(mod.links().websiteUrl());
        embed.setTimestamp(Instant.now());

        embed.addField("Downloads:", String.format("%,d", (long) mod.downloadCount()), true);
        embed.addField("Featured:", StringUtils.trueFalseToYesNo(mod.isFeatured()), true);

        String status = WordUtils.capitalize(
                mod.status()
                .name()
                .toLowerCase(Locale.ROOT)
                .replace("_", " ")
        );
        embed.addField("Status:", status, true);

        embed.addField("Author:", mod.authors().stream().map(ModAuthor::name).collect(Collectors.joining(", ")), true);
        embed.addField("Categories:", mod.categories().stream().map(Category::name).collect(Collectors.joining(", ")), true);

        Set<String> versions = new HashSet<>();
        for (File file : mod.latestFiles()) {
            versions.addAll(file.gameVersions());
        }

        embed.addField("Versions:", String.join(", ", versions), true);

        embed.addField("Created:", TimeFormat.DATE_SHORT.format(mod.getDateCreatedAsInstant()), true);
        embed.addField("Released:", TimeFormat.DATE_SHORT.format(mod.getDateReleasedAsInstant()), true);
        embed.addField("Updated:", TimeFormat.DATE_SHORT.format(mod.getDateModifiedAsInstant()), true);

        embed.setImage(!mod.screenshots().isEmpty() ? mod.screenshots().getFirst().url() : null);

        return embed;
    }

    private static boolean containsIgnoreCase(String string, String search) {
        return string.toLowerCase().contains(search.toLowerCase());
    }
}
