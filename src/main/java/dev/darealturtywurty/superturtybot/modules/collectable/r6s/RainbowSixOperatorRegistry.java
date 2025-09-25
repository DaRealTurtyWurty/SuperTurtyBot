package dev.darealturtywurty.superturtybot.modules.collectable.r6s;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import dev.darealturtywurty.superturtybot.registry.Registry;

@Registerer
public class RainbowSixOperatorRegistry {
    public static final Registry<RainbowSixOperatorCollectable> RAINBOW_SIX_OPERATOR_REGISTRY = new Registry<>();

    public static RainbowSixOperatorCollectable register(String name, RainbowSixOperatorCollectable.Builder builder) {
        return RAINBOW_SIX_OPERATOR_REGISTRY.register(name, builder.build());
    }

    public static final RainbowSixOperatorCollectable SLEDGE = register("sledge", RainbowSixOperatorCollectable.builder()
            .name("Sledge")
            .emoji("sledge")
            .question("What 3 letter agency is Sledge from?")
            .answer("SAS")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable THATCHER = register("thatcher", RainbowSixOperatorCollectable.builder()
            .name("Thatcher")
            .emoji("thatcher")
            .question("What type of \"Grenade\" does Thatcher use?")
            .answer("EMP")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable MUTE = register("mute", RainbowSixOperatorCollectable.builder()
            .name("Mute")
            .emoji("mute")
            .question("What type of jammer does Mute use?")
            .answer("Signal Disruptor")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable ASH = register("ash", RainbowSixOperatorCollectable.builder()
            .name("Ash")
            .emoji("ash")
            .question("True or False: Ash is a hard breacher.")
            .answerNo()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable THERMITE = register("thermite", RainbowSixOperatorCollectable.builder()
            .name("Thermite")
            .emoji("thermite")
            .question("What type of breach does Thermite use?")
            .answer("Exothermic Charge")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable CASTLE = register("castle", RainbowSixOperatorCollectable.builder()
            .name("Castle")
            .emoji("castle")
            .question("How many melee hits can Castle's barricades take?")
            .answer(9)
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable PULSE = register("pulse", RainbowSixOperatorCollectable.builder()
            .name("Pulse")
            .emoji("pulse")
            .question("What phrase does a popular streamer use to describe Pulse's gadget?")
            .answer("Cardiac Sensor deployed")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable TWITCH = register("twitch", RainbowSixOperatorCollectable.builder()
            .name("Twitch")
            .emoji("twitch")
            .question("How many shocks does it take for a Twitch drone to destroy Frost's welcome mat? (0 for not possible)")
            .answer(3)
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable MONTAGNE = register("montagne", RainbowSixOperatorCollectable.builder()
            .name("Montagne")
            .emoji("montagne")
            .question("What is another name for Montagne's shield?")
            .answer("Le Roc")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable DOC = register("doc", RainbowSixOperatorCollectable.builder()
            .name("Doc")
            .emoji("doc")
            .question("What is Doc's real first name?")
            .answer("Gustave")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable ROOK = register("rook", RainbowSixOperatorCollectable.builder()
            .name("Rook")
            .emoji("rook")
            .question("What is another name for a single piece of Rook's armor?")
            .answer()
                .or("Rhino Armor", "Trauma Plate")
                .finish()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable GLAZ = register("glaz", RainbowSixOperatorCollectable.builder()
            .name("Glaz")
            .emoji("glaz")
            .question("On which map does Glax have a unique buff on?")
            .answer()
                .or("Plane", "Presidential Plane")
                .finish()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable FUZE = register("fuze", RainbowSixOperatorCollectable.builder()
            .name("Fuze")
            .emoji("fuze")
            .question("How many grenades does Fuze's gadget deploy?")
            .answer(5)
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable KAPKAN = register("kapkan", RainbowSixOperatorCollectable.builder()
            .name("Kapkan")
            .emoji("kapkan")
            .question("True or False: Kapkan EDDs can detonate whilst meleeing a barricade.")
            .answerYes()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable OG_TACHANKA = register("og_tachanka", RainbowSixOperatorCollectable.builder()
            .name("OG Tachanka")
            .emoji("og_tachanka")
            .question("Which operator does Tachanka often flirt with?")
            .answer("Finka")
            .rarity(CollectableRarity.MYTHICAL));

    public static final RainbowSixOperatorCollectable TACHANKA = register("tachanka", RainbowSixOperatorCollectable.builder()
            .name("Tachanka")
            .emoji("tachanka")
            .question("How much ammo does Tachanka's Shumika Launcher have in reserve?")
            .answer(15)
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable BLITZ = register("blitz", RainbowSixOperatorCollectable.builder()
            .name("Blitz")
            .emoji("blitz")
            .question("Which non-Ubisoft game is there a skin for Blitz?")
            .answer("Dead by Daylight")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable IQ = register("iq", RainbowSixOperatorCollectable.builder()
            .name("IQ")
            .emoji("iq")
            .question("What weapon skin can IQ obtain from completing Article 5?")
            .answer("Bartlett University")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable JAGER = register("jager", RainbowSixOperatorCollectable.builder()
            .name("Jäger")
            .emoji("jager")
            .question("Can Jäger's ADS intercept Zero's Argus cameras?")
            .answerYes()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable BANDIT = register("bandit", RainbowSixOperatorCollectable.builder()
            .name("Bandit")
            .emoji("bandit")
            .question("Do Bandit's batteries affect the effectiveness of Maverick's blowtorch?")
            .answerNo()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable RECRUIT = register("recruit", RainbowSixOperatorCollectable.builder()
            .name("Recruit")
            .emoji("recruit")
            .question("What are the 5 colors of the Recruit's uniforms?")
            .answer()
                .segments("Blue", "Green", "Orange", "Red", "Yellow")
                .finish()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable BUCK = register("buck", RainbowSixOperatorCollectable.builder()
            .name("Buck")
            .emoji("buck")
            .question("What is the name of Buck's shotgun attachment?")
            .answer("Skeleton Key")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable FROST = register("frost", RainbowSixOperatorCollectable.builder()
            .name("Frost")
            .emoji("frost")
            .question("Which operator might Frost be in a romantic relationship with?")
            .answer("Fenrir")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable OG_BLACKBEARD = register("og_blackbeard", RainbowSixOperatorCollectable.builder()
            .name("OG Blackbeard")
            .emoji("blackbeard")
            .question("How much HP does OG Blackbeard's shield have?")
            .answer(20)
            .rarity(CollectableRarity.LEGENDARY));

    public static final RainbowSixOperatorCollectable BLACKBEARD = register("blackbeard", RainbowSixOperatorCollectable.builder()
            .name("Blackbeard")
            .emoji("blackbeard")
            .question("What is the name of Blackbeard's rifle?")
            .answer()
                .or("SR-25", "Mk17 CQB")
                .finish()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable VALKYRIE = register("valkyrie", RainbowSixOperatorCollectable.builder()
            .name("Valkyrie")
            .emoji("valkyrie")
            .question("What colour tint do Valkyrie's Black Eye cameras have?")
            .answer("Blue")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable CAPITAO = register("capitao", RainbowSixOperatorCollectable.builder()
            .name("Capitão")
            .emoji("capitao")
            .question("What are the name of Capitão's fire bolts?")
            .answer("Asphyxiating Bolts")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable CAVEIRA = register("caveira", RainbowSixOperatorCollectable.builder()
            .name("Caveira")
            .emoji("caveira")
            .question("What text shows up on the enemy team when Caveira successfully interrogates an enemy?")
            .answer("Location revealed by Caveira")
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable HIBANA = register("hibana", RainbowSixOperatorCollectable.builder()
            .name("Hibana")
            .emoji("hibana")
            .question("What type of pellets does Hibana's gadget fire?")
            .answer("X-KAIROS")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable ECHO = register("echo", RainbowSixOperatorCollectable.builder()
            .name("Echo")
            .emoji("echo")
            .question("What is the name of Echo's drone?")
            .answer("Yokai")
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable JACKAL = register("jackal", RainbowSixOperatorCollectable.builder()
            .name("Jackal")
            .emoji("jackal")
            .question("What body part does Jackal's gadget track?")
            .answer()
                .or("Feet", "Foot", "Footprints", "Footprint", "Sole", "Soles")
                .finish()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable MIRA = register("mira", RainbowSixOperatorCollectable.builder()
            .name("Mira")
            .emoji("mira")
            .question("True or False: Maverick can instantly destroy a Mira Black Mirror with his blowtorch.")
            .answerYes()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable YING = register("ying", RainbowSixOperatorCollectable.builder()
            .name("Ying")
            .emoji("ying")
            .question("What is the name of Ying's gadget?")
            .answer("Candela")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable LESION = register("lesion", RainbowSixOperatorCollectable.builder()
            .name("Lesion")
            .emoji("lesion")
            .question("Can you melee Lesion's Gu Mines without triggering them?")
            .answerNo()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable ZOFIA = register("zofia", RainbowSixOperatorCollectable.builder()
            .name("Zofia")
            .emoji("zofia")
            .question("What relation is Zofia to Ela?")
            .answer("Sister")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable ELA = register("ela", RainbowSixOperatorCollectable.builder()
            .name("Ela")
            .emoji("ela")
            .question("What is the name of Ela's concussion mines?")
            .answer("Grzmot Mine")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable DOKKAEBI = register("dokkaebi", RainbowSixOperatorCollectable.builder()
            .name("Dokkaebi")
            .emoji("dokkaebi")
            .question("What is the name of Dokkaebi's phone hacking gadget?")
            .answer("Logic Bomb")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable VIGIL = register("vigil", RainbowSixOperatorCollectable.builder()
            .name("Vigil")
            .emoji("vigil")
            .question("Operation White Noise introduced Vigil alongside how many other operators?")
            .answer(2)
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable LION = register("lion", RainbowSixOperatorCollectable.builder()
            .name("Lion")
            .emoji("lion")
            .question("True or False: Lion's EE-ONE-D scan reveals defenders who stay perfectly still.")
            .answerNo()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable FINKA = register("finka", RainbowSixOperatorCollectable.builder()
            .name("Finka")
            .emoji("finka")
            .question("True or False: Finka's Adrenal Surge can revive a downed teammate through solid walls.")
            .answerYes()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable MAESTRO = register("maestro", RainbowSixOperatorCollectable.builder()
            .name("Maestro")
            .emoji("maestro")
            .question("When Maestro fires an Evil Eye, what happens to the camera's protective casing?")
            .answer()
                .or("It opens", "It retracts", "It lifts up")
                .finish()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable ALIBI = register("alibi", RainbowSixOperatorCollectable.builder()
            .name("Alibi")
            .emoji("alibi")
            .question("How many Prisma decoys can Alibi deploy in a round?")
            .answer(3)
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable MAVERICK = register("maverick", RainbowSixOperatorCollectable.builder()
            .name("Maverick")
            .emoji("maverick")
            .question("True or False: Maverick can use his blowtorch to create a hole in a reinforced wall.")
            .answerYes()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable OG_CLASH = register("og_clash", RainbowSixOperatorCollectable.builder()
            .name("OG Clash")
            .emoji("clash")
            .question("Could Kali's LV Explosive Lances destroy Clash's shield?")
            .answerNo()
            .rarity(CollectableRarity.MYTHICAL));

    public static final RainbowSixOperatorCollectable CLASH = register("clash", RainbowSixOperatorCollectable.builder()
            .name("Clash")
            .emoji("clash")
            .question("What is the name of Clash's shield?")
            .answer("CCE Shield")
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable NOMAD = register("nomad", RainbowSixOperatorCollectable.builder()
            .name("Nomad")
            .emoji("nomad")
            .question("True or False: Nomad's Airjab can be used to destroy defender gadgets.")
            .answerNo()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable KAID = register("kaid", RainbowSixOperatorCollectable.builder()
            .name("Kaid")
            .emoji("kaid")
            .question("True or False: Kaid's Electroclaw can electrify through walls and floors.")
            .answerYes()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable GRIDLOCK = register("gridlock", RainbowSixOperatorCollectable.builder()
            .name("Gridlock")
            .emoji("gridlock")
            .question("What is the name of Gridlock's gadget?")
            .answer("Trax Stingers")
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable MOZZIE = register("mozzie", RainbowSixOperatorCollectable.builder()
            .name("Mozzie")
            .emoji("mozzie")
            .question("What is Mozzie's nationality?")
            .answer()
                .or("Australian", "Aussie", "Ozzy", "Ozzie", "Australia")
                .finish()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable NOKK = register("nokk", RainbowSixOperatorCollectable.builder()
            .name("Nøkk")
            .emoji("nokk")
            .question("True or False: Nøkk's face has been revealed in-game.")
            .answerNo()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable WARDEN = register("warden", RainbowSixOperatorCollectable.builder()
            .name("Warden")
            .emoji("r6_warden")
            .question("True or False: Warden's Glance Smart Glasses can see through smoke.")
            .answerYes()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable AMARU = register("amaru", RainbowSixOperatorCollectable.builder()
            .name("Amaru")
            .emoji("amaru")
            .question("Does Amaru's Garra Hook allow her to rappel up and over hatches?")
            .answerYes()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable OG_GOYO = register("og_goyo", RainbowSixOperatorCollectable.builder()
            .name("OG Goyo")
            .emoji("goyo")
            .question("What is the name of Goyo's shield?")
            .answer("Volcan Shield")
            .rarity(CollectableRarity.LEGENDARY));

    public static final RainbowSixOperatorCollectable GOYO = register("goyo", RainbowSixOperatorCollectable.builder()
            .name("Goyo")
            .emoji("goyo")
            .question("What type of damage do Goyo's Volcan Canisters deal when they explode?")
            .answer("Fire")
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable KALI = register("kali", RainbowSixOperatorCollectable.builder()
            .name("Kali")
            .emoji("kali")
            .question("Can Kali's LV Explosive Lances be disrupted by Mute's Signal Disruptors?")
            .answerNo()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable WAMAI = register("wamai", RainbowSixOperatorCollectable.builder()
            .name("Wamai")
            .emoji("wamai")
            .question("Can Mag-NETs be EMP'd?")
            .answerYes()
            .rarity(CollectableRarity.UNCOMMON));


    public static final RainbowSixOperatorCollectable IANA = register("iana", RainbowSixOperatorCollectable.builder()
            .name("Iana")
            .emoji("iana")
            .question("True or False: Vigil can hide from Iana's Gemini Replicator.")
            .answerYes()
            .rarity(CollectableRarity.LEGENDARY));

    public static final RainbowSixOperatorCollectable ORYX = register("oryx", RainbowSixOperatorCollectable.builder()
            .name("Oryx")
            .emoji("oryx")
            .question("Other than Oryx main dash ability, what other ability does he have?")
            .answer()
                .or("Climb up hatches", "Climb hatches", "Hatch climb", "Hatch climbing")
                .finish()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable ACE = register("ace", RainbowSixOperatorCollectable.builder()
            .name("Ace")
            .emoji("ace")
            .question("What is the name of Ace's breaching gadget?")
            .answer("S.E.L.M.A. Aqua Breacher")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable MELUSI = register("melusi", RainbowSixOperatorCollectable.builder()
            .name("Melusi")
            .emoji("melusi")
            .question("What is the name of Melusi's gadget?")
            .answer("Banshee")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable ZERO = register("zero", RainbowSixOperatorCollectable.builder()
            .name("Zero")
            .emoji("zero")
            .question("Can Zero's Argus cameras pierce through reinforced walls?")
            .answerYes()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable ARUNI = register("aruni", RainbowSixOperatorCollectable.builder()
            .name("Aruni")
            .emoji("aruni")
            .question("What is Aruni's secondary unique ability?")
            .answer()
                .or("Stronger punches", "Stronger punch", "Hardened punches", "Hardened punch", "Hardened knuckles", "Stronger knuckles", "Stronger melee", "Hardened melee")
                .finish()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable FLORES = register("flores", RainbowSixOperatorCollectable.builder()
            .name("Flores")
            .emoji("flores")
            .question("Can RCE-Ratero drones be hacked by Mozzie?")
            .answerYes()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable THUNDERBIRD = register("thunderbird", RainbowSixOperatorCollectable.builder()
            .name("Thunderbird")
            .emoji("thunderbird")
            .question("What is the name of the operation that introduced Thunderbird?")
            .answer("North Star")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable OSA = register("osa", RainbowSixOperatorCollectable.builder()
            .name("Osa")
            .emoji("osa")
            .question("True or False: Osa's Talon-8 shield can be picked up after being cracked.")
            .answerNo()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable THORN = register("thorn", RainbowSixOperatorCollectable.builder()
            .name("Thorn")
            .emoji("thorn")
            .question("What is the name of Thorn's gadget?")
            .answer("Razorbloom Shell")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable AZAMI = register("azami", RainbowSixOperatorCollectable.builder()
            .name("Azami")
            .emoji("azami")
            .question("How many melee hits can Azami's Kiba Barrier take?")
            .answer(3)
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable SENS = register("sens", RainbowSixOperatorCollectable.builder()
            .name("Sens")
            .emoji("sens")
            .question("True or False: Sens's gadget deals damage to defenders who pass through it.")
            .answerNo()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable GRIM = register("grim", RainbowSixOperatorCollectable.builder()
            .name("Grim")
            .emoji("grim")
            .question("Can Vigil prevent being pinged by Grim's gadget?")
            .answerYes()
            .rarity(CollectableRarity.UNCOMMON));

    public static final RainbowSixOperatorCollectable SOLIS = register("solis", RainbowSixOperatorCollectable.builder()
            .name("Solis")
            .emoji("solis")
            .question("What major nerf did Solis receive in a patch after release?")
            .answer()
                .or("Gadget can no longer be used in prep phase", "Gadget cannot be used in prep phase", "Cannot use gadget in prep phase", "Gadget disabled in prep phase")
                .finish()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable BRAVA = register("brava", RainbowSixOperatorCollectable.builder()
            .name("Brava")
            .emoji("brava")
            .question("If a Kludge Drone is hacked by a Mozzie pest, can Nomad's Airjabs be hacked by the Kludge Drone?")
            .answerYes()
            .rarity(CollectableRarity.LEGENDARY));

    public static final RainbowSixOperatorCollectable FENRIR = register("fenrir", RainbowSixOperatorCollectable.builder()
            .name("Fenrir")
            .emoji("fenrir")
            .question("True or False: Fenrir's Dread Mines **used** to be bulletproof.")
            .answerYes()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable RAM = register("ram", RainbowSixOperatorCollectable.builder()
            .name("Ram")
            .emoji("ram")
            .question("Can BU-GI's be hacked by mozzie pests?")
            .answerNo()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable TUBARAO = register("tubarao", RainbowSixOperatorCollectable.builder()
            .name("Tubarão")
            .emoji("tubarao")
            .question("Zoto Canisters freeze all gadgets except for which attacker gadget?")
            .answer()
                .or("Maverick's blowtorch", "Maverick blowtorch", "Maverick's torch", "Maverick torch", "Breaching torch", "Breaching blowtorch", "Tactical Breaching Torch")
                .finish()
            .rarity(CollectableRarity.EPIC));

    // Deimos, Striker, Sentry, Skopos, Rauora, Denari

    public static final RainbowSixOperatorCollectable DEIMOS = register("deimos", RainbowSixOperatorCollectable.builder()
            .name("Deimos")
            .emoji("deimos")
            .question("What was the first name of the prominent character in the Rainbow Six Universe that Deimos killed?")
            .answer()
                .or("Harry", "Six", "Harry Pandey", "Harry 'Six' Pandey", "Six Pandey")
                .finish()
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable STRIKER = register("striker", RainbowSixOperatorCollectable.builder()
            .name("Striker")
            .emoji("striker")
            .question("Is striker male or female?")
            .answer("Female")
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable SENTRY = register("sentry", RainbowSixOperatorCollectable.builder()
            .name("Sentry")
            .emoji("sentry")
            .question("True or False: According to the lore of the game, Sentry is dead.")
            .answerYes()
            .rarity(CollectableRarity.COMMON));

    public static final RainbowSixOperatorCollectable SKOPOS = register("skopos", RainbowSixOperatorCollectable.builder()
            .name("Skopós")
            .emoji("skopos")
            .question("What item does Skopós drop on the ground instead of a mobile phone when a Dokkaebi is present?")
            .answer("Circuit Board")
            .rarity(CollectableRarity.RARE));

    public static final RainbowSixOperatorCollectable RAUORA = register("rauora", RainbowSixOperatorCollectable.builder()
            .name("Rauora")
            .emoji("rauora")
            .question("Can D.O.M. panels be placed where an Aruni gate is present?")
            .answerNo()
            .rarity(CollectableRarity.EPIC));

    public static final RainbowSixOperatorCollectable DENARI = register("denari", RainbowSixOperatorCollectable.builder()
            .name("Denari")
            .emoji("denari")
            .question("What country is Denari from?")
            .answer("Switzerland")
            .rarity(CollectableRarity.LEGENDARY));

    public static void load() {
        Constants.LOGGER.info("Loaded Rainbow Six Operator Collectables: {}", RAINBOW_SIX_OPERATOR_REGISTRY.size());
    }
}
