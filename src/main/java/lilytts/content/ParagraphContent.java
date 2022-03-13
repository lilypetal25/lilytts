package lilytts.content;

public class ParagraphContent extends ContentItem {
    private final String content;

    public ParagraphContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }
}
