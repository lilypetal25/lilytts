package parsing;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import content.ContentItem;

public interface ContentParser {
    List<ContentItem> readContent(Reader input) throws IOException;
}
