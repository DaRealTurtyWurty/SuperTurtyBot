package dev.darealturtywurty.superturtybot.commands.fun.tod;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.fun.tod.data.TODPack;
import dev.darealturtywurty.superturtybot.commands.fun.tod.data.TODResult;
import dev.darealturtywurty.superturtybot.commands.fun.tod.data.TODType;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import lombok.Data;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public class TruthCommand extends SubcommandCommand {
    public TruthCommand() {
        super("truth", "Get a random truth question to answer!");


    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

    }

    public static String getRandomTruth(long guildId) {
        if(guildId == -1L) {
            return TruthOrDareCommand.DEFAULT_TRUTHS.get((int) (Math.random() * TruthOrDareCommand.DEFAULT_TRUTHS.size()));
        }

        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        if(data == null) {
            data = new GuildData(guildId);
            Database.getDatabase().guildData.insertOne(data);
        }

        TODPack pack = data.getTODPack();
        if(pack == null) {
            pack = TODPack.DEFAULT;
        }

        TODResult result = TODType.TRUTH.pickRandom(pack);
        
    }
}
