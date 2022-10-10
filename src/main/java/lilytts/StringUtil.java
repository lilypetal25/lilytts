package lilytts;

public class StringUtil {
    private StringUtil() {
    }

    public static boolean nullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static String substringBefore(String value, String substring) {
        int index = value.indexOf(substring);
        return index > 0 ? value.substring(0, index) : substring;
    }
}