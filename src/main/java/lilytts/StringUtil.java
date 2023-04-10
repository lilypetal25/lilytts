package lilytts;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private StringUtil() {
    }

    public static boolean nullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static String substringBefore(String value, String substring) {
        int index = value.indexOf(substring);
        return index >= 0 ? value.substring(0, index) : value;
    }

    public static String removeFileExtension(String fileName) {
        final int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }

    public static String prefixRepeated(String input, Character character, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Invalid character count: " + count);
        }

        if (count == 0) {
            return input;
        }

        StringBuilder builder = new StringBuilder(input.length() + count);

        for (int i = 0; i < count; i++) {
            builder.append(character);
        }

        builder.append(input);
        return builder.toString();
    }

    public static Comparator<CharSequence> alphaNumComparator() {
        return (first, second) -> {
            final String[] paddedStrings = StringUtil.padNumbers(first, second, '0');
            return paddedStrings[0].compareTo(paddedStrings[1]);
        };
    }

    public static String[] padNumbers(CharSequence first, CharSequence second, Character padCharacter) {
        final Matcher matcher1 = NUMBER_PATTERN.matcher(first);
        final Matcher matcher2 = NUMBER_PATTERN.matcher(second);

        final StringBuilder result1 = new StringBuilder(first.length());
        final StringBuilder result2 = new StringBuilder(second.length());

        while (matcher1.find() && matcher2.find()) {
            final String match1 = matcher1.group();
            final String match2 = matcher2.group();

            int match1PadCount = Math.max(0, match2.length() - match1.length());
            int match2PadCount = Math.max(0, match1.length() - match2.length());

            matcher1.appendReplacement(result1, prefixRepeated(match1, padCharacter, match1PadCount));
            matcher2.appendReplacement(result2, prefixRepeated(match2, padCharacter, match2PadCount));
        }

        matcher1.appendTail(result1);
        matcher2.appendTail(result2);

        return new String[] { result1.toString(), result2.toString() };
    }
}