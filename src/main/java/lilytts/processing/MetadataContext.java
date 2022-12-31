package lilytts.processing;

import java.io.File;
import java.util.List;

import lilytts.content.ContentItem;

public class MetadataContext {
    private File sourceFile;
    private List<ContentItem> content;
    private int fileIndex;

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public List<ContentItem> getContent() {
        return content;
    }

    public void setContent(List<ContentItem> content) {
        this.content = content;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int partIndex) {
        this.fileIndex = partIndex;
    }
}