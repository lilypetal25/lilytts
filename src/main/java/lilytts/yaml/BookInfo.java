package lilytts.yaml;

public class BookInfo {
    private String author;
    private String title;
    private String publishedYear;
    private String coverImage;
    
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String authorName) {
        this.author = authorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String bookTitle) {
        this.title = bookTitle;
    }

    public String getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(String bookYear) {
        this.publishedYear = bookYear;
    }

    public String getCoverImage() {
        return coverImage;
    }
    
    public void setCoverImage(String coverImageFile) {
        this.coverImage = coverImageFile;
    }
}
