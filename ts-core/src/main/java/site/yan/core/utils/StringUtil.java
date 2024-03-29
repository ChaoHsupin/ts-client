package site.yan.core.utils;

public class StringUtil {
    public StringUtil() {
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static String trimNewlineSymbolAndRemoveExtraSpace(String input) {
        return input != null && !input.isEmpty() ? input.replaceAll("\r|\n|\t", " ").replaceAll("( )+", " ") : input;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }
}