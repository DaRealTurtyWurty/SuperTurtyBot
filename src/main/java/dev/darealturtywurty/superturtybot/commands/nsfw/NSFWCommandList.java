package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.FileUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import okhttp3.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand.addRegenerateButton;

public class NSFWCommandList {
    public record NSFWReddit(String name, String... subreddits) {
    }

    public static void addAll(Set<NSFWReddit> cmds) {
        cmds.add(new NSFWReddit("ass", "Ass", "BigAsses", "AssOnTheGlass", "SpreadEm", "TheUnderbun", "Booty",
                "booty_gifs"));
        cmds.add(new NSFWReddit("porn", "porn", "PornVids", "pornstarhq", "porninaminute", "suicidegirls"));
        cmds.add(
                new NSFWReddit("nsfw", "nsfw", "NSFW_GIFS", "NSFW_HTML5", "NSFW_Videos", "HighResNSFW", "NSFWOutfits"));
        cmds.add(new NSFWReddit("gay", "ladybonersgw", "GayBears", "massivecock", "GaybrosGoneWild", "penis",
                "monsterdicks", "ThickDick", "GaybrosGoneWild", "TotallyStraight", "gayporn", "twinks", "broslikeus",
                "CumFromAnal", "gaymersgonewild", "GayGifs", "gaynsfw", "HomemadeGayPorn", "gaycumsluts", "Men2Men",
                "yaoi", "gfur", "CumCannonAddicts", "gaybears", "gaycruising", "GayKink", "NSFW_GAY", "gaystrugglefuck",
                "lovegaymale", "BarebackGayPorn", "gayotters", "TwinkLove", "GayDaddiesPics", "gayholdthemoan",
                "Frotting", "GayFreeUse", "GaySnapchatImages", "gayasshole", "manlove", "helpinghand", "GayWatersports",
                "ManSex", "GayChubs", "AlphaMalePorn", "BHMGoneWild", "gaycumshots", "GayBlowjobs", "gaybreeding",
                "gayrimming", "rule34gay", "blackballed", "BisexualHentai", "baramanga", "MrBlowJob", "GayCreampie",
                "GayCocksuckers", "Gaycouplesgonewild", "GaySex", "boysfucking", "GayNSFWFunny", "GayWincest",
                "GaySelfies", "frot", "Gfurcomics", "Scally", "BateBuds", "VintageGayPics", "GayHiddenCams",
                "HelixStudios", "GayPOCxxx", "gayfacials", "GayYoungOldPorn", "GayPokePorn", "GuysFingering",
                "Homosexual", "guyskissing", "gayfootfetish", "boysandtoys", "GayPornCentral", "GayExxxtras", "BelAmi",
                "morningbro", "VineArchive", "gaygingers", "wetmale", "GayThong", "ImaginaryGayBoners", "nsfw_gays"));
        cmds.add(new NSFWReddit("cock", "cock", "penis"));
        cmds.add(new NSFWReddit("pussy", "LipsThatGrip", "Pussy", "RearPussy", "FireCrotch", "GodPussy", "Innie",
                "pelfie", "CelebrityPussy", "MoundofVenus", "vagina", "GushingGirls", "SideLips", "pussyrating",
                "beef_flaps", "WomenLookingDown", "PussyFlashing", "AsianPussy", "shavedpussies", "PussyBeforeAndAfter",
                "OpeningPussy", "closeup"));
        cmds.add(new NSFWReddit("4k", "hq_nsfw", "HighResNSFW", "60FPSPorn", "4k_porn", "UHDnsfw"));
        cmds.add(new NSFWReddit("anal", "Anal", "Painal", "Asshole"));
        cmds.add(new NSFWReddit("asian", "AsianPussy", "AsianAsshole", "AsianAsses", "AsianNipples", "AsianBush",
                "AmateurAsianGirls", "SelfshotAsians", "filipinasgonewild", "JapanesePornIdols", "Graphis",
                "AsianCumsluts", "AsianBlowjobs", "asiandeepthroat", "asian_gifs", "AsianChicks", "AsianSellers",
                "asian_fever", "asian_o_faces", "BustyAsians", "AsianLesbian", "AsiainsFlashing", "UncensoredAsian",
                "BlacksOnAsians", "asiansgonewild", "juicyasians", "AsianHotties", "realasians", "AsianNSFW",
                "nextdoorasians", "asianporn", "paag", "IndianBabes", "indiansgonewild", "NSFW_Japan", "kpopfap",
                "NSFW_Korea"));
        cmds.add(new NSFWReddit("bbc", "BBCSluts", "blackcock"));
        cmds.add(new NSFWReddit("bdsm", "Bondage", "BDSM", "Lesdom", "FaceSitting"));
        cmds.add(new NSFWReddit("boobs", "Pokies", "NipSlip", "Boobs", "Boobies", "YouTubeTitties", "TittyDrop",
                "BigTitsSmallTits", "Cleavage", "TheUnderboob", "BoltedOnTits", "BustyPetite", "BurstingOut",
                "DirtySmall", "NaturalTitties", "BoobBounce"));
        cmds.add(new NSFWReddit("cosplay", "nsfwcosplay", "cosplaygirls", "NSFWCostumes", "cosplaybutts", "CosplayLewd",
                "CosplayBoobs", "Sexyvelma", "gwcosplay", "IrinaSabetskaya", "WarriorWomen", "VictorianSluts",
                "JessicaNigri2", "Cosporn", "FeliciaVox", "NudeCosplay", "BekeCosplay", "HarleyQuinnNSFW",
                "HogwartsGoneWild", "CosplayBeauties", "Cosplayheels", "SexyCosplayBabes", "cosplay_nsfwshoops"));
        cmds.add(new NSFWReddit("cum", "CumSluts", "CumFetish", "AmateurCumSluts", "CreamPies", "GirlsFinishingTheJob",
                "Bukkake", "FacialFun"));
        cmds.add(new NSFWReddit("feet", "SexyGirlsInBoots", "Feet_NSFW", "FootFetish"));
        cmds.add(new NSFWReddit("ebony", "WomenOfColor", "darkangels", "blackchickswhitedicks", "ebony", "Afrodisiac"));
        cmds.add(new NSFWReddit("gangbang", "gangbang"));
        cmds.add(new NSFWReddit("lesbian", "StraightGirlsPlaying", "lesdom", "amateurlesbians", "tonguesucking",
                "lezdom", "Tribbing", "straponlesbians", "lesbians", "ExoticLesbians", "girlsgropinggirls",
                "HDLesbianGifs", "lesbianOral", "girlskissing", "facesittinglesbians", "lesbian_anal", "lesbianpov",
                "mmgirls", "girlsspankinggirls", "interraciallesbian", "breastsucking", "dykesgonewild", "Lesbos",
                "AsianLesbian", "lesbianasslick", "dyke", "scissoring", "DoubleDildo", "GirlsCuddling", "Lesbian_gifs",
                "HiddenStrapon", "girlswearingstrapons", "strapon"));
        cmds.add(new NSFWReddit("interracial", "damngoodinterracial"));
        cmds.add(new NSFWReddit("gonewild", "gonewild"));
        cmds.add(new NSFWReddit("pawg", "pawg", "pawgtastic"));
        cmds.add(new NSFWReddit("public", "NotSafeForNature", "gwpublic", "publicsexporn", "nakedadventures",
                "Yolooutdoors"));
        cmds.add(new NSFWReddit("teen", "legalteens", "collegesluts", "adorableporn", "legalteensXXX", "gonewild18",
                "18_19", "just18", "PornStarletHQ", "fauxbait", "barelylegalteens"));
        cmds.add(new NSFWReddit("thigh", "Thick", "Curvy", "DatGap", "TheRatio", "ThickThighs", "Legs"));
        cmds.add(new NSFWReddit("trap", "traps", "Tgirls", "gonewildtrans", "tgifs", "shemales",
                "transporn"));
        cmds.add(new NSFWReddit("boobjob", "titfuck", "clothedtitfuck"));
        cmds.add(new NSFWReddit("petite", "Petite", "XSmallGirls", "BustyPetite", "dirtysmall", "petitegonewild",
                "funsized", "hugedicktinychick", "skinnytail"));
        cmds.add(new NSFWReddit("furry", "yiff", "yiffcomics", "femyiff", "yiffgif", "FeralPokePorn", "Hyiff",
                "Sharktits", "fyiff", "Yiffbondage", "fursuitsex", "Footpaws", "Furry_Backsack", "YiffMinus",
                "yiffplus"));
        cmds.add(new NSFWReddit("hentaigif", "hentai_gifs", "HENTAI_GIF", "HentaiVids"));
        cmds.add(new NSFWReddit("yuri", "Yurigif", "Yuri"));
        cmds.add(new NSFWReddit("oppai", "oppai_gif"));
        cmds.add(new NSFWReddit("r6s", "Rule34RainbowSix"));
        cmds.add(new NSFWReddit("passion", "PassionX"));
        cmds.add(new NSFWReddit("hardcore", "NSFWHardcore", "SheLikesItRough", "jigglefuck", "whenitgoesin",
                "outercourse", "pegging", "insertions", "xsome", "shefuckshim"));
        cmds.add(new NSFWReddit("milf", "MILF", "Cougars", "RealMoms", "AgedBeauty", "RealOlderWomen"));
        cmds.add(new NSFWReddit("funny", "NSFW_WTF", "WTF_PORN_GIFS", "rule34", "rule34gifs", "WhyWouldYouFuckThat",
                "BizarreSex", "ScaryBilbo", "gonewidl", "NSFWFunny", "ConfusedBoners", "TrashyBoners"));
        cmds.add(new NSFWReddit("overwatch", "Rule34Overwatch", "Overwatch_Porn", "OverwatchNSFW", "Overwatch_Rule34",
                "OverwatchRule34", "overwatch_hentai"));
        cmds.add(new NSFWReddit("apex", "ApexHorizonR34", "ApexValkyrieR34", "ApexLegends_Porn"));
        cmds.add(new NSFWReddit("valorant", "valorantrule34"));
        cmds.add(new NSFWReddit("femboy", "FemBoys", "Sissies", "FemboyHentai"));
        cmds.add(new NSFWReddit("random", "gonewild", "nsfw", "RealGirls", "NSFW_GIF", "holdthemoan", "BustyPetite",
                "cumsluts", "PetiteGoneWild", "adorableporn", "nsfw_gifs", "GirlsFinishingTheJob", "AsiansGoneWild",
                "rule34", "collegesluts", "Amateur", "BiggerThanYouThought", "hentai", "TittyDrop",
                "porninfifteenseconds", "ass", "porn", "pawg", "milf", "OnOff", "HappyEmbarrassedGirls", "LipsThatGrip",
                "Blowjobs", "celebnsfw", "nsfwhardcore", "GWCouples", "dirtysmall", "gentlemanboners", "pussy",
                "Boobies", "WatchItForThePlot", "trashyboners", "juicyasians", "gonewild30plus", "nsfwcosplay",
                "goddesses", "palegirls", "cosplaygirls", "GodPussy", "anal", "curvy", "girlsinyogapants", "asstastic",
                "gonewildcurvy", "freeuse", "workgonewild", "StraightGirlsPlaying", "AsianHotties", "lesbians",
                "BreedingMaterial", "NSFW_Snapchat", "60fpsporn", "thick", "TinyTits", "wifesharing", "grool",
                "FestivalSluts", "bigasses", "gwcumsluts", "TooCuteForPorn", "Hotwife", "Stacked", "BigBoobsGW",
                "bodyperfection", "GirlswithGlasses", "rearpussy", "quiver", "SexInFrontOfOthers", "ginger", "redheads",
                "gettingherselfoff", "whenitgoesin", "SheLikesItRough", "amateurcumsluts", "creampies", "HENTAI_GIF",
                "boobbounce", "Hotchickswithtattoos", "porn_gifs", "WouldYouFuckMyWife", "theratio", "fitgirls",
                "deepthroat", "CuteLittleButts", "burstingout", "facedownassup", "tightdresses", "Upskirt",
                "altgonewild", "HugeDickTinyChick", "hugeboobs", "boobs", "squirting", "cumfetish", "JiggleFuck",
                "Gonewild18", "O_Faces", "xsmallgirls", "canthold", "suicidegirls", "gonewildcouples", "bigtiddygothgf",
                "Cuckold", "CollegeAmateurs", "JerkOffToCelebs", "bdsm", "GWNerdy", "AnalGW", "simps", "chubby",
                "GoneMild", "asshole", "NSFW_Japan", "BonerMaterial", "ghostnipples", "gonewildcolor", "nsfwoutfits",
                "PrettyGirls", "latinas", "traps", "gwpublic", "Unashamed", "HighResNSFW", "PublicFlashing",
                "Nude_Selfie", "randomsexiness", "homegrowntits", "2busty2hide", "IndianBabes", "bimbofetish",
                "cumcoveredfucking", "Bondage", "gifsgonewild", "AmateurPorn", "snapleaks", "jilling", "girlskissing",
                "NSFW_HTML5", "datgap", "assholegonewild", "futanari", "realasians", "FunWithFriends", "lingerie",
                "CuteModeSlutMode", "CamSluts", "distension", "IndiansGoneWild", "buttplug", "Nudes", "ErinAshford",
                "RealPublicNudity", "MassiveCock", "RealAhegao", "nsfw2", "GoneWildSmiles", "Innie",
                "amateurgirlsbigcocks", "SlimThick", "ecchi", "blackchickswhitedicks", "LabiaGW", "AssholeBehindThong",
                "AsianNSFW", "DarkAngels", "Ebony", "boltedontits", "SnowWhites", "funsized", "GroupOfNudeGirls",
                "tanlines", "pronebone", "SexyTummies", "RileyReid", "ChangingRooms", "Tgirls", "Overwatch_Porn",
                "pokies", "suctiondildos", "pelfie", "SocialMediaSluts", "Nsfw_Amateurs", "fortyfivefiftyfive",
                "GoneWildPlus", "BDSMGW", "hardbodies", "SexyFrex", "UnderwearGW", "RepressedGoneWild", "WtSSTaDaMiT",
                "passionx", "GirlswithNeonHair", "doujinshi", "damngoodinterracial", "FrogButt", "sex_comics",
                "cleavage", "thighdeology", "assinthong", "outercourse", "downblouse", "voluptuous", "CumHaters",
                "FacialFun", "BorednIgnored", "WomenOfColor", "extramile", "WhyEvenWearAnything", "rule34_comics",
                "snapchat_sluts", "lanarhoades", "gangbang", "BreastEnvy", "FlashingGirls", "bigtitsinbikinis",
                "couplesgonewild", "seethru", "Ifyouhadtopickone", "thighhighs", "wincest", "thickloads",
                "GaybrosGoneWild", "sexygirls", "GirlsHumpingThings", "FlashingAndFlaunting", "ladybonersgw",
                "Swingersgw", "The_Best_NSFW_GIFS", "HotStuffNSFW", "PreggoPorn", "BigBoobsGonewild", "PokePorn",
                "HungryButts", "FaceFuck", "YogaPants", "bustyasians", "PantiesToTheSide", "booty", "Bottomless_Vixens",
                "Breeding", "throatpies", "forcedorgasms", "iWantToFuckHer", "realmoms", "slutwife", "SpreadEm",
                "TwinGirls", "CelebrityPussy", "NSFW_Plowcam", "creampie", "GoneWildScrubs", "gothsluts",
                "JustHotWomen", "IWantToSuckCock", "femalepov", "aa_cups", "treesgonewild", "blowjobsandwich",
                "amazingtits", "DegradingHoles", "BBW", "MiaMalkova", "torpedotits", "stockings", "dykesgonewild",
                "tits", "FitNakedGirls", "iwanttobeher", "stripgirls", "shorthairchicks", "ChristianGirls",
                "Celebswithbigtits", "Xsome", "ThickThighs", "Miakhalifa", "sarah_xxx", "cameltoe", "MassiveTitsnAss",
                "cuckquean", "petite", "OldSchoolCoolNSFW", "Nipples", "onmww", "girlswhoride", "ahegao",
                "omgbeckylookathiscock", "Rule34LoL", "WesternHentai", "WeddingsGoneWild", "RemyLaCroix",
                "DrunkDrunkenPorn", "SexyButNotPorn", "NSFW_Korea", "titfuck", "hipcleavage", "StreamersGoneWild",
                "leggingsgonewild", "LatinasGW", "MasterOfAnal", "AsianPorn", "painal", "whooties", "BadDragon",
                "MonsterGirl", "TributeMe", "NostalgiaFapping", "BBCSluts", "OralCreampie", "hopelesssofrantic",
                "GoneErotic", "facesitting", "Oilporn", "kpopfap", "FemBoys", "SexyFlowerWater", "Sissies", "Orgasms",
                "ButtsAndBareFeet", "Afrodisiac", "Puffies", "cumonclothes", "AngelaWhite", "Ohlympics", "OnStageGW",
                "camwhores", "bikinis", "yiff", "TotallyStraight", "Sabrina_Nichole", "PornStarHQ", "Thicker",
                "Shemales", "transporn", "Incest_Gifs", "nextdoorasians", "Pegging", "MoundofVenus", "OnHerKnees",
                "Workoutgonewild", "JessicaNigri", "LegalTeensXXX", "porninaminute", "Femdom", "YouTubersGoneWild",
                "BreakingTheSeal", "pulsatingcumshots", "GoneWildTrans", "NSFWCostumes", "hentaibondage", "fuckmeat",
                "TessaFowler", "RuinedOrgasms", "AgedBeauty", "HairyPussy", "GoneWildHairy", "legs", "FilthyGirls",
                "mmgirls", "orgasmcontrol", "40plusGoneWild", "BlowjobGifs", "VolleyballGirls", "Slut", "anal_gifs",
                "NSFWBarista", "pizzadare", "snapchatgw", "creampiegifs", "forcedcreampie", "Blonde", "cosplaybutts",
                "penis", "EmilyBloom", "TrueFMK", "AdrianaChechik", "ShinyPorn", "JapanesePorn2", "CelebrityButts",
                "feet", "Annoyedtobenude", "CasualJiggles", "AmazingCurves", "GifsOfRemoval", "titstouchingtits",
                "traphentai", "BubbleButts", "EngorgedVeinyBreasts", "cfnm", "samespecies", "coltish", "WorkIt",
                "spreadeagle", "PussyMound", "ThickChixxx", "collared", "before_after_cumsluts", "cumshots",
                "skinnytail", "Gloryholes", "gonewildchubby", "NakedAdventures", "AmateurDeepthroat", "MixedRaceGirls",
                "NotSafeForNature", "LenaPaul", "twerking", "exposedinpublic", "biggerthanherhead", "Playboy",
                "maturemilf", "lactation", "spreading", "girlsdoingstuffnaked", "BlowJob", "slutsofsnapchat",
                "DirtyGaming", "AlexaPearl", "PantyPeel", "Evalovia", "celebsnaked", "tight_shorts", "TheHangingBoobs",
                "LingerieGW", "booty_queens", "OnOffCelebs", "ButtSharpies", "cock", "cheatingwives", "HentaiBeast",
                "asiangirlswhitecocks", "Lesbian_gifs", "gayporn", "throatbarrier", "LoveToWatchYouLeave", "insertions",
                "AbusePorn2", "DaniDaniels", "GillianBarnes", "ShemalesParadise", "buttsthatgrip", "SpitRoasted",
                "NudeCelebsOnly", "UpvotedBecauseButt", "Titties", "sweatermeat", "classysexy", "AvaAddams",
                "bigdickgirl", "MyCherryCrush", "enf", "Tgifs", "Rule34Overwatch", "braless", "Shadman", "ElsaJean",
                "bodyshots", "twinks", "GirlsinSchoolUniforms", "gaybrosgonemild", "cottontails", "abelladanger",
                "snapchat_nudes", "Presenting", "WeddingRingsShowing", "Sashagrey", "Squatfuck", "yuri", "Dollywinks",
                "Milfie", "mila_azul", "ClothedTitfuck", "TheUnderbun", "SluttyHalloween", "wetspot", "beachgirls",
                "uncommonposes", "NadyaNabakova", "FuckingPerfect", "Tentai", "bois", "dillion_harper",
                "ImpresssedByCum", "hugenaturals", "SaraJUnderwood", "MissAlice_18", "amateur_milfs", "RedheadGifs",
                "orgasmiccontractions", "Splitview", "doublepenetration", "AsianAmericanPorn", "SheFucksHim",
                "AnimeMILFS", "MiddleEasternHotties", "Lordosis", "POV", "broslikeus", "HighMileageHoles", "LilyIvy",
                "WomenBendingOver", "SchoolGirlSkirts", "GiannaMichaels", "CumshotSelfies", "bigareolas", "PornStars",
                "ratemycock", "Choker", "Bukkake", "TwitchGoneWild", "CedehsGifs", "Alisai", "tightsqueeze",
                "SexiestPetites", "BlancNoir", "smallboobs", "PremiumSnapchat", "TightShorts", "GoneWildCD", "wet",
                "panties", "AsianCumsluts", "DeliciousTraps", "CheatingSluts", "homesex", "JemWolfie", "groupsex",
                "xxxcaptions", "WhiteCheeks", "sissyhypno", "CumFromAnal", "Roughanal", "KimmyGranger", "AthleticGirls",
                "Threesome", "Throatfucking", "realsexyselfies", "PAWGtastic", "bootypetite", "AmateurWifes", "NekoIRL",
                "AreolasGW", "BokuNoEroAcademia", "EyeRollOrgasm", "nakedgirlsdancing", "CosplayLewd", "GirlsOnTop",
                "GirlsShowering", "ratemyboobs", "womenofcolorXXX", "Page3Glamour", "Pee", "public", "Objects",
                "Hitomi_Tanaka", "NSFWfashion", "obsf", "wifepictrading", "EraserNipples", "Ashe_Maree", "ChavGirls",
                "SoHotItHurts", "Kendra_Sunderland", "HugeHangers", "collegensfw", "datbuttfromthefront", "KateeOwen",
                "MilitaryGoneWild", "gag_spit", "LittleCaprice", "Fingering", "pantsu", "Bisexy", "RedditorCum",
                "thick_hentai", "funsizedasian", "anriokita", "janicegriffith", "PrettyLittleCumsluts", "Amateurincest",
                "MiaMelano", "bigonewild", "ToplessInJeans", "CelebrityNipples", "prematurecumshots", "smilers",
                "leahgotti", "DeadEyes", "LaurenSummer", "AnalOrgasms", "mycleavage", "PublicBoys", "PickOne",
                "secretlittle", "brunette", "doggy", "legsup", "cunnilingus", "StealthVibes", "B_Cups",
                "after_the_shot", "pussyjobs", "phgonewild", "CumKiss", "AshleyAlban", "wholesomehentai",
                "IncestComics", "AlexisTexas", "consentacles", "GFTJCreampie", "javdreams", "bikinibridge",
                "BoredandIgnored", "scissoring", "piercednipples", "bbyPocahontas", "SheMakesHerSuck", "paag",
                "kinksters_gone_wild", "ComplexionExcellence", "highheelsNSFW", "mangonewild", "HoleWreckers",
                "japanpornstars", "fuckyeahdrunksluts", "starwarsnsfw", "felching", "gaymersgonewild", "asaakira",
                "ThickDick", "AlbumBabes", "gentlefemdom", "FitGirlsFucking", "Anjelica_Ebbi", "ExhibitionistSex",
                "Sexy", "CaughtFucking", "blacktears", "SunDressesGoneWild", "bigtitsmallnip", "nicoleaniston",
                "smalltitsbigass", "FaceofPain", "lesdom", "Bulges", "IndianTeens", "HomemadePorn", "AmateurSlutWives",
                "cuckoldcaptions", "naturaltitties", "HomemadeNsfw", "analinsertions", "KatyaClover", "STPeach",
                "casualnudity", "postorgasm", "legendarylootz", "knockmeup", "BoobsBetweenArms", "thongs", "BBWGW",
                "gape", "metart", "NSFW_China", "momson", "indiangirls", "NakedOnStage", "slightcellulite", "KyliePage",
                "Anal_witch", "Hot_Women_Gifs", "asslick", "gilf", "GayGifs", "FreeuseHentai", "MeganRain",
                "CelebrityCandids", "WomenOfColour", "cougars", "IndianPorn", "CollegeInitiation", "nsfwcelebs",
                "2Booty", "AnimeBooty", "cumontongue", "asianandlovingit", "PussyWedgie", "tailplug", "PornGifs",
                "onoffcollages", "TooBig", "CumAgain", "peegonewild", "AmandaCerny", "KelsiMonroe",
                "ballsdeepandcumming", "leotards", "Fisting", "Pantyfetish", "Naruto_Hentai", "facial", "emogirls",
                "milkingtable", "GameOverGirls", "straponlesbians", "TSonFM", "cumlube", "DSLs", "MouthWideOpen",
                "thinspo", "VintageBabes", "TheLostWoods", "PerfectPussies", "asian_gifs", "TallGoneWild",
                "keriberry_420", "Sissyperfection", "BikiniBodies", "Dildo_Gifs", "disneyporn", "cat_girls",
                "randomsexygifs", "GirlsWearingVS", "CosplayBoobs", "StrugglePorn", "IncestGifs", "jacking",
                "BlowjobEyeContact", "sophiedee", "GirlsinStripedSocks", "legwrap", "girlsinleggings", "vagina",
                "bestofcollege", "Facials", "nsfwanimegifs", "GushingGirls", "gaynsfw", "Hotdogging", "hangers",
                "amazonposition", "ebonyamateurs", "HelplessHentai", "Ratemypussy", "StormiMaya", "Mooning",
                "SceneGirls", "JustOneBoob", "fuckingmachines", "nsfw_busted", "heteroflexible", "JackAndJill",
                "FayeReagan", "helgalovekaty", "BBW_Chubby", "TheRareOnes", "Whoregasm", "NSFWOddlySatisfying",
                "PetiteGirls", "fishnets", "TheUnderboob", "bimbofication", "Handjob", "RWBYNSFW", "creampiegangbangs",
                "sexygirlsinjeans", "foreskin", "sideboob", "WhiteAndThick", "manyvids", "standingout", "RealJilling",
                "SideLips", "Rule34RainbowSix", "waifusgonewild", "adultgifs", "Saggy", "Strippersonthejob",
                "Tori_Black", "madison_ivy", "KylieJenner", "LisaAnn", "BrandiLove", "edging", "CumHentai",
                "Holly_Peers", "timestop", "IShouldBuyABoat", "Showersex", "BoltedOnBooty", "Spanking", "handinpanties",
                "FutanariPegging", "bigboobs", "gentlemanbonersgifs", "OhNoMomWentWild", "analgonewild", "handjobs",
                "PiperPerri", "Sexyvelma", "JerkingHimOntoHer", "lesbianOral", "AllKindOfPorn", "SoFuckable",
                "boobgifs", "hentaifemdom", "HotWifeRequests", "Dominated", "tscumsluts", "PixelArtNSFW",
                "breastsucking", "ropes", "alteredbuttholes", "virtualgeisha", "JadaStevens", "shewantstofuck",
                "dreamjobs", "underboob", "DadsGoneWild", "GuysFromBehind", "skirtnoshirt", "nsfwsports", "SkirtRiding",
                "lindseypelas", "DontForgetTheBalls", "KimKardashianPics", "femdomgonewild", "HugeDildos", "redhead",
                "cosmickitten", "pegging_unkinked", "GirlsWithToys", "TeenTitansPorn", "CumSwallowing", "LexiBelle",
                "celebJObuds", "realitydicks", "ProgressiveGrowth", "HomemadeGayPorn", "ArabPorn", "FlexiGirls",
                "NSFWPublic", "SexyFlightAttendants_", "BelleDelphine", "RugsOnly", "BeautifulTitsAndAss",
                "MariaRyabushkina", "translucent_porn", "DillionHarper", "superheroporn", "cumwalk", "DumpsterSluts",
                "AthleticBabes", "HidoriRose", "Nekomimi", "AvatarPorn", "BigBlackBootyGIFS", "bustybabes",
                "GirlsinLaceFishnets", "gaycumsluts", "Joymii", "amateur_threesomes", "MandyMuse", "ClothingKink",
                "Curls", "Stoyaxxx", "Paizuri", "SuckingItDry", "ThickAsians", "watersports", "wifeshare", "wifeporn",
                "EnhancedFucktoys", "TrashyPorn", "HotWifeLifestyle", "pussyrating", "HentaiSource", "karleegrey",
                "valentinanappi", "chiisaihentai", "backdimples", "patriciacaprice", "pantyhose", "beef_flaps",
                "BustyNaturals", "PerkyChubby", "SauceForGif", "squirting_gifs", "BehindGonewild", "SexyGirlsInBoots",
                "Bottomless", "PunkGirls", "MalesMasturbating", "bjoverflow", "Sexy_Ed", "upherbutt", "lineups",
                "treatemright", "bigclit", "feelthemup", "Men2Men", "Balls", "BestBooties", "hertonguesout",
                "Bondage_Porn", "solesandholes", "SiriPornstar", "TotalBabes", "comics18_story", "FireCrotch", "Hairy",
                "Bustyfit", "girlsdoingnerdythings", "MalenaMorgan", "bryci", "slightlychubby", "Asianpornin15seconds",
                "Creaming", "naughtyinpublic", "InstagramHotties", "LucyLi", "Cartoon_Porn", "nipslip", "xray",
                "runwaynudity", "BonersInPublic", "peachfuzz", "sexting", "cumbtwntits", "Hucow", "chastity",
                "LegalCollegeGirls", "Flushed", "GifSauces", "cum", "russiangirls", "OneDickManyChicks", "Alathenia",
                "CarlieJo", "areolas", "cocklady", "Feet_NSFW", "UHDnsfw", "tbulges", "whaletail", "SourcePornMaker",
                "AidraFox_XXX", "PetaJensen", "EdibleButtholes", "thickwhitegirls", "BethLily", "tflop", "CelebFakes",
                "NSFW_nospam", "AsianHottiesGIFS", "CellShots", "GirlsWithiPhones", "boyshorts", "DickSlips", "Busty",
                "Pushing", "Xev_Bellringer", "SugarBaby", "rimjob", "dildo", "nekane", "cocktwerking", "TinderNSFW",
                "PUBLICNUDITY", "scrubsgonewild", "muchihentai", "sabrinalynnci", "UncensoredAsian", "cfnf",
                "FootFetish", "softies", "MasturbationGoneWild", "PussyFlashing", "WomenLookingDown", "TheRedFox",
                "AnaCheri", "arianamarie", "PornStarletHQ", "KagneyLinnKarter", "DrawMeNSFW", "milfcumsluts",
                "reversecowgirl", "KendraLust", "MicroBikini", "NotADildo", "bootyshorts", "dekaihentai",
                "VerifiedFeet", "OctaviaMay", "LovingFamily", "Sluts_Blowjobs", "taboofans", "pigtails", "AsianAss",
                "augustames", "yaoi", "ButterflyWings", "Bigtitssmalltits", "CumInTheAir", "TightsAndTightClothes",
                "LiaraRoux", "fuckyeahcollegesluts", "JuliaJAV", "KahoShibuya", "showergirls", "Cherubesque",
                "intercrural", "lesbianpov", "Fay_Suicide", "liz_103", "SideStripeShorts", "HarleyDean", "LyzMania",
                "CloseUpSex", "buttsex", "NSFW_Hardbodies", "Sukebei", "petplay", "realmilf", "MILFs",
                "TitsAssandNoClass", "flatchests", "swimsuitsex", "viola_bailey", "GWAustralia", "girlsmasturbating",
                "Siswet19", "SnapchatSext", "BlackGirlBlowjobs", "easterneuropeangirls", "realbikinis", "ArtGW",
                "lifeisabeach", "gfur", "AsianBlowjobs", "XXX_Animated_Gifs", "nsfwgif", "shemale_gifs", "berpl",
                "RippedLowerGarments", "animeplot", "sissykik", "FTVgirls", "futanari_Comics", "publicplug",
                "imindigowhite", "twerk", "BoltedOnMaxed", "smashbros34", "handbra", "AsianPussy", "CelebSexScenes",
                "HereInMyCar", "NudeBeach", "JynxMaze", "AlyssaAtNightFans", "ChurchOfTheBBC", "blondegirlsfucking",
                "ChristyMack", "TeenyGinger", "EbonyGirls", "JavPreview", "LarkinLoveXXX", "bralessinmotion", "xart",
                "CuteGuyButts", "BreastExpansion", "cumov", "OppaiLove", "Fire_Emblem_R34", "OverwatchNSFW",
                "CheatingCaptions", "socalgonewild", "BigBrotherNSFW", "Upshorts", "GoneWildPetite", "CumCannonAddicts",
                "VacationsGW", "PornstarsHD", "happygaps", "sodomy", "GirlsCuddling", "ClopClop", "GarterBelts",
                "preggo", "CumSwap", "jilling_under_panties", "lucypinder", "RachelStarr", "LucieWildeIsRetarded",
                "A_Cups", "Real_Amateurs", "analsquirt", "britishpornstars", "cumflation", "sissycaptions", "raceplay",
                "EmilyBloomsPussy", "femboy", "sidewinders", "Shemale_Big_Cock", "AuroraXoxo", "Ass_to_Ass", "nobra",
                "slingbikini", "shavedpussies", "gaybears", "blowbang", "GirlsWatchingPorn", "selfservice",
                "SmallerThanYouThought", "SauceforNSFWads", "slutsincollege", "Cumontits", "maturewomen",
                "Alison_Tyler", "happycuckold", "Allthewaythrough", "Brazzers", "tipsdonttouch", "SauceForScience",
                "waist", "ClassicalNudes", "KuroiHada", "NSFW_Wallpapers", "AssOnTheGlass", "UnrealGirls", "sillygirls",
                "ImaginaryBoners", "thefullbush", "ShortShorts", "chickswearingchucks", "Dbz34", "hugefutanari",
                "Thighs", "Penetration_gifs", "sweatysex", "EbonyCuties", "Hotwifecaption", "BigAnimeTiddies",
                "Roughsex", "NSFW_Tributes", "HardcoreNSFW", "hentaicaptions", "GinaValentina", "Slutoon",
                "animemidriff", "MissionarySoles", "hurthappy", "justTurnedEighteen", "Pounded", "ravenhaired",
                "Underbun", "Bathing", "SlimeGirls", "Perky", "BBWVideos", "gaycruising", "strapon", "Gingerpuss",
                "FFNBPS", "fitdrawngirls", "WrestleFap", "joeyfisher", "JustStraightSex", "Sundresses", "sportsbrasGW",
                "teachersgonewild", "fucklicking", "corychase", "ThickFit", "standit", "MomsGoneMild", "CheekyBottoms",
                "rule34_albums", "NippleRipple", "CumOnGlasses", "Sissy_humiliation", "LaBeauteFeminine", "GayKink",
                "NSFW_GAY", "Hegre", "StuckHentai", "justthejewels", "TopNotchBooty", "TittyTime", "Beardsandboners",
                "CelebsGW", "Rule34_Futanari", "footjobs", "ShionUtsunomiya", "KindaLooksLike", "MyCalvins",
                "PussyBeforeAndAfter", "VerticalPorn", "ComfyButts", "giannadior", "PublicHentai", "Gravure", "aocat",
                "freshfromtheshower", "selfshots", "Pajamas", "transformation", "HypnoHentai", "lipbite",
                "cumplay_gifs", "AmateurAsianGirls", "girlsflashing", "BlackIsBetter", "gaystrugglefuck", "WhipItOut",
                "CelestiaVega", "Connie_Carter", "Ladyboys", "femdom_gifs", "JaydenJaymes", "AJAPPLEGATE",
                "GrabHerTitties", "SloppySeconds", "Anacheriexclusive", "BlowjobOnAllFours", "vulva", "ThinClothing",
                "yiffcomics", "bananatits", "OhCumOn", "fetish", "choking", "manass", "dickgirls", "ballsucking",
                "JustFitnessGirls", "NSFW_Comics", "Bailey_Brooke", "Solo_Girls", "shioritsukada", "orgy", "CuteTraps",
                "DDLCRule34", "AlinaLopez", "SpringBreakSluts", "IncestFlixxx", "TightShirts", "girlsinyogashorts",
                "anklepanties", "HentaiManga", "NSFWBraids", "flexi", "Mexicana", "DesiBoners", "gonewildmetal",
                "kinky", "dyke", "amateurlesbians", "DeepThroatTears", "Momokun_MariahMallad", "BridgetteB", "vickili",
                "GoneWildPierced", "NicoletteShea", "DarkestWomen", "adorablehentai", "blowjob_eyes", "Rearcock",
                "Carisha", "gwcosplay", "Lela_Star", "CumOnBlackGirls", "CadeyMercury", "HoneySavage", "NSFWsmiles",
                "MaitlandWard2", "UpskirtHentai", "booty_gifs", "DirtyFamilyPhotos", "boobstrap", "BlackTapeProject",
                "rule34cartoons", "Fairytail_hentai", "Blondes", "slimgirls", "CuckoldCommunity", "GonewildAlbums",
                "lovegaymale", "NSFW_Outdoors", "FoxyDi", "Eliza_cs", "TheHottestBabes", "BigBoobsWithFriends",
                "Creampie_Porn", "NaomiWoods", "Wife", "spitroast", "NSFW_GlF", "JessaRhodes", "c0rtanablue",
                "HentaiHumiliation", "EuroGirls", "Gemplugs", "sexytgirls", "hotguyswithtattoos", "funpiece",
                "mariorule34", "thighhighhentai", "oliveskin", "KoreanHotties", "LockerRoom", "Cummy", "Malmalloy",
                "CockOutline", "tscum", "AzerothPorn", "BBCsissies", "thanksgravity", "squirtfromanal",
                "ContainTheLoad", "KeishaGrey", "Your_Little_Angel", "KrissyLynn", "NSFW_Sources", "Selected_NSFW",
                "AdorableNeonGirls", "HentaiPetgirls", "petitepeaches", "selfiesInTheNude", "GamedayGoneWild",
                "nsfwbuys", "TopDownThong", "BondageBlowjobs", "springbreakers", "snapchatboobs", "openbra",
                "harrypotterporn", "closeup", "chesthairporn", "IndianFetish", "HotInTheKitchen", "autofellatio",
                "MadisonDeck", "AnimeFeet", "Witcher_NSFW", "lynaritaa", "ridingxxx", "BarebackGayPorn", "maddybelle",
                "JessicaRobbin", "Flashing", "Sexfight", "PajamaBabes", "PinkChocolate", "OpeningPussy",
                "HollyMichaels", "GirlsinWrupPants", "Grooltasting", "RedheadedGoddesses", "secretbridgexxx",
                "ToplessInPanties", "AsianPornIn1Minute", "leannadecker", "O_Face", "nopanties", "MaleUnderwear",
                "Rapunzel", "Ribcage", "buttloads", "asianbabes", "Subwife", "Transex", "AbellaAnderson",
                "Futadomworld", "SexyOutfits", "MensHighJinx", "AbigailRatchford", "femyiff", "shibari", "MirrorSelfie",
                "orgasm", "SeeThrough", "EmilyGrey", "Tribbing", "PornIn30Seconds", "leilalowfire", "Alkethadea",
                "kaliroses", "StandingAsshole", "instahotties", "MakeUpFetish", "OpenShirt", "EpicCleavage",
                "TinyAsianTits", "SexyShemales", "AirTight", "jennahaze", "SaraJay", "BarelyContained", "jav",
                "CharityCrawford", "CutCocks", "MariahLeonne", "bicuckold", "beggingforit", "PhatAssWhiteGirl",
                "fuckmywife", "olgakobzar", "brittany_elizabeth", "HDLesbianGifs", "StandingCarryFuck", "IncestSnaps",
                "WedgieGirls", "GirlsInSocks", "UncutPorn", "sheerpanties", "undies", "Ponytails", "cumshotgifs",
                "latinaporn", "JapaneseHotties", "ChineseHotties", "rape_roleplay", "AgePlaying", "trapgifs",
                "Triplepenetration", "rule34gifs", "Best_NSFW_Content", "WhiteBuns", "boobgrabs", "creampiepanties",
                "analcreampies", "bangmybully", "NSFWSnapchat", "MissBanana", "freeuseFonM", "MakingOff",
                "FirstInsertion", "frostedbholes", "HentaiBreeding", "HentaiAnime", "PoolsidePorn", "IndianLily",
                "tbooty", "RiaeSuicide", "cfnmfetish", "PussyJuices", "yiffgif", "hardanal", "PublicUpskirts",
                "uncensoredhentai", "PinkandBare", "coveredincum", "HypnoGoneWild", "girlscontrolled", "CelebNudes",
                "CelebrityPenis", "MotherDaughter", "gayotters", "lickingdick", "arielrebel", "forearmporn", "MenGW",
                "nakedladies", "XChangePill", "lesbian_anal", "bobbersandjobbers", "HorsecockFuta", "SissyChastity",
                "ecchigifs", "April_ONeil", "ProneBoneAnal", "rimming", "LaylaLondon", "PregnantPetite",
                "AriaAlexander", "CoffeeGoneWild", "DakotaSkye", "CamGirls", "WomenAtWork", "SashaFoxxx", "HaleyReed",
                "TrafficTits", "whoredrobe", "IrinaSabetskaya", "openholes", "AsianFetish", "HardBoltOns",
                "realolderwomen", "Oviposition", "FutanariHentai", "FreckledRedheads", "PerfectTits", "Waif", "cumshot",
                "femsub", "LaundryDay", "tongueoutbjs", "hotofficegirls", "boysgonewild", "swimsuithentai",
                "couplesgonewildplus", "candidasshole", "topless", "hotwife_cuckold", "gifcest", "puffynipples",
                "spitfetish", "DarkBitsNPieces", "ShaeSummers", "Now_Kiss", "LaurenPhillips", "KarmaRx", "NileyHott",
                "EmbarrassedHentai", "NSFW_ANALQUEENS", "tiedAnal", "JulieKennedy", "WarriorWomen", "grower",
                "HairyAssGirls", "datgrip", "VictorianSluts", "CuckqueanCommunity", "TeaseMePleaseMe", "PassionSex",
                "CelebrityPokies", "TwinkLove", "rhian_sugden", "4k_porn", "PhoenixMarie", "KaydenKross", "AlettaOcean",
                "DanielleSharp", "bodypaint", "Girlsdoporngifs", "lesbianasslick", "Straps", "Colombianas",
                "reversepov", "WhatHappensInCollege", "CarolinaRamirez", "natalielust", "CheerleadersGonewild", "xrxse",
                "PlainJaneNSFW", "dontslutshame", "GodBooty", "BeautifulSubbyFans", "chocolateskin",
                "StormiMayaAlvarez", "Hotness", "Stuffers", "WetTshirts", "SeeThroughLeggings", "skivvies",
                "iateacrayon", "NSFWskyrim", "tinydick", "gagged", "primes", "GayDaddiesPics", "WoodNymphs",
                "Womenorgasm", "JessicaNigri2", "cutefutanari", "redheadxxx", "laurenpisciotta", "PickHerOutfit",
                "Precum", "MaledomEmpire", "CinnamonWomen", "busty_porn_vids", "cockpulse", "gayholdthemoan", "Incase",
                "SuckingHerOwnNipples", "BigTitsSmallNips", "pornception", "EmilyAgnes", "Nancy_A", "StellaCox",
                "Frotting", "cumonherpanties", "JadeKush", "HentaiAnal", "xmasgirls", "ASSians", "SexAndSoles",
                "Perfect_NSFW", "HotAsianMilfs", "MuricaNSFW", "GirlsWithHeadTowels", "PussySlip", "sizecomparison",
                "bbwbikinis", "FakeCum", "facialcumshots", "THEGOLDSTANDARD", "nudist_beach", "sybian", "rosie_jones",
                "YoungMonroe", "BimboOrNot", "HeSquats", "HBombs", "Sissygasms", "oppai_gif", "Playboy_Albums",
                "GayFreeUse", "tinder_sluts", "VoyeurBeach", "OneHotOneNot", "AssToMouth", "SchoolgirlsXXX",
                "BouncingTits", "HardcoreHentaiBondage", "kato", "Vintage_NSFW_GIFS", "DollyLittle", "abusedsluts",
                "KendraRoll", "fingerinbutt", "CumCocktails", "skylanovea", "HookedUpHentai", "unexpectedcum",
                "DaisyStone", "NSFW_ASS", "Madison_Deck", "PAWGirls", "Innies", "asiansissification", "casualblowjobs",
                "LapDanceSexStance", "tonguesucking", "FreshGIF", "rule34pinups", "Cuffed", "StomachDownFeetUp",
                "AmateurXXX", "frenchmaid", "WetAndMessy", "8muses", "laracroftNSFW", "TheRearPussy", "ssbbw",
                "VintageSmut", "DirtyPantiesGW", "damselsindistress", "FemdomHumiliation", "ABDL", "wetfetish",
                "Reflections", "motiontrackedboobs", "BookNymphs", "POVTranny", "snowgirls", "AmateurAllure",
                "PatreonGirls", "MorganHultgren", "CodiVore", "frprn", "leannecrow", "thongbodysuit", "animelegs",
                "aika", "InvertedNipples", "BoltedOnAsians", "JoselynCano", "Pornstars_NSFW", "AnissaKate", "her1stbbc",
                "asiansgonemild", "Samantha_Saint", "Chaturbates", "elegantperversion", "Bodysuit", "WhoreLipstick",
                "between2cocks", "BritneyAmber", "PantyJobs", "slutsandalcohols", "SexShows", "WifeWantstoPlay",
                "chanelsantini", "Venusfoxxx", "jav_gifs", "lilliasright", "ondww", "PerkierThanYouThought",
                "ShemaleGalleries", "AthenaFaris", "perfectloopNSFW", "AmateurGotBoobs", "HighResASS", "Blondeass",
                "brunetteass", "AnalPorn", "bubbling", "rule34feet", "braandpanties", "GaySnapchatImages",
                "clothedwomen", "TheLandingStrip", "SweNsfw", "BlackGirlPics", "shinybondage", "nsfwcelebgifs",
                "GirlsInMessyRooms", "SkinnyAnal", "masturbation", "CharlotteMcKinney", "AsianPersuasion",
                "BellyButtons", "BondageGIFS_HighRES", "pantiesandstockings", "redlingerie", "gayasshole", "CumInHair",
                "Bbwmilf", "croptopgirls", "DancingBear", "sexycougar", "Metroid34", "LatinaMilfs", "OnHerBack",
                "TaylorAlesia", "cuteAssCuterface", "cockkisses", "NSFW_showcase", "fbb_NSFW", "pm_your_pokemon_team",
                "kati3kat", "mollyjane", "katerina", "pussypump", "myChippyLipton", "juliannee", "lucia_javorcekova",
                "VictoriaJune", "veradijkmans", "CaribbeanGirls", "Ass_to_ssA", "maturewoman", "SUMMERtimeheat",
                "selfpix", "xPosing", "LadiesInLeather", "DressedAndUndressed", "Aprons", "GirlswithBodypaint",
                "FeralPokePorn", "rule34_ass", "KillLaHentai", "Naturalgirls", "hersheyskisstits", "SmallNipples",
                "GoneInsane", "curvesarebeautiful", "submissive", "CuckoldPregnancy", "nsfw_hd", "countrygirls",
                "jockstraps", "dpgirls", "ariel_model", "Ashlynn_Brooke", "Twistys", "messyjessie58", "OnlyHotMilfs",
                "SelfshotAsians", "chickswithchokers", "bigblackasses", "LipsThatUsedToGrip", "Asians_gonewild",
                "EvaNotty", "BrittneyWhite", "thong", "fakecreampie", "pregnantporn", "BlacksOnAsians", "romirain",
                "ginger_banks", "AnnaBellPeaks", "WheelsGoneWild", "GangbangChicks", "JuliaAnn", "paki_girls",
                "doublevaginal", "ai_uehara", "AzumiNakama", "Exhibitionistfun", "CassidyBanks", "RubbingHerPussy",
                "nsfw_bets", "mommybully", "pencilskirts", "audrey_", "grailwhores", "NYCNakedFunTimes", "DressTwerk",
                "RetailFlashing", "Liya_Silver", "AnalInk", "WoahPoon", "piercedtits", "nsfwskirts",
                "GirlsinPinkUndies", "plumper", "OldenPorn", "slutwives", "girlswearingstrapons", "Babes",
                "exotic_oasis", "FMN", "manlove", "tsexual", "SwordSwallowers", "exmormon_nsfw", "standingmissionary",
                "upset", "voyeurpics", "GirlsRimGuys", "beachpussy", "BDSM_Artwork", "NikkiBenz", "TheHottestPornStars",
                "AsBigAsYouThought", "helpinghand", "AnalFaces", "nakedinthekitchen", "slightlypregnant",
                "HiddenStrapon", "hydroerotic", "immodest", "KrystalBoyd", "Klara", "doubleanal", "EllaKnox",
                "ZahraElise", "CarolinaSweets", "squadunknown", "HeartShapedHentai", "Bootyland", "RandomActsOfNSFW",
                "OnlyGoodPorn", "beach", "Bikini", "WomenWearingShirts", "upskirtpics", "puffypussy", "SloMoBoobs",
                "BeforeAndAfterBoltons", "keyholdercaptions", "GirlsGoneBitcoin", "nakedcelebs", "interracial_porn",
                "miela", "GaymersGoneMild", "RotationGirls", "MarshaMay", "brownandbumpy", "DickPics4Freedom",
                "SissyInspiration", "ClaraBabyLegs", "AllAmateurPorn", "menslockerroom", "BrooklynChase", "SweatyGirls",
                "Shemaleselffacials", "alejandraguilmant", "femalechastity", "selffuck", "LilithLust", "Blondbunny",
                "AnalHook", "AnyaOlsen", "ClothesFail", "BurningManNudes", "impregnation", "lesserknownpornstars",
                "snapchatnude", "EricaFett", "RateMyGf", "HoneyGold", "cockcompare", "biggerthanithought", "MonsterMen",
                "JustViolet", "AshlyAnderson", "DxDiamond", "eyedride", "ThePussyPop", "wolfgirlanon", "AsianAsshole",
                "CumFarting", "Trim", "ChocolateMilf", "crotchlesspanties", "BoredIgnored", "HQHentai",
                "blondehairblueeyes", "HairyArmpits", "ClassicXXX", "VintageCelebsNSFW", "GayWatersports",
                "NibbleMyNipples", "MenWithToys", "AbbyWinters", "VictoriaRaeBlack", "ericacampbell",
                "CarlottaChampagne", "HapasGoneWild", "LarkinLove", "Latina_Porn", "KureaHasumi", "nsfw_snapchat_share",
                "Cougar", "SissyArtwork", "swimsuit", "interraciallesbian", "shortskirts", "Manhandled", "CeCeCapella",
                "suckingmytits", "CagedAndFucked", "BlairWilliams", "CumTributeKpop", "HotBlackChicks", "Full_Nelson",
                "FeelTheFemale", "milkyteaa", "JeffMilton", "BonnieRotten", "Tiffany_Cappotelli", "UmaJolie",
                "FlirtyWife", "BunnyGirls", "Taxpayers", "AniButler", "dirtyporn", "SoloMasturbation",
                "GirlsTakingOver", "dominantwhitemen", "Danny_Being_Danny", "masserect", "IntenseBDSM", "StretchingIt",
                "amateurs", "Bad_ass_girlfriends", "tshirtsandtanktops", "girlsontheirbacks", "asseffect",
                "Bleach_Hentai", "Pink", "titsagainstglass", "paleskin", "Spermjoy", "TrueBukkake", "JewishBabes",
                "sea_girls", "JapanesePorn", "Vore", "JordanCarver", "HarrietSugarcookie", "cutekorean", "Mia_khalifa",
                "amwf_alice", "LovelyLilith", "BlackAndBusty", "HipHopGoneWild", "bdsm_gifs", "AaliyahHadid",
                "BellaThorne18", "NSFW_pregnant", "rule34bondage", "areolapeaks", "weirdboobs", "KIKSnaps",
                "Carter_Cruise", "StrippingOffShirts", "KiaraMia", "snorl4x", "SensualJane", "netorare", "PGWVideo",
                "GinaGerson", "malepubes", "RealWhores", "girlsuncovered", "surprise_sex", "ChanelPreston",
                "StephKegels", "SunnyLeone", "JenniferAnn", "Alina_Buryachenko", "nsfw_OnOff_gonewild",
                "LidiaKrasnoruzheva", "ShemaleCumHandsFree", "OnePieceSuits", "Niemira", "bamtitties", "ohgeelizzyp",
                "Booty_Lovers", "CrochetBikinis", "PornstarVSPornstar", "NSFWTiedTogether", "BeautifulBustyBabes",
                "Tiger_Chilli", "HotGirls", "onherstomach", "FaceAndAsshole", "NowYouReallySeeMe", "dota2smut",
                "FuckableAmateurs", "FlannelGetsMeHot", "girlsinplaidskirts", "sexyuniforms", "Cosporn",
                "underwaterbabes", "moxxigonewild", "BeefFlaps", "HipBones", "Bustier", "heavyhangers", "HairyCurvy",
                "Scandinaviangirls", "BareGirls", "porngif", "NSFW_GFY", "LadyGagasAss", "men_in_panties", "Singlets",
                "ManSex", "GayChubs", "OralSex", "Lucy_Vixen", "TiffanyThompson", "RosieJones", "AmyAnderssen",
                "JenSelter", "AlphaMalePorn", "myult1mateischarging", "AssFucking", "Touhou_NSFW", "comfiecozie",
                "CelebrityWankMaterial", "gaysian", "nsfw_Best_Porn_Gif", "shemaleselfie", "UnderTail",
                "SweatpantGirls", "Melissamoore", "Soaking_Panties", "onlyonenaked", "slutwear", "VeronicaRodriguez",
                "AbigailMac", "Brazzers_Network", "JasmineJames", "girlslovecum", "LaciKaySomers", "Doggystyle_gifs",
                "HighheelsGW", "SIXTYNINEBLOWJOBS", "HerPOV", "shirtbiting", "alex_grey", "sexy_saffron",
                "DildoThroating", "legsSpread", "facesittinglesbians", "dildos", "pornstarwannabe", "SnapchatXXX",
                "RileeMarks", "BlowjobPractice", "DesiDressedUndressed", "luciewilde", "FeetInYourFace",
                "undertable_porn", "showmeyourtits", "SecretSex", "StruggleLoving", "NSFW_SEXY_GIF", "ILikeLittleButts",
                "juicybooty", "datass", "Morphs", "GloriaV", "sockgirls", "GirlsinTUBEsocks", "girlsinpantyhose",
                "kneesocks", "tights", "Sharktits", "boltedonlips", "feetish", "Hips", "peachlips", "OneInOneOut",
                "veins", "AsianNipples", "BHMGoneWild", "GonewildFaces", "FuckMarryOrKill", "Sexyness", "carnalclass",
                "RaveGirls", "AnalFisting", "foodfuckers", "TeenKasia", "londonandrews", "TrophyWives",
                "NSFW_Pussy_Teen_Ass", "blondesinblue", "Porn_Palace", "blowjob_gifs", "best_nsfw_milf",
                "Solo_Gonewild", "HaveToHaveHer", "LegsGW", "PornstarFashion", "Princess_Leia", "maturemompics",
                "gaycumshots", "justamber", "animearmpits", "wgbeforeafter", "StripGIF", "biggirlsgonewild", "Hyiff",
                "blackcock", "kennedyleigh", "ZootopiaPorn", "DawnWillow", "TankTugging", "AsianLesbian", "labia",
                "TransGoneWild", "ShinMegamiHentai", "TitfuckBlowjob", "pronepawgs", "PaigeTurnah", "titplaysex",
                "bignips", "Hairpulling", "bangbros_porn", "yummyscarlet", "InterracialBreeding", "noelleeaston",
                "suckingaftercumming", "DoubleDildo", "RealGirlsGoneWild", "StrainedButtons", "HangingTitsGW",
                "Thenvsnow", "VikiOdintcova", "AshleyAdams", "reversedeepthroat", "JustForYou95", "SparrowXXX",
                "minus8", "ButtTooBig", "Noseballs", "Bodystockings", "pasties", "Chakuero", "Topless_Vixens",
                "shinypants", "Pantiesdown", "AnimatedPorn", "BioshockPorn", "girlswithbangs", "DatV", "shavedgirls",
                "PM_ME_YOUR_TITS_GIRL", "nosecum", "Gonewild_GIFS", "GWCouples4Ladies", "guysgonewild", "nsfw_sets",
                "AnyoneForTennis", "JungleFever", "GirlsHoldingDicks", "jenniferwhite", "SarinaValentina",
                "GentlemenGW", "showing_off_her_ass", "ChubbyDudes", "preggohentai", "MilenaVelba", "Anal_toys",
                "GayBlowjobs", "AriellaFerrera", "Panties4Sale", "NSFWarframe", "happydeepthroat", "maseratixxx",
                "koreangirls", "Altdudesgonewild", "CelebJOMaterial", "ZettaiRyouikiIRL", "skinny_girls_porn",
                "JessicaAshley", "PennyPax", "WetPanties", "corruptionhentai", "DiamondJackson", "gwcumselfie",
                "NudeCosplay", "OrgasmTorture", "Technical_DP", "Jessica_Davies", "GROOLGW", "SkinnyBusty", "leggings",
                "gaybreeding", "pilloryporn", "EscapistPorn", "DesiMilfsGW", "ProxyPaige", "SarahVandella",
                "bustyamateurasians", "Clits", "Striptease", "littleblackdress", "PersianBabes", "NatalieMonroe",
                "QueenofSpades", "girllookingintofridge", "chi_love", "HoldingIt", "SlaveWorld", "BBWnudists",
                "toplesscelebs", "Coralinne_Suicide", "Katrina_Jade", "BBWnThiccness", "mombod", "takerpov",
                "MarinaVisconti", "IvyWolfe", "NSFWToys", "FeliciaVox", "LanaKendrick", "DerrionKeller",
                "AnkleGrabbers", "Tushy", "underarms", "gingerdudes", "FapFap", "GroupOfNudeMILFs", "nudeamateurporn",
                "smalldicks", "FreckledCumsluts", "Cumonboobs", "nsfwblackpeoplegifs", "socksgonewild", "lezdom",
                "chastitytraining", "roadhead", "igawyrwal", "LilyC", "DioraBaird", "alinali", "MiaSollis",
                "cheekyasian", "Jenya_D", "MegTurney2", "MaitlandWard", "90sTits", "RebeccaStilles69",
                "StevenUniverseNSFW", "johnpersonsthepit", "randomtgirl", "bbwselfies", "KeriSable", "movie_nudes",
                "gayrimming", "rule34gay", "dnd_nsfw", "UniformedMen", "eyecontact", "Public_Sex", "extreme_gifs",
                "BDSM_Smiles", "shemalery", "Threesomes", "Aspen_Rae", "ThePantyDrawer", "Doggy_Style",
                "masturbation_gif", "predicamentbondage", "FilipinoHotties", "BellaRose", "gstrings", "BallsDeepThroat",
                "corsetsnsfw", "katerinahartlova", "MelisaMendiny", "JoselineKelly", "saohentai", "HandsInPanties",
                "EwaSonnet", "Sofi_A", "CreampieEating", "ExoticLesbians", "gonewild_secrets", "doublebarrelblowjobs",
                "BlackGirlsLoveAnal", "Sydneycolexxx", "BekeCosplay", "bigtitspics", "ms_modestly_immodest",
                "GorgeousGirlsNSFW", "JurASSicAss", "pahg", "frozenporn", "ATPorn", "LatexUnderClothes", "pantyslide",
                "IncestDoujinshi", "hairbra", "AsianChicks", "gwchallenge", "CrossEyedFap", "librarygirls",
                "Christy_Mack", "Indiana_A", "Rule34_anal", "armpitfetish", "SonicPorn", "taboocaptions", "Graphis",
                "titfuck_obsession", "BrookeWylde", "JessieRogers", "pussystacking", "FlawedBoltons", "soles",
                "amateur_anal", "augusttaylor", "CuteAndSporty", "bentoverblowjobs", "Rule34cumsluts", "amwfporn",
                "blackballed", "SamanthaRone", "NSFW_EBONY", "bitchsissies", "Samantha_Lily", "CumCocks", "TGirlPoV",
                "Amateur_pov", "shaved_asians", "playboy_playmates", "Secretary", "audreybradford", "Ellie_Silk",
                "dailymilf", "CMNF", "Boats_and_Beauties", "CurlyHairNSFW", "PantyStuffing", "Panties_Showing",
                "tgirlsurprise", "AryaFae", "Blindfolded", "PetiteBoobBounce", "PushHerHead", "amateuroral",
                "nsfw_college_ass", "OnOffDudes", "AnyaIvy", "asiandeepthroat", "FlannelGoneWild", "ThaiHotties",
                "hentai_gifs", "latinasgonewild", "Piss", "Esperanza_Gomez", "GirlsRiding", "PornstarRating",
                "KylaCole", "GamerGal69", "Amy_Jackson", "MardiGrasFlashing", "sexartlove", "alwaystheshyones",
                "animebodysuits", "HarleyQuinnNSFW", "missprincesskay", "HotorNotFemDom", "BisexualHentai",
                "JapanesePornIdols", "Buttjobs", "CycleSeats", "DomesticGirls", "sneakersgonewild", "monokini",
                "satinpanties", "truedownblouse", "Slippery", "baramanga", "gonewanton", "IsThatCUM",
                "MotionTrackedPorn", "GirlsInTanningBeds", "femaleasiananal", "DPSEX", "fellatio", "BibiJones",
                "nubilefilms", "YesSheSquats", "malemodels", "MrBlowJob", "GirlsInDiapers", "GayCreampie",
                "IrynaIvanova", "hq_nsfw", "cummedpanties", "GayCocksuckers", "latexclothing", "snapnsfw",
                "GracelessSubmission", "whitelingerie", "Sammy_Braddy", "asstopussy", "insanelyhairymen",
                "sophiehoward", "piercingbulge", "purplehailstorm", "Keeptheglasseson", "frontdoggystyle",
                "hentaichastity", "BreeDaniels", "GirlsManhandled", "JillianJanson", "VerucaJames", "AmandaLeeFans",
                "OutdoorBondage", "AlexTanner", "Gaycouplesgonewild", "tittyfuck", "NSFWReactions", "AssReveal",
                "MH34u", "TransOnOff", "RedHeadsInYogaPants", "colorsexcomics", "NSFW_GIF_ORIGINALS", "BacklitBeauty",
                "CumOnKisses", "KaylaKayden", "ClitDepthGIFs", "tittypop", "India_Summer", "thighgap", "gloria_sol",
                "hentai411", "Cali_Carter", "ThumbinPooper", "nakednews", "girlsgropinggirls", "SmallerYetBigger",
                "AthenaPalomino", "CamilaBernal", "realperfection", "DicksOnGirls", "underbooty", "BoobsParadise",
                "MaidHentai", "belle_delphine", "selfshotgirls", "cupless", "cottonpanties", "Aparthigh", "AdultComix",
                "KimPossiblePorn", "girlswithfreckles", "girlslickingcum", "AsianAmericanHotties", "Ebonyasshole",
                "femalesgonewild", "mengonewild", "maledom", "PornPleasure", "nsfwnonporn", "NaturalWomen",
                "HardcoreSex", "NoBSNSFW", "ravergirl", "myfreecams", "GaySex", "BiancaBeauchamp", "JanaDefi",
                "HannaHilton", "eva_angelina", "Overwatch_Rule34", "BoobGap", "boysfucking", "GayNSFWFunny",
                "VirginKillerSweater", "pantsinggirls2", "BrittanyFurlan", "FemdomMatriarchy", "butts", "Denise_Milani",
                "Zishy", "malemodelsNSFW", "menshowering", "stocking_paradise", "Blackdick", "NSFW_missionary",
                "PublicFetish", "KiannaDior", "meetpornstar", "MySecretsOut", "GayWincest", "FTV_Girls", "AnalGape",
                "DirtyPanties", "from_behind", "BlowjobSelfies", "GaySelfies", "frot", "karlakush", "VietnameseHotties",
                "ChloeKhan", "BusinessBabes", "MadisonIvy", "AnalCowgirl", "DominoPresley", "pussygape", "SexyNSFWGifs",
                "anastasia_shcheglova", "nakedbakers", "wifeyworld", "tattooed_redheads", "nippleplay", "C_Cups",
                "MilenaAngel", "superheroinesdefeated", "ForeheadCum", "leotardsex", "retrousse_nose", "fyiff",
                "NickeyHuntsman", "Alyssa_Arce", "CumSelfie", "bathroomselfies", "seatbelteffect", "ringlightporn",
                "BetterThanPorn", "Stripping", "casualdickholding", "Tittybombing", "oralfrombehind", "mistygates",
                "WickedWeasel", "Paladins_Porn", "DarkSkinHentai", "AllysonBettie", "WeddingRingShowing",
                "NaughtyFrenchie", "Violet_Starr", "SweatyGirls_NSFW", "JuicyBrazilians", "BringHerToHeaven",
                "JuicyBabes", "HogwartsGoneWild", "fashionextramile", "BrittanyElizabeth", "kbj", "StefanieKnight",
                "SnapchatPremiums", "fortnitegonewild", "highwaistedthongs", "PizzaThot", "Redheadass", "bestofboobies",
                "agedlikefinewine", "moddedgirls", "heels", "NoTop", "ThinChicksWithTits", "gwbooks", "kinbaku",
                "lockedup", "furryfemdom", "sexgifs", "worldcupgirls", "NakedFamousPeople", "SupeApp",
                "AsianCuckoldCaptions", "TGirl_Feet", "Hairymanass", "asiansinswimsuits", "MaleArmpits", "PainBDSM",
                "nikki_sims", "cutecunts", "BellyExpansion", "mouthfuls", "gonewildbbw", "thegoodcrack",
                "MCBourbonnais", "AmateurCumshots", "overknees", "nude_beach", "gonemildcurvy", "VanillaRomanceOnly",
                "dicks", "Gfurcomics", "Scally", "priyarai", "SpiderGirls", "Anal_Missionary", "penismeasured", "gtsim",
                "amateur_shemales", "NSFW_Tumblr", "BigBootiesGoneWild", "handsfree", "uphisbutt", "showcase",
                "ReZeroHentai", "tiedgirls", "ThroatFuck", "SissyExtremism", "jocks", "brideporn", "orc34",
                "FullNelson", "EmbarrassedNude", "blondebush", "CapriCavanni", "Porn_Star_Feet", "ChloeFoster",
                "NSFW_HORNY_GIRLS", "SexyArabGirls", "EmilyAddison", "landingstrip", "SodomizedSoulSisters",
                "skinnyfit", "sixty_nine", "MonsterTits", "PerfectPussy", "TinyAsianPussy", "ropebondage",
                "SpecialMoons", "Slutty_Alice", "ColorfulPorn", "FreeBoob", "420_Girls", "miniskirt", "wwwtw",
                "Thicchaps", "gapedbuttstilltight", "JLullaby", "Simulingus", "amateurcuckold_pz", "ProneJilling",
                "brewbies", "HotGirlsNSFW", "HentaiVisualArts", "Emma_Leigh", "desixchick", "NoTraps",
                "SexyFlightAttendants", "CumTributeAsianGirls", "Milfinstockings", "airboobs", "KanMusuNights",
                "submittedts", "TeaGirls", "girlsinanklesocks", "realrule34", "onlyblondes", "Bitches_Like_it_Big",
                "Oriental", "hottestvoyeurs", "fursuitsex", "WhichOneWouldYouPick", "nsfw_bw", "passionpics",
                "onoffceleb", "malepornstars", "OutDoorSex", "gag", "Calicoo", "Maria_Ozawa", "skindiamond",
                "alisonangel", "JacquelineDevries", "gonewildrpdr", "BustyNonNude", "HomemadeBDSM", "Yiffbondage",
                "NSFW_DICK_and_Cock", "PunkBiBiBi", "BateBuds", "SkinnydippingNSFW", "pullingdownthepanties",
                "ElegantNSFW", "VintageGayPics", "ArmsUp", "nailFetish", "AmarnaMiller", "ExposedByAnother",
                "NSFWAnimeWallpaper", "Yoga_Babes", "gaysiansgonewild", "Ovipositor", "preggoGW", "SexyGoosebumps",
                "SabineJemeljanova", "jennkaelin", "AssGaping", "TowelGirls", "HentaiWesternComics", "NudeGifs",
                "MilfPanties", "FemdomMilking", "WindowBeauty", "tiny_saggers", "Ifyouhadtochoosejust1",
                "AmateurAlbums", "KeeleyHazell", "ShemaleSwallowsOwnCum", "nuru", "Incest_Captions", "pumpedpussy",
                "ClaireGerhardstein", "CuckoldComics", "DirtySnapStories", "blackmale", "girlsbeingstripped", "AssJob",
                "pantylines", "pornstar", "jilling_with_oral", "Surprised_Pornstars", "LilyRader", "GothamXXX",
                "ChangingRoom", "Incest_Hentai_Manga", "AnalPileDriver", "ElfHentai", "avataylor", "KellyDivine",
                "AmiaMiley", "XMasBabes", "ropetutorials", "fitgirlshentai", "onepiecetanlines", "dani_jensen",
                "MonsterGirls", "bendover", "mature", "Soyacide", "nsfw_maledom", "nsfw_cumsluts", "SheenaShaw",
                "faptoberfest", "Rilee_Marks", "BottomlessVixens", "ellealexandra", "PrettyCumSluts", "Rate_my_dick",
                "Clarabelle_Says", "DeepthroatSlime", "YGWBT", "WallPaperWorthy", "YoungTwins", "penthouse",
                "AlessaSavage", "CJ_Miles", "Naughtysoulmates", "NikkiEliot", "EllaHughesXXX", "QueenBlondie",
                "AmandaAhola", "Rule34LifeisStrange", "ItsHerAsshole", "Maria_Domark", "CockCuddling", "120FPSPorn",
                "lyingbacklegsspread", "InstagramThots", "LyingOnBellyNSFW", "GOTporn", "girlsinhoodies",
                "brasgonewild", "MorningGirls", "OnAllFours", "CelebCumSluts", "latinascaliente", "womenofcolorgifs",
                "milfgw", "tickling", "boundgirls", "NSFW_PORN_ONLY", "TumblrArchives", "Sweet_Sexuality",
                "WellWornBimbos", "dp_porn", "WendyFiore", "PublicDisplay", "TheFamilyTrap", "peta_jensen", "TiannaG",
                "titsnboobs", "AnnaNystrom", "MatureLadies", "FFSocks", "HorrorMovieNudes", "FapFolders",
                "smokingfetish", "dumbdolls", "BestRedditPorn", "gendertransformation", "BdsmNsfw", "EmmEffEmm",
                "EbonyHardcore", "milf_pictures", "Leglock", "orgasm_gif_squirt", "BDSMgonewild", "15jar", "legalporno",
                "Daddypics", "nsfw_hot_amateur", "Drawn_Horsecock", "NadineJansen", "fbb", "WifeSwapping",
                "vikingsgonewild", "Claire_Abbott_", "CEIcaptions", "twinksinstraightporn", "MCUPorn", "cohf",
                "hitomitanaka", "FMK", "hairywomenaresexy", "ankletporn", "ass_grab", "Titty_Fuck", "Real_Nude_Celebs",
                "Boltontits", "IntactPorn", "anime_nsfw", "pantiesinhermouth", "ReallyFitandThin", "gamersgonewild",
                "Pornin15seconds", "HelixStudios", "Standjob", "BustyNats", "mfm", "Hotwivesprep", "lilylove",
                "LondonKeyes", "NSFWeyes", "MeganSalinas", "assfingering", "Outies", "PerfectNipples", "girlsinchucks",
                "BRAww", "pissing", "septembercarrino", "Nibbles_n_Bites", "SloppyHentai", "BoobHunter", "Sexysenna",
                "PENIS_PENIS_PENIS", "LucyCat", "WomenInBras", "NinaNorth", "RedditsBestTits", "Meguri",
                "slowmotioncumshots", "AssAddicted", "only_MILFs", "peachesdoe97", "TheRedHeadedRabbit", "LillyFord",
                "LadiesForLadies", "lachupyourdaughters", "AyumiAnime", "HometownAmateurs", "predsgirl92",
                "BigDickSluts", "SoSaree", "AnalLadies", "Brownasshole", "buttstouchingbutts", "bestofblowjobs",
                "notits", "realgirlsphotoalbums", "boyshort", "GrannyPanties", "WomenInUniform", "meido", "DyedPubes",
                "VintageAmateurs", "SexDolls", "NSFW_BDSM", "awwyea", "SexyWallpapers", "WomenOfColorRisque",
                "PunkLovers", "GirlsWithBikes", "celebnipslips", "NSFWebms", "Schoolgirlsubslut", "CeelCee",
                "EightBitBailey", "GayPOCxxx", "INEGentlemanBoners", "PeggingCaptions", "MatureAction",
                "SexclusiveSelling", "SmallBoobsBigButt", "LatestPorn", "HighHeelTgirls", "Dattaint", "Fretton",
                "BoltedOnTS", "momsinsexyclothing", "Lamia", "BallBusting", "udders", "ggonewild", "SissyCensoredPorn",
                "ShylaStylez", "LiftedSkirts", "gayfacials", "Pullable", "CubsGoneWild", "JennaShea", "POV_Blowjobs",
                "Filipina", "analdildo", "Butchporn", "DylanRyder", "freedomphotos", "dogging", "rough_sex_gifs",
                "PUSSY_GIRLS", "BarelyDressed", "AdultBuys", "leotard", "WeAllGoWild", "BustyAmateurs", "carpetndrapes",
                "Pulldown", "pantiesaside", "SkinnyGirls", "BustyNaturalPornstars", "BraTittyfuck", "guysinsweatpants",
                "GracefulSubmission", "BetweenTwoMouths", "DoubleBlowjob", "runningshorts", "GravureGirls", "RayleneX",
                "SlaveAuctions", "gaggedcumshots", "reddtube", "suckingtogether", "NextDoorBoobies", "ACupAsses",
                "KatieCummings", "LewdLive", "CosplayBeauties", "CumshotFlinchers", "HotwifeLife", "FranceskaJaimes",
                "boxershorts", "clit", "femdomcaptions", "StretchedToBursting", "RingGag", "Kellymadison", "perkytits",
                "Whitney_Westgate", "NSFW_Versus", "mhbahj", "clenching", "DaphneRosen", "BathtubBeauties",
                "RippedUpperGarments", "BlondeAsians", "longthicknipples", "selffistinggirls", "SpitPlay",
                "rachelaldana", "BigDickSurprise", "hotgirlsinyogapants", "sophiereade", "katiebanks",
                "CurvyElvishGirl", "tidybush", "TiffanyWatson", "MarleyBrinx", "Anal_xxx", "ArubaJasmineFans",
                "porngifsonly", "BimbosParadise", "fitthescreen", "Pollewd", "JavSource", "girlsinhoods",
                "Onesiesgonewild", "desigirlswhitecocks", "Nsfw_JavView", "SeeThroughThongs", "assgifs", "OnlyAnal",
                "bentatthewaist", "HungryButtsGW", "hugeass", "HotMILFs", "AmateurHotties", "girlswithguns",
                "HeavyEyeliner", "workoutgirls", "cameltoepics", "Boots", "Cosplayheels", "wetontheoutside", "TrueClop",
                "boltedondicks", "Busty_gifs", "Asian_Fever", "naughtyatwork", "BDSM_NoSpam", "lesBDSM", "badassgirls",
                "TumblrPorn", "AnimatedPornGifs", "Celebsreality", "bimboxxx", "AnOralFixation", "AriaGiovanni",
                "jenni_gregg", "hashtagy0l0swaggang", "NaughtyAlysha", "VictoriaSecret", "BadBunny", "PaleSnowBunny",
                "Brawesome", "HungryBabes", "GayYoungOldPorn", "nsfw_sexy_girls", "SluttySelfies", "GayPokePorn",
                "PupPlay", "JpopFap", "Dark_nipples", "teaseonly", "GoneWildGifs", "Chasers", "GirlsOnTheToilet",
                "FantasyIsland", "tapebondage", "phloa", "polkadotgirls", "KateeLife", "Redhead_Ginger_Porn",
                "swimsuits", "LovelyHands", "ChicasReales", "TightSight", "WildFemale", "SleepingMen",
                "breathofthegonewild", "NSFWBulletTime", "smallfutanari", "FemDomBDSM", "GuysFingering", "JennyBlighe",
                "girlsontoilets", "rule34pee", "asamisuicide", "trapsexuals", "FrenchFilles", "MorganLee", "EbonyMILF",
                "NSFW_EYES", "MetalBondage", "sexynudeamateurs", "bigbrotherbros", "hotelhotties", "smalltitsbignips",
                "khalifa", "AlliRae", "FingerInAsshole", "girlsspankinggirls", "justthetip", "longlegs", "Perfect_Tits",
                "gor", "AmyRied", "snapfuck", "BrasOnTitsOut", "SandyVag", "IndonesianHotties", "jenni_lee", "COCKSLAP",
                "AmateurBrits", "Pouting", "AnnaTatu", "ShaftShining", "KathleenEggleton", "LadySlut",
                "SkinTightCotton", "ShittyHandbras", "Asian_O_faces", "jigglejiggle", "HomemadeGirls", "NakedAsians",
                "submissivemen", "SelfPiss", "BlueBones", "petgirls", "nudists", "rule34_abuse", "NikkiSapphire",
                "CelebsUnveiling", "PrincessKelsxo", "HungTraps", "LexiVixi", "beautifulbutt", "LittleBritt",
                "BatmanPorn", "CalvinSelfies", "tsporn", "tgirl_frot", "Susann", "kdramafap", "FlashingTheGoods",
                "AspenReign", "lostlilkitty", "sexycellulite", "nsfw_messages", "GirlsWithBigHats",
                "Pornstar_VS_Pornstar", "ChocolateStarfish", "YouTubeFakes", "PornGifsbyBot", "hairymilfs",
                "Amateur_Bitches", "Amateurselfpic", "SexyStarWars", "Feetup", "Zelda_Romance", "Plumpers",
                "hotlatinas", "NordicWomen", "javure", "SoftcoreJapan", "EdmontonGoneWild", "MasturbatorsAnonymous",
                "AsianPee", "Ranked_Girls", "TIFT", "NSFW_hardcore", "nsfwcloseups", "SceneBoys", "metalgirls",
                "sacrilicious", "Homosexual", "guyskissing", "Lass", "malena_morgan", "JennyPoussin", "CelesteStar",
                "shay_laren", "frontmagazine", "NutsBabes", "Belle_in_the_woods", "YogaHotness", "PlayboyBunnies",
                "Neonkisses", "MatureAsian", "classygirls", "4uporn", "Thinwomen", "LaceyLaLa", "RoughShemanal",
                "Boobie", "bikinimalfunctions", "ThatPerfectAss", "LatexLucy", "nothingbutheels", "bootfetish",
                "NSFW_PUSSY_HD", "cgrey1433", "CandyCovered", "Katya_Sambuca", "gayfootfetish", "junamaki",
                "MEFetishism", "CassidyBanksXXX", "GuysHumpingThings", "ThickandBBWJeans", "LegsForDays", "TeenAnal",
                "Galdalou", "JizzChoke", "asianladyboy", "ChessieKay", "tgcumshots", "sextwithme",
                "creampie_anal_pussy", "Headless", "Footpaws", "Str8GuysFromBehind", "Asian_Anal", "BestPornstars",
                "FillHerUp", "AssUpBJ", "KamaSutraIllustrated", "sidepussy", "SophieDalzell", "IndianHot", "Maxisma",
                "nylon", "StripHer", "ReverseCowgirlRiding", "Tittyjobs", "GoneWildPublic", "NSFW_swingers",
                "HotGayStoners", "heelsandass", "HotwifeXXXCaptions", "TheRearView", "enemas", "natural_red",
                "NoHandsBlowjobs", "Carnivalsluts", "ThirdLeg", "GirlsWithBobs", "finalfantasynsfw", "AsianBush",
                "insertions_2", "EuropeanCuties", "Asian_Bondage", "FrillyPanties", "assholeselfies",
                "Western_Sex_Comics", "christinamodel", "Pumping", "sluttywife", "transpornmovies", "flexiblesmut",
                "BlowjobTongue", "ExtremeCameltoe", "TheCoverUp", "faponher", "KeepTheShoesOn", "hurtssogood",
                "bobbi_starr", "Fingerbite", "hippie_chicks", "JessieAndrews", "GigiRivera", "PNNBPS",
                "thick_shorthair", "iliketakeoutfood", "lvangel69", "Anastasia_Kvitko", "ana_cheri", "sheknows",
                "dimples_of_venus", "ExsqueezeMe", "ParisDylan", "NetVideoGirls", "WantYouWanking", "LaurenKHarper",
                "NaughtyDressGirls", "camgirl_pics", "CodiLake", "Bangable", "RankThem", "naughtyporn",
                "SunKissedStunners", "BackingUp", "Lewdism", "bigtitsandhighheels", "ToplessInYogaPants", "cumonbraces",
                "AdorableBondage", "nsfwdancingirl", "officialmollybennett", "PerfectWhiteBoobs", "SolesAndButtholes",
                "Assgrab", "Self_Perfection", "ShinyFetish", "Thapt", "malespandex", "MilitaryMen", "DropEm",
                "Mass_Effect_Porn", "ClopComics", "ArcherPorn", "ShingekiNoHentai", "GirlswithPigtails",
                "PerfectThighs", "Titsgalore", "EthnicGirlFacials", "polishnsfw", "ketogonewild", "malesgonewild",
                "kinkyporn", "nsfwnew", "anilingus", "Aella_Girl", "iheartbigtits", "100sexiest", "Shy__RedheadV2",
                "AJoy4Ever", "TBoIRule34", "CelebrityManAss", "ddlg", "Jessica_nigri", "AnthroClop", "DudeClub",
                "amateur_bbws", "GirlsofGoneWildCurvy", "DanielleSellers", "Busty_Porn_Videos", "boysandtoys", "ariane",
                "ChestEnvy", "gwcurvy", "bearsinbriefs", "pantygifs", "POCLadyBoners", "TeannaTrump", "SummerBrielle",
                "PornLegend", "hotgirlswithtattoos", "GirlsSpreadLegs", "KinkyStuff", "HotWithSauce", "Colorslash",
                "geekbonersNSFW", "Huniepop", "ComicPlot", "boob_gifs", "LatinaXXX", "Wife_Selfie", "tumblr_nsfw_gifs",
                "KatieKox", "Bulge", "celebgifs", "pixiechick", "breakfeminazis", "nudemalecelebs", "HarnessGags",
                "Titstheseason", "BathtimeGW", "sissymaid", "Tgroups", "DreamFuck", "celebcollections", "medfet",
                "Anal_Sex_NSFW", "hotdoggystyle", "iamatease", "SexyAtHome", "CelebrityCrease", "Naughty_America",
                "AsianAsses", "basketballshorts", "Furry_Backsack", "OverwatchRule34", "SidneyTanlines",
                "PrincessPixie", "Mofos_Network", "CFCM", "60fpscum", "HeatherVandeven", "GayPornCentral",
                "NSFW_skinnywithabs", "tittysqueeze", "TitTats", "MarieMcCray", "georgiajones", "Randomgirls",
                "TeeDubsBooty", "Lacey_Banghard", "HayleyMarieCoppin", "bedroomselfies", "RealMetalchicks",
                "NeighborhoodNympho", "breastsubever", "facedacam", "KylieCole", "ClubCarmellaBing", "realnudecelebs",
                "BelAmi", "BoobsAndBooze", "GirlsWithBodyArt", "Pelfies", "wethair", "Kortney_Kane", "nekogirls",
                "laidout", "MistyStone", "AlternativeBeauty", "nsfwpics", "truthdust", "RateMyRack",
                "nsfwGameOfThrones", "dickpic", "Oral_Fixxxation", "povdoggy", "HollyKiddo", "sexychubbywife",
                "JelenaJensen", "camgirl_gifs", "NSFWHijab", "WhatHappensOnSnapchat", "Lana_Rhoades", "tetitas",
                "CarinnhaWhite", "SexyShemaleOutfits", "Alice_on_thc", "AsianAddicted", "morepbandj",
                "ClimbersGoneWild", "chickpeasyx", "OneShySubmissive", "SGCoralinne", "LanaRuby", "Petite_NSFW",
                "sexynsfw", "BootyGIFs", "Foxtails", "puremilf", "PearlGirls", "HotLeggings", "meninuniform",
                "sexysuperheroines", "glitch_porn", "SlutBusty", "hotamputees", "SSBBW_LOVE", "VintageErotica",
                "LadiesGoneWild", "PornLovers", "debs_and_doxies", "AnimatedGIF", "surfinggirls", "Lesbos", "GirlPlay",
                "Hayden_Winters", "BestoftheBreast", "TheBestCreampies", "Carli3", "bombshellbra", "bigdicks",
                "BigBoobBasement", "InstaFap", "hotandhorny", "sarahsb89", "huge_boobs", "Wardrobemalfunction",
                "celebdominatrixs", "NSFWPonytails", "AvaDevine", "SolidSnakes", "YeOldeNudes", "daddybears",
                "amateur_asians", "yiffplus", "lecherous_hump", "xxxassist", "NSWF_Porn_GIF_Teen",
                "MummificationBondage", "Ebony_BBW", "Bangbros_Network", "UnderboobGW", "paletits", "NSFW_Feet",
                "SpankingBottoms", "webcamgirls", "BlowjobsGalore", "PennysLittleSecret", "expandolicious",
                "shemalesinboots", "Flatties", "YiffMinus", "sexyhairnsfw", "Hidden_Penetration", "breastbondage",
                "Nubiles_Network", "HattieWatson", "fit2phat", "cumlover", "onhertoes", "AmateurFucking",
                "Arianny_Celeste", "ChildbearingHips", "Girlswithbigcameras", "CamShow", "GirlsWithBareFeet",
                "NaughtyPlayground", "amateur_GIF", "ShowingOffInPublic", "ThatLook", "RosieDanvers", "LoveRandalin",
                "onoffcompare", "Fit_NSFW", "tightdress", "all_belly_no_tits", "overwatch_hentai", "amateur_ebonys",
                "blacktraps", "TwinGuys", "AsiansFlashing", "b00b3d", "nsfw_3D_porn", "eufrat", "sexyback", "Omorashi",
                "Pokemon_Go_Porn_NSFW", "ArtOfSucking", "GirlsBeingUsed", "xHamsterOfficial", "ballgag",
                "rule34futanari", "AustralianHotties", "JuelzVentura", "lilycarter", "Petitetastic", "JewSnap",
                "WYCSTVTTU", "Cardigonewild", "TrueENF", "CourtneyandAbby", "RearSplits", "torturechamber", "backboob",
                "visiblepantyline", "gaydadbigdick", "TightWetClingy", "KneesUp", "Bumfun", "BraLines", "nsfw_gfys",
                "holdingherlegs", "femaletongues", "GirlsInShortShorts", "ThighShots", "Contortionists",
                "IndianHotties", "MadisonScott", "Forniphilia", "TaylorVixen", "vocaloidhentai", "BitingHerLip",
                "sweaterpuppies", "nsfwswimsuit", "kates_playground", "Ponygirl", "sensipearl", "HarleySpencer",
                "Jbby27", "cowgirl_gifs", "SpiritSnake", "azeeenbarbie69", "KreamxKween", "bbwgifs", "VineArchive",
                "Mia_Malkova", "CasuallyMisclothed", "FreeWifeHigh", "NSFWInked", "purple_honeyOfficial",
                "VividVentures", "ChicksAndDicks", "LetMeSeeHerAsshole", "WifeBoobs", "cummytummies", "NoTorso",
                "thick_clothed", "PortalPorn", "inthebushes", "hanging", "WhatWouldYouRateHer", "cumtrays",
                "BrownBubbas", "sexybunnies", "Adultpics", "nakedbabes", "nsfwonly", "spod", "pinupstyle", "nsfwHTML5",
                "girlsboxing", "sappho", "thebackdoor", "MouthStretching", "MissIvyJean", "menincars", "gaygingers",
                "tanlinesNSFW", "BestFreePorn", "adoniclove", "NatureBoys", "crotchgrab", "Pecs", "wetmale",
                "NudeLadies", "poolboys", "Sophie_Coady", "piper_perri", "RealGF", "Spanktai", "Pov_Porn_Gifs",
                "FullOfFantasies", "EbonyBabes", "GayThong", "Kendra_Lust", "PolyGrumps", "PaleBabes",
                "GoneWildPlusSize", "NiceCans", "SaggyTits", "MultiCocked", "AsianUpskirt", "venezuelangirls", "nopan",
                "Actually_curvy", "CelebrityAnus", "gaygonewild", "aww_boobs", "GirlsinRippedDenim", "GrowingTheFamily",
                "PantyPlay", "xxxvrsites", "NSFW_Uncut", "fffxxx", "GrandmotherlyGILFs", "agegapsex", "AllAboutButts",
                "best_busty_petite", "balletboots", "nsfw_MixedGirls", "AllieHaze_", "FireBush", "oldschoolboners",
                "Creamywetpussies", "cumonher", "NoFemaleNudityPorn", "SchoolgirlBondage", "whipping", "BrasNSFW",
                "heavyrubber", "Tgirl_pics", "Flexible", "theperfectwife", "NSFWScience", "NerdyGW",
                "shamelesslyunshaven", "ahritime", "FoodOnGirls", "GWBodyWriting", "gonewildTraps", "DragonageNSFW",
                "Godsgirls", "PlayfulPikachu", "JigglingCelebs", "cuckold_gifs", "TheSockDrawer", "Bleached_Assholes",
                "Alexis_Kline", "CollegeSnapchats", "milfalbum", "Guysinshortshorts", "amsinhd", "lookback",
                "PussyJuicy", "animeJOmaterial", "indianmilf", "pantygirl710", "IrinaBuromskih", "AgainstGlass",
                "AliceAardvark", "pinchingnipples", "sixtysecondhentai", "AsianDownblouse", "Elizabeth_Marxs",
                "AddictedToBlackGirls", "doublehandy", "ValoryIrene", "RebeccaCrow", "Noelle_Easton",
                "Artsy_NSFW_Wallpapers", "vipissy", "BombShellBlondes", "SexyDresses", "leggs", "AmaiLiu", "BustyBrits",
                "FMA_Hentai", "GirlsInPlaid", "collarbone", "pornwatchers", "emary95m", "shemalesporn",
                "Harmony_Reigns", "lilbitofeverything", "XXXwebms", "RachelDemita", "KikAdult", "TheOneFingerSelfie",
                "animepantiesIRL", "lookatmexxx", "pettankohentai", "samij420", "NekkidWomen", "OppaiHearts",
                "Leah_Gotti", "IAmFrustrated", "averii", "TransgenderGalleries", "BabyBlondeNurse", "lostlikethought",
                "BigBlackBooty", "PerfectBlowjobEyes", "PornSexyTeenXXX", "She_squats", "femalemaleanalingus",
                "Vapers_GoneWild", "WetBabes", "avatar34", "skaro", "fillyfiddlers", "hairychicks", "AsianOL",
                "MotorcyclesGoneWild", "goneclothed", "pornpedia", "SaturdayMorningGirls", "celebupskirts",
                "sapphicgifs", "thirdgender", "NeverUntieMe", "IvySnow", "MaryJaneJohnson", "StunningPorn",
                "sw33tandslutty", "LowHangingBalls", "StruggleHentai", "NudieWorld", "ImaginaryGayBoners",
                "Fiametta451", "XXXcitedBrunette", "TheGifer", "BestCurves", "MindFuckPorn", "Big_Tits_Big_Nips_HQ",
                "SexyMai", "PornCuties", "milfboltons", "asbigasithought", "Bella_Thorne_nude", "DirtyCutie8",
                "hentaicharts", "Proxy_Paige", "veryhairy", "just_tits", "Reiq", "Cum_Covered_Hentai",
                "lewd_phone_wallpapers", "HollyHalston", "BustyGoddess", "BottomView", "kneeling", "NakedPics",
                "AppleSoey", "GirlsWithKicks", "voyeurzone", "VixxxinDliteOfficial", "MishaCross_",
                "hotguyswithglasses", "TitsTatsAss", "SistersofChrist", "SidePanties", "NSFW_GIFS_PORN", "bustychicks",
                "Wifey", "TaylorRain", "queenconcise", "TheNeedToKnead", "AnalCasting", "aww_RealGirls", "girlsSelfies",
                "nsfworldtour", "CourtnieQuinlan", "bbwcumsluts", "cosplay_nsfwshoops", "Gonewilds_Secret",
                "Evil_Angel", "OutsideNude", "NSFW_RealGirlfriends", "GoneWildNotReviled", "TaliaC", "KalenaA",
                "SexyCosplayBabes", "gone_wild", "ganguro", "Youngblackandsexy", "sally_anon", "BiggerThanHerFace",
                "RateMyAss", "EquineFuta", "tastyfreckles", "Hot_Milf", "On_Her_Knees", "dailypornvideos", "HGsLB",
                "Lena_Anderson", "SophieMuse", "misspeppperr", "SizzlingSexyBabes", "Kelsi_Monroe", "Kaethir",
                "Lena_Paul", "Angela_White", "PantiesNetwork", "Elsa_Jean", "Adriana_Chechik", "Boobies_Are_Awesome",
                "Miabellabunny", "GoneWildHotties", "BTypeBooty", "MuffyMasters", "CreampieInAsia", "BigCockGirls",
                "LadyDicks", "Provocateur_Addict", "xxxpensivetastes", "SissyHentai", "JessicaDolan", "SomethingExtra",
                "NiceTitties", "pennyforyourkinks", "GreatestTits", "SwimsuitBooty", "Leshub", "celebrityfakes",
                "boobkarma", "BigBoobies", "hugeracks", "BustyBabesGalore", "KeepitClassy", "formylover", "Nightlysex",
                "FoxyLadies", "GirlsWithBigGuns", "NudeBeauty", "QualityNsfw", "rud_fuckers", "GirlWithThePiercings",
                "Caitlyn87", "holycherriesbatcave", "Gif_sources", "JustTooCute", "LShima", "DelorisJean",
                "scale10crazy", "BigTitsLittleNips", "GirlsWashingCars", "tanlinemismatch", "NudeTastic", "HiddenCams",
                "browneyedgirl6257", "titshansen", "gaygentlemanboners", "Asshole_Lover", "nsfw_gays", "freckledfire",
                "JessaRhodes_", "bestof_gangbangs", "LeelooGoneWild", "TINYlips", "LilRedVelvettt", "izzyrssii",
                "whimsicalkitten", "JuuicyJasmine", "CharlieGirl", "NSFWGfysOnly", "ShinySluts", "CumOnTheirFaces",
                "naughtynextdoor", "amazing_girls", "truesexypizza", "justperfect", "Asscentric", "OfficialNYLDY",
                "ChocolateCumsluts", "NSFW_2in1", "kaGW", "Openpanel", "nsfwslowmo", "Sauna_NSFW",
                "americanapparelmodels", "just_the_tip", "Sexy_Panties", "NSFW_Hentai_pics", "OhNoDadWentWild",
                "sweetfru1t", "SluttyDress", "nudesforpizza", "bigboobiesDDD", "Remy_Lacroix", "princessdahliamoon",
                "TaTa_Returns", "tiny_blonde", "WhoresWithMore", "ShemalePictures", "LadyboyLover", "ShemaleDaily",
                "SabinaDragon", "NattiMaretta", "NSFW_Media", "SexyInJeans", "GirlsGoneDogeCoin", "Victory_Girls",
                "NSFWSector", "TeamVRB", "MaxineSapphire", "OnePieceSwimsuits", "rule34", "Overwatch_Porn",
                "rule34_comics", "PokePorn", "Rule34LoL", "Rule34Overwatch", "Shadman", "starwarsnsfw", "2Booty",
                "Naruto_Hentai", "TheLostWoods", "disneyporn", "Rule34RainbowSix", "PixelArtNSFW", "TeenTitansPorn",
                "superheroporn", "AvatarPorn", "SourcePornMaker", "smashbros34", "OverwatchNSFW", "ClopClop", "Dbz34",
                "Slutoon", "fitdrawngirls", "rule34_albums", "Rule34_Futanari", "rule34celebs", "rule34cartoons",
                "mariorule34", "AzerothPorn", "harrypotterporn", "Witcher_NSFW", "rule34gifs", "iateacrayon",
                "NSFWskyrim", "Incase", "rule34pinups", "laracroftNSFW", "rule34feet", "Metroid34", "FeralPokePorn",
                "rule34_ass", "MonsterMen", "masserect", "asseffect", "rule34bondage", "dota2smut", "moxxigonewild",
                "UnderTail", "ZootopiaPorn", "minus8", "BioshockPorn", "NSFWarframe", "FapFap", "StevenUniverseNSFW",
                "frozenporn", "ATPorn", "Rule34_anal", "SonicPorn", "Rule34cumsluts", "KimPossiblePorn",
                "Overwatch_Rule34", "superheroinesdefeated", "Paladins_Porn", "fortnitegonewild", "PizzaThot",
                "JLullaby", "realrule34", "GothamXXX", "Rule34LifeisStrange", "GOTporn", "TrueClop",
                "breathofthegonewild", "rule34pee", "gor", "rule34_abuse", "BatmanPorn", "SexyStarWars",
                "Zelda_Romance", "finalfantasynsfw", "Mass_Effect_Porn", "ClopComics", "ArcherPorn", "TBoIRule34",
                "AnthroClop", "Huniepop", "OverwatchRule34", "sexysuperheroines", "Homesmut", "SolidSnakes",
                "overwatch_hentai", "nsfw_3D_porn", "rule34futanari", "PortalPorn", "PolyGrumps", "ahritime",
                "DragonageNSFW", "avatar34", "skaro", "fillyfiddlers", "Reiq", "BTypeBooty", "kaGW", "GaybrosGoneWild",
                "TotallyStraight", "gayporn", "twinks", "broslikeus", "CumFromAnal", "gaymersgonewild", "GayGifs",
                "gaynsfw", "HomemadeGayPorn", "gaycumsluts", "Men2Men", "yaoi", "gfur", "CumCannonAddicts", "gaybears",
                "gaycruising", "GayKink", "NSFW_GAY", "gaystrugglefuck", "lovegaymale", "BarebackGayPorn", "gayotters",
                "TwinkLove", "GayDaddiesPics", "gayholdthemoan", "Frotting", "GayFreeUse", "GaySnapchatImages",
                "gayasshole", "manlove", "helpinghand", "GayWatersports", "ManSex", "GayChubs", "AlphaMalePorn",
                "BHMGoneWild", "gaycumshots", "GayBlowjobs", "gaybreeding", "gayrimming", "rule34gay", "blackballed",
                "BisexualHentai", "baramanga", "MrBlowJob", "GayCreampie", "GayCocksuckers", "Gaycouplesgonewild",
                "GaySex", "boysfucking", "GayNSFWFunny", "GayWincest", "GaySelfies", "frot", "Gfurcomics", "Scally",
                "BateBuds", "VintageGayPics", "GayHiddenCams", "HelixStudios", "GayPOCxxx", "gayfacials",
                "GayYoungOldPorn", "GayPokePorn", "GuysFingering", "Homosexual", "guyskissing", "gayfootfetish",
                "boysandtoys", "GayPornCentral", "GayExxxtras", "BelAmi", "morningbro", "VineArchive", "gaygingers",
                "wetmale", "GayThong", "ImaginaryGayBoners", "nsfw_gays"));
    }

