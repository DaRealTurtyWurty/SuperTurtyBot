package dev.darealturtywurty.superturtybot.commands.fun.tod;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TruthOrDareCommand extends CoreCommand {
    public static final List<String> DEFAULT_TRUTHS = new ArrayList<>();
    public static final List<String> DEFAULT_DARES = new ArrayList<>();

    public TruthOrDareCommand() {
        super(new Types(true, false, false, false));

        addSubcommands(new TruthCommand(), new DareCommand(), new RandomCommand(), new CreatePackCommand(),
                new DeletePackCommand(), new ListPacksCommand(), new UsePackCommand(), new RenamePackCommand(),
                new PackDescriptionCommand(), new ViewPackCommand(), new AddTruthCommand(), new AddDareCommand(),
                new RemoveTruthCommand(), new RemoveDareCommand());

        try(InputStream truthsStream = TurtyBot.loadResource("truths.txt");
            InputStream daresStream = TurtyBot.loadResource("dares.txt")) {
            if (truthsStream != null) {
                List<String> lines = IOUtils.readLines(truthsStream, StandardCharsets.UTF_8);
                DEFAULT_TRUTHS.addAll(lines);
            }

            if (daresStream != null) {
                List<String> lines = IOUtils.readLines(daresStream, StandardCharsets.UTF_8);
                DEFAULT_DARES.addAll(lines);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load default truths and dares!", exception);
        }
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return ""; // TODO: Add description
    }

    @Override
    public String getName() {
        return "truthordare";
    }

    @Override
    public String getRichName() {
        return "Truth or Dare";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }
}
