package dev.darealturtywurty.superturtybot.commands.util;

import java.util.concurrent.ThreadLocalRandom;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class TopicCommand extends CoreCommand {
    private static final String[] TOPICS = { "If you were in a circus, which character would you be?",
        "What is the worst advice you have given?", "What is one thing you should never say at a wedding?",
        "What is the worst pickup line you have ever heard?",
        "If you could only store one type of food in your pocket, what would you carry?",
        "What is the worst present you have ever received and why?",
        "If you were a farm animal, which would you be and why?", "What is the worst first date you have ever been on?",
        "Have you ever stalked someone on social media?", "What is the best part about taking a selfie?",
        "What is your favorite celebrity scandal?",
        "If you could do anything illegal without getting caught, what would you do?",
        "What is the weirdest food combination you've ever tried?",
        "Did you have an imaginary friend? What was his/her name?",
        "Have you ever had a dream where everyone was in their underwear?", "Who's your favorite comedian?",
        "Have you ever been on a blind date?", "If you could make up a school subject, what would it be?",
        "What is your very favorite letter of the alphabet? Why?",
        "If someone gave you 20 dollars, what would you buy with it?",
        "If you had a chance to eat dessert for breakfast every day, what dessert would you choose?",
        "What is the first thing you think about when you wake up in the morning?",
        "Which animal at the zoo do you like best?",
        "If you had the opportunity to invent a new ice cream flavor, what would it be?",
        "What is the silliest thing someone has ever said to you?", "What's your favorite word?",
        "Would you switch roles with your parent if you had the chance?", "Do you prefer cold weather or hot weather?",
        "If you never had to eat one vegetable, which would it be?",
        "What is/was your favorite part about school? Your least favorite?",
        "If you could be famous, would you want to? Why?", "Who is a celebrity you admire?",
        "If you could have more friends, would you?", "Where do you want to be ten years from now?",
        "If you had $100, what would you spend it on?",
        "If you were to choose one way to be disciplined, what would it be?",
        "What do you think are the best traits for a person to have?", "Would you ever get a tattoo? What would it be?",
        "If you could go anywhere in the world, where would you choose and why?",
        "What is something you wish you could do everyday?", "What are the top three things on your bucket list?",
        "How do you think you will die?", "What is the biggest risk you've ever taken?",
        "If someone gave you an envelope with your death date inside of it, would you open it?",
        "What is your idea of the perfect day?", "Do you think your priorities have changed since you were younger?",
        "What is the most memorable lesson you learned from your parents?",
        "What is the most difficult thing you've ever done?", "What's something not many people know about you?",
        "What do you like to cook the most?", "What's your favorite TV show?", "What is your favorite book?",
        "What's your dream job?", "Do you have any nicknames?", "What talent do you wish you had?",
        "Where do you see yourself living when you retire?", "What is your favorite weekend activity?",
        "Do you have any pet peeves?", "Are you a cat person or a dog person?",
        "What is the silliest thing you've posted online?", "When you die, what would you want to be reincarnated as?",
        "Who would you swap lives with for a day?", "What is the strangest gift you have ever received?",
        "What is the funniest gift you have ever given?", "Would you rather be invisible or have X-ray vision?",
        "If you could only save one item from a house fire, what would it be?",
        "What is the one food you could eat for the rest of your life?",
        "What's one movie you could watch over and over?", "Where's the most exotic place you've ever been?",
        "If you could have picked your own name, what would it be?", "What time period would you travel to?",
        "What is one thing you can't live without?", "What is your least favorite chore?",
        "If you could describe yourself in three words, what would they be?", "What instrument would you like to play?",
        "Would you want to live on a boat, a mountain or an island?",
        "If you could be an animal, what would it be and why?", "If you could be any age, what age would you choose?",
        "What's one thing you've won and how did you win it?", "What was the first job you ever had?",
        "What's the most fun project you've ever worked on?", "Have you ever won an award?",
        "How old were you when you had your first job?", "How long can you go without checking your phone?",
        "Have you ever really kept a New Year's resolution?", "What bad habits do you wish you could stop?",
        "Have you ever been stalked on social media?", "Can you tell when someone is lying?",
        "Be honest. Are you a jealous person?", "Do you prefer polaroid or digital cameras?",
        "If someone offered to tell you your future, would you accept it?", "Have you ever stolen anything?",
        "If you were to remove one social media app from your phone, which would it be and why?",
        "If you could have tea with a fictional character, who would that be?",
        "If you were on death row, what would your last meal be?",
        "If you could sit down with your 13-year old self, what would you say?",
        "If you could only pack one thing for a trip (besides clothing) what would it be?",
        "What is your spirit animal?", "What would be the title of your memoire?",
        "What would your theme song be if you had your own show?",
        "What would you do if you were home alone and the power went out?",
        "If your plane was going down, who would you would call?", "What would your rock band group be called?",
        "Batman or Superman, who would win?", "What's the worst thing one can say on a first date?",
        "Working on anything exciting lately?", "Has this been a busy time for you?",
        "What are you doing this weekend?",
        "If you had to pick any character in a book, movie, or TV show who is most similar to you, who would you choose? Why?",
        "When you were growing up, what was your dream job? Is any part of that still true?",
        "What's your biggest fear?", "Is there a charitable cause you support?",
        "If you had to pick one-skydiving, bungee jumping, or scuba diving-which would you do?",
        "Which of your family members are you most like?", "Do you think there are aliens on other planets?",
        "Who would win in a fight, a robot or a dinosaur?", "What was your worst fashion disaster?",
        "What's your plan if there was a zombie apocalypse?", "What's your favorite form of social media?",
        "What do you think is the best show on Netflix right now?",
        "Do you listen to any podcasts? Which is your favorite?", "Have you been on any interesting trips lately?",
        "What do you think has been the best movie of the year so far?",
        "Do you think you're an introvert or an extrovert?", "What's your strangest hidden talent?",
        "Where would you go on vacation if you had no budget?", "What is one thing you can't live without?",
        "How many countries have you been to?", "What's your favorite city you've visited?",
        "Would you rather travel via plane or boat?", "Would you rather be really hot or really cold?",
        "What are your thoughts on the British royal family?",
        "Do you like documentaries? Have you watched any good ones recently?", "What's your favorite sport?",
        "What sport do you wish you were really good at?", "Do you have any pets?", "What was the last movie you saw?",
        "What hobby do you wish you had more time for?", "If you weren't here, what would you be doing?",
        "Do you like to cook?", "What song have you had stuck in your head this week?",
        "What is something you've failed at recently?", "What's the biggest challenge you've taken on this year?",
        "Do you ever sing in the shower?", "What's the best prank you've ever played on someone?",
        "What do you think is the funniest movie ever?", "What weird conspiracy theory do you believe?",
        "Is a hot dog a sandwich?",
        "\"Go to bed, you'll feel better in the morning\" is the human version of \"Did you turn it off and turn it back on again?\"",
        "Maybe plants are really farming us, giving us oxygen until we eventually expire and turn into mulch which they can consume.",
        "Lawyers hope you get sued, doctors hope you get sick, cops hope you're criminal, mechanics hope you have car trouble, but only a thief wishes prosperity for you." };

    public TopicCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets a random topic to talk about. Great for a conversation starter.";
    }

    @Override
    public String getName() {
        return "topic";
    }

    @Override
    public String getRichName() {
        return "Topic";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String topic = TOPICS[ThreadLocalRandom.current().nextInt(TOPICS.length - 1)];
        reply(event, topic);
    }
}
