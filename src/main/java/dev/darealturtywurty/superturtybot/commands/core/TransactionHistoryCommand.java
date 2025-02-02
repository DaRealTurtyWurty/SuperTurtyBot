package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.commands.economy.EconomyTransactionHistoryCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;

public class TransactionHistoryCommand extends CoreCommand {
    public TransactionHistoryCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(new EconomyTransactionHistoryCommand());
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Shows the transaction history of a user.";
    }

    @Override
    public String getName() {
        return "transactionhistory";
    }

    @Override
    public String getRichName() {
        return "Transaction History";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
