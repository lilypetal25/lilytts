package lilytts.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lilytts.content.ChapterTitleContent;
import lilytts.content.ContentItem;
import lilytts.content.ParagraphContent;

public class ContentSplitter {
    public static class Builder {
        private int maxPartCharacters = 5000;

        private Builder() {
        }

        public Builder withMaxPartCharacters(int maxPartCharacters) {
            this.maxPartCharacters = maxPartCharacters;
            return this;
        }

        public ContentSplitter build() {
            return new ContentSplitter(this.maxPartCharacters);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final int maxPartCharacters;

    private ContentSplitter(int maxPartCharacters) {
        this.maxPartCharacters = maxPartCharacters;
    }

    public List<List<ContentItem>> splitContent(Iterable<ContentItem> items) {
        final int totalLength = StreamSupport.stream(items.spliterator(), false)
            .collect(Collectors.summingInt(ContentSplitter::getSpokenCharacterCount));

        // Decide how many parts we should split the content into.
        final int numberOfParts = (int) Math.ceil(((double) totalLength) / maxPartCharacters);
        final int averagePartLength = totalLength / numberOfParts;

        final List<List<ContentItem>> parts = new ArrayList<>();
        List<ContentItem> previousPart = null;
        List<ContentItem> currentPart = new ArrayList<>();
        int currentPartLength = 0;

        for (ContentItem item : items) {
            // If this item is silence and we haven't started the next part yet, add it to the last part.
            // It's better to have silence at the end of tracks, and we don't want any tracks that are only silence.
            if (currentPart.size() == 0 && getSpokenCharacterCount(item) == 0 && previousPart != null) {
                previousPart.add(item);
                continue;
            }

            if (currentPartLength + getSpokenCharacterCount(item) < averagePartLength) {
                currentPart.add(item);
                currentPartLength += getSpokenCharacterCount(item);
                continue;
            }

            // Decide whether to end this part before the current item, or afterwards.
            final boolean exceedsMaxCharacters = currentPartLength + getSpokenCharacterCount(item) > maxPartCharacters;
            final int roundDownDistance = averagePartLength - currentPartLength;
            final int roundUpDistance = (currentPartLength + getSpokenCharacterCount(item)) - averagePartLength;

            parts.add(currentPart);
            previousPart = currentPart;

            if (exceedsMaxCharacters || roundDownDistance < roundUpDistance) {
                // Start the new part with item.
                currentPart = new ArrayList<>();
                currentPart.add(item);
                currentPartLength = getSpokenCharacterCount(item);
            } else {
                // Add item to this part and start a new, empty part.
                currentPart.add(item);
                currentPart = new ArrayList<>();
                currentPartLength = 0;
            }
        }

        if (currentPart.size() > 0) {
            parts.add(currentPart);
        }

        return parts;
    }

    private static int getSpokenCharacterCount(ContentItem item) {
        // TODO: This might be better using the visitor pattern.
        if (item instanceof ParagraphContent) {
            return ((ParagraphContent) item).getContent().length();
        } else if (item instanceof ChapterTitleContent) {
            return ((ChapterTitleContent) item).getContent().length();
        }

        return 0;
    }
}