    public static void addAll(Map<String, Consumer<CommandData>> map) {
        map.put("hentai", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hentai").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(), event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai. Please report the following error to the bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai is unavailable right now. Please try again later!").setFiles()
                        .setComponents().setEmbeds().queue();
            }
        });

        map.put("anal", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hanal").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai anal. Please report the following error to the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai anal is unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("boobjob", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=paizuri").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai boobjobs. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai boobjobs are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("boobs", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hboobs").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai boobs. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai boobs are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("fox", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hkitsune").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai foxes. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai foxes are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("kemonomimi", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=kemonomimi").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai kemonomimi. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai kemonomimi is unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("midriff", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hmidriff").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai midriff. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai midriff is unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("neko", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hneko").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai nekos. Please report the following error to " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai nekos are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("tentacle", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=tentacle").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai tentacles. Please report the following error to" + " " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai tentacles are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("thigh", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hthigh").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai thighs. Please report the following error to" + " " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai thighs are unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("yaoi", event -> {
            final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=yaoi").build();
            try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                final String result = Constants.GSON.fromJson(Objects.requireNonNull(response.body()).string(),
                        JsonObject.class).get("message").getAsString();
                event.hook().editOriginalEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                        .setFiles().setComponents().queue();
                addRegenerateButton(event.hook(), event.user(), event.group(),
                        event.subcommand());
            } catch (final IOException exception) {
                String strBuilder = "```\n" + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(
                        exception) + "```";

                event.hook().editOriginal(
                                "There was an issue retrieving hentai yaoi. Please report the following error to" + " " + "the " + "bot owner:\n" + strBuilder)
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final JsonSyntaxException exception) {
                event.hook().editOriginal("Hentai yaoi is unavailable right now. Please try again later!")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });

        map.put("loli", event -> {
            final String[] GIFS = {"https://media.giphy.com/media/jmSjPi6soIoQCFwaXJ/giphy.gif", "https://media.giphy.com/media/7zSO56YSB0DhNvBUWt/giphy.gif", "https://media.giphy.com/media/3o6wNPIj7WBQcJCReE/giphy.gif", "https://media.giphy.com/media/kxevHLcRrMbtC1SXa8/giphy.gif", "https://media.giphy.com/media/3dkPQ9JnxxQnVVMdOl/giphy.gif", "https://media.giphy.com/media/3osBLaQjYdcuVYpgXu/giphy.gif", "https://media.giphy.com/media/3oFzmnMZAOzN5XLh5K/giphy.gif", "https://media.giphy.com/media/l1IBiRf6RDmlnhguQ/giphy.gif"};

            final String FBI_AUDIO = "src/main/resources/audio/fbi_open_up.mp3";

            final var embed = new EmbedBuilder();
            embed.setTitle("You are under arrest!");
            embed.setDescription(event.user().getAsMention() + ", you have been spotted looking for loli!");
            embed.setImage(GIFS[ThreadLocalRandom.current().nextInt(GIFS.length)]);
            embed.setTimestamp(Instant.now());
            event.hook().editOriginalEmbeds(embed.build()).setFiles().setComponents().mentionRepliedUser(false).queue();

            if (event.hook().getInteraction().isFromGuild() && Objects.requireNonNull(
                            Objects.requireNonNull(event.hook().getInteraction().getMember()).getVoiceState())
                    .inAudioChannel()) {
                try {
                    AudioManager.play(event.hook().getInteraction().getGuild(),
                            event.hook().getInteraction().getMember().getVoiceState().getChannel(),
                            Path.of(FBI_AUDIO).toUri().toURL());
                } catch (MalformedURLException exception) {
                    Constants.LOGGER.info("Failed to load file: '{}'", FBI_AUDIO);
                }
            }
        });

        map.put("rule34", event -> {
            String searchTerm = event.rule34SearchTerm().orElse("");
            try {
                final String encodedInput = URLEncoder.encode(searchTerm.trim().replace(" ", "_"),
                        StandardCharsets.UTF_8);
                final Document document = Jsoup.connect(
                        "https://rule34.xxx/index.php?page=post&s=list&tags=" + encodedInput).get();
                final Element pagination = document.getElementsByClass("pagination").first();

                int pages;
                if (pagination == null) {
                    pages = 1;
                } else {
                    final List<Element> pageButtons = pagination.childNodes().stream()
                            .filter(node -> node.hasAttr("href")).map(Element.class::cast).toList();
                    pages = pageButtons.size() - 1;
                    if (pages < 1) {
                        pages = 1;
                    }

                    for (final Element pageBtn : pageButtons) {
                        if ("last page".equalsIgnoreCase(pageBtn.attr("alt"))) {
                            pages = Integer.parseInt(pageBtn.attr("href").split("&pid=")[1]) / 42;
                            break;
                        }
                    }
                }

                final int randomPage = ThreadLocalRandom.current().nextInt(pages);
                final String newURL = "https://rule34.xxx/index.php?page=post&s=list&tags=" + encodedInput + "&pid=" + randomPage * 42;
                final Document newPage = Jsoup.connect(newURL).get();

                final Element imageList = newPage.getElementsByClass("image-list").first();
                List<String> images = new ArrayList<>();
                if (imageList != null) {
                    images = imageList.getElementsByClass("thumb").stream().map(element -> element.select("a"))
                            .map(element -> element.attr("href")).collect(Collectors.toList());
                }

                if (images.isEmpty()) {
                    event.hook().editOriginal("No results found for `" + searchTerm + "`.").setFiles().setComponents()
                            .setEmbeds().queue();
                    return;
                }

                Collections.shuffle(images);
                final Document imagePage = Jsoup.connect("https://rule34.xxx/" + images.getFirst()).get();
                final Element image = imagePage.selectFirst("img#image");
                if (image == null) {
                    event.hook().editOriginal("No results found for `" + searchTerm + "`.").setFiles().setComponents()
                            .setEmbeds().queue();
                    return;
                }

                final String imageURL = image.attr("src");
                event.hook().editOriginal(imageURL).queue();
            } catch (final IOException exception) {
                event.hook().editOriginal("There was an issue accessing the rule34 database! Please try again later.")
                        .setFiles().setComponents().setEmbeds().queue();
            } catch (final NullPointerException exception) {
                event.hook().editOriginal(
                                "I have not found any rule34 for '" + searchTerm.trim() + "'! This could however be a bug, feel free to try again.")
                        .setFiles().setComponents().setEmbeds().queue();
            }
        });
    }

    public record CommandData(InteractionHook hook, User user, String group, String subcommand,
                              Optional<String> rule34SearchTerm) {
        public static CommandData from(final SlashCommandInteractionEvent event) {
            return new NSFWCommandList.CommandData(event.getHook(), event.getUser(), event.getSubcommandGroup(),
                    event.getSubcommandName(),
                    event.getOption("search_term", Optional.empty(),
                            optionMapping -> Optional.of(optionMapping.getAsString())));
        }

        public static CommandData from(final ButtonInteractionEvent event) {
            if (!Objects.requireNonNull(event.getButton().getId()).startsWith("regenerate-"))
                return null;

            String[] split = event.getButton().getId().split("-");
            if (split.length != 5) {
                event.getHook().editOriginal(
                                "There has been an error processing the command you tried to run. Please try again!")
                        .setFiles().setComponents().setEmbeds().queue();
                return null;
            }

            long messageId = Long.parseLong(split[1]);
            long userId = Long.parseLong(split[2]);
            String group = split[3];
            String subcommand = split[4];

            if (event.getMessageIdLong() != messageId) {
                return null;
            }

            if (event.getUser().getIdLong() != userId) {
                event.getHook().editOriginal("You do not have permission to regenerate this command!")
                        .setFiles().setComponents().setEmbeds().queue();
                return null;
            }

            return new NSFWCommandList.CommandData(event.getHook(), event.getUser(), group,
                    subcommand, Optional.empty());
        }
    }
}
