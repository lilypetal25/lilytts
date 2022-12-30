package lilytts.processing;

import java.util.List;
import lilytts.content.ContentItem;

public interface ContentSplitter {
    List<List<ContentItem>> splitContent(Iterable<ContentItem> items);
}