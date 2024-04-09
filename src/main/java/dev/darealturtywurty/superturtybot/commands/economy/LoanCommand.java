package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class LoanCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("request", "Request a loan from the bank that you must pay back with interest at a later date.")
                        .addOptions(new OptionData(OptionType.INTEGER, "amount", "The amount of money to request as a loan", true).setMinValue(1000)),
                new SubcommandData("pay", "Pay back a loan you have taken out from the bank.")
                        .addOptions(new OptionData(OptionType.STRING, "id", "The ID of the loan you want to pay back", true, true),
                                new OptionData(OptionType.INTEGER, "amount", "The amount of money to pay back", true).setMinValue(100)),
                new SubcommandData("list", "List all of the loans you have taken out from the bank.")
                        .addOptions(new OptionData(OptionType.BOOLEAN, "paid", "Whether or not to only list paid off loans", false)),
                new SubcommandData("info", "Get information about a loan you have taken out from the bank.")
                        .addOptions(new OptionData(OptionType.STRING, "id", "The ID of the loan you want to get information about", false, true))
        );
    }

    @Override
    public String getDescription() {
        return "Request a loan from the bank that you must pay back with interest at a later date.";
    }

    @Override
    public String getName() {
        return "loan";
    }

    @Override
    public String getRichName() {
        return "Loan";
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if(event.getGuild() == null || event.isAcknowledged() || !event.getName().equals(getName()))
            return;

        if (event.getSubcommandName() == null) {
            event.replyChoiceStrings("request", "pay", "list", "info").queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "pay", "info" -> {
                Economy account = EconomyManager.getOrCreateAccount(event.getGuild(), event.getUser());
                event.replyChoiceStrings(account.getLoans().stream().map(Loan::getId).toList()).queue();
            }
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());

        switch (subcommand) {
            case "request" -> {
                if (account.getNextLoan() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You must wait %s before requesting another loan!".formatted(
                            TimeFormat.RELATIVE.format(account.getNextLoan()))).queue();
                    return;
                }

                int amount = event.getOption("amount", 1000, OptionMapping::getAsInt);
                if (amount < 1000) {
                    event.getHook().editOriginal("❌ You must request at least %s1000!").queue();
                    return;
                }

                List<Loan> loans = account.getLoans().stream().filter(loan -> !loan.isPaidOff()).toList();
                if (loans.size() >= 3) {
                    event.getHook().editOriginal("❌ You can only have a maximum of 3 loans at a time!").queue();
                    return;
                }

                System.out.println(EconomyManager.getCreditScore(account));

                Loan loan = EconomyManager.addLoan(account, amount);
                event.getHook().editOriginal("✅ You have successfully received a loan of %s%d! You will have to pay back %s%d %s with an interest rate of %s%%!".formatted(
                        config.getEconomyCurrency(),
                        amount,
                        config.getEconomyCurrency(),
                        amount,
                        TimeFormat.RELATIVE.format(loan.getTimeToPay()),
                        loan.getInterestRate())
                ).queue();
            }

            case "pay" -> {
                String id = event.getOption("id", null, OptionMapping::getAsString);
                if (id == null) {
                    event.getHook().editOriginal("❌ You must provide the ID of the loan you want to pay back!").queue();
                    return;
                }

                Loan loan = account.getLoans().stream().filter(l -> l.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
                if (loan == null) {
                    event.getHook().editOriginal("❌ You do not have a loan with the ID of %s! Use `/loan list` to view all of your loans.".formatted(id)).queue();
                    return;
                }

                int amount = event.getOption("amount", 100, OptionMapping::getAsInt);
                if (amount < 100) {
                    event.getHook().editOriginal("❌ You must pay back at least %s100!".formatted(config.getEconomyCurrency())).queue();
                    return;
                }

                if (amount > account.getBank()) {
                    event.getHook().editOriginal("❌ You do not have enough money in the bank to pay back this loan!").queue();
                    return;
                }

                boolean paidOff = EconomyManager.payLoan(account, loan, amount);
                if (paidOff) {
                    event.getHook().editOriginal("✅ You have successfully paid off your loan!").queue();
                } else {
                    event.getHook().editOriginal("✅ You have successfully paid back %s%d of your loan! You still have %s%d left to pay back!".formatted(
                            config.getEconomyCurrency(),
                            amount,
                            config.getEconomyCurrency(),
                            loan.calculateAmountLeftToPay()
                    )).queue();
                }
            }

            case "list" -> {
                OptionMapping paid = event.getOption("paid");
                List<Loan> loans = account.getLoans();
                if(loans.isEmpty()){
                    event.getHook().editOriginal("❌ You do not have any loans!").queue();
                    return;
                }

                if(paid != null){
                    boolean isPaid = paid.getAsBoolean();
                    loans = loans.stream().filter(loan -> loan.isPaidOff() == isPaid).toList();
                }

                if(loans.isEmpty()) {
                    event.getHook().editOriginal("❌ You do not have any loans %s!".formatted(paid.getAsBoolean() ? "that are paid off" : "that are not paid off")).queue();
                    return;
                }

                PaginatedEmbed.Builder builder = createLoanEmbed(config, loans);
                builder.timestamp(Instant.now());
                builder.authorOnly(event.getUser().getIdLong());
                builder.footer("Requested by %s".formatted(event.getUser().getEffectiveName()), event.getUser().getEffectiveAvatarUrl());
                builder.build(event.getJDA()).send(event.getHook(),
                        () -> event.getHook().editOriginal("❌ Something went wrong while trying to retrieve you your loans!").queue());
            }

            case "info" -> {
                String id = event.getOption("id", null, OptionMapping::getAsString);
                if (id == null) {
                    event.getHook().editOriginal("❌ You must provide the ID of the loan you want to get information about!").queue();
                    return;
                }

                Loan loan = account.getLoans().stream().filter(l -> l.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
                if (loan == null) {
                    event.getHook().editOriginal("❌ You do not have a loan with the ID of %s! Use `/loan list` to view all of your loans.".formatted(id)).queue();
                    return;
                }

                var embed = new EmbedBuilder();
                embed.setTitle("Loan Information");
                embed.setTimestamp(Instant.now());
                embed.setColor(loan.isPaidOff() ? Color.GREEN : Color.RED);
                embed.addField("Loan ID", loan.getId(), false);
                embed.addField("Original Amount", "%s%d".formatted(config.getEconomyCurrency(), loan.getAmount()), false);
                embed.addField("Interest Rate", "%s%%".formatted(loan.getInterestRate()), false);
                embed.addField("Time to Pay", TimeFormat.RELATIVE.format(loan.getTimeToPay()), false);
                embed.addField("Paid Off", loan.isPaidOff() ? "Yes" : "No", false);
                embed.addField("Total Amount to Pay", "%s%d".formatted(config.getEconomyCurrency(), loan.calculateTotalAmountToPay()), false);
                embed.addField("Amount Left to Pay", "%s%d".formatted(config.getEconomyCurrency(), loan.calculateAmountLeftToPay()), false);
                embed.addField("Amount Paid", "%s%d".formatted(config.getEconomyCurrency(), loan.getAmountPaid()), false);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @NotNull
    private static PaginatedEmbed.Builder createLoanEmbed(GuildData config, List<Loan> loans) {
        PaginatedEmbed.ContentsBuilder contents = new PaginatedEmbed.ContentsBuilder();
        for (Loan loan : loans) {
            contents.field("Loan ID: %s".formatted(loan.getId()), """
                    Amount: %s%d
                    Interest Rate: %s%%
                    Time to Pay: %s
                    Paid Off: %s
                    """.formatted(
                    config.getEconomyCurrency(),
                    loan.getAmount(),
                    loan.getInterestRate(),
                    TimeFormat.RELATIVE.format(loan.getTimeToPay()),
                    loan.isPaidOff() ? "Yes" : "No"
            ), false);
        }

        PaginatedEmbed.Builder builder = new PaginatedEmbed.Builder(5, contents);
        builder.title("Your Loans");
        builder.color(Color.ORANGE);
        return builder;
    }
}
