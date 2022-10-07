package lilytts;

public class StringUtil {
    private StringUtil() {
    }

    public static boolean nullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}