package parsing.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import content.ChapterTitleContent;
import content.ContentItem;
import content.ParagraphContent;
import content.SectionBreakContent;
import parsing.ContentParser;

public class TextContentParser implements ContentParser {
    private static final Pattern SECTION_BREAK_PATTERN = Pattern.compile("^-{3,}$");

    public static class Builder {
        private boolean recognizeChapter = true;
        private boolean recognizeSectionBreaks = true;

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

        public TextContentParser build() {
            return new TextContentParser(recognizeChapter, recognizeSectionBreaks);
        }
    }

    private final boolean recognizeChapter;
    private final boolean recognizeSectionBreaks;

    private TextContentParser(
            final boolean recognizeChapter,
            final boolean recognizeSectionBreaks) {
        this.recognizeChapter = recognizeChapter;
        this.recognizeSectionBreaks = recognizeSectionBreaks;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<ContentItem> readContent(Reader input) throws IOException {
        final BufferedReader reader = new BufferedReader(input);

        ArrayList<ContentItem> results = new ArrayList<>();

        String nextLine = readNextLineWithContent(reader);

        if (this.recognizeChapter && nextLine != null) {
            results.add(new ChapterTitleContent(nextLine));
            nextLine = readNextLineWithContent(reader);
        }

        while (nextLine != null) {
            if (this.recognizeSectionBreaks && isSectionBreak(nextLine)) {
                results.add(new SectionBreakContent());
            } else {
                results.add(new ParagraphContent(nextLine));
            }

            nextLine = readNextLineWithContent(reader);
        }

        return results;
    }

    private static String readNextLineWithContent(BufferedReader reader) throws IOException {
        String nextLine = reader.readLine();

        while (nextLine != null && nextLine.isBlank()) {
            nextLine = reader.readLine();
        }

        return nextLine;
    }

    private static boolean isSectionBreak(String line) {
        return SECTION_BREAK_PATTERN.matcher(line).find();
    }
}
