package dev.darealturtywurty.superturtybot.modules.collectable.minecraft;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.collectable.Answer;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import dev.darealturtywurty.superturtybot.registry.Registry;

@Registerer
public class MinecraftMobRegistry {
    public static final Registry<MinecraftMobCollectable> MOB_REGISTRY = new Registry<>();

    public static MinecraftMobCollectable register(String name, MinecraftMobCollectable.Builder builder) {
        return MOB_REGISTRY.register(name, builder.build());
    }

    public static final MinecraftMobCollectable ALLAY = register("allay", new MinecraftMobCollectable.Builder()
            .name("Allay")
            .emoji("allay")
            .question("What is the name of the movie that is the inspiration for the allay advancement?")
            .answer("Toy Story")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable ARMADILLO = register("armadillo", new MinecraftMobCollectable.Builder()
            .name("Armadillo")
            .emoji("armadillo")
            .question("How many unique (doesn't appear in other entities) entity data properties does the armadillo have?")
            .answer(2)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable AXOLOTL = register("axolotl", new MinecraftMobCollectable.Builder()
            .name("Axolotl")
            .emoji("pink_axolotl")
            .question("How many colors can the axolotl come in?")
            .answer(5)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable BAT = register("bat", new MinecraftMobCollectable.Builder()
            .name("Bat")
            .emoji("bat")
            .question("True or False: Bats drop experience when killed.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable CAMEL = register("camel", new MinecraftMobCollectable.Builder()
            .name("Camel")
            .emoji("camel")
            .question("How many individual items can be \"used\" on a camel?")
            .answer(3)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable CAT = register("cat", new MinecraftMobCollectable.Builder()
            .name("Cat")
            .emoji("cat")
            .question("Stray cats will attack which baby (and only baby) mob?")
            .answer("Turtle")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable CHICKEN = register("chicken", new MinecraftMobCollectable.Builder()
            .name("Chicken")
            .emoji("chicken")
            .question("In Java Edition, how many different mobs can spawn as a chicken jockey?")
            .answer(4)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable COD = register("cod", new MinecraftMobCollectable.Builder()
            .name("Cod")
            .emoji("cod")
            .question("What is the rarest item that cod can drop? (Java Edition)")
            .answer("Bone Meal")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable COW = register("cow", new MinecraftMobCollectable.Builder()
            .name("Cow")
            .emoji("cow")
            .question("True or False: Cows will attempt to avoid walking into berry bushes.")
            .answerTrue()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable DONKEY = register("donkey", new MinecraftMobCollectable.Builder()
            .name("Donkey")
            .emoji("donkey")
            .question("How many slots does a donkey have in its chest?")
            .answer(15)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable FROG = register("frog", new MinecraftMobCollectable.Builder()
            .name("Frog")
            .emoji("frog")
            .question("How many blocks high can frogs jump?")
            .answer(8)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable GLOW_SQUID = register("glow_squid", new MinecraftMobCollectable.Builder()
            .name("Glow Squid")
            .emoji("glow_squid")
            .question("Can glow squids swim in lava?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable HORSE = register("horse", new MinecraftMobCollectable.Builder()
            .name("Horse")
            .emoji("horse")
            .question("How many different colours of horse are there?")
            .answer(7)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable MOOSHROOM = register("mooshroom", new MinecraftMobCollectable.Builder()
            .name("Mooshroom")
            .emoji("mooshroom")
            .question("What is the name of the biome that mooshrooms spawn in?")
            .answer("Mushroom Fields")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable MULE = register("mule", new MinecraftMobCollectable.Builder()
            .name("Mule")
            .emoji("mule")
            .question("What is the maximum amount of health a mule can have?")
            .answer(30)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable OCELOT = register("ocelot", new MinecraftMobCollectable.Builder()
            .name("Ocelot")
            .emoji("ocelot")
            .question("True or False: Ocelots can be tamed.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable PARROT = register("parrot", new MinecraftMobCollectable.Builder()
            .name("Parrot")
            .emoji("parrot")
            .question("True or False: There is a splash text noting to not feed parrots avocados.")
            .answerTrue()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable PIG = register("pig", new MinecraftMobCollectable.Builder()
            .name("Pig")
            .emoji("pig")
            .question("Can pigs be bred with beetroots?")
            .answerYes()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable PUFFERFISH = register("pufferfish", new MinecraftMobCollectable.Builder()
            .name("Pufferfish")
            .emoji("pufferfish")
            .question("What is the name of the enchantment that can be used to deal additional damage to pufferfish?")
            .answer("Impaling")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable RABBIT = register("rabbit", new MinecraftMobCollectable.Builder()
            .name("Rabbit")
            .emoji("rabbit")
            .question("Will rabbits eat your crops?")
            .answerYes()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable SALMON = register("salmon", new MinecraftMobCollectable.Builder()
            .name("Salmon")
            .emoji("salmon")
            .question("Do salmon only spawn in rivers?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable SHEEP = register("sheep", new MinecraftMobCollectable.Builder()
            .name("Sheep")
            .emoji("sheep")
            .question("How many hearts did sheep have in Beta Minecraft?")
            .answer(5)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable SKELETON_HORSE = register("skeleton_horse", new MinecraftMobCollectable.Builder()
            .name("Skeleton Horse")
            .emoji("skeleton_horse")
            .question("How many horses can spawn in a skeleton horse trap?")
            .answer(4)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.MYTHICAL));

    public static final MinecraftMobCollectable SNIFFER = register("sniffer", new MinecraftMobCollectable.Builder()
            .name("Sniffer")
            .emoji("sniffer")
            .question("True or False: The sniffer can sniff out of podzol blocks.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable SNOW_GOLEM = register("snow_golem", new MinecraftMobCollectable.Builder()
            .name("Snow Golem")
            .emoji("snow_golem")
            .question("Can snow golems spawn naturally? (Java Edition)")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable SQUID = register("squid", new MinecraftMobCollectable.Builder()
            .name("Squid")
            .emoji("squid")
            .question("How many tentacles does a squid have?")
            .answer(10)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable STRIDER = register("strider", new MinecraftMobCollectable.Builder()
            .name("Strider")
            .emoji("strider")
            .question("Are striders harmed by snowballs?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable TADPOLE = register("tadpole", new MinecraftMobCollectable.Builder()
            .name("Tadpole")
            .emoji("tadpole")
            .question("What item will cause tadpoles to follow the player?")
            .answer("Slime Ball")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable TROPICAL_FISH = register("tropical_fish", new MinecraftMobCollectable.Builder()
            .name("Tropical Fish")
            .emoji("tropical_fish")
            .question("How many natural variations of tropical fish are there?")
            .answer(2700)
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable TURTLE = register("turtle", new MinecraftMobCollectable.Builder()
            .name("Turtle")
            .emoji("turtle")
            .question("Can turtles spawn on stoney shores?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable VILLAGER = register("villager", new MinecraftMobCollectable.Builder()
            .name("Villager")
            .emoji("villager")
            .question("What was the profession of the villager that was commonly referred to as Dr. Trayaurus (from DanTDM's videos)?")
            .answer("Librarian")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable WANDERING_TRADER = register("wandering_trader", new MinecraftMobCollectable.Builder()
            .name("Wandering Trader")
            .emoji("wandering_trader")
            .question("What block will wandering traders choose to spawn at if it exists near the player?")
            .answer("Bell")
            .category(MinecraftMobCollectable.MobCategory.PASSIVE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable BEE = register("bee", new MinecraftMobCollectable.Builder()
            .name("Bee")
            .emoji("bee")
            .question("How many different types of trees can spawn with bee nests?")
            .answer(4)
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable CAVE_SPIDER = register("cave_spider", new MinecraftMobCollectable.Builder()
            .name("Cave Spider")
            .emoji("cave_spider")
            .question("Which mob are cave spiders scared of?")
            .answer("Armadillo")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable DOLPHIN = register("dolphin", new MinecraftMobCollectable.Builder()
            .name("Dolphin")
            .emoji("dolphin")
            .question("Can dolphins survive out of water if it is raining?")
            .answerYes()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable DROWNED = register("drowned", new MinecraftMobCollectable.Builder()
            .name("Drowned")
            .emoji("drowned")
            .question("What 3 items can the drowned spawn with (in hand)?")
            .answer()
                .segments("Trident", "Nautilus Shell", "Fishing Rod")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable ENDERMAN = register("enderman", new MinecraftMobCollectable.Builder()
            .name("Enderman")
            .emoji("enderman")
            .question("What two overworld biomes can the enderman **NOT** spawn in?")
            .answer()
                .segments("Mushroom Fields", "Deep Dark")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable FOX = register("fox", new MinecraftMobCollectable.Builder()
            .name("Fox")
            .emoji("fox")
            .question("What is the name of the modloader that uses the fox as its logo?")
            .answer("NeoForge")
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable GOAT = register("goat", new MinecraftMobCollectable.Builder()
            .name("Goat")
            .emoji("goat")
            .question("Can goats regrow their horns?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable IRON_GOLEM = register("iron_golem", new MinecraftMobCollectable.Builder()
            .name("Iron Golem")
            .emoji("iron_golem")
            .question("How many iron ingots does it take to create an iron golem?")
            .answer(36)
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable LLAMA = register("llama", new MinecraftMobCollectable.Builder()
            .name("Llama")
            .emoji("llama")
            .question("What is the maximum number of slots a llama can have in its chest?")
            .answer(15)
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable PANDA = register("panda", new MinecraftMobCollectable.Builder()
            .name("Panda")
            .emoji("panda")
            .question("What are the different personalities that pandas can have?")
            .answer()
                .segments("Normal", "Lazy", "Worried", "Playful", "Aggressive", "Weak", "Brown")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable PIGLIN = register("piglin", new MinecraftMobCollectable.Builder()
            .name("Piglin")
            .emoji("piglin")
            .question("What two biomes can piglins spawn in?")
            .answer()
                .segments("Crimson Forest", "Nether Wastes")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable POLAR_BEAR = register("polar_bear", new MinecraftMobCollectable.Builder()
            .name("Polar Bear")
            .emoji("polar_bear")
            .question("Which fish is more common to drop from a polar bear?")
            .answer("Cod")
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable SPIDER = register("spider", new MinecraftMobCollectable.Builder()
            .name("Spider")
            .emoji("spider")
            .question("Name 2 possible status effects that the spider can spawn with.")
            .answer()
                .anyNof(2, "Strength", "Speed", "Invisibility", "Regeneration")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable TRADER_LLAMA = register("trader_llama", new MinecraftMobCollectable.Builder()
            .name("Trader Llama")
            .emoji("trader_llama")
            .question("Can trader llamas be tamed?")
            .answerYes()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable WOLF = register("wolf", new MinecraftMobCollectable.Builder()
            .name("Wolf")
            .emoji("wolf")
            .question("How many wolf variants are there?")
            .answer(9)
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable ZOMBIFIED_PIGLIN = register("zombified_piglin", new MinecraftMobCollectable.Builder()
            .name("Zombified Piglin")
            .emoji("zombified_piglin")
            .question("True or False: Zombie Pigmen can drown.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.NEUTRAL)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable BLAZE = register("blaze", new MinecraftMobCollectable.Builder()
            .name("Blaze")
            .emoji("blaze")
            .question("True or False: Blazes take damage from snowy weather.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable BOGGED = register("bogged", new MinecraftMobCollectable.Builder()
            .name("Bogged")
            .emoji("bogged")
            .question("What two items can you get from shearing a bogged?")
            .answer()
                .segments("Brown Mushroom", "Red Mushroom")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable BREEZE = register("breeze", new MinecraftMobCollectable.Builder()
            .name("Breeze")
            .emoji("breeze")
            .question("Can a breeze's wind charge extinguish a fire?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable CREEPER = register("creeper", new MinecraftMobCollectable.Builder()
            .name("Creeper")
            .emoji("creeper")
            .question("What is the name of the mob that the creeper was meant to be?")
            .answer("Pig")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable ELDER_GUARDIAN = register("elder_guardian", new MinecraftMobCollectable.Builder()
            .name("Elder Guardian")
            .emoji("elder_guardian")
            .question("How many elder guardians can spawn in an ocean monument?")
            .answer(3)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable ENDERMITE = register("endermite", new MinecraftMobCollectable.Builder()
            .name("Endermite")
            .emoji("endermite")
            .question("What enchantment can be used to deal additional damage to endermites?")
            .answer("Bane of Arthropods")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable EVOKER = register("evoker", new MinecraftMobCollectable.Builder()
            .name("Evoker")
            .emoji("evoker")
            .question("What is the name of the structure that the evoker can spawn in?")
            .answer("Woodland Mansion")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable GHAST = register("ghast", new MinecraftMobCollectable.Builder()
            .name("Ghast")
            .emoji("ghast")
            .question("What are the two items that ghasts can drop?")
            .answer()
                .segments("Gunpowder", "Ghast Tear")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable GUARDIAN = register("guardian", new MinecraftMobCollectable.Builder()
            .name("Guardian")
            .emoji("guardian")
            .question("How long does the guardian's laser attack last?")
            .answer("2 seconds")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable HOGLIN = register("hoglin", new MinecraftMobCollectable.Builder()
            .name("Hoglin")
            .emoji("hoglin")
            .question("True or False: Hoglins can be bred.")
            .answerTrue()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable HUSK = register("husk", new MinecraftMobCollectable.Builder()
            .name("Husk")
            .emoji("husk")
            .question("Can husks turn directly into drowned?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable MAGMA_CUBE = register("magma_cube", new MinecraftMobCollectable.Builder()
            .name("Magma Cube")
            .emoji("magma_cube")
            .question("How many blocks high can a large magma cube jump?")
            .answer(6)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable PHANTOM = register("phantom", new MinecraftMobCollectable.Builder()
            .name("Phantom")
            .emoji("phantom")
            .question("How many days does it take for phantoms to spawn?")
            .answer(3)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable PIGLIN_BRUTE = register("piglin_brute", new MinecraftMobCollectable.Builder()
            .name("Piglin Brute")
            .emoji("piglin_brute")
            .question("What item do piglin brutes hold?")
            .answer()
                .or("Golden Axe", "Gold Axe")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable PILLAGER = register("pillager", new MinecraftMobCollectable.Builder()
            .name("Pillager")
            .emoji("pillager")
            .question("How long does the bad omen effect that is received from killing a pillager captain last?")
            .answer("100 minutes")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable RAVAGER = register("ravager", new MinecraftMobCollectable.Builder()
            .name("Ravager")
            .emoji("ravager")
            .question("What wave does the ravager start spawning in during a raid?")
            .answer(3)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable SHULKER = register("shulker", new MinecraftMobCollectable.Builder()
            .name("Shulker")
            .emoji("shulker")
            .question("How many seconds of levitation does the shulker's projectile give?")
            .answer(10)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable SILVERFISH = register("silverfish", new MinecraftMobCollectable.Builder()
            .name("Silverfish")
            .emoji("silverfish")
            .question("List 3 blocks that silverfish can hide in.")
            .answer()
                .anyNof(3, "Stone", "Cobblestone", "Stone Bricks", "Mossy Stone Bricks", "Cracked Stone Bricks", "Chiseled Stone Bricks", "Deepslate")
                .not("Mossy Cobblestone", "Andesite", "Diorite", "Granite", "Polished Andesite", "Polished Diorite", "Polished Granite", "Smooth Stone")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable SKELETON = register("skeleton", new MinecraftMobCollectable.Builder()
            .name("Skeleton")
            .emoji("skeleton")
            .question("What nether biome can skeletons spawn in?")
            .answer("Soul Sand Valley")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable SLIME = register("slime", new MinecraftMobCollectable.Builder()
            .name("Slime")
            .emoji("slime")
            .question("What is the name of the potion effect that can spawn slimes?")
            .answer("Oozing")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable STRAY = register("stray", new MinecraftMobCollectable.Builder()
            .name("Stray")
            .emoji("stray")
            .question("Name 3 biomes that strays can spawn in.")
            .answer()
                .anyNof(3, "Snowy Tundra", "Snowy Plains", "Ice Spikes", "Frozen Ocean", "Frozen River", "Deep Frozen Ocean", "Legacy Frozen Ocean")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable VEX = register("vex", new MinecraftMobCollectable.Builder()
            .name("Vex")
            .emoji("vex")
            .question("True or False: Vexes are considered undead.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable VINDICATOR = register("vindicator", new MinecraftMobCollectable.Builder()
            .name("Vindicator")
            .emoji("vindicator")
            .question("What movie is the vindicator's name \"Johnny\" a reference to?")
            .answer("The Shining")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable WARDEN = register("warden", new MinecraftMobCollectable.Builder()
            .name("Warden")
            .emoji("warden")
            .question("How many times must sculk shriekers be activated to summon a warden?")
            .answer(4)
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.MYTHICAL));

    public static final MinecraftMobCollectable WITCH = register("witch", new MinecraftMobCollectable.Builder()
            .name("Witch")
            .emoji("witch")
            .question("Name 2 potions that witches can drink.")
            .answer()
                .anyNof(2, "Instant Health", "Fire Resistance", "Swiftness", "Water Breathing")
                .not("Invisibility", "Night Vision", "Leaping", "Harming", "Poison", "Regeneration", "Strength", "Weakness", "Turtle Master", "Slow Falling")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable WITHER_SKELETON = register("wither_skeleton", new MinecraftMobCollectable.Builder()
            .name("Wither Skeleton")
            .emoji("wither_skeleton")
            .question("What is the name of the structure that wither skeletons can spawn in?")
            .answer("Nether Fortress")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable ZOGLIN = register("zoglin", new MinecraftMobCollectable.Builder()
            .name("Zoglin")
            .emoji("zoglin")
            .question("How long does it take for a hoglin to turn into a zoglin?")
            .answer("15 seconds")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable ZOMBIE = register("zombie", new MinecraftMobCollectable.Builder()
            .name("Zombie")
            .emoji("zombie")
            .question("True or False: It is possible for a zombie to drop a cooked food item.")
            .answerTrue()
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable ZOMBIE_VILLAGER = register("zombie_villager", new MinecraftMobCollectable.Builder()
            .name("Zombie Villager")
            .emoji("zombie_villager")
            .question("What is the name of the potion effect that can cure a zombie villager?")
            .answer("Weakness")
            .category(MinecraftMobCollectable.MobCategory.HOSTILE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable ENDERDRAGON = register("enderdragon", new MinecraftMobCollectable.Builder()
            .name("Ender Dragon")
            .emoji("enderdragon")
            .question("How many health points does the ender dragon have?")
            .answer(200)
            .category(MinecraftMobCollectable.MobCategory.BOSS)
            .rarity(CollectableRarity.MYTHICAL));

    public static final MinecraftMobCollectable WITHER = register("wither", new MinecraftMobCollectable.Builder()
            .name("Wither")
            .emoji("wither")
            .question("What update was the wither added to Minecraft? (in Java Edition)")
            .answer("1.4.2")
            .category(MinecraftMobCollectable.MobCategory.BOSS)
            .rarity(CollectableRarity.MYTHICAL));

    public static final MinecraftMobCollectable AGENT = register("agent", new MinecraftMobCollectable.Builder()
            .name("Agent")
            .emoji("agent")
            .question("Is the agent available in the Java Edition of Minecraft?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable GIANT = register("giant", new MinecraftMobCollectable.Builder()
            .name("Giant")
            .emoji("zombie")
            .question("True or False: Giants can spawn naturally in the game.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable ILLUSIONER = register("illusioner", new MinecraftMobCollectable.Builder()
            .name("Illusioner")
            .emoji("illusioner")
            .question("True or False: Illusioners cannot join raids.")
            .answerFalse()
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable NPC = register("npc", new MinecraftMobCollectable.Builder()
            .name("NPC")
            .emoji("npc")
            .question("Do NPCs have AI?")
            .answerNo()
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable THE_KILLER_BUNNY = register("the_killer_bunny", new MinecraftMobCollectable.Builder()
            .name("The Killer Bunny")
            .emoji("killer_bunny")
            .question("What is the name of the movie that the killer bunny is a reference to?")
            .answer("Monty Python and the Holy Grail")
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable ZOMBIE_HORSE = register("zombie_horse", new MinecraftMobCollectable.Builder()
            .name("Zombie Horse")
            .emoji("zombie_horse")
            .question("What enchantment can be used to deal additional damage to zombie horses?")
            .answer("Smite")
            .category(MinecraftMobCollectable.MobCategory.UNUSED)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable DIAMOND_CHICKEN = register("diamond_chicken", new MinecraftMobCollectable.Builder()
            .name("Diamond Chicken")
            .emoji("diamond_chicken")
            .question("What two items do diamond chickens lay instead of eggs?")
            .answer()
                .segment("Diamond")
                .segment(new Answer.OrSegment.Builder()
                        .segment("Lapis Lazuli", false, true)
                        .segment("Lapis", false, true))
                .finish()
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable LOVE_GOLEM = register("love_golem", new MinecraftMobCollectable.Builder()
            .name("Love Golem")
            .emoji("love_golem")
            .question("What is the name of the April Fools update that the love golem was added in?")
            .answer("Love and Hugs Update")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable MOON_COW = register("moon_cow", new MinecraftMobCollectable.Builder()
            .name("Moon Cow")
            .emoji("moon_cow")
            .question("Name 2 items that moon cows can drop.")
            .answer()
                .anyNof(2, "Cheese", "Glass Bottle", "Bone", "Glass")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable NERD_CREEPER = register("nerd_creeper", new MinecraftMobCollectable.Builder()
            .name("Nerd Creeper")
            .emoji("nerd_creeper")
            .question("What is the cheat code that must be entered to spawn a nerd creeper?")
            .answer("NEEEERD")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable PINK_WITHER = register("pink_wither", new MinecraftMobCollectable.Builder()
            .name("Pink Wither")
            .emoji("friendly_wither")
            .question("What item will cause the pink wither to follow the player?")
            .answer("Sugar")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.LEGENDARY));

    public static final MinecraftMobCollectable RAY_TRACING = register("ray_tracing", new MinecraftMobCollectable.Builder()
            .name("Ray Tracing")
            .emoji("ray_tracing")
            .question("What is the single phrase that will be said by a Ray Tracing (mob) when in French Mode?")
            .answer("Omelette du fromage")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable REDSTONE_BUG = register("redstone_bug", new MinecraftMobCollectable.Builder()
            .name("Redstone Bug")
            .emoji("redstone_bug")
            .question("What is the default chance of a redstone bug spawning?")
            .answer("1%")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.COMMON));

    public static final MinecraftMobCollectable SMILING_CREEPER = register("smiling_creeper", new MinecraftMobCollectable.Builder()
            .name("Smiling Creeper")
            .emoji("smiling_creeper")
            .question("What item does the smiling creeper drop when it explodes?")
            .answer("Poppy")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable BATATO = register("batato", new MinecraftMobCollectable.Builder()
            .name("Batato")
            .emoji("batato")
            .question("What dimension does the batato spawn in?")
            .answer("Potato Dimension")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.UNCOMMON));

    public static final MinecraftMobCollectable MEGA_SPUD = register("mega_spud", new MinecraftMobCollectable.Builder()
            .name("Mega Spud")
            .emoji("mega_spud")
            .question("What is the name of the structure that the mega spud can spawn in?")
            .answer("Colosseum")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable POISONOUS_POTATO_ZOMBIE = register("poisonous_potato_zombie", new MinecraftMobCollectable.Builder()
            .name("Poisonous Potato Zombie")
            .emoji("poisonous_potato_zombie")
            .question("From which mob is it possible for a poisonous potato zombie to spawn?")
            .answer("Mega Spud")
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.RARE));

    public static final MinecraftMobCollectable PLAGUEWHALE_SLAB = register("plaguewhale_slab", new MinecraftMobCollectable.Builder()
            .name("Plaguewhale Slab")
            .emoji("plague_whale_sub")
            .question("What two possible potion effects can the plaguewhale slab give?")
            .answer()
                .segments("Poison", "Wither")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static final MinecraftMobCollectable TOXIFIN_SLAB = register("toxifin_slab", new MinecraftMobCollectable.Builder()
            .name("Toxifin Slab")
            .emoji("toxifin_slab")
            .question("What 2 items can the toxifin slab drop?")
            .answer()
                .segments("Toxic Resin", "Toxic Beam")
                .finish()
            .category(MinecraftMobCollectable.MobCategory.JOKE)
            .rarity(CollectableRarity.EPIC));

    public static void load() {
        Constants.LOGGER.info("Loaded Minecraft Mob Collectables");
    }
}

