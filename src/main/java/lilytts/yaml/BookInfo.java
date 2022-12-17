package lilytts.yaml;

import java.io.File;

public class BookInfo {
    private String authorName;
    private String bookTitle;
    private String bookYear;
    private File coverImageFile;
    
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

    public String getBookYear() {
        return bookYear;
    }

    public void setBookYear(String bookYear) {
        this.bookYear = bookYear;
    }

    public File getCoverImageFile() {
        return coverImageFile;
    }
    
    public void setCoverImageFile(File coverImageFile) {
        this.coverImageFile = coverImageFile;
    }
}
