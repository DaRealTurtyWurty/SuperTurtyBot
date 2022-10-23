package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AmongUsCommand extends CoreCommand {
    public AmongUsCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "Sus.";
    }
    
    @Override
    public String getName() {
        return "amongus";
    }
    
    @Override
    public String getRichName() {
        return "Among Us";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        event.getMessage().reply(
            "Red 🔴 📛 sus 💦 💦. Red 🔴 🔴 suuuus. I 👁👄 👁 said 🤠🗣 💬👱🏿💦 red 👹 🔴, sus 💦 💦, hahahahaha 🤣 🤣. Why 🤔 🤔 arent you 👉😯 👈 laughing 😂 😂? I 👁🍊 👥 just made 👑 👑 a reference 👀👄🙀 👀👄🙀 to the popular 👍😁😂 😂 video 📹 📹 game 🎮 🎮 Among 🇷🇴🎛 💰 Us 👨 👨! How can you 👈 👈 not laugh 😂 😂 at it? Emergency meeting 💯 🤝! Guys 👦 👨, this here guy 👨 👱🏻👨🏻 doesn't laugh 🤣 ☑😂😅 at my funny 😃😂 🍺😛😃 Among 💰 💰 Us 👨 👨 memes 🐸 😂! Lets 🙆 🙆 beat ✊👊🏻 😰👊 him 👴 👨 to death 💀💥❓ 💀! Dead 💀😂 ☠ body 💃 💃 reported ☎ 🧐! Skip 🐧 🏃🏼! Skip 🐧 🐧! Vote 🔝 🔝 blue 💙 💙! Blue 💙 💙 was not an impostor 😎 😠. Among 😂 🙆🏽🅰 us 👨 👨 in a nutshell 😠 😠 hahahaha 😂👌👋 😂. What?! Youre still 🤞🙌 🤞🙌 not laughing 😂 😂 your 👉 👉 ass 🍑 🅰 off 📴 📴☠? I 👁 👁 made 👑 👑 SEVERAL 💯 💯 funny 😀😂😛 😃❓ references 👀👄🙀 📖 to Among 💰 💑👨‍❤️‍👨👩‍❤️‍👩 Us 👨 🇺🇸 and YOU 👈🏼 😂👉🔥 STILL 🤞🙌 🙄 ARENT LAUGHING 😂 😂😎💦??!!! Bruh ⚠ 😳🤣😂. Ya 🙏🎼 🙀 hear 👂 👂 that? Wooooooosh 💦👽👾 💦👽👾. Whats 😦 😦 woooosh 🚁 🚁? Oh 🙀 🙀, nothing ❌ 🚫. Just the sound 👂 🔊 of a joke 😂 😂 flying ✈ ✈ over 😳🙊💦 🔁 your 👉 👉 head 💆 💆. Whats 😦 🤔 that? You 👈 👉 think 💭 💭 im 👌 💘 annoying 😠 😠? Kinda 🙅 🙅 sus 💦 💦, bro 👆 🌈☺👬. Hahahaha 😂 😂! Anyway 🔛 🔛, yea 😀 💯, gotta 👉 👉 go 🏃 🏃 do tasks ✔ 📋. Hahahaha 😂 😂!")
            .mentionRepliedUser(false).queue();
    }
}
