package lilytts.processing;

import java.io.File;
import java.util.List;

import lilytts.content.ContentItem;

public class MetadataContext {
    private File sourceFile;
    private List<ContentItem> content;
    private int partsInFile;
    private int partIndex;
    private int totalProcessedParts;

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

    public int getPartsInFile() {
        return partsInFile;
    }

    public void setPartsInFile(int partsInFile) {
        this.partsInFile = partsInFile;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }

    public int getTotalProcessedParts() {
        return totalProcessedParts;
    }

    public void setTotalProcessedParts(int totalProcessedParts) {
        this.totalProcessedParts = totalProcessedParts;
    }
}