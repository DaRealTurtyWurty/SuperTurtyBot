package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EconomyTransactionHistoryCommand extends SubcommandCommand {
    public EconomyTransactionHistoryCommand() {
        super("economy", "Shows the economy transaction history of a user.");
        addOption(OptionType.USER, "user", "The user to get the transaction history of.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ This command can only be used in servers!", false, true);
            return;
        }

        event.deferReply().queue();

        Optional<Economy> optAccount = EconomyManager.getAccount(guild, user);
        if (optAccount.isEmpty()) {
            event.getHook().sendMessage("❌ That user does not have an account!").mentionRepliedUser(false).queue();
            return;
        }

        Economy account = optAccount.get();
        if (account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot view your transaction history! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        if (account.getTransactions().size() < 2) {
            event.getHook().sendMessage("❌ That user has no transactions!").mentionRepliedUser(false).queue();
            return;
        }

        GuildData config = GuildData.getOrCreateGuildData(guild);
        List<MoneyTransaction> transactions = account.getTransactions()
                .stream()
                .sorted(Comparator.comparingLong(MoneyTransaction::timestamp))
                .toList();

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (int index = 0; index < transactions.size(); index++) {
            MoneyTransaction transaction = transactions.get(index);
            String typeName = MoneyTransaction.getTypeName(transaction.type());
            String amount = StringUtils.numberFormat(transaction.amount(), config);
            String targetLine = transaction.targetId() == null ? "" : "\nTarget: <@" + transaction.targetId() + ">";
            contents.field("Transaction #" + (index + 1), "Type: `%s`\nAmount: `%s`%s\nOccurred: %s".formatted(
                    typeName,
                    amount,
                    targetLine,
                    TimeFormat.RELATIVE.format(transaction.timestamp())
            ));
        }

        PaginatedEmbed paginatedEmbed = new PaginatedEmbed.Builder(5, contents)
                .title("%s's Transaction History".formatted(user.getName()))
                .authorOnly(event.getUser().getIdLong())
                .color(event.getMember() != null ? event.getMember().getColorRaw() : 0)
                .timestamp(Instant.now())
                .footer("Requested by %s".formatted(event.getUser().getAsTag()), event.getUser().getEffectiveAvatarUrl())
                .build(event.getJDA());
        paginatedEmbed.send(event.getHook());
    }
}
