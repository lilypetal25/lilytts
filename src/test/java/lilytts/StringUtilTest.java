package lilytts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {
    @Test
    public void testPadNumbers() {
        assertPadNumbersResult(
                "", "", '0',
                "", "");

        assertPadNumbersResult(
                "", "second", '0',
                "", "second");

        assertPadNumbersResult(
                "1", "2", '0',
                "1", "2");

        assertPadNumbersResult(
                "Chapter 1.txt", "Chapter 10.txt", '0',
                "Chapter 01.txt", "Chapter 10.txt");

        assertPadNumbersResult(
                "Chapter 1.txt", "Chapter 10.txt", ' ',
                "Chapter  1.txt", "Chapter 10.txt");

        assertPadNumbersResult(
                "Chapter 1.txt", "Chapter 1000.txt", '0',
                "Chapter 0001.txt", "Chapter 1000.txt");

        assertPadNumbersResult(
                "Chapter 24.txt", "Chapter 5.txt", '0',
                "Chapter 24.txt", "Chapter 05.txt");

        assertPadNumbersResult(
                "1.25.7", "8.3.80", '0',
                "1.25.07", "8.03.80");

        assertPadNumbersResult(
                "5.25", "abcd 80.3 abc 80 def 40", '0',
                "05.25", "abcd 80.03 abc 80 def 40");
    }

    private static void assertPadNumbersResult(String input1, String input2, Character padCharacter, String expected1, String expected2) {
        final String[] paddedNumbers = StringUtil.padNumbers(input1, input2, padCharacter);
        assertEquals(expected1, paddedNumbers[0]);
        assertEquals(expected2, paddedNumbers[1]);
    }
}
