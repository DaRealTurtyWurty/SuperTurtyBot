package dev.darealturtywurty.superturtybot.modules.collectable.country;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import dev.darealturtywurty.superturtybot.registry.Registry;

@Registerer
public class CountryCollectableRegistry {
    public static final Registry<CountryCollectable> COUNTRY_REGISTRY = new Registry<>();

    public static CountryCollectable register(String name, CountryCollectable.Builder collectable) {
        return COUNTRY_REGISTRY.register(name, collectable.build());
    }

    public static final CountryCollectable AFGHANISTAN = register("afghanistan", CountryCollectable.builder()
            .name("Afghanistan")
            .emoji("flag_af")
            .question("What is the capital of Afghanistan?")
            .answer("kabul")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ALBANIA = register("albania", CountryCollectable.builder()
            .name("Albania")
            .emoji("flag_al")
            .question("What is the capital of Albania?")
            .answer("tirana")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ALGERIA = register("algeria", CountryCollectable.builder()
            .name("Algeria")
            .emoji("flag_dz")
            .question("What is the capital of Algeria?")
            .answer("algiers")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ANDORRA = register("andorra", CountryCollectable.builder()
            .name("Andorra")
            .emoji("flag_ad")
            .question("What is the capital of Andorra?")
            .answer("andorra la vella")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ANGOLA = register("angola", CountryCollectable.builder()
            .name("Angola")
            .emoji("flag_ao")
            .question("What is the capital of Angola?")
            .answer("luanda")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ANTIGUA_AND_BARBUDA = register("antigua_and_barbuda", CountryCollectable.builder()
            .name("Antigua and Barbuda")
            .emoji("flag_ag")
            .question("What is the capital of Antigua and Barbuda?")
            .answer("st. john's")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ARGENTINA = register("argentina", CountryCollectable.builder()
            .name("Argentina")
            .emoji("flag_ar")
            .question("What is the capital of Argentina?")
            .answer("buenos aires")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ARMENIA = register("armenia", CountryCollectable.builder()
            .name("Armenia")
            .emoji("flag_am")
            .question("What is the capital of Armenia?")
            .answer("yerevan")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable AUSTRALIA = register("australia", CountryCollectable.builder()
            .name("Australia")
            .emoji("flag_au")
            .question("What is the capital of Australia?")
            .answer("canberra")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable AUSTRIA = register("austria", CountryCollectable.builder()
            .name("Austria")
            .emoji("flag_at")
            .question("What is the capital of Austria?")
            .answer("vienna")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable AZERBAIJAN = register("azerbaijan", CountryCollectable.builder()
            .name("Azerbaijan")
            .emoji("flag_az")
            .question("What is the capital of Azerbaijan?")
            .answer("baku")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BAHAMAS = register("bahamas", CountryCollectable.builder()
            .name("The Bahamas")
            .emoji("flag_bs")
            .question("What is the capital of The Bahamas?")
            .answer("nassau")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BAHRAIN = register("bahrain", CountryCollectable.builder()
            .name("Bahrain")
            .emoji("flag_bh")
            .question("What is the capital of Bahrain?")
            .answer("manama")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BANGLADESH = register("bangladesh", CountryCollectable.builder()
            .name("Bangladesh")
            .emoji("flag_bd")
            .question("What is the capital of Bangladesh?")
            .answer("dhaka")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BARBADOS = register("barbados", CountryCollectable.builder()
            .name("Barbados")
            .emoji("flag_bb")
            .question("What is the capital of Barbados?")
            .answer("bridgetown")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BELARUS = register("belarus", CountryCollectable.builder()
            .name("Belarus")
            .emoji("flag_by")
            .question("What is the capital of Belarus?")
            .answer("minsk")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BELGIUM = register("belgium", CountryCollectable.builder()
            .name("Belgium")
            .emoji("flag_be")
            .question("What is the capital of Belgium?")
            .answer("brussels")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BELIZE = register("belize", CountryCollectable.builder()
            .name("Belize")
            .emoji("flag_bz")
            .question("What is the capital of Belize?")
            .answer("belmopan")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BENIN = register("benin", CountryCollectable.builder()
            .name("Benin")
            .emoji("flag_bj")
            .question("What is the capital of Benin?")
            .answer("porto-novo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BHUTAN = register("bhutan", CountryCollectable.builder()
            .name("Bhutan")
            .emoji("flag_bt")
            .question("What is the capital of Bhutan?")
            .answer("thimphu")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BOLIVIA = register("bolivia", CountryCollectable.builder()
            .name("Bolivia")
            .emoji("flag_bo")
            .question("What is the capital of Bolivia?")
            .answer("sucre")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BOSNIA_AND_HERZEGOVINA = register("bosnia_and_herzegovina", CountryCollectable.builder()
            .name("Bosnia and Herzegovina")
            .emoji("flag_ba")
            .question("What is the capital of Bosnia and Herzegovina?")
            .answer("sarajevo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BOTSWANA = register("botswana", CountryCollectable.builder()
            .name("Botswana")
            .emoji("flag_bw")
            .question("What is the capital of Botswana?")
            .answer("gaborone")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BRAZIL = register("brazil", CountryCollectable.builder()
            .name("Brazil")
            .emoji("flag_br")
            .question("What is the capital of Brazil?")
            .answer("brasilia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BRUNEI = register("brunei", CountryCollectable.builder()
            .name("Brunei")
            .emoji("flag_bn")
            .question("What is the capital of Brunei?")
            .answer("bandar seri begawan")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BULGARIA = register("bulgaria", CountryCollectable.builder()
            .name("Bulgaria")
            .emoji("flag_bg")
            .question("What is the capital of Bulgaria?")
            .answer("sofia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BURKINA_FASO = register("burkina_faso", CountryCollectable.builder()
            .name("Burkina Faso")
            .emoji("flag_bf")
            .question("What is the capital of Burkina Faso?")
            .answer("ouagadougou")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable BURUNDI = register("burundi", CountryCollectable.builder()
            .name("Burundi")
            .emoji("flag_bi")
            .question("What is the capital of Burundi?")
            .answer("gitega")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CAPE_VERDE = register("cape_verde", CountryCollectable.builder()
            .name("Cape Verde")
            .emoji("flag_cv")
            .question("What is the capital of Cape Verde?")
            .answer("praia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CAMBODIA = register("cambodia", CountryCollectable.builder()
            .name("Cambodia")
            .emoji("flag_kh")
            .question("What is the capital of Cambodia?")
            .answer("phnom penh")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CAMEROON = register("cameroon", CountryCollectable.builder()
            .name("Cameroon")
            .emoji("flag_cm")
            .question("What is the capital of Cameroon?")
            .answer("yaounde")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CANADA = register("canada", CountryCollectable.builder()
            .name("Canada")
            .emoji("flag_ca")
            .question("What is the capital of Canada?")
            .answer("ottawa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CENTRAL_AFRICAN_REPUBLIC = register("central_african_republic", CountryCollectable.builder()
            .name("the Central African Republic")
            .emoji("flag_cf")
            .question("What is the capital of the Central African Republic?")
            .answer("bangui")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CHAD = register("chad", CountryCollectable.builder()
            .name("Chad")
            .emoji("flag_td")
            .question("What is the capital of Chad?")
            .answer("n'djamena")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CHILE = register("chile", CountryCollectable.builder()
            .name("Chile")
            .emoji("flag_cl")
            .question("What is the capital of Chile?")
            .answer("santiago")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CHINA = register("china", CountryCollectable.builder()
            .name("China")
            .emoji("flag_cn")
            .question("What is the capital of China?")
            .answer("beijing")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable COLOMBIA = register("colombia", CountryCollectable.builder()
            .name("Colombia")
            .emoji("flag_co")
            .question("What is the capital of Colombia?")
            .answer("bogota")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable COMOROS = register("comoros", CountryCollectable.builder()
            .name("the Comoros")
            .emoji("flag_km")
            .question("What is the capital of the Comoros?")
            .answer("moroni")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CONGO = register("congo", CountryCollectable.builder()
            .name("the Republic of the Congo")
            .emoji("flag_cg")
            .question("What is the capital of the Republic of the Congo?")
            .answer("brazzaville")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable DEMOCRATIC_REPUBLIC_OF_THE_CONGO = register("democratic_republic_of_the_congo", CountryCollectable.builder()
            .name("the Democratic Republic of the Congo")
            .emoji("flag_cd")
            .question("What is the capital of the Democratic Republic of the Congo?")
            .answer("kinshasa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable COSTA_RICA = register("costa_rica", CountryCollectable.builder()
            .name("Costa Rica")
            .emoji("flag_cr")
            .question("What is the capital of Costa Rica?")
            .answer("san jose")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable COTE_DIVOIRE = register("cote_divoire", CountryCollectable.builder()
            .name("C�te d'Ivoire")
            .emoji("flag_ci")
            .question("What is the capital of C�te d'Ivoire?")
            .answer("yamoussoukro")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CROATIA = register("croatia", CountryCollectable.builder()
            .name("Croatia")
            .emoji("flag_hr")
            .question("What is the capital of Croatia?")
            .answer("zagreb")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CUBA = register("cuba", CountryCollectable.builder()
            .name("Cuba")
            .emoji("flag_cu")
            .question("What is the capital of Cuba?")
            .answer("havana")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CYPRUS = register("cyprus", CountryCollectable.builder()
            .name("Cyprus")
            .emoji("flag_cy")
            .question("What is the capital of Cyprus?")
            .answer("nicosia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable CZECHIA = register("czechia", CountryCollectable.builder()
            .name("Czechia")
            .emoji("flag_cz")
            .question("What is the capital of Czechia?")
            .answer("prague")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable DENMARK = register("denmark", CountryCollectable.builder()
            .name("Denmark")
            .emoji("flag_dk")
            .question("What is the capital of Denmark?")
            .answer("copenhagen")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable DJIBOUTI = register("djibouti", CountryCollectable.builder()
            .name("Djibouti")
            .emoji("flag_dj")
            .question("What is the capital of Djibouti?")
            .answer("djibouti")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable DOMINICA = register("dominica", CountryCollectable.builder()
            .name("Dominica")
            .emoji("flag_dm")
            .question("What is the capital of Dominica?")
            .answer("roseau")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable DOMINICAN_REPUBLIC = register("dominican_republic", CountryCollectable.builder()
            .name("the Dominican Republic")
            .emoji("flag_do")
            .question("What is the capital of the Dominican Republic?")
            .answer("santo domingo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ECUADOR = register("ecuador", CountryCollectable.builder()
            .name("Ecuador")
            .emoji("flag_ec")
            .question("What is the capital of Ecuador?")
            .answer("quito")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable EGYPT = register("egypt", CountryCollectable.builder()
            .name("Egypt")
            .emoji("flag_eg")
            .question("What is the capital of Egypt?")
            .answer("cairo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable EL_SALVADOR = register("el_salvador", CountryCollectable.builder()
            .name("El Salvador")
            .emoji("flag_sv")
            .question("What is the capital of El Salvador?")
            .answer("san salvador")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable EQUATORIAL_GUINEA = register("equatorial_guinea", CountryCollectable.builder()
            .name("Equatorial Guinea")
            .emoji("flag_gq")
            .question("What is the capital of Equatorial Guinea?")
            .answer("malabo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ERITREA = register("eritrea", CountryCollectable.builder()
            .name("Eritrea")
            .emoji("flag_er")
            .question("What is the capital of Eritrea?")
            .answer("asmara")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ESTONIA = register("estonia", CountryCollectable.builder()
            .name("Estonia")
            .emoji("flag_ee")
            .question("What is the capital of Estonia?")
            .answer("tallinn")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ESWATINI = register("eswatini", CountryCollectable.builder()
            .name("Eswatini")
            .emoji("flag_sz")
            .question("What is the capital of Eswatini?")
            .answer("mbabane")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ETHIOPIA = register("ethiopia", CountryCollectable.builder()
            .name("Ethiopia")
            .emoji("flag_et")
            .question("What is the capital of Ethiopia?")
            .answer("addis ababa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable FIJI = register("fiji", CountryCollectable.builder()
            .name("Fiji")
            .emoji("flag_fj")
            .question("What is the capital of Fiji?")
            .answer("suva")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable FINLAND = register("finland", CountryCollectable.builder()
            .name("Finland")
            .emoji("flag_fi")
            .question("What is the capital of Finland?")
            .answer("helsinki")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable FRANCE = register("france", CountryCollectable.builder()
            .name("France")
            .emoji("flag_fr")
            .question("What is the capital of France?")
            .answer("paris")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GABON = register("gabon", CountryCollectable.builder()
            .name("Gabon")
            .emoji("flag_ga")
            .question("What is the capital of Gabon?")
            .answer("libreville")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GAMBIA = register("gambia", CountryCollectable.builder()
            .name("The Gambia")
            .emoji("flag_gm")
            .question("What is the capital of The Gambia?")
            .answer("banjul")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GEORGIA = register("georgia", CountryCollectable.builder()
            .name("Georgia")
            .emoji("flag_ge")
            .question("What is the capital of Georgia?")
            .answer("tbilisi")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GERMANY = register("germany", CountryCollectable.builder()
            .name("Germany")
            .emoji("flag_de")
            .question("What is the capital of Germany?")
            .answer("berlin")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GHANA = register("ghana", CountryCollectable.builder()
            .name("Ghana")
            .emoji("flag_gh")
            .question("What is the capital of Ghana?")
            .answer("accra")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GREECE = register("greece", CountryCollectable.builder()
            .name("Greece")
            .emoji("flag_gr")
            .question("What is the capital of Greece?")
            .answer("athens")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GRENADA = register("grenada", CountryCollectable.builder()
            .name("Grenada")
            .emoji("flag_gd")
            .question("What is the capital of Grenada?")
            .answer("st. george's")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GUATEMALA = register("guatemala", CountryCollectable.builder()
            .name("Guatemala")
            .emoji("flag_gt")
            .question("What is the capital of Guatemala?")
            .answer("guatemala city")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GUINEA = register("guinea", CountryCollectable.builder()
            .name("Guinea")
            .emoji("flag_gn")
            .question("What is the capital of Guinea?")
            .answer("conakry")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GUINEA_BISSAU = register("guinea_bissau", CountryCollectable.builder()
            .name("Guinea-Bissau")
            .emoji("flag_gw")
            .question("What is the capital of Guinea-Bissau?")
            .answer("bissau")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable GUYANA = register("guyana", CountryCollectable.builder()
            .name("Guyana")
            .emoji("flag_gy")
            .question("What is the capital of Guyana?")
            .answer("georgetown")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable HAITI = register("haiti", CountryCollectable.builder()
            .name("Haiti")
            .emoji("flag_ht")
            .question("What is the capital of Haiti?")
            .answer("port-au-prince")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable HONDURAS = register("honduras", CountryCollectable.builder()
            .name("Honduras")
            .emoji("flag_hn")
            .question("What is the capital of Honduras?")
            .answer("tegucigalpa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable HUNGARY = register("hungary", CountryCollectable.builder()
            .name("Hungary")
            .emoji("flag_hu")
            .question("What is the capital of Hungary?")
            .answer("budapest")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ICELAND = register("iceland", CountryCollectable.builder()
            .name("Iceland")
            .emoji("flag_is")
            .question("What is the capital of Iceland?")
            .answer("reykjavik")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable INDIA = register("india", CountryCollectable.builder()
            .name("India")
            .emoji("flag_in")
            .question("What is the capital of India?")
            .answer("new delhi")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable INDONESIA = register("indonesia", CountryCollectable.builder()
            .name("Indonesia")
            .emoji("flag_id")
            .question("What is the capital of Indonesia?")
            .answer("jakarta")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable IRAN = register("iran", CountryCollectable.builder()
            .name("Iran")
            .emoji("flag_ir")
            .question("What is the capital of Iran?")
            .answer("tehran")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable IRAQ = register("iraq", CountryCollectable.builder()
            .name("Iraq")
            .emoji("flag_iq")
            .question("What is the capital of Iraq?")
            .answer("baghdad")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable IRELAND = register("ireland", CountryCollectable.builder()
            .name("Ireland")
            .emoji("flag_ie")
            .question("What is the capital of Ireland?")
            .answer("dublin")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ISRAEL = register("israel", CountryCollectable.builder()
            .name("Israel")
            .emoji("flag_il")
            .question("What is the capital of Israel?")
            .answer("jerusalem")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ITALY = register("italy", CountryCollectable.builder()
            .name("Italy")
            .emoji("flag_it")
            .question("What is the capital of Italy?")
            .answer("rome")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable JAMAICA = register("jamaica", CountryCollectable.builder()
            .name("Jamaica")
            .emoji("flag_jm")
            .question("What is the capital of Jamaica?")
            .answer("kingston")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable JAPAN = register("japan", CountryCollectable.builder()
            .name("Japan")
            .emoji("flag_jp")
            .question("What is the capital of Japan?")
            .answer("tokyo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable JORDAN = register("jordan", CountryCollectable.builder()
            .name("Jordan")
            .emoji("flag_jo")
            .question("What is the capital of Jordan?")
            .answer("amman")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable KAZAKHSTAN = register("kazakhstan", CountryCollectable.builder()
            .name("Kazakhstan")
            .emoji("flag_kz")
            .question("What is the capital of Kazakhstan?")
            .answer("astana")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable KENYA = register("kenya", CountryCollectable.builder()
            .name("Kenya")
            .emoji("flag_ke")
            .question("What is the capital of Kenya?")
            .answer("nairobi")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable KIRIBATI = register("kiribati", CountryCollectable.builder()
            .name("Kiribati")
            .emoji("flag_ki")
            .question("What is the capital of Kiribati?")
            .answer("tarawa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable KUWAIT = register("kuwait", CountryCollectable.builder()
            .name("Kuwait")
            .emoji("flag_kw")
            .question("What is the capital of Kuwait?")
            .answer("kuwait city")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable KYRGYZSTAN = register("kyrgyzstan", CountryCollectable.builder()
            .name("Kyrgyzstan")
            .emoji("flag_kg")
            .question("What is the capital of Kyrgyzstan?")
            .answer("bishkek")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LAOS = register("laos", CountryCollectable.builder()
            .name("Laos")
            .emoji("flag_la")
            .question("What is the capital of Laos?")
            .answer("vientiane")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LATVIA = register("latvia", CountryCollectable.builder()
            .name("Latvia")
            .emoji("flag_lv")
            .question("What is the capital of Latvia?")
            .answer("riga")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LEBANON = register("lebanon", CountryCollectable.builder()
            .name("Lebanon")
            .emoji("flag_lb")
            .question("What is the capital of Lebanon?")
            .answer("beirut")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LESOTHO = register("lesotho", CountryCollectable.builder()
            .name("Lesotho")
            .emoji("flag_ls")
            .question("What is the capital of Lesotho?")
            .answer("maseru")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LIBERIA = register("liberia", CountryCollectable.builder()
            .name("Liberia")
            .emoji("flag_lr")
            .question("What is the capital of Liberia?")
            .answer("monrovia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LIBYA = register("libya", CountryCollectable.builder()
            .name("Libya")
            .emoji("flag_ly")
            .question("What is the capital of Libya?")
            .answer("tripoli")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LIECHTENSTEIN = register("liechtenstein", CountryCollectable.builder()
            .name("Liechtenstein")
            .emoji("flag_li")
            .question("What is the capital of Liechtenstein?")
            .answer("vaduz")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LITHUANIA = register("lithuania", CountryCollectable.builder()
            .name("Lithuania")
            .emoji("flag_lt")
            .question("What is the capital of Lithuania?")
            .answer("vilnius")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable LUXEMBOURG = register("luxembourg", CountryCollectable.builder()
            .name("Luxembourg")
            .emoji("flag_lu")
            .question("What is the capital of Luxembourg?")
            .answer("luxembourg")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MADAGASCAR = register("madagascar", CountryCollectable.builder()
            .name("Madagascar")
            .emoji("flag_mg")
            .question("What is the capital of Madagascar?")
            .answer("antananarivo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MALAWI = register("malawi", CountryCollectable.builder()
            .name("Malawi")
            .emoji("flag_mw")
            .question("What is the capital of Malawi?")
            .answer("lilongwe")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MALAYSIA = register("malaysia", CountryCollectable.builder()
            .name("Malaysia")
            .emoji("flag_my")
            .question("What is the capital of Malaysia?")
            .answer("kuala lumpur")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MALDIVES = register("maldives", CountryCollectable.builder()
            .name("the Maldives")
            .emoji("flag_mv")
            .question("What is the capital of the Maldives?")
            .answer("male")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MALI = register("mali", CountryCollectable.builder()
            .name("Mali")
            .emoji("flag_ml")
            .question("What is the capital of Mali?")
            .answer("bamako")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MALTA = register("malta", CountryCollectable.builder()
            .name("Malta")
            .emoji("flag_mt")
            .question("What is the capital of Malta?")
            .answer("valletta")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MARSHALL_ISLANDS = register("marshall_islands", CountryCollectable.builder()
            .name("the Marshall Islands")
            .emoji("flag_mh")
            .question("What is the capital of the Marshall Islands?")
            .answer("majuro")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MAURITANIA = register("mauritania", CountryCollectable.builder()
            .name("Mauritania")
            .emoji("flag_mr")
            .question("What is the capital of Mauritania?")
            .answer("nouakchott")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MAURITIUS = register("mauritius", CountryCollectable.builder()
            .name("Mauritius")
            .emoji("flag_mu")
            .question("What is the capital of Mauritius?")
            .answer("port louis")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MEXICO = register("mexico", CountryCollectable.builder()
            .name("Mexico")
            .emoji("flag_mx")
            .question("What is the capital of Mexico?")
            .answer("mexico city")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MICRONESIA = register("micronesia", CountryCollectable.builder()
            .name("the Federated States of Micronesia")
            .emoji("flag_fm")
            .question("What is the capital of the Federated States of Micronesia?")
            .answer("palikir")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MOLDOVA = register("moldova", CountryCollectable.builder()
            .name("Moldova")
            .emoji("flag_md")
            .question("What is the capital of Moldova?")
            .answer("chisinau")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MONACO = register("monaco", CountryCollectable.builder()
            .name("Monaco")
            .emoji("flag_mc")
            .question("What is the capital of Monaco?")
            .answer("monaco")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MONGOLIA = register("mongolia", CountryCollectable.builder()
            .name("Mongolia")
            .emoji("flag_mn")
            .question("What is the capital of Mongolia?")
            .answer("ulaanbaatar")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MONTENEGRO = register("montenegro", CountryCollectable.builder()
            .name("Montenegro")
            .emoji("flag_me")
            .question("What is the capital of Montenegro?")
            .answer("podgorica")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MOROCCO = register("morocco", CountryCollectable.builder()
            .name("Morocco")
            .emoji("flag_ma")
            .question("What is the capital of Morocco?")
            .answer("rabat")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MOZAMBIQUE = register("mozambique", CountryCollectable.builder()
            .name("Mozambique")
            .emoji("flag_mz")
            .question("What is the capital of Mozambique?")
            .answer("maputo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable MYANMAR = register("myanmar", CountryCollectable.builder()
            .name("Myanmar")
            .emoji("flag_mm")
            .question("What is the capital of Myanmar?")
            .answer("naypyidaw")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NAMIBIA = register("namibia", CountryCollectable.builder()
            .name("Namibia")
            .emoji("flag_na")
            .question("What is the capital of Namibia?")
            .answer("windhoek")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NAURU = register("nauru", CountryCollectable.builder()
            .name("Nauru")
            .emoji("flag_nr")
            .question("What is the capital of Nauru?")
            .answer("yaren")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NEPAL = register("nepal", CountryCollectable.builder()
            .name("Nepal")
            .emoji("flag_np")
            .question("What is the capital of Nepal?")
            .answer("kathmandu")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NETHERLANDS = register("netherlands", CountryCollectable.builder()
            .name("the Netherlands")
            .emoji("flag_nl")
            .question("What is the capital of the Netherlands?")
            .answer("amsterdam")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NEW_ZEALAND = register("new_zealand", CountryCollectable.builder()
            .name("New Zealand")
            .emoji("flag_nz")
            .question("What is the capital of New Zealand?")
            .answer("wellington")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NICARAGUA = register("nicaragua", CountryCollectable.builder()
            .name("Nicaragua")
            .emoji("flag_ni")
            .question("What is the capital of Nicaragua?")
            .answer("managua")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NIGER = register("niger", CountryCollectable.builder()
            .name("Niger")
            .emoji("flag_ne")
            .question("What is the capital of Niger?")
            .answer("niamey")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NIGERIA = register("nigeria", CountryCollectable.builder()
            .name("Nigeria")
            .emoji("flag_ng")
            .question("What is the capital of Nigeria?")
            .answer("abuja")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NORTH_KOREA = register("north_korea", CountryCollectable.builder()
            .name("North Korea")
            .emoji("flag_kp")
            .question("What is the capital of North Korea?")
            .answer("pyongyang")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NORTH_MACEDONIA = register("north_macedonia", CountryCollectable.builder()
            .name("North Macedonia")
            .emoji("flag_mk")
            .question("What is the capital of North Macedonia?")
            .answer("skopje")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable NORWAY = register("norway", CountryCollectable.builder()
            .name("Norway")
            .emoji("flag_no")
            .question("What is the capital of Norway?")
            .answer("oslo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable OMAN = register("oman", CountryCollectable.builder()
            .name("Oman")
            .emoji("flag_om")
            .question("What is the capital of Oman?")
            .answer("muscat")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PAKISTAN = register("pakistan", CountryCollectable.builder()
            .name("Pakistan")
            .emoji("flag_pk")
            .question("What is the capital of Pakistan?")
            .answer("islamabad")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PALAU = register("palau", CountryCollectable.builder()
            .name("Palau")
            .emoji("flag_pw")
            .question("What is the capital of Palau?")
            .answer("ngerulmud")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PALESTINE = register("palestine", CountryCollectable.builder()
            .name("Palestine")
            .emoji("flag_ps")
            .question("What is the capital of Palestine?")
            .answer("ramallah")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PANAMA = register("panama", CountryCollectable.builder()
            .name("Panama")
            .emoji("flag_pa")
            .question("What is the capital of Panama?")
            .answer("panama city")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PAPUA_NEW_GUINEA = register("papua_new_guinea", CountryCollectable.builder()
            .name("Papua New Guinea")
            .emoji("flag_pg")
            .question("What is the capital of Papua New Guinea?")
            .answer("port moresby")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PARAGUAY = register("paraguay", CountryCollectable.builder()
            .name("Paraguay")
            .emoji("flag_py")
            .question("What is the capital of Paraguay?")
            .answer("asuncion")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PERU = register("peru", CountryCollectable.builder()
            .name("Peru")
            .emoji("flag_pe")
            .question("What is the capital of Peru?")
            .answer("lima")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PHILIPPINES = register("philippines", CountryCollectable.builder()
            .name("the Philippines")
            .emoji("flag_ph")
            .question("What is the capital of the Philippines?")
            .answer("manila")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable POLAND = register("poland", CountryCollectable.builder()
            .name("Poland")
            .emoji("flag_pl")
            .question("What is the capital of Poland?")
            .answer("warsaw")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable PORTUGAL = register("portugal", CountryCollectable.builder()
            .name("Portugal")
            .emoji("flag_pt")
            .question("What is the capital of Portugal?")
            .answer("lisbon")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable QATAR = register("qatar", CountryCollectable.builder()
            .name("Qatar")
            .emoji("flag_qa")
            .question("What is the capital of Qatar?")
            .answer("doha")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ROMANIA = register("romania", CountryCollectable.builder()
            .name("Romania")
            .emoji("flag_ro")
            .question("What is the capital of Romania?")
            .answer("bucharest")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable RUSSIA = register("russia", CountryCollectable.builder()
            .name("Russia")
            .emoji("flag_ru")
            .question("What is the capital of Russia?")
            .answer("moscow")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable RWANDA = register("rwanda", CountryCollectable.builder()
            .name("Rwanda")
            .emoji("flag_rw")
            .question("What is the capital of Rwanda?")
            .answer("kigali")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAINT_KITTS_AND_NEVIS = register("saint_kitts_and_nevis", CountryCollectable.builder()
            .name("Saint Kitts and Nevis")
            .emoji("flag_kn")
            .question("What is the capital of Saint Kitts and Nevis?")
            .answer("basseterre")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAINT_LUCIA = register("saint_lucia", CountryCollectable.builder()
            .name("Saint Lucia")
            .emoji("flag_lc")
            .question("What is the capital of Saint Lucia?")
            .answer("castries")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAINT_VINCENT_AND_THE_GRENADINES = register("saint_vincent_and_the_grenadines", CountryCollectable.builder()
            .name("Saint Vincent and the Grenadines")
            .emoji("flag_vc")
            .question("What is the capital of Saint Vincent and the Grenadines?")
            .answer("kingstown")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAMOA = register("samoa", CountryCollectable.builder()
            .name("Samoa")
            .emoji("flag_ws")
            .question("What is the capital of Samoa?")
            .answer("apia")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAN_MARINO = register("san_marino", CountryCollectable.builder()
            .name("San Marino")
            .emoji("flag_sm")
            .question("What is the capital of San Marino?")
            .answer("san marino")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAO_TOME_AND_PRINCIPE = register("sao_tome_and_principe", CountryCollectable.builder()
            .name("S�o Tom� and Pr�ncipe")
            .emoji("flag_st")
            .question("What is the capital of S�o Tom� and Pr�ncipe?")
            .answer("sao tome")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SAUDI_ARABIA = register("saudi_arabia", CountryCollectable.builder()
            .name("Saudi Arabia")
            .emoji("flag_sa")
            .question("What is the capital of Saudi Arabia?")
            .answer("riyadh")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SENEGAL = register("senegal", CountryCollectable.builder()
            .name("Senegal")
            .emoji("flag_sn")
            .question("What is the capital of Senegal?")
            .answer("dakar")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SERBIA = register("serbia", CountryCollectable.builder()
            .name("Serbia")
            .emoji("flag_rs")
            .question("What is the capital of Serbia?")
            .answer("belgrade")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SEYCHELLES = register("seychelles", CountryCollectable.builder()
            .name("the Seychelles")
            .emoji("flag_sc")
            .question("What is the capital of the Seychelles?")
            .answer("victoria")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SIERRA_LEONE = register("sierra_leone", CountryCollectable.builder()
            .name("Sierra Leone")
            .emoji("flag_sl")
            .question("What is the capital of Sierra Leone?")
            .answer("freetown")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SINGAPORE = register("singapore", CountryCollectable.builder()
            .name("Singapore")
            .emoji("flag_sg")
            .question("What is the capital of Singapore?")
            .answer("singapore")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SLOVAKIA = register("slovakia", CountryCollectable.builder()
            .name("Slovakia")
            .emoji("flag_sk")
            .question("What is the capital of Slovakia?")
            .answer("bratislava")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SLOVENIA = register("slovenia", CountryCollectable.builder()
            .name("Slovenia")
            .emoji("flag_si")
            .question("What is the capital of Slovenia?")
            .answer("ljubljana")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SOLOMON_ISLANDS = register("solomon_islands", CountryCollectable.builder()
            .name("the Solomon Islands")
            .emoji("flag_sb")
            .question("What is the capital of the Solomon Islands?")
            .answer("honiara")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SOMALIA = register("somalia", CountryCollectable.builder()
            .name("Somalia")
            .emoji("flag_so")
            .question("What is the capital of Somalia?")
            .answer("mogadishu")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SOUTH_AFRICA = register("south_africa", CountryCollectable.builder()
            .name("South Africa")
            .emoji("flag_za")
            .question("What is the capital of South Africa?")
            .answer("pretoria")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SOUTH_KOREA = register("south_korea", CountryCollectable.builder()
            .name("South Korea")
            .emoji("flag_kr")
            .question("What is the capital of South Korea?")
            .answer("seoul")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SOUTH_SUDAN = register("south_sudan", CountryCollectable.builder()
            .name("South Sudan")
            .emoji("flag_ss")
            .question("What is the capital of South Sudan?")
            .answer("juba")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SPAIN = register("spain", CountryCollectable.builder()
            .name("Spain")
            .emoji("flag_es")
            .question("What is the capital of Spain?")
            .answer("madrid")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SRI_LANKA = register("sri_lanka", CountryCollectable.builder()
            .name("Sri Lanka")
            .emoji("flag_lk")
            .question("What is the capital of Sri Lanka?")
            .answer("sri jayawardenepura kotte")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SUDAN = register("sudan", CountryCollectable.builder()
            .name("Sudan")
            .emoji("flag_sd")
            .question("What is the capital of Sudan?")
            .answer("khartoum")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SURINAME = register("suriname", CountryCollectable.builder()
            .name("Suriname")
            .emoji("flag_sr")
            .question("What is the capital of Suriname?")
            .answer("paramaribo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SWEDEN = register("sweden", CountryCollectable.builder()
            .name("Sweden")
            .emoji("flag_se")
            .question("What is the capital of Sweden?")
            .answer("stockholm")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SWITZERLAND = register("switzerland", CountryCollectable.builder()
            .name("Switzerland")
            .emoji("flag_ch")
            .question("What is the capital of Switzerland?")
            .answer("bern")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable SYRIA = register("syria", CountryCollectable.builder()
            .name("Syria")
            .emoji("flag_sy")
            .question("What is the capital of Syria?")
            .answer("damascus")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TAIWAN = register("taiwan", CountryCollectable.builder()
            .name("Taiwan")
            .emoji("flag_tw")
            .question("What is the capital of Taiwan?")
            .answer("taipei")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TAJIKISTAN = register("tajikistan", CountryCollectable.builder()
            .name("Tajikistan")
            .emoji("flag_tj")
            .question("What is the capital of Tajikistan?")
            .answer("dushanbe")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TANZANIA = register("tanzania", CountryCollectable.builder()
            .name("Tanzania")
            .emoji("flag_tz")
            .question("What is the capital of Tanzania?")
            .answer("dodoma")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable THAILAND = register("thailand", CountryCollectable.builder()
            .name("Thailand")
            .emoji("flag_th")
            .question("What is the capital of Thailand?")
            .answer("bangkok")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TIMOR_LESTE = register("timor_leste", CountryCollectable.builder()
            .name("Timor-Leste")
            .emoji("flag_tl")
            .question("What is the capital of Timor-Leste?")
            .answer("dili")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TOGO = register("togo", CountryCollectable.builder()
            .name("Togo")
            .emoji("flag_tg")
            .question("What is the capital of Togo?")
            .answer("lome")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TONGA = register("tonga", CountryCollectable.builder()
            .name("Tonga")
            .emoji("flag_to")
            .question("What is the capital of Tonga?")
            .answer("nuku'alofa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TRINIDAD_AND_TOBAGO = register("trinidad_and_tobago", CountryCollectable.builder()
            .name("Trinidad and Tobago")
            .emoji("flag_tt")
            .question("What is the capital of Trinidad and Tobago?")
            .answer("port of spain")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TUNISIA = register("tunisia", CountryCollectable.builder()
            .name("Tunisia")
            .emoji("flag_tn")
            .question("What is the capital of Tunisia?")
            .answer("tunis")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TURKEY = register("turkey", CountryCollectable.builder()
            .name("Turkey")
            .emoji("flag_tr")
            .question("What is the capital of Turkey?")
            .answer("ankara")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TURKMENISTAN = register("turkmenistan", CountryCollectable.builder()
            .name("Turkmenistan")
            .emoji("flag_tm")
            .question("What is the capital of Turkmenistan?")
            .answer("ashgabat")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable TUVALU = register("tuvalu", CountryCollectable.builder()
            .name("Tuvalu")
            .emoji("flag_tv")
            .question("What is the capital of Tuvalu?")
            .answer("funafuti")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UGANDA = register("uganda", CountryCollectable.builder()
            .name("Uganda")
            .emoji("flag_ug")
            .question("What is the capital of Uganda?")
            .answer("kampala")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UKRAINE = register("ukraine", CountryCollectable.builder()
            .name("Ukraine")
            .emoji("flag_ua")
            .question("What is the capital of Ukraine?")
            .answer("kyiv")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UNITED_ARAB_EMIRATES = register("united_arab_emirates", CountryCollectable.builder()
            .name("the United Arab Emirates")
            .emoji("flag_ae")
            .question("What is the capital of the United Arab Emirates?")
            .answer("abu dhabi")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UNITED_KINGDOM = register("united_kingdom", CountryCollectable.builder()
            .name("the United Kingdom")
            .emoji("flag_gb")
            .question("What is the capital of the United Kingdom?")
            .answer("london")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UNITED_STATES = register("united_states", CountryCollectable.builder()
            .name("the United States")
            .emoji("flag_us")
            .question("What is the capital of the United States?")
            .answer("washington")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable URUGUAY = register("uruguay", CountryCollectable.builder()
            .name("Uruguay")
            .emoji("flag_uy")
            .question("What is the capital of Uruguay?")
            .answer("montevideo")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable UZBEKISTAN = register("uzbekistan", CountryCollectable.builder()
            .name("Uzbekistan")
            .emoji("flag_uz")
            .question("What is the capital of Uzbekistan?")
            .answer("tashkent")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable VANUATU = register("vanuatu", CountryCollectable.builder()
            .name("Vanuatu")
            .emoji("flag_vu")
            .question("What is the capital of Vanuatu?")
            .answer("port vila")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable VATICAN_CITY = register("vatican_city", CountryCollectable.builder()
            .name("Vatican City")
            .emoji("flag_va")
            .question("What is the capital of Vatican City?")
            .answer("vatican city")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable VENEZUELA = register("venezuela", CountryCollectable.builder()
            .name("Venezuela")
            .emoji("flag_ve")
            .question("What is the capital of Venezuela?")
            .answer("caracas")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable VIETNAM = register("vietnam", CountryCollectable.builder()
            .name("Vietnam")
            .emoji("flag_vn")
            .question("What is the capital of Vietnam?")
            .answer("hanoi")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable WESTERN_SAHARA = register("western_sahara", CountryCollectable.builder()
            .name("Western Sahara")
            .emoji("flag_eh")
            .question("What is the capital of Western Sahara?")
            .answer("el aaiun")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable YEMEN = register("yemen", CountryCollectable.builder()
            .name("Yemen")
            .emoji("flag_ye")
            .question("What is the capital of Yemen?")
            .answer("sanaa")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ZAMBIA = register("zambia", CountryCollectable.builder()
            .name("Zambia")
            .emoji("flag_zm")
            .question("What is the capital of Zambia?")
            .answer("lusaka")
            .rarity(CollectableRarity.COMMON)
    );

    public static final CountryCollectable ZIMBABWE = register("zimbabwe", CountryCollectable.builder()
            .name("Zimbabwe")
            .emoji("flag_zw")
            .question("What is the capital of Zimbabwe?")
            .answer("harare")
            .rarity(CollectableRarity.COMMON)
    );

    public static void load() {
        Constants.LOGGER.info("Loaded Country Collectables: {}", COUNTRY_REGISTRY.size());
    }
}
