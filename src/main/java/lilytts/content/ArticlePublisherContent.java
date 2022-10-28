package lilytts.content;

public class ArticlePublisherContent extends ContentItem {
    private String content;
    private String publisher;
    
    public ArticlePublisherContent(String content, String publisher) {
        this.content = content;
        this.publisher = publisher;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getPublisher() {
        return publisher;
    }
}
