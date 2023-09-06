package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetRobloxUserAvatarCommand extends CoreCommand {
    public GetRobloxUserAvatarCommand() {
        super(new Types(true, false,false,false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "userid", "The userid of the roblox user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the User Roblox Avatar.";
    }

    @Override
    public String getName() {
        return "roblox-avatar";
    }

    @Override
    public String getRichName() {
        return "Roblox Avatar";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
       String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);
       if(robloxUserId == null){
           reply(event, "❌ You must provide a UserID!", false, true);
           return;
       }

       String RobloxAvatarUrl = "https://avatar.roblox.com/v1/users/%s/avatar"
               .formatted(robloxUserId);


       final Request request = new Request.Builder().url(RobloxAvatarUrl).get().build();

       try(Response response = new OkHttpClient().newCall(request).execute()){
           if(!response.isSuccessful()){
               event.getHook()
                       .sendMessage("❌ Failed to get response!")
                       .mentionRepliedUser(false)
                       .queue();
           }

           ResponseBody body = response.body();
           if(body == null){
               event.getHook()
                       .sendMessage("❌ Failed to get response!")
                       .mentionRepliedUser(false)
                       .queue();
               return;
           }
           String bodyString = body.string();

           if (bodyString.isBlank()) {
               event.getHook()
                       .sendMessage("❌ Failed to get response!")
                       .mentionRepliedUser(false)
                       .queue();
               return;
           }
           PlayerProfile robloxResponse = PlayerProfile.fromJsonString(bodyString);

           StringBuilder data = new StringBuilder();

           for(Asset roblox : robloxResponse.assets){


               data.append("\n**Roblox Asset Name: **" + roblox.name + "\n**ID: **" + roblox.id + "\n **Asset type: **" + roblox.assetType.name);

           }

           var embed = new EmbedBuilder()
                   .setTitle("User ID: " + robloxUserId)
                   .setDescription(data)
                   .setFooter("Player Animation Type: " + robloxResponse.playerAvatarType)
                   .setColor(0xf4dcb4)
                   .build();

           event.deferReply().addEmbeds(embed).queue();
           event.getHook().sendMessageEmbeds(embed);

       }catch(IOException exception){
           reply(event, "Failed to response!");
           Constants.LOGGER.error("Failed to get response!", exception);
       }
    }
    @Data
    public class PlayerProfile {
        private Scales scales;
        private String playerAvatarType;
        private BodyColors bodyColors;
        private List<Asset> assets;
        private boolean defaultShirtApplied;
        private boolean defaultPantsApplied;
        private List<Emote> emotes;

        private static PlayerProfile fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, PlayerProfile.class);
        }
    }
    @Data
    class Scales {
        private double height;
        private double width;
        private double head;
        private double depth;
        private double proportion;
        private double bodyType;
    }

    @Data
    class BodyColors {
        private int headColorId;
        private int torsoColorId;
        private int rightArmColorId;
        private int leftArmColorId;
        private int rightLegColorId;
        private int leftLegColorId;
    }

    @Data
    class Asset {
        private long id;
        private String name;
        private AssetType assetType;
        private long currentVersionId;
        private Meta meta;
    }

    @Data
    class AssetType {
        private long id;
        private String name;
    }

    @Data
    class Meta {
        private int order;
        private int version;
    }

    @Data
    class Emote {
        private long assetId;
        private String assetName;
        private int position;
    }
    }
