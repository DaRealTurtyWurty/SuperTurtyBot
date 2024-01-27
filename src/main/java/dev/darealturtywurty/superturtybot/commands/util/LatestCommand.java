package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.*;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.core.util.object.CoupledPair;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LatestCommand extends CoreCommand {
    public LatestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("minecraft", "Get the latest Minecraft version"),
                new SubcommandData("forge", "Get the latest Forge version"),
                new SubcommandData("fabric", "Get the latest Fabric version"),
                new SubcommandData("quilt", "Get the latest Quilt version"),
                new SubcommandData("parchment", "Get the latest Parchment version"),
                new SubcommandData("neoforge", "Get the latest NeoForge version"),
                new SubcommandData("all", "Get the latest versions of all"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Get the latest version of Forge, Fabric, Quilt, Parchment, or Minecraft";
    }

    @Override
    public String getName() {
        return "latest";
    }

    @Override
    public String getRichName() {
        return "Latest";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(event.getSubcommandName() == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        switch (event.getSubcommandName()) {
            case "minecraft" -> {
                Either<CoupledPair<MinecraftVersion>, HttpStatus> response = ApiHandler.getLatestMinecraft();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Minecraft versions!").queue();
                    return;
                }

                CoupledPair<MinecraftVersion> versions = response.getLeft();

                var embed = new EmbedBuilder()
                        .setTitle("Latest Minecraft Versions")
                        .addField("Release", versions.getLeft().version(), true)
                        .addField("Snapshot", versions.getRight().version(), true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "forge" -> {
                Either<CoupledPair<ForgeVersion>, HttpStatus> response = ApiHandler.getLatestForge();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Forge versions!").queue();
                    return;
                }

                CoupledPair<ForgeVersion> versions = response.getLeft();
                var embed = new EmbedBuilder()
                        .setTitle("Latest Forge Versions")
                        .addField("Recommended", versions.getLeft().version(), true)
                        .addField("Latest", versions.getRight().version(), true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "fabric" -> {
                Either<CoupledPair<FabricVersion>, HttpStatus> response = ApiHandler.getLatestFabric();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Fabric versions!").queue();
                    return;
                }

                CoupledPair<FabricVersion> versions = response.getLeft();
                var embed = new EmbedBuilder()
                        .setTitle("Latest Fabric Versions")
                        .addField("Stable", versions.getLeft().version(), true)
                        .addField("Latest", versions.getRight().version(), true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "quilt" -> {
                Either<CoupledPair<QuiltVersion>, HttpStatus> response = ApiHandler.getLatestQuilt();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Quilt versions!").queue();
                    return;
                }

                CoupledPair<QuiltVersion> versions = response.getLeft();
                var embed = new EmbedBuilder()
                        .setTitle("Latest Quilt Versions")
                        .addField("Stable", versions.getLeft().version(), true)
                        .addField("Latest", versions.getRight().version(), true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "neoforge" -> {
                Either<CoupledPair<NeoforgeVersion>, HttpStatus> response = ApiHandler.getLatestNeoforge();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Neoforge versions!").queue();
                    return;
                }

                CoupledPair<NeoforgeVersion> versions = response.getLeft();
                var embed = new EmbedBuilder()
                        .setTitle("Latest Neoforge Versions")
                        .addField("Recommended", versions.getLeft().version(), true)
                        .addField("Latest", versions.getRight().version(), true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "parchment" -> {
                Either<ParchmentVersion, HttpStatus> response = ApiHandler.getLatestParchment();
                if (response.isRight()) {
                    event.getHook().sendMessage("❌ Failed to get latest Parchment version!").queue();
                    return;
                }

                ParchmentVersion version = response.getLeft();
                var embed = new EmbedBuilder()
                        .setTitle("Latest Parchment Version")
                        .addField("Version", version.version(), true)
                        .addField("Minecraft Version", version.version().split("-")[1], true);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
            case "all" -> event.getHook().editOriginalEmbeds(createAllEmbed().build()).queue();
        }
    }

    @NotNull
    private static EmbedBuilder createAllEmbed() {
        Either<CoupledPair<MinecraftVersion>, HttpStatus> minecraftResponse = ApiHandler.getLatestMinecraft();
        Either<CoupledPair<ForgeVersion>, HttpStatus> forgeResponse = ApiHandler.getLatestForge();
        Either<CoupledPair<FabricVersion>, HttpStatus> fabricResponse = ApiHandler.getLatestFabric();
        Either<CoupledPair<QuiltVersion>, HttpStatus> quiltResponse = ApiHandler.getLatestQuilt();
        Either<CoupledPair<NeoforgeVersion>, HttpStatus> neoforgeResponse = ApiHandler.getLatestNeoforge();
        Either<ParchmentVersion, HttpStatus> parchmentResponse = ApiHandler.getLatestParchment();

        var embed = new EmbedBuilder();
        if (minecraftResponse.isLeft()) {
            CoupledPair<MinecraftVersion> versions = minecraftResponse.getLeft();
            embed.addField("Minecraft", "Release: " + versions.getLeft().version() + "\nSnapshot: " + versions.getRight().version(), false);
        } else {
            embed.addField("Minecraft", "Failed to get latest Minecraft versions!", false);
        }

        if (forgeResponse.isLeft()) {
            CoupledPair<ForgeVersion> versions = forgeResponse.getLeft();
            embed.addField("Forge", "Stable: " + versions.getLeft().version() + "\nLatest: " + versions.getRight().version(), false);
        } else {
            embed.addField("Forge", "Failed to get latest Forge versions!", false);
        }

        if (fabricResponse.isLeft()) {
            CoupledPair<FabricVersion> versions = fabricResponse.getLeft();
            embed.addField("Fabric", "Stable: " + versions.getLeft().version() + "\nLatest: " + versions.getRight().version(), false);
        } else {
            embed.addField("Fabric", "Failed to get latest Fabric versions!", false);
        }

        if (quiltResponse.isLeft()) {
            CoupledPair<QuiltVersion> versions = quiltResponse.getLeft();
            embed.addField("Quilt", "Stable: " + versions.getLeft().version() + "\nLatest: " + versions.getRight().version(), false);
        } else {
            embed.addField("Quilt", "Failed to get latest Quilt versions!", false);
        }

        if (neoforgeResponse.isLeft()) {
            CoupledPair<NeoforgeVersion> versions = neoforgeResponse.getLeft();
            embed.addField("NeoForge", "Stable: " + versions.getLeft().version() + "\nLatest: " + versions.getRight().version(), false);
        } else {
            embed.addField("NeoForge", "Failed to get latest NeoForge versions!", false);
        }

        if (parchmentResponse.isLeft()) {
            ParchmentVersion version = parchmentResponse.getLeft();
            embed.addField("Parchment", "Version: " + version.version() + "\nMinecraft Version: " + version.version().split("-")[1], false);
        } else {
            embed.addField("Parchment", "Failed to get latest Parchment version!", false);
        }
        return embed;
    }
}
