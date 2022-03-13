package lilytts.parsing;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import lilytts.content.ContentItem;

public interface ContentParser {
    List<ContentItem> readContent(Reader input) throws IOException;
}
