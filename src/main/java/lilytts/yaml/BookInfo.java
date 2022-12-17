package lilytts.yaml;

public class BookInfo {
    private String authorName;
    private String bookTitle;
    private String publishedYear;
    private String coverImage;
    
    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
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
