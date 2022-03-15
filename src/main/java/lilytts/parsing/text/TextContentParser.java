package lilytts.parsing.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lilytts.content.ChapterEndContent;
import lilytts.content.ChapterTitleContent;
import lilytts.content.ContentItem;
import lilytts.content.ParagraphContent;
import lilytts.content.SectionBreakContent;
import lilytts.parsing.ContentParser;

public class TextContentParser implements ContentParser {
    private static final Pattern SECTION_BREAK_PATTERN = Pattern.compile("-{3,}((?<title>[^-]+)-{3,})?");

    public static class Builder {
        private boolean recognizeChapter = true;
        private boolean recognizeSectionBreaks = true;
        private boolean appendChapterEnd = true;

        private Builder() {
        }

        public Builder setRecognizeChapter(boolean recognizeChapter) {
            this.recognizeChapter = recognizeChapter;
            return this;
        }

        public Builder setRecognizeSectionBreaks(boolean recognizeSectionBreaks) {
            this.recognizeSectionBreaks = recognizeSectionBreaks;
            return this;
        }

        public Builder setAppendChapterEnd(boolean appendChapterEnd) {
            this.appendChapterEnd = appendChapterEnd;
            return this;
        }

        public TextContentParser build() {
            return new TextContentParser(
                recognizeChapter,
                recognizeSectionBreaks,
                appendChapterEnd);
        }
    }

    private final boolean recognizeChapter;
    private final boolean recognizeSectionBreaks;
    private final boolean appendChapterEnd;

    private TextContentParser(
            final boolean recognizeChapter,
            final boolean recognizeSectionBreaks,
            final boolean appendChapterEnd) {
        this.recognizeChapter = recognizeChapter;
        this.recognizeSectionBreaks = recognizeSectionBreaks;
        this.appendChapterEnd = appendChapterEnd;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<ContentItem> readContent(Reader input) throws IOException {
        final BufferedReader reader = new BufferedReader(input);

        ArrayList<ContentItem> results = new ArrayList<>();

        String nextBlock = readNextBlock(reader);

        if (this.recognizeChapter && nextBlock != null) {
            results.add(new ChapterTitleContent(nextBlock));
            nextBlock = readNextBlock(reader);
        }

        while (nextBlock != null) {
            final Matcher sectionBreakMatcher = SECTION_BREAK_PATTERN.matcher(nextBlock);

            if (this.recognizeSectionBreaks && sectionBreakMatcher.matches()) {
                final String sectionTitle = toNonNullString(sectionBreakMatcher.group("title")).trim();
                results.add(new SectionBreakContent(sectionTitle));
            } else {
                results.add(new ParagraphContent(nextBlock));
            }
            
            nextBlock = readNextBlock(reader);
        }

        if (this.appendChapterEnd) {
            results.add(new ChapterEndContent());
        }

        return results;
    }

    private static String readNextBlock(BufferedReader reader) throws IOException {
        StringBuilder result = new StringBuilder();

        String nextLine = reader.readLine();

        // Skip any blank lines preceding the text block.
        while (nextLine != null && nextLine.isBlank()) {
            nextLine = reader.readLine();
        }

        while (nextLine != null && !nextLine.isBlank()) {
            // Separate lines by a space.
            if (result.length() > 0) {
                result.append(' ');
            }

            result.append(nextLine.trim());
            nextLine = reader.readLine();
        }

        return result.length() > 0 ? result.toString() : null;
    }

    private static String toNonNullString(String input) {
        return input != null ? input : "";
    }
}
