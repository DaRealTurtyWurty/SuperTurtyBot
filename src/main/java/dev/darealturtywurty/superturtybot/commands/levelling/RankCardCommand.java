package dev.darealturtywurty.superturtybot.commands.levelling;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RankCardCommand extends CoreCommand {
    public RankCardCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.LEVELLING;
    }

    @Override
    public String getName() {
        return "rankcard";
    }

    @Override
    public String getRichName() {
        return "Rank Card";
    }

    @Override
    public String getDescription() {
        return "Equip an earned rank card item, or reset a slot by leaving item empty.";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public String getHowToUse() {
        return "/rankcard slot:<slot> item:<earned item>\n/rankcard slot:<slot> (reset to its default color)";
    }

    @Override
    public List<OptionData> createOptions() {
        var slot = new OptionData(OptionType.STRING, "slot", "The part of your card to customize", true);
        for (RankCardItem.Type type : RankCardItem.Type.values()) {
            slot.addChoice(type.displayName, type.directory);
        }
        return List.of(slot, new OptionData(OptionType.STRING, "item", "An owned item; leave empty to reset this slot")
            .setAutoComplete(true));
    }

    private static RankCardItem.Type findType(String value) {
        return Arrays.stream(RankCardItem.Type.values()).filter(type -> type.directory.equals(value))
            .findFirst().orElse(null);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()) || !event.getFocusedOption().getName().equals("item"))
            return;
        if (event.getGuild() == null) {
            event.replyChoices().queue();
            return;
        }
        Levelling profile = Database.getDatabase().levelling.find(Filters.and(
            Filters.eq("guild", event.getGuild().getIdLong()), Filters.eq("user", event.getUser().getIdLong())))
            .first();
        if (profile == null) {
            event.replyChoices().queue();
            return;
        }
        RankCardItem.Type type = findType(event.getOption("slot", "", OptionMapping::getAsString));
        String search = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        event.replyChoices(profile.getInventory().stream().distinct().map(RankCardItemRegistry.RANK_CARD_ITEMS::get)
            .filter(Objects::nonNull).filter(item -> type == null || item.type == type)
            .filter(item -> item.getName().contains(search) || item.data.contains(search)
                || item.getDisplayName().toLowerCase(Locale.ROOT).contains(search))
            .sorted(Comparator.comparing(RankCardItem::getName)).limit(25)
            .map(item -> new Command.Choice(item.getDisplayName() + " (" + item.rarity.saveName + ")", item.getName()))
            .toList()).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        if (!LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
            reply(event, "❌ Levelling has been disabled for this server!", false, true);
            return;
        }
        RankCardItem.Type type = findType(event.getOption("slot", "", OptionMapping::getAsString));
        if (type == null) {
            reply(event, "❌ Choose a valid card slot.", false, true);
            return;
        }
        event.deferReply(true).queue();
        Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getUser().getIdLong()));
        Levelling profile = Database.getDatabase().levelling.find(filter).first();
        if (profile == null) {
            profile = new Levelling(event.getGuild().getIdLong(), event.getUser().getIdLong());
            Database.getDatabase().levelling.insertOne(profile);
        }
        String name = event.getOption("item", "", OptionMapping::getAsString).trim().toLowerCase(Locale.ROOT);
        RankCardItem item = RankCardItemRegistry.RANK_CARD_ITEMS.get(name);
        if (!name.isEmpty() && (item == null || item.type != type || !profile.getInventory().contains(name))) {
            event.getHook()
                .editOriginal("❌ Choose an item you own for this slot. Use `/xpinventory` to see your items.")
                .queue();
            return;
        }
        Database.getDatabase().levelling.updateOne(Filters.and(filter, Filters.eq("rankCard", null)),
            Updates.set("rankCard", profile.getRankCard()));
        Database.getDatabase().levelling.updateOne(filter,
            Updates.set("rankCard." + type.cardField, item == null ? "" : item.data));
        event.getHook().editOriginal(item == null
            ? "✅ Reset " + type.displayName + " to its default color. View your card with `/rank`."
            : "✅ Equipped **" + item.getDisplayName() + "** in " + type.displayName + ". View your card with `/rank`.")
            .queue();
    }
}
