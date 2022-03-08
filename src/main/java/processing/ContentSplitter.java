// TODO: All packages should be nested under lilytts.
package processing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import content.ChapterTitleContent;
import content.ContentItem;
import content.ParagraphContent;

public class ContentSplitter {
    public static class Builder {
        private int maxChunkCharacters = 1500;

        private Builder() {
        }

        public Builder withMaxChunkCharacters(int maxChunkCharacters) {
            this.maxChunkCharacters = maxChunkCharacters;
            return this;
        }

        public ContentSplitter build() {
            return new ContentSplitter(this.maxChunkCharacters);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final int maxChunkCharacters;

    private ContentSplitter(int maxChunkCharacters) {
        this.maxChunkCharacters = maxChunkCharacters;
    }

    public List<List<ContentItem>> splitContent(List<ContentItem> items) {
        final int totalLength = items.stream().collect(Collectors.summingInt(ContentSplitter::getLength));

        final int numberOfChunks = (int) Math.ceil(((double) totalLength) / maxChunkCharacters);
        final int averageChunkLength = totalLength / numberOfChunks;

        final List<List<ContentItem>> chunks = new ArrayList<>();
        List<ContentItem> currentChunk = new ArrayList<>();
        int currentChunkLength = 0;

        for (ContentItem item : items) {
            if (currentChunkLength + getLength(item) < averageChunkLength) {
                currentChunk.add(item);
                currentChunkLength += getLength(item);
                continue;
            }

            // Decide whether to end this chunk before the current item, or afterwards.
            final int roundDownDistance = averageChunkLength - currentChunkLength;
            final int roundUpDistance = (currentChunkLength + getLength(item)) - averageChunkLength;

            chunks.add(currentChunk);

            if (roundDownDistance < roundUpDistance) {
                currentChunk = new ArrayList<>();
                currentChunk.add(item);
                currentChunkLength = getLength(item);
            } else {
                currentChunk.add(item);
                currentChunk = new ArrayList<>();
                currentChunkLength = 0;
            }
        }

        // TODO: There are hypothetical scenarios where all the items in the last chunk have 0 length (they are all silence).
        if (currentChunk.size() > 0) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private static int getLength(ContentItem item) {
        // TODO: This might be better using the visitor pattern.
        if (item instanceof ParagraphContent) {
            return ((ParagraphContent) item).getContent().length();
        } else if (item instanceof ChapterTitleContent) {
            return ((ChapterTitleContent) item).getContent().length();
        }

        return 0;
    }
}
