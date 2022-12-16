package lilytts.synthesis;

import java.util.regex.Pattern;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.regex.Matcher;

public class AzureCostEstimator implements CostEstimator {
    private static final Pattern[] EXCLUDED_PATTERNS = new Pattern[] {
        Pattern.compile("<\\?xml [^>]+ \\?>", CASE_INSENSITIVE),
        Pattern.compile("<speak [^>]+>", CASE_INSENSITIVE),
        Pattern.compile("<\\/speak>", CASE_INSENSITIVE),
        Pattern.compile("<voice [^>]+>", CASE_INSENSITIVE),
        Pattern.compile("<\\/voice>", CASE_INSENSITIVE),
    };

    private static final double CHARACTER_PRICE_DOLLARS = 16.0 / 1000000.0;

    public double getEstimatedCost(String ssmlContent) {
        int characterCount = countCharacters(ssmlContent);

        for (Pattern pattern : EXCLUDED_PATTERNS) {
            final Matcher matcher = pattern.matcher(ssmlContent);

            while (matcher.find()) {
                characterCount -= countCharacters(matcher.group());
            }
        }

        return ((double)characterCount) * CHARACTER_PRICE_DOLLARS;
    }

    private static int countCharacters(String value) {
        // For Azure pricing, every Unicode code point is counted as 1 character.
        return value.codePointCount(0, value.length());
    }
}
