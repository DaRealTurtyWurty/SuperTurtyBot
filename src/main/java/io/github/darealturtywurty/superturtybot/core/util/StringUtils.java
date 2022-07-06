package io.github.darealturtywurty.superturtybot.core.util;

import org.apache.commons.text.WordUtils;

public final class StringUtils {
    private StringUtils() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }
    
    public static String replaceHTMLCodes(String str) {
        return str.replace("&amp;", "&").replace("&copy;", "©").replace("&trade;", "™").replace("&thinsp;", "\u2009")
            .replace("&ensp;", "\u2002").replace("&emsp;", "\u2003").replace("&hearts;", "♥").replace("&star;", "☆")
            .replace("&starf;", "★").replace("&bigstar;", "★").replace("&euro;", "€").replace("&mdash;", "—")
            .replace("&ndash;", "–");
    }
    
    public static String trueFalseToYesNo(final boolean value) {
        return value ? "Yes" : "No";
    }
    
    public static String upperSnakeToSpacedPascal(String str) {
        return WordUtils.capitalize(str.toLowerCase().replace("_", " "));
    }
}
