package dev.darealturtywurty.superturtybot.commands.levelling;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetXPCommand extends EconomyCommand {
    public SetXPCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public String getDescription() {
        return "Adds, removes or sets a user's xp!";
    }

    @Override
    public String getName() {
        return "setxp";
    }

    @Override
    public String getRichName() {
        return "Set XP";
    }

    @Override
    public String getAccess() {
        return "Server Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event, Guild guild, GuildConfig config) {
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
        long user = 0;
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

        if(!LevellingManager.INSTANCE.areLevelsEnabled(guild)) {
            reply(event, "❌ Levelling is not enabled in this server!", false);
            return;
        }

        Levelling levelling = Database.getDatabase().levelling.find(Filters.and(Filters.eq("guild", guild.getIdLong()),
                Filters.eq("user", user))).first();
        if (levelling == null) {
            levelling = new Levelling(guild.getIdLong(), user);
            Database.getDatabase().levelling.insertOne(levelling);
        }

        switch (type) {
            case ADD -> {
                LevellingManager.INSTANCE.addXP(guild, user1, amount);
                reply(event, "✅ Added %sxp to %s".formatted(amount, user1.getAsMention()), false);
            }
            case REMOVE -> {
                LevellingManager.INSTANCE.removeXP(guild, user1, amount);
                reply(event, "✅ Removed %sxp from %s".formatted(amount, user1.getAsMention()), false);
            }
            case SET -> {
                LevellingManager.INSTANCE.setXP(guild, user1, amount);
                reply(event, "✅ Set %s's xp to %s".formatted(user1.getAsMention(), amount), false);
            }
            default -> reply(event, "🤓 Hackerman!", false);
        }
    }

    public enum Type {
        SET,
        ADD,
        REMOVE,
        INVALID
    }
}
