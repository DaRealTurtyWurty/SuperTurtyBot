package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class AnalyzeLogCommand extends CoreCommand {
    public AnalyzeLogCommand() {
        super(new Types(false, false, true, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Analyzes the log file for errors and warnings.";
    }

    @Override
    public String getName() {
        return "analyzelog";
    }

    @Override
    public String getRichName() {
        return "Analyze Log";
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Message message = event.getTarget();
        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.isEmpty()) {
            event.reply("This message does not contain a log!").setEphemeral(true).queue();
            return;
        }

        if (attachments.size() > 1) {
            event.reply("I can only analyze messages that contain 1 file!").setEphemeral(true).queue();
            return;
        }

        Message.Attachment attachment = attachments.getFirst();
        if (!attachment.getFileName().endsWith(".log")) {
            event.reply("This file is not a log file!").setEphemeral(true).queue();
            return;
        }

        event.deferReply(false).setContent("Analyzing....").queue();

        String content = attachment.getProxy().download().thenApply(inputStream -> {
            try {
                return new String(inputStream.readAllBytes());
            } catch (IOException exception) {
                Constants.LOGGER.error("An error occurred while reading a log file!", exception);
                return null;
            }
        }).join();

        if (content == null) {
            event.getHook().editOriginal("An error occurred while reading the log file!").queue();
            return;
        }

        String[] lines = content.split("\n");
        Optional<EnvironmentInformation> information = findEnvironmentInformation(lines);
        if (information.isEmpty()) {
            event.getHook().editOriginal("Could not parse environment information!").queue();
            return;
        }

        List<PossibleError> possibleErrors = locateErrors(information.get(), lines);

        var embed = new EmbedBuilder()
                .setTitle("Analyzed %s's log file".formatted(message.getAuthor().getName()))
                .setColor(Color.BLUE)
                .setTimestamp(message.getTimeCreated().toInstant())
                .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

        embed.addField("Environment Information",
                "Minecraft Version: %s%nForge Version: %s%nJava Version: %s%nOperating System: %s%nArchitecture: %s"
                        .formatted(
                                information.get().mcVersion(),
                                information.get().forgeVersion(),
                                information.get().javaVersion(),
                                information.get().operatingSystem(),
                                information.get().architecture()
                        ), false);

        if (possibleErrors.isEmpty()) {
            embed.setDescription("No errors or warnings were found in this log file!");
        } else {
            embed.setDescription("Possible errors and warnings were found in this log file!");
            embed.addField("Possible Errors and Warnings",
                    String.join("\n\n", possibleErrors.stream().map(error -> "**Problem**:%n%s%n**Possible Solution**:%n%s".formatted(
                            error.message(),
                            error.solution())
                    ).toList()), false);
        }

        event.getHook().editOriginalEmbeds(embed.build()).setContent("🧐").queue();
    }

    private static Optional<EnvironmentInformation> findEnvironmentInformation(String[] lines) {
        String modLauncherRunningArgs = null;
        String modLauncherStartingArgs = null;
        for (String line : lines) {
            if (modLauncherRunningArgs != null && modLauncherStartingArgs != null) break;
            if (!line.contains("[cpw.mods.modlauncher.Launcher/MODLAUNCHER]") && !line.contains("[cp.mo.mo.Launcher/MODLAUNCHER]")) continue;

            if (modLauncherRunningArgs == null && line.contains("ModLauncher running: args")) {
                modLauncherRunningArgs = line;
                continue;
            }

            // check for ModLauncher 10.0.8+10.0.8+main.0ef7e830 starting (excluding the version part in the middle)
            if (modLauncherStartingArgs == null && line.contains("ModLauncher ") && line.contains(" starting")) {
                modLauncherStartingArgs = line;
            }
        }

        if (modLauncherRunningArgs == null || modLauncherStartingArgs == null) return Optional.empty();

        String[] runningArgs = modLauncherRunningArgs.split("ModLauncher running: args")[1]
                .replace("[", "")
                .replace("]", "")
                .split(",");
        String startingArgs = modLauncherStartingArgs.split("ModLauncher .+? starting:")[1];

        // convert string[] to map
        Map<String, String> runningArgsMap = new HashMap<>();
        String key = null;
        for (String arg : runningArgs) {
            if (key == null) {
                key = arg;
                continue;
            }

            runningArgsMap.put(key.trim(), arg.trim());
            key = null;
        }

        String forgeVersion = runningArgsMap.get("--fml.forgeVersion");
        String mcVersion = runningArgsMap.get("--fml.mcVersion");

        String[] javaVersionSplit = startingArgs.split("java version ")[1].split(";");
        String javaVersion = javaVersionSplit[0].trim();

        String[] osSplit = javaVersionSplit[1].split("OS")[1].split("arch");
        String operatingSystem = osSplit[0].trim();

        String arch = osSplit[1].split("version")[0].trim();

        return Optional.of(new EnvironmentInformation(javaVersion, operatingSystem, arch, forgeVersion, mcVersion));
    }

    private static List<PossibleError> locateErrors(EnvironmentInformation information, String[] lines) {
        List<PossibleError> possibleErrors = new ArrayList<>();

        for (String line : lines) {
            if (line.contains("Exception loading blockstate definition")) {
                String location = line.split("Exception loading blockstate definition:")[1].split("'")[1].trim();
                if (line.contains("missing model for variant")) {
                    String variant = line.split("missing model for variant:")[1].split("'")[1];

                    StringBuilder solution = new StringBuilder("Your blockstate file (which should be located at `%s`) is missing a model for the variant `%s`!%n".formatted(location, variant));
                    if (variant.endsWith("#")) {
                        solution.append("In this case, you appear to be missing the default variant for your blockstate file (which should be an empty string `\"\"`)!\n");
                    } else {
                        solution.append("In this case, you appear to be missing the variant `%s` for your blockstate file!%n".formatted(variant.split("#")[1]));
                    }

                    solution.append("Make sure that the blockstate file exists at that location and that you are specifying the `model` for that variant!");
                    possibleErrors.add(new PossibleError("Missing model for variant!", solution.toString()));
                } else if (line.contains("Missing model, expected to find a string")) {
                    String solution = "Your blockstate file (which should be located at `%s`) is missing a `model` property for one of its variants!%n".formatted(location);
                    possibleErrors.add(new PossibleError("Missing model property!", solution));
                } else if (line.contains("Neither 'variants' nor 'multipart' found")) {
                    String solution = "Your blockstate file (which should be located at `%s`) is missing a `variants` or `multipart` property!%n".formatted(location);
                    possibleErrors.add(new PossibleError("Missing variants or multipart property!", solution));
                } else if (line.contains("Unknown blockstate property")) {
                    String property = line.split("Unknown blockstate property: ")[1].split("'")[1].trim();
                    String solution = "Your blockstate file (which should be located at `%s`) contains an unknown property `%s`!%n".formatted(location, property);
                    possibleErrors.add(new PossibleError("Unknown blockstate property!", solution));
                } else if (line.contains("Unknown value")) {
                    String value = line.split("Unknown value: ")[1].split("'")[1].trim();
                    String property = line.split("for blockstate property: ")[1].split("'")[1].trim();
                    String[] possibleValues = line.split("for blockstate property: ")[1].split("'")[2].replace("[", "").replace("]", "").trim().split(",");

                    String solution = "Your blockstate file (which should be located at `%s`) contains an unknown value `%s` for the property `%s`!%n".formatted(location, value, property);
                    solution += "Possible values for this property are: `%s`".formatted(String.join("`, `", possibleValues));
                    possibleErrors.add(new PossibleError("Unknown value for blockstate property!", solution));
                } else if (line.contains("Overlapping definition with")) {
                    String variant = line.split("for variant: ")[1].split("'")[1].trim();
                    String otherVariant = line.split("Overlapping definition with: ")[1].trim();

                    String solution = "Your blockstate file (which should be located at `%s`) contains an overlapping definition for the variant `%s`!%n".formatted(location, variant);
                    solution += "This variant is already defined in the blockstate file `%s`!".formatted(otherVariant);
                    possibleErrors.add(new PossibleError("Overlapping definition for variant!", solution));
                }

                continue;
            }

            if (line.contains("Unable to load model")) {
                String model = line.split("Unable to load model: ")[1].split("'")[1].trim();
                String blockstate = line.split("referenced from: ")[1].split(": ")[1].trim();

                if (line.contains("java.io.FileNotFoundException")) {
                    String location = line.split("java.io.FileNotFoundException: ")[1].trim();
                    String solution = "(Referenced from `%s` blockstate): Your model file (which should be located at `%s`) is missing!%n".formatted(blockstate, location);

                    if (!model.split(":")[0].equalsIgnoreCase(blockstate.split(":")[0])) {
                        solution += "It is possible that the value you supplied for the `model` property was not a string or maybe you put an invalid namespace for the model!";
                    } else {
                        solution += "Make sure that the model file exists at that location!";
                    }

                    possibleErrors.add(new PossibleError("Missing model file!", solution));
                }

                continue;
            }

            if (line.contains("Unable to resolve texture reference")) {
                String reference = line.split("Unable to resolve texture reference: ")[1].split(" in ")[0].trim();
                String model = line.split("Unable to resolve texture reference: ")[1].split(" in ")[1].trim();

                String solution = "(Referenced from `%s` model): Your model file (which should be located at `%s`) is missing a texture reference `%s`!%n".formatted(model, model, reference);
                solution += "Check that you don't have any typos in the `textures` object of the model file and that the texture file exists at that location!";
                possibleErrors.add(new PossibleError("Missing texture reference!", solution));
                continue;
            }

            if (line.contains("limits mip level from")) {
                String texture = line.split("Texture ")[1].split(" with size")[0].trim();
                String[] size = line.split("with size ")[1].split(" limits mip level from")[0].trim().split("x");
                int width = Integer.parseInt(size[0]);
                int height = Integer.parseInt(size[1]);

                String solution = "Your texture file (which should be located at `%s`) has a size of `%dx%d`!%n".formatted(texture, width, height);
                if (width != height) {
                    solution += "The texture file should have a 'square' and 'power of 2' size (e.g. `16x16`, `32x32`, `64x64`, etc.)!";
                } else {
                    // if the width is not a power of 2
                    if ((width & (width - 1)) != 0) {
                        solution += "The width of the texture file should be a 'power of 2' (e.g. `16`, `32`, `64`, etc.)!";
                    }

                    // if the height is not a power of 2
                    if ((height & (height - 1)) != 0) {
                        solution += "The height of the texture file should be a 'power of 2' (e.g. `16`, `32`, `64`, etc.)!";
                    }
                }

                possibleErrors.add(new PossibleError("Invalid texture size!", solution));
                continue;
            }

            if (line.contains("Using missing texture")) {
                if (line.contains("file") && line.contains("not found")) {
                    String location = line.split("Using missing texture, file ")[1].split(" not found")[0].trim();

                    String solution = "Your texture file (which should be located at `%s`) is missing!%n".formatted(location);
                    solution += "Make sure that the texture file exists at that location!";
                    possibleErrors.add(new PossibleError("Missing texture file!", solution));
                } else if (line.contains("unable to load")) {
                    String location = line.split("Using missing texture, unable to load ")[1].split(" : ")[0].trim();
                    String exception = line.split("Using missing texture, unable to load ")[1].split(" : ")[1].trim();

                    String solution = "Your texture file (which should be located at `%s`) has failed to load!%n".formatted(location);
                    solution += "The exception thrown was: `%s`%n".formatted(exception);
                    solution += "Make sure that the texture file is a normal `.png` image and that it is not corrupted!";
                    possibleErrors.add(new PossibleError("Failed to load texture file!", solution));
                }

                continue;
            }

            if (line.contains("Unable to parse metadata from")) {
                String location = line.split("Unable to parse metadata from ")[1].split(" : ")[0].trim();
                String exception = line.split("Unable to parse metadata from ")[1].split(" : ")[1].trim();

                String solution = "Your texture file (which should be located at `%s`) has invalid animation metadata!%n".formatted(location);
                solution += "The exception thrown was: `%s`%n".formatted(exception);
                solution += "Make sure that the animation metadata is a valid mcmeta and that the frames are all within range.\n";
                solution += "You can view the animation mcmeta file format here: https://minecraft.wiki/w/Resource_pack#Animation";
                possibleErrors.add(new PossibleError("Invalid animation metadata!", solution));
                continue;
            }

            // format: "Invalid frame duration on sprite {location} frame {frameNo}: {frameTime}"
            if (line.contains("Invalid frame duration on sprite")) {
                String location = line.split("Invalid frame duration on sprite ")[1].split(" frame ")[0].trim();
                String frameNo = line.split("Invalid frame duration on sprite ")[1].split(" frame ")[1].split(": ")[0].trim();
                String frameTime = line.split("Invalid frame duration on sprite ")[1].split(" frame ")[1].split(": ")[1].trim();

                String solution = "Your texture file (which should be located at `%s`) has an invalid frame duration!%n".formatted(location);
                solution += "This is the frame number: `%s`%n".formatted(frameNo);
                solution += "This is the frame duration: `%s`%n".formatted(frameTime);
                solution += "Make sure that the frame duration is a valid number!\n";
                solution += "You can view the animation mcmeta file format here: https://minecraft.wiki/w/Resource_pack#Animation";
                possibleErrors.add(new PossibleError("Invalid frame duration!", solution));
                continue;
            }

            // format: "Invalid frame index on sprite {location} frame {frameNo}: {frameIndex}"
            if (line.contains("Invalid frame index on sprite")) {
                String location = line.split("Invalid frame index on sprite ")[1].split(" frame ")[0].trim();
                String frameNo = line.split("Invalid frame index on sprite ")[1].split(" frame ")[1].split(": ")[0].trim();
                String frameIndex = line.split("Invalid frame index on sprite ")[1].split(" frame ")[1].split(": ")[1].trim();

                String solution = "Your texture file (which should be located at `%s`) has an invalid frame index!%n".formatted(location);
                solution += "This is the frame number: `%s`%n".formatted(frameNo);
                solution += "This is the frame index: `%s`%n".formatted(frameIndex);
                solution += "Make sure that the frame index is a valid number and is within the range that the texture uses!\n";
                solution += "You can view the animation mcmeta file format here: https://minecraft.wiki/w/Resource_pack#Animation";
                possibleErrors.add(new PossibleError("Invalid frame index!", solution));
            }

            // format: "Couldn't load advancement {location}: {advancement}"
            if (line.contains("Couldn't load advancement")) {
                String location = line.split("Couldn't load advancement ")[1].split(": ")[0].trim();
                String advancement = line.split("Couldn't load advancement ")[1].split(": ")[1].trim();

                String solution = "Your advancement file (which should be located at `%s`) has failed to load!%n".formatted(location);
                solution += "This is the advancement that failed to load: `%s`%n".formatted(advancement);
                solution += "Make sure that the advancement file is a valid json and that it is not corrupted!\n";
                solution += "You can view the advancement file format here: https://minecraft.wiki/w/Advancement/JSON_format#File_format";
                possibleErrors.add(new PossibleError("Failed to load advancement file!", solution));
                continue;
            }

            // format: "Skipped language file: {namespace}:{filename} ({exception})"
            if (line.contains("Skipped language file")) {
                String namespace = line.split("Skipped language file: ")[1].split(":")[0].trim();
                String filename = line.split("Skipped language file: ")[1].split(":")[1].split(" ")[0].trim();
                String exception = line.split("Skipped language file: ")[1].split(namespace + ":" + filename)[1].trim();

                String solution = "Your language file (which should be located at `%s:%s`) has failed to load!%n".formatted(namespace, filename);
                solution += "The exception thrown was: `%s`%n".formatted(exception);
                solution += "Make sure that the language file is a valid json and follows the format of `{\n\t\"abc\":\"def\",\n\t\"ghi\":\"jkl\"\n}`!\n";
                solution += "You can view information about the language file format here: https://forge.gemwire.uk/wiki/Internationalization#Language_files";
                possibleErrors.add(new PossibleError("Failed to load language file!", solution));
                continue;
            }

            //format: "Failed to load translations for {modid} from pack {packname}"
            if (line.contains("Failed to load translations for")) {
                String modid = line.split("Failed to load translations for ")[1].split(" from pack ")[0].trim();
                String packname = line.split("Failed to load translations for ")[1].split(" from pack ")[1].trim();

                String solution = "The language file in pack: `%s` for mod: `%s` has failed to load!%n".formatted(packname, modid);
                solution += "Make sure that the language file is a valid json and follows the format of `{\n\t\"abc\":\"def\",\n\t\"ghi\":\"jkl\"\n}`!\n";
                solution += "You can view information about the language file format here: https://forge.gemwire.uk/wiki/Internationalization#Language_files";
                possibleErrors.add(new PossibleError("Failed to load language file!", solution));
                continue;
            }

            if (line.contains("Invalid sounds.json in resourcepack")) {
                String packname = line.split("Invalid sounds.json in resourcepack: ")[1].split("'")[1].trim();

                String solution = "The sounds.json file in pack: `%s` has failed to load!%n".formatted(packname);
                solution += "Make sure that the sounds.json file is a valid sounds.json!\n";
                solution += "You can view the format for the sounds.json here: https://minecraft.wiki/w/Sounds.json";
                possibleErrors.add(new PossibleError("Failed to load sounds.json file!", solution));
                continue;
            }

            if (line.contains("hs_err_pid")) {
                String location = line.replace("#", "").trim();
                String solution = "A fatal error has occurred which caused the JVM (Java Virtual Machine) to crash!";
                solution += " This usually occurs due to some sort of memory issue, or a general problem with the JVM.\n";
                solution += "One common cause of a JVM crash is an outdated graphics driver, so make sure that your graphics driver is up to date!\n";
                solution += "Alternatively, it could be an issue with one of your other drivers, so it is always best to make sure you have the latest drivers installed.\n";
                solution += "If you want to, you can try and review the JVM crash log at `%s` to see if you can find the cause of the crash. However, these logs are often very cryptic and sometimes can contain sensitive information.".formatted(location);
                possibleErrors.add(new PossibleError("JVM Crash!", solution));
                continue;
            }

            // Missing textures in model apollo:test_item#inventory:
            //    minecraft:textures/atlas/blocks.png:apollo:item/test_item
            if(line.contains("Missing textures in model")) {
                String model = line.split("Missing textures in model ")[1].split("#")[0].trim();
                List<String> textures = new ArrayList<>(List.of(line.split("Missing textures in model ")[1].split("#")[1].split("\n")));
                textures.removeFirst(); // remove the first line which is just #inventory

                var solution = new StringBuilder("The model `%s` is missing the following textures:\n".formatted(model));
                for(String texture : textures) {
                    solution.append("`%s`\n".formatted(texture.trim()));
                }

                solution.append("Make sure that the textures exist and are in the correct location!\n");
                solution.append("You can view information about the model format here: \n\nItems: https://minecraft.wiki/w/Tutorials/Models#Item_models\nBlocks: https://minecraft.wiki/w/Tutorials/Models#Block_models\n\n");

                if(isGreaterThanOrEqual(information.mcVersion(), "1.19.3")) {
                    // note: in 1.19.3+ textures must be in 'item' and 'block' not 'items' and 'blocks'
                    solution.append("Make sure that the textures exist and are in the correct location!\n");
                    solution.append("Note: In 1.19.3+ the textures must be in the `item` and `block` folders, not the `items` and `blocks` folders!\n");
                }

                possibleErrors.add(new PossibleError("Missing textures in model!", solution.toString()));
                //noinspection UnnecessaryContinue
                continue;
            }
        }

        return possibleErrors;
    }

    private static boolean isGreaterThanOrEqual(String versionString, String targetVersionString) {
        String[] version = versionString.split("\\.");
        String[] targetVersion = targetVersionString.split("\\.");

        int major = Integer.parseInt(version[0]);
        int minor = Integer.parseInt(version[1]);
        int patch = version.length > 2 ? Integer.parseInt(version[2]) : 0;

        int targetMajor = Integer.parseInt(targetVersion[0]);
        int targetMinor = Integer.parseInt(targetVersion[1]);
        int targetPatch = targetVersion.length > 2 ? Integer.parseInt(targetVersion[2]) : 0;

        if (major > targetMajor) {
            return true;
        } else if (major == targetMajor) {
            if (minor > targetMinor) {
                return true;
            } else if (minor == targetMinor) {
                return patch >= targetPatch;
            }
        }

        return false;
    }

    public record EnvironmentInformation(String javaVersion, String operatingSystem, String architecture,
                                         String forgeVersion, String mcVersion) {
    }

    public record PossibleError(String message, String solution) {
    }
}
