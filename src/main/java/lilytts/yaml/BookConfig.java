package lilytts.yaml;

import java.util.List;

public class BookConfig {
    private List<AzureSpeechConnection> azureConnections;
    private AzureSynthesisConfig synthesis = new AzureSynthesisConfig();
    private BookInfo bookInfo = new BookInfo();
    private BookSourceFilesConfig files = new BookSourceFilesConfig();

    public List<AzureSpeechConnection> getAzureConnections() {
        return azureConnections;
    }

    public void setAzureConnections(List<AzureSpeechConnection> azureConnections) {
        this.azureConnections = azureConnections;
    }

    public AzureSynthesisConfig getSynthesis() {
        return synthesis;
    }

    public void setSynthesis(AzureSynthesisConfig synthesis) {
        this.synthesis = synthesis;
    }

    public BookInfo getBookInfo() {
        return bookInfo;
    }

    public void setBookInfo(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }

    public BookSourceFilesConfig getFiles() {
        return files;
    }

    public void setFiles(BookSourceFilesConfig files) {
        this.files = files;
    }
}
