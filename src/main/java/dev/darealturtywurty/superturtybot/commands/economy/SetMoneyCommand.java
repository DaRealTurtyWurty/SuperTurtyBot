package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetMoneyCommand extends EconomyCommand {
    public SetMoneyCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public String getDescription() {
        return "Sets the money of a user.";
    }

    @Override
    public String getName() {
        return "setmoney";
    }

    @Override
    public String getRichName() {
        return "Set Money";
    }

    @Override
    public String getAccess() {
        return "Server Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event, Guild guild, GuildData config) {
        if (event.getAuthor().getIdLong() != guild.getOwnerIdLong()) {
            reply(event, "❌ You must be the owner of the server to use this command!", false);
            return;
        }

        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");
        if (args.length < 3) {
            reply(event, "❌ You must provide a user and an amount!", false);
            return;
        }

        Type type = Type.SET;
        long user;
        int amount = 0;
        try {
            user = Long.parseLong(args[1]);
        } catch (NumberFormatException ignored) {
            type = switch (args[1]) {
                case "add" -> Type.ADD;
                case "remove" -> Type.REMOVE;
                case "set" -> Type.SET;
                default -> {
                    reply(event, "❌ You must provide a user and an amount (and optionally an action; `add` or `remove`)!", false);
                    yield Type.INVALID;
                }
            };

            if (type != Type.INVALID) {
                try {
                    user = Long.parseLong(args[2]);
                } catch (NumberFormatException ignored2) {
                    reply(event, "❌ You must provide a user!", false);
                    return;
                }
            } else {
                return;
            }

            try {
                amount = Integer.parseInt(args[3]);
                if (amount == 0 && type != Type.SET) {
                    reply(event, "❌ You cannot add or remove 0!", false);
                    return;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException ignored1) {
                reply(event, "❌ You must provide an amount!", false);
                return;
            }
        }

        if (amount == 0) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                reply(event, "❌ You must provide an amount!", false);
                return;
            }
        }

        User user1 = event.getJDA().getUserById(user);
        if (user1 == null || guild.getMember(user1) == null) {
            reply(event, "❌ You must provide a valid user in this server!", false);
            return;
        }

        Economy account = EconomyManager.getAccount(guild, user1);
        switch (type) {
            case ADD -> {
                EconomyManager.addMoney(account, amount);
                reply(event, "✅ Added %s%d to %s's balance!"
                                .formatted(config.getEconomyCurrency(), amount, user1.getAsMention()),
                        false);
            }
            case REMOVE -> {
                EconomyManager.removeMoney(account, amount);
                reply(event, "✅ Removed %s%d from %s's balance!"
                                .formatted(config.getEconomyCurrency(), amount, user1.getAsMention()),
                        false);
            }
            case SET -> {
                EconomyManager.setMoney(account, amount, true);
                EconomyManager.setMoney(account, 0, false);
                reply(event, "✅ Set %s's balance to %s%d!"
                                .formatted(user1.getAsMention(), config.getEconomyCurrency(), amount),
                        false);
            }
            default -> reply(event, "🤓 Hackerman!", false);
        }

        EconomyManager.updateAccount(account);
    }

    public enum Type {
        SET,
        ADD,
        REMOVE,
        INVALID
    }
}
