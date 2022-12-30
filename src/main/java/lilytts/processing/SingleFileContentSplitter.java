package lilytts.processing;

import java.util.List;
import java.util.stream.StreamSupport;

import lilytts.content.ContentItem;

public class SingleFileContentSplitter implements ContentSplitter {

    public static ContentSplitter create() {
        return new SingleFileContentSplitter();
    }

    private SingleFileContentSplitter() {
    }

    @Override
    public List<List<ContentItem>> splitContent(Iterable<ContentItem> items) {
        return List.of(StreamSupport.stream(items.spliterator(), false).toList());
    }
    
}
