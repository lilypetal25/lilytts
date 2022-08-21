package lilytts.content;

import java.util.List;

public class Article {
    private String title;
    private String author;
    private String publication;
    private final List<List<ContentItem>> parts;

    public Article(List<List<ContentItem>> parts) {
        this.parts = parts;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublication() {
        return this.publication;
    }

    public void setPublication(String publication) {
        this.publication = publication;
    }

    public List<List<ContentItem>> getParts() {
        return this.parts;
    }
}
