package lilytts.yaml;

public class BookConfig {
    private AzureSynthesisConfig audio = new AzureSynthesisConfig();
    private BookInfo metadata = new BookInfo();
    private BookSourceFilesConfig files = new BookSourceFilesConfig();

    public AzureSynthesisConfig getAudio() {
        return audio;
    }

    public void setAudio(AzureSynthesisConfig synthesis) {
        this.audio = synthesis;
    }

    public BookInfo getMetadata() {
        return metadata;
    }

    public void setMetadata(BookInfo bookInfo) {
        this.metadata = bookInfo;
    }

    public BookSourceFilesConfig getFiles() {
        return files;
    }

    public void setFiles(BookSourceFilesConfig files) {
        this.files = files;
    }
}
